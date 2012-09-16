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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.UnexpectedTypeException;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.groups.Default;

import org.apache.bval.jsr303.util.ConstraintDefinitionValidator;
import org.apache.bval.jsr303.util.SecureActions;
import org.apache.bval.model.Features;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.util.AccessStrategy;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

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
    public boolean processAnnotations(MetaProperty prop, Class<?> owner, AnnotatedElement element,
        AccessStrategy access, AppendValidation appender) throws IllegalAccessException, InvocationTargetException {

        boolean changed = false;
        for (Annotation annotation : element.getDeclaredAnnotations()) {
            changed |= processAnnotation(annotation, prop, owner, access, appender);
        }
        return changed;
    }

    /**
     * Convenience method to process a single class-level annotation.
     * 
     * @param <A>
     *            annotation type
     * @param annotation
     *            to process
     * @param owner
     *            bean type
     * @param appender
     *            handling accumulation
     * @return whether any processing took place
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public final <A extends Annotation> boolean processAnnotation(A annotation, Class<?> owner, AppendValidation appender)
        throws IllegalAccessException, InvocationTargetException {
        return processAnnotation(annotation, null, owner, null, appender);
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
    public <A extends Annotation> boolean processAnnotation(A annotation, MetaProperty prop, Class<?> owner,
        AccessStrategy access, AppendValidation appender) throws IllegalAccessException, InvocationTargetException {
        if (annotation instanceof Valid) {
            return addAccessStrategy(prop, access);
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
        Object result =
            SecureActions.getAnnotationValue(annotation, ConstraintAnnotationAttributes.VALUE.getAttributeName());
        if (result instanceof Annotation[]) {
            boolean changed = false;
            for (Annotation each : (Annotation[]) result) {
                changed |= processAnnotation(each, prop, owner, access, appender);
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
    public boolean addAccessStrategy(MetaProperty prop, AccessStrategy access) {
        if (prop == null) {
            return false;
        }
        AccessStrategy[] strategies = prop.getFeature(Features.Property.REF_CASCADE);
        if (strategies == null) {
            strategies = new AccessStrategy[] { access };
            prop.putFeature(Features.Property.REF_CASCADE, strategies);
        } else if (!ArrayUtils.contains(strategies, access)) {
            prop.putFeature(Features.Property.REF_CASCADE, ArrayUtils.add(strategies, access));
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
     * @param constraintClasses
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
        Class<? extends ConstraintValidator<A, ?>>[] constraintClasses, MetaProperty prop, Class<?> owner,
        AccessStrategy access, AppendValidation appender) throws IllegalAccessException, InvocationTargetException {

        final ConstraintValidator<A, ?> validator =
            getConstraintValidator(annotation, constraintClasses, owner, access);
        final AnnotationConstraintBuilder<A> builder =
            new AnnotationConstraintBuilder<A>(constraintClasses, validator, annotation, owner, access);

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
            Map<Type, Class<? extends ConstraintValidator<A, ?>>> validatorTypes =
                getValidatorsTypes(constraintClasses);
            final List<Type> assignableTypes = new ArrayList<Type>(constraintClasses.length);
            fillAssignableTypes(type, validatorTypes.keySet(), assignableTypes);
            reduceAssignableTypes(assignableTypes);
            checkOneType(assignableTypes, type, owner, annotation, access);

            @SuppressWarnings("unchecked")
            final ConstraintValidator<A, ? super T> validator =
                (ConstraintValidator<A, ? super T>) factoryContext.getConstraintValidatorFactory()
                    .getInstance(validatorTypes.get(assignableTypes.get(0)));
            if (validator == null) {
                throw new ValidationException("Factory returned null validator for: "
                    + validatorTypes.get(assignableTypes.get(0)));

            }
            return validator;
            // NOTE: validator initialization deferred until append phase
        }
        return null;
    }

    private static void checkOneType(List<Type> types, Type targetType, Class<?> owner, Annotation anno,
        AccessStrategy access) {

        if (types.isEmpty()) {
            StringBuilder buf =
                new StringBuilder().append("No validator could be found for type ").append(stringForType(targetType))
                    .append(". See: @").append(anno.annotationType().getSimpleName()).append(" at ").append(
                        stringForLocation(owner, access));
            throw new UnexpectedTypeException(buf.toString());
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
        for (Type validatorType : validatorsTypes) {
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
    private static <A extends Annotation> Map<Type, Class<? extends ConstraintValidator<A, ?>>> getValidatorsTypes(
        Class<? extends ConstraintValidator<A, ?>>[] constraintValidatorClasses) {
        Map<Type, Class<? extends ConstraintValidator<A, ?>>> validatorsTypes =
            new HashMap<Type, Class<? extends ConstraintValidator<A, ?>>>();
        for (Class<? extends ConstraintValidator<A, ?>> validatorType : constraintValidatorClasses) {
            Type validatedType =
                TypeUtils.getTypeArguments(validatorType, ConstraintValidator.class).get(
                    ConstraintValidator.class.getTypeParameters()[1]);
            if (validatedType == null) {
                throw new ValidationException(String.format("Could not detect validated type for %s", validatorType));
            }
            if (validatedType instanceof GenericArrayType) {
                Type componentType = TypeUtils.getArrayComponentType(validatedType);
                if (componentType instanceof Class<?>) {
                    validatedType = Array.newInstance((Class<?>) componentType, 0).getClass();
                }
            }
            validatorsTypes.put(validatedType, validatorType);
        }
        return validatorsTypes;
    }

}
