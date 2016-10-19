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
package org.apache.bval.jsr;

import org.apache.bval.jsr.util.NodeImpl;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.model.Validation;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;
import org.apache.bval.util.AccessStrategy;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintTarget;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.Payload;
import javax.validation.UnexpectedTypeException;
import javax.validation.ValidationException;
import javax.validation.constraintvalidation.SupportedValidationTarget;
import javax.validation.constraintvalidation.ValidationTarget;
import javax.validation.metadata.ConstraintDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Description: Adapter between Constraint (JSR303) and Validation (Core)<br/>
 * this instance is immutable!<br/>
 */
public class ConstraintValidation<T extends Annotation> implements Validation, ConstraintDescriptor<T> {
    private final AccessStrategy access;
    private final boolean reportFromComposite;
    private final Map<String, Object> attributes;
    private T annotation; // for metadata request API
    private volatile ConstraintValidator<T, ?> validator;

    private Set<ConstraintValidation<?>> composedConstraints;

    private boolean validated = false;

    /**
     * the owner is the type where the validation comes from. it is used to
     * support implicit grouping.
     */
    private final Class<?> owner;
    private Set<Class<?>> groups;
    private Set<Class<? extends Payload>> payload;
    private Class<? extends ConstraintValidator<T, ?>>[] validatorClasses;
    private ConstraintTarget validationAppliesTo = null;

    public ConstraintValidation(Class<? extends ConstraintValidator<T, ?>>[] validatorClasses,
                                T annotation, Class<?> owner, AccessStrategy access,
                                boolean reportFromComposite, ConstraintTarget target) {
        this.attributes = new HashMap<String, Object>();
        this.validatorClasses = ArrayUtils.clone(validatorClasses);
        this.annotation = annotation;
        this.owner = owner;
        this.access = access;
        this.reportFromComposite = reportFromComposite;
        this.validationAppliesTo = target;
    }

    /**
     * Return a {@link Serializable} {@link ConstraintDescriptor} capturing a
     * snapshot of current state.
     *
     * @return {@link ConstraintDescriptor}
     */
    public ConstraintDescriptor<T> asSerializableDescriptor() {
        return new ConstraintDescriptorImpl<T>(this);
    }

    void setGroups(final Set<Class<?>> groups) {
        this.groups = groups;
        ConstraintAnnotationAttributes.GROUPS.put(attributes, groups.toArray(new Class<?>[groups.size()]));
    }

    void setGroups(final Class<?>[] groups) {
        this.groups = new HashSet<Class<?>>();
        Collections.addAll(this.groups, groups);
        ConstraintAnnotationAttributes.GROUPS.put(attributes, groups);
    }

