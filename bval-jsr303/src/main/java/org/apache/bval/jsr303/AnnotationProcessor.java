/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bval.jsr303;

import org.apache.bval.jsr303.util.ConstraintDefinitionValidator;
import org.apache.bval.model.Features;
import org.apache.bval.model.Meta;
import org.apache.bval.model.MetaBean;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintValidator;
import javax.validation.UnexpectedTypeException;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraintvalidation.SupportedValidationTarget;
import javax.validation.constraintvalidation.ValidationTarget;
import javax.validation.groups.ConvertGroup;
import javax.validation.groups.Default;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Description: implements uniform handling of JSR303 {@link Constraint}
 * annotations, including composed constraints and the resolution of
 * {@link ConstraintValidator} implementations.
 */
public final class AnnotationProcessor {
    /** {@link ApacheFactoryContext} used */
    private final ApacheFactoryContext factoryContext;

    /**
     * Create a new {@link AnnotationProcessor} instance.
     * 
     * @param factoryContext
     */
    public AnnotationProcessor(ApacheFactoryContext factoryContext) {
        this.factoryContext = factoryContext;
    }

    /**
     * Process JSR303 annotations.
     * 
     * @param prop
     *            potentially null
     * @param owner
     *            bean type
     * @param element
     *            whose annotations to read
     * @param access
     *            strategy for <code>prop</code>
     * @param appender
     *            handling accumulation
     * @return whether any processing took place
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public boolean processAnnotations(Meta prop, Class<?> owner, AnnotatedElement element,
        AccessStrategy access, AppendValidation appender) throws IllegalAccessException, InvocationTargetException {

        boolean changed = false;
        for (final Annotation annotation : element.getDeclaredAnnotations()) {
            final Class<?> type = annotation.annotationType();
            if (type.getName().startsWith("java.lang.annotation")) {
                continue;
            }
            changed |= processAnnotation(annotation, prop, owner, access, appender, true);
        }
        return changed;
    }

    /**
     * Process a single annotation.
     * 
     * @param <A>
     *            annotation type
     * @param annotation
     *            to process
     * @param prop
     *            potentially null
     * @param owner
     *            bean type
     * @param access
     *            strategy for <code>prop</code>
     * @param appender
     *            handling accumulation
     * @return whether any processing took place
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public <A extends Annotation> boolean processAnnotation(A annotation, Meta prop, Class<?> owner,
        AccessStrategy access, AppendValidation appender, boolean reflection) throws IllegalAccessException, InvocationTargetException {
        if (annotation instanceof Valid) {
            return addAccessStrategy(prop, access);
        }

        if (ConvertGroup.class.isInstance(annotation) || ConvertGroup.List.class.isInstance(annotation)) {
            if (!reflection) {
                Collection<Annotation> annotations = prop.getFeature(Jsr303Features.Property.ANNOTATIONS_TO_PROCESS);
                if (annotations == null) {
                    annotations = new ArrayList<Annotation>();
                    prop.putFeature(Jsr303Features.Property.ANNOTATIONS_TO_PROCESS, annotations);
                }
                annotations.add(annotation);
            }
            return true;
        }

        /**
         * An annotation is considered a constraint definition if its retention
         * policy contains RUNTIME and if the annotation itself is annotated
         * with javax.validation.Constraint.
         */
        Constraint vcAnno = annotation.annotationType().getAnnotation(Constraint.class);
        if (vcAnno != null) {
            ConstraintDefinitionValidator.validateConstraintDefinition(annotation);
            Class<? extends ConstraintValidator<A, ?>>[] validatorClasses;
            validatorClasses = findConstraintValidatorClasses(annotation, vcAnno);
            return applyConstraint(annotation, validatorClasses, prop, owner, access, appender);
        }
        /**
         * Multi-valued constraints: To support this requirement, the bean
         * validation provider treats regular annotations (annotations not
         * annotated by @Constraint) whose value element has a return type of an
         * array of constraint annotations in a special way.
         */
        final Object result = Reflection.INSTANCE.getAnnotationValue(annotation, ConstraintAnnotationAttributes.VALUE.getAttributeName());
        if (result instanceof Annotation[]) {
            boolean changed = false;
            for (final Annotation each : (Annotation[]) result) {
                changed |= processAnnotation(each, prop, owner, access, appender, reflection);
            }
            return changed;
        }
        return false;
    }

    /**
     * Add the specified {@link AccessStrategy} to <code>prop</code>; noop if
     * <code>prop == null</code>.
     * 
     * @param prop
     * @param access
     * @return whether anything took place.
     */
    public boolean addAccessStrategy(Meta prop, AccessStrategy access) {
        if (prop == null) {
            return false;
        }
        AccessStrategy[] strategies = prop.getFeature(Features.Property.REF_CASCADE);
        if (strategies == null) {
            strategies = new AccessStrategy[] { access };
            prop.putFeature(Features.Property.REF_CASCADE, strategies);
        } else if (!ArrayUtils.contains(strategies, access)) {
            AccessStrategy[] newStrategies = new AccessStrategy[strategies.length + 1];
            System.arraycopy(strategies, 0, newStrategies, 0, strategies.length);
            newStrategies[strategies.length] = access;
            prop.putFeature(Features.Property.REF_CASCADE, newStrategies);
        }
        return true;
    }

    /**
     * Find available {@link ConstraintValidation} classes for a given
     * constraint annotation.
     * 
     * @param annotation
     * @param vcAnno
     * @return {@link ConstraintValidation} implementation class array
     */
    @SuppressWarnings("unchecked")
    private <A extends Annotation> Class<? extends ConstraintValidator<A, ?>>[] findConstraintValidatorClasses(
        A annotation, Constraint vcAnno) {
        if (vcAnno == null) {
            vcAnno = annotation.annotationType().getAnnotation(Constraint.class);
        }
        Class<? extends ConstraintValidator<A, ?>>[] validatorClasses;
        Class<A> annotationType = (Class<A>) annotation.annotationType();
        validatorClasses = factoryContext.getFactory().getConstraintsCache().getConstraintValidators(annotationType);
        if (validatorClasses == null) {
            validatorClasses = (Class<? extends ConstraintValidator<A, ?>>[]) vcAnno.validatedBy();
            if (validatorClasses.length == 0) {
                validatorClasses =
                    factoryContext.getFactory().getDefaultConstraints().getValidatorClasses(annotationType);
            }
        }
        return validatorClasses;
    }

    /**
     * Apply a constraint to the specified <code>appender</code>.
     * 
     * @param annotation
     *            constraint annotation
     * @param rawConstraintClasses
     *            known {@link ConstraintValidator} implementation classes for
     *            <code>annotation</code>
     * @param prop
     *            meta-property
     * @param owner
     *            type
     * @param access
     *            strategy
     * @param appender
     * @return success flag
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private <A extends Annotation> boolean applyConstraint(A annotation,
        Class<? extends ConstraintValidator<A, ?>>[] rawConstraintClasses, Meta prop, Class<?> owner,
        AccessStrategy access, AppendValidation appender) throws IllegalAccessException, InvocationTargetException {

        final Class<? extends ConstraintValidator<A, ?>>[] constraintClasses = select(rawConstraintClasses, access);
        if (constraintClasses != null && constraintClasses.length == 0 && rawConstraintClasses.length > 0) {
            return false;
        }

        RuntimeException missingValidatorException = null;
        ConstraintValidator<A, ?> validator = null;
        try {
            validator = getConstraintValidator(annotation, constraintClasses, owner, access);
        } catch (final RuntimeException e) {
            missingValidatorException = e;
        }
        final AnnotationConstraintBuilder<A> builder =
            new AnnotationConstraintBuilder<A>(constraintClasses, validator, annotation, owner, access, null, missingValidatorException);

        // JSR-303 3.4.4: Add implicit groups
        if (prop != null && prop.getParentMetaBean() != null) {
            MetaBean parentMetaBean = prop.getParentMetaBean();
            // If:
            // - the owner is an interface
            // - the class of the metabean being build is different than the
            // owner
            // - and only the Default group is defined
            // Then: add the owner interface as implicit groups
            if (builder.getConstraintValidation().getOwner().isInterface()
                && parentMetaBean.getBeanClass() != builder.getConstraintValidation().getOwner()
                && builder.getConstraintValidation().getGroups().size() == 1
                && builder.getConstraintValidation().getGroups().contains(Default.class)) {
                Set<Class<?>> groups = builder.getConstraintValidation().getGroups();
                groups.add(builder.getConstraintValidation().getOwner());
                builder.getConstraintValidation().setGroups(groups);
            }
        }

        // If already building a constraint composition tree, ensure that:
        // - the parent groups are inherited
        // - the parent payload is inherited
        if (appender instanceof AppendValidationToBuilder) {
            AppendValidationToBuilder avb = (AppendValidationToBuilder) appender;
            builder.getConstraintValidation().setGroups(avb.getInheritedGroups());
            builder.getConstraintValidation().setPayload(avb.getInheritedPayload());
        }

        // process composed constraints:
        // here are not other superclasses possible, because annotations do not
        // inherit!
        processAnnotations(prop, owner, annotation.annotationType(), access, new AppendValidationToBuilder(builder));

        // Even if the validator is null, it must be added to mimic the RI impl
        appender.append(builder.getConstraintValidation());
        return true;
    }

    private static <A extends Annotation> Class<? extends ConstraintValidator<A, ?>>[] select(
            final Class<? extends ConstraintValidator<A, ?>>[] rawConstraintClasses, final AccessStrategy access) {
        final boolean isReturn = ReturnAccess.class.isInstance(access);
        final boolean isParam = ParametersAccess.class.isInstance(access);
        if (rawConstraintClasses != null && (isReturn || isParam)) {
            final Collection<Class<? extends ConstraintValidator<A, ?>>> selected = new ArrayList<Class<? extends ConstraintValidator<A, ?>>>();
            for (final Class<? extends ConstraintValidator<A, ?>> constraint : rawConstraintClasses) {
                final SupportedValidationTarget target = constraint.getAnnotation(SupportedValidationTarget.class);
                if (target == null && isReturn) {
                    selected.add(constraint);
                } else if (target != null) {
                    for (final ValidationTarget validationTarget : target.value()) {
                        if (isReturn && ValidationTarget.ANNOTATED_ELEMENT.equals(validationTarget)) {
                            selected.add(constraint);
                        } else if (isParam && ValidationTarget.PARAMETERS.equals(validationTarget)) {
                            selected.add(constraint);
                        }
                    }
            }
            }
            return selected.toArray(new Class[selected.size()]);
        }
        return rawConstraintClasses;
    }

    private <A extends Annotation, T> ConstraintValidator<A, ? super T> getConstraintValidator(A annotation,
        Class<? extends ConstraintValidator<A, ?>>[] constraintClasses, Class<?> owner, AccessStrategy access) {
        if (constraintClasses != null && constraintClasses.length > 0) {
            Type type = determineTargetedType(owner, access);

            /**
             * spec says in chapter 3.5.3.: The ConstraintValidator chosen to
             * validate a declared type T is the one where the type supported by
             * the ConstraintValidator is a supertype of T and where there is no
             * other ConstraintValidator whose supported type is a supertype of
             * T and not a supertype of the chosen ConstraintValidator supported
             * type.
             */
            final Map<Type, Collection<Class<? extends ConstraintValidator<A, ?>>>> validatorTypes = getValidatorsTypes(constraintClasses);
            reduceTarget(validatorTypes, access);

            final List<Type> assignableTypes = new ArrayList<Type>(constraintClasses.length);
            fillAssignableTypes(type, validatorTypes.keySet(), assignableTypes);
            reduceAssignableTypes(assignableTypes);
            checkOneType(assignableTypes, type, owner, annotation, access);

            if ((type == Object.class || type == Object[].class) && validatorTypes.containsKey(Object.class) && validatorTypes.containsKey(Object[].class)) {
                throw new ConstraintDefinitionException("Only a validator for Object or Object[] should be provided for cross parameter validators");
            }

            final Collection<Class<? extends ConstraintValidator<A, ?>>> key = validatorTypes.get(assignableTypes.get(0));
            if (key.size() > 1) {
                final String message = "Factory returned " + key.size() + " validators";
                if (ParametersAccess.class.isInstance(access)) { // cross parameter
                    throw new ConstraintDefinitionException(message);
                }
                throw new UnexpectedTypeException(message);
            }

            @SuppressWarnings("unchecked")
            final ConstraintValidator<A, ? super T> validator = (ConstraintValidator<A, ? super T>) factoryContext.getConstraintValidatorFactory().getInstance(key.iterator().next());
            if (validator == null) {
                throw new ValidationException("Factory returned null validator for: " + key);

            }
            return validator;
            // NOTE: validator initialization deferred until append phase
        }
        return null;
    }

    private <A extends Annotation> void reduceTarget(final Map<Type, Collection<Class<? extends ConstraintValidator<A, ?>>>> validator, final AccessStrategy access) {
        for (final Map.Entry<Type, Collection<Class<? extends ConstraintValidator<A, ?>>>> entry : validator.entrySet()) {
            final Collection<Class<? extends ConstraintValidator<A, ?>>> validators = entry.getValue();
            final Iterator<Class<? extends ConstraintValidator<A, ?>>> it = validators.iterator();
            while (it.hasNext()) {
                final Type v = it.next();
                if (!Class.class.isInstance(v)) {
                    continue; // TODO: handle this case
                }

                final Class<?> clazz = Class.class.cast(v);
                final SupportedValidationTarget target = clazz.getAnnotation(SupportedValidationTarget.class);
                if (target != null) {
                    final Collection<ValidationTarget> targets = Arrays.asList(target.value());
                    final boolean isParameter = ParameterAccess.class.isInstance(access) || ParametersAccess.class.isInstance(access);
                    if ((isParameter && !targets.contains(ValidationTarget.PARAMETERS))
                            || (!isParameter && !targets.contains(ValidationTarget.ANNOTATED_ELEMENT))) {
                        it.remove();
                    }
                }
            }
            if (validators.isEmpty()) {
                validator.remove(entry.getKey());
            }
        }
    }

    private static void checkOneType(List<Type> types, Type targetType, Class<?> owner, Annotation anno,
        AccessStrategy access) {

        if (types.isEmpty()) {
            final String message = "No validator could be found for type " + stringForType(targetType)
                    + ". See: @" + anno.annotationType().getSimpleName() + " at " + stringForLocation(owner, access);
            if (Object[].class.equals(targetType)) { // cross parameter
                throw new ConstraintDefinitionException(message);
            }
            throw new UnexpectedTypeException(message);
        } else if (types.size() > 1) {
            StringBuilder buf = new StringBuilder();
            buf.append("Ambiguous validators for type ");
            buf.append(stringForType(targetType));
            buf.append(". See: @").append(anno.annotationType().getSimpleName()).append(" at ").append(
                stringForLocation(owner, access));
            buf.append(". Validators are: ");
            boolean comma = false;
            for (Type each : types) {
                if (comma)
                    buf.append(", ");
                comma = true;
                buf.append(each);
            }
            throw new UnexpectedTypeException(buf.toString());
        }
    }

    /** implements spec chapter 3.5.3. ConstraintValidator resolution algorithm. */
    private static Type determineTargetedType(Class<?> owner, AccessStrategy access) {
        // if the constraint declaration is hosted on a class or an interface,
        // the targeted type is the class or the interface.
        if (access == null)
            return owner;
        Type type = access.getJavaType();
        if (type == null)
            return Object.class;
        if (type instanceof Class<?>)
            type = ClassUtils.primitiveToWrapper((Class<?>) type);
        return type;
    }

    private static String stringForType(Type clazz) {
        if (clazz instanceof Class<?>) {
            if (((Class<?>) clazz).isArray()) {
                return ((Class<?>) clazz).getComponentType().getName() + "[]";
            } else {
                return ((Class<?>) clazz).getName();
            }
        } else {
            return clazz.toString();
        }
    }

    private static String stringForLocation(Class<?> owner, AccessStrategy access) {
        if (access != null) {
            return access.toString();
        } else {
            return owner.getName();
        }
    }

    private static void fillAssignableTypes(Type type, Set<Type> validatorsTypes, List<Type> suitableTypes) {
        for (final Type validatorType : validatorsTypes) {
            if (org.apache.commons.lang3.reflect.TypeUtils.isAssignable(type, validatorType)
                && !suitableTypes.contains(validatorType)) {
                suitableTypes.add(validatorType);
            }
        }
    }

    /**
     * Tries to reduce all assignable classes down to a single class.
     * 
     * @param assignableTypes
     *            The set of all classes which are assignable to the class of
     *            the value to be validated and which are handled by at least
     *            one of the validators for the specified constraint.
     */
    private static void reduceAssignableTypes(List<Type> assignableTypes) {
        if (assignableTypes.size() <= 1) {
            return; // no need to reduce
        }
        boolean removed;
        do {
            removed = false;
            final Type type = assignableTypes.get(0);
            for (int i = 1; i < assignableTypes.size(); i++) {
                Type nextType = assignableTypes.get(i);
                if (TypeUtils.isAssignable(nextType, type)) {
                    assignableTypes.remove(0);
                    i--;
                    removed = true;
                } else if (TypeUtils.isAssignable(type, nextType)) {
                    assignableTypes.remove(i--);
                    removed = true;
                }
            }
        } while (removed && assignableTypes.size() > 1);

    }

    /**
     * Given a set of {@link ConstraintValidator} implementation classes, map
     * those to their target types.
     * 
     * @param constraintValidatorClasses
     * @return {@link Map} of {@link Type} : {@link ConstraintValidator} subtype
     */
    private static <A extends Annotation> Map<Type, Collection<Class<? extends ConstraintValidator<A, ?>>>> getValidatorsTypes(
        Class<? extends ConstraintValidator<A, ?>>[] constraintValidatorClasses) {
        final Map<Type, Collection<Class<? extends ConstraintValidator<A, ?>>>> validatorsTypes = new HashMap<Type, Collection<Class<? extends ConstraintValidator<A, ?>>>>();
        for (Class<? extends ConstraintValidator<A, ?>> validatorType : constraintValidatorClasses) {
            Type validatedType = TypeUtils.getTypeArguments(validatorType, ConstraintValidator.class).get(ConstraintValidator.class.getTypeParameters()[1]);
            if (validatedType == null) {
                throw new ValidationException(String.format("Could not detect validated type for %s", validatorType));
            }
            if (validatedType instanceof GenericArrayType) {
                Type componentType = TypeUtils.getArrayComponentType(validatedType);
                if (componentType instanceof Class<?>) {
                    validatedType = Array.newInstance((Class<?>) componentType, 0).getClass();
                }
            }
            if (!validatorsTypes.containsKey(validatedType)) {
                validatorsTypes.put(validatedType, new ArrayList<Class<? extends ConstraintValidator<A, ?>>>());
            }
            validatorsTypes.get(validatedType).add(validatorType);
        }
        return validatorsTypes;
    }

}