    void setPayload(Set<Class<? extends Payload>> payload) {
        this.payload = payload;
        ConstraintAnnotationAttributes.PAYLOAD.put(attributes, payload.toArray(new Class[payload.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReportAsSingleViolation() {
        return reportFromComposite;
    }

    /**
     * Add a composing constraint.
     *
     * @param aConstraintValidation to add
     */
    public void addComposed(ConstraintValidation<?> aConstraintValidation) {
        if (composedConstraints == null) {
            composedConstraints = new HashSet<ConstraintValidation<?>>();
        }
        composedConstraints.add(aConstraintValidation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <L extends ValidationListener> void validate(ValidationContext<L> context) {
        validateGroupContext((GroupValidationContext<?>) context);
    }

    /**
     * Validate a {@link GroupValidationContext}.
     *
     * @param context root
     */
    public void validateGroupContext(final GroupValidationContext<?> context) {
        if (validator == null) {
            synchronized (this) {
                if (validator == null) {
                    try {
                        validator = getConstraintValidator(
                                context.getConstraintValidatorFactory(), annotation, validatorClasses, owner, access);
                        if (validator != null) {
                            validator.initialize(annotation);
                        }
                    } catch (final RuntimeException re) {
                        if (ValidationException.class.isInstance(re)) {
                            throw re;
                        }
                        throw new ConstraintDefinitionException(re);
                    }
                }
            }
        }

        context.setConstraintValidation(this);
        /**
         * execute unless the given validation constraint has already been
         * processed during this validation routine (as part of a previous group
         * match)
         */
        if (!isMemberOf(context.getCurrentGroup().getGroup())) {
            return; // do not validate in the current group
        }
        if (context.getCurrentOwner() != null && !this.owner.equals(context.getCurrentOwner())) {
            return;
        }
        if (validator != null && !context.collectValidated(validator))
            return; // already done

        if (context.getMetaProperty() != null && !isReachable(context)) {
            return;
        }

        // process composed constraints
        if (isReportAsSingleViolation()) {
            final ConstraintValidationListener<?> listener = context.getListener();
            listener.beginReportAsSingle();

            boolean failed = listener.hasViolations();
            try {
                // stop validating when already failed and
                // ReportAsSingleInvalidConstraint = true ?
                for (Iterator<ConstraintValidation<?>> composed = getComposingValidations().iterator(); !failed
                    && composed.hasNext();) {
                    composed.next().validate(context);
                    failed = listener.hasViolations();
                }
            } finally {
                listener.endReportAsSingle();
                // Restore current constraint validation
                context.setConstraintValidation(this);
            }

            if (failed) {
                // TODO RSt - how should the composed constraint error report look like?
                addErrors(context, new ConstraintValidatorContextImpl(context, this)); // add defaultErrorMessage only
                return;
            }
        } else {
            for (ConstraintValidation<?> composed : getComposingValidations()) {
                composed.validate(context);
            }

            // Restore current constraint validation
            context.setConstraintValidation(this);
        }

        if (validator != null) {
            @SuppressWarnings("unchecked")
            final ConstraintValidator<T, Object> objectValidator = (ConstraintValidator<T, Object>) validator;
            final ConstraintValidatorContextImpl jsrContext = new ConstraintValidatorContextImpl(context, this);
            if (!objectValidator.isValid(context.getValidatedValue(), jsrContext)) {
                addErrors(context, jsrContext);
            }
        }
    }

    private <A extends Annotation> ConstraintValidator<A, ? super T> getConstraintValidator(
            ConstraintValidatorFactory factory, A annotation,
        Class<? extends ConstraintValidator<A, ?>>[] constraintClasses, Class<?> owner, AccessStrategy access) {
        if (ArrayUtils.isNotEmpty(constraintClasses)) {
            final Type type = determineTargetedType(owner, access);

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

            if ((type.equals(Object.class) || type.equals(Object[].class)) && validatorTypes.containsKey(Object.class)
                && validatorTypes.containsKey(Object[].class)) {
                throw new ConstraintDefinitionException(
                    "Only a validator for Object or Object[] should be provided for cross-parameter validators");
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
            final ConstraintValidator<A, ? super T> validator = (ConstraintValidator<A, ? super T>) factory.getInstance(key.iterator().next());
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
        }
        if (types.size() > 1) {
            throw new UnexpectedTypeException(String.format(
                "Ambiguous validators for type %s. See: @%s at %s. Validators are: %s", stringForType(targetType), anno
                    .annotationType().getSimpleName(), stringForLocation(owner, access), StringUtils.join(types, ", ")));
        }
    }

    private static String stringForType(Type clazz) {
        if (clazz instanceof Class<?>) {
            return ((Class<?>) clazz).isArray() ? ((Class<?>) clazz).getComponentType().getName() + "[]" : ((Class<?>) clazz).getName();
        }
        return clazz.toString();
    }

    private static String stringForLocation(Class<?> owner, AccessStrategy access) {
        return access == null ? owner.getName() : access.toString();
    }

    private static void fillAssignableTypes(Type type, Set<Type> validatorsTypes, List<Type> suitableTypes) {
        for (final Type validatorType : validatorsTypes) {
            if (TypeUtils.isAssignable(type, validatorType) && !suitableTypes.contains(validatorType)) {
                suitableTypes.add(validatorType);
            }
        }
    }

    /**
     * Tries to reduce all assignable classes down to a single class.
     *
     * @param assignableTypes The set of all classes which are assignable to the class of
     *                        the value to be validated and which are handled by at least
     *                        one of the validators for the specified constraint.
     */
    private static void reduceAssignableTypes(List<Type> assignableTypes) {
        if (assignableTypes.size() <= 1) {
            return; // no need to reduce
        }
        boolean removed = false;
        do {
            final Type type = assignableTypes.get(0);
            for (int i = 1; i < assignableTypes.size(); i++) {
                final Type nextType = assignableTypes.get(i);
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

    private static <A extends Annotation> Map<Type, Collection<Class<? extends ConstraintValidator<A, ?>>>> getValidatorsTypes(
        Class<? extends ConstraintValidator<A, ?>>[] constraintValidatorClasses) {
        final Map<Type, Collection<Class<? extends ConstraintValidator<A, ?>>>> validatorsTypes =
            new HashMap<Type, Collection<Class<? extends ConstraintValidator<A, ?>>>>();
        for (Class<? extends ConstraintValidator<A, ?>> validatorType : constraintValidatorClasses) {
            Type validatedType =
                TypeUtils.getTypeArguments(validatorType, ConstraintValidator.class).get(
                    ConstraintValidator.class.getTypeParameters()[1]);
            if (validatedType == null) {
                throw new ValidationException(String.format("Could not detect validated type for %s", validatorType));
            }
            if (validatedType instanceof GenericArrayType) {
                final Type componentType = TypeUtils.getArrayComponentType(validatedType);
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

    /**
     * implements spec chapter 3.5.3. ConstraintValidator resolution algorithm.
     */
    private static Type determineTargetedType(Class<?> owner, AccessStrategy access) {
        // if the constraint declaration is hosted on a class or an interface,
        // the targeted type is the class or the interface.
        if (access == null) {
            return owner;
        }
        final Type type = access.getJavaType();
        if (type == null) {
            return Object.class;
        }
        return type instanceof Class<?> ? ClassUtils.primitiveToWrapper((Class<?>) type) : type;
    }

    /**
     * Initialize the validator (if not <code>null</code>) with the stored
     * annotation.
     */
    public void initialize() {
        if (null != validator) {
            try {
                validator.initialize(annotation);
            } catch (RuntimeException e) {
                // Either a "legit" problem initializing the validator or a
                // ClassCastException if the validator associated annotation is
                // not a supertype of the validated annotation.
                throw new ConstraintDefinitionException("Incorrect validator ["
                    + validator.getClass().getCanonicalName() + "] for annotation "
                    + annotation.annotationType().getCanonicalName(), e);
            }
        }
    }

    private boolean isReachable(GroupValidationContext<?> context) {
        final PathImpl path = context.getPropertyPath();
        final NodeImpl node = path.getLeafNode();
        PathImpl beanPath = path.getPathWithoutLeafNode();
        if (beanPath == null) {
            beanPath = PathImpl.create();
        }
        try {
            if (!context.getTraversableResolver().isReachable(context.getBean(), node,
                context.getRootMetaBean().getBeanClass(), beanPath, access.getElementType())) {
                return false;
            }
        } catch (RuntimeException e) {
            throw new ValidationException("Error in TraversableResolver.isReachable() for " + context.getBean(), e);
        }
        return true;
    }

    private void addErrors(GroupValidationContext<?> context, ConstraintValidatorContextImpl jsrContext) {
        for (ValidationListener.Error each : jsrContext.getErrorMessages()) {
            context.getListener().addError(each, context);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ConstraintValidation{" + validator + '}';
    }

    /**
     * Get the message template used by this constraint.
     *
     * @return String
     */
    @Override
    public String getMessageTemplate() {
        return ConstraintAnnotationAttributes.MESSAGE.get(attributes);
    }

    public ConstraintValidator<T, ?> getValidator() {
        return validator;
    }

    protected boolean isMemberOf(Class<?> reqGroup) {
        return groups.contains(reqGroup);
    }

    public Class<?> getOwner() {
        return owner;
    }

    @Override
    public T getAnnotation() {
        return annotation;
    }

    public AccessStrategy getAccess() {
        return access;
    }

    public void setAnnotation(T annotation) {
        this.annotation = annotation;
    }

    // ///////////////////////// ConstraintDescriptor implementation

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<ConstraintDescriptor<?>> getComposingConstraints() {
        return composedConstraints == null ? Collections.EMPTY_SET : composedConstraints;
    }

    /**
     * Get the composing {@link ConstraintValidation} objects. This is
     * effectively an implementation-specific analogue to
     * {@link #getComposingConstraints()}.
     *
     * @return {@link Set} of {@link ConstraintValidation}
     */
    Set<ConstraintValidation<?>> getComposingValidations() {
        return composedConstraints == null ? Collections.<ConstraintValidation<?>> emptySet() : composedConstraints;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<?>> getGroups() {
        return groups;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<? extends Payload>> getPayload() {
        return payload;
    }

    @Override
    public ConstraintTarget getValidationAppliesTo() {
        return validationAppliesTo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Class<? extends ConstraintValidator<T, ?>>> getConstraintValidatorClasses() {
        return validatorClasses == null ? Collections.<Class<? extends ConstraintValidator<T, ?>>> emptyList() : Arrays.asList(validatorClasses);
    }

    public void setValidationAppliesTo(final ConstraintTarget validationAppliesTo) {
        this.validationAppliesTo = validationAppliesTo;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(final boolean validated) {
        this.validated = validated;
    }
}
