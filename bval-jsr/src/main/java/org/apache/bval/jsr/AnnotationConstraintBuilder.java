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

import org.apache.bval.jsr.groups.GroupsComputer;
import org.apache.bval.jsr.xml.AnnotationProxyBuilder;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.reflection.TypeUtils;
import org.apache.commons.weaver.privilizer.Privileged;

import javax.validation.Constraint;
import javax.validation.ConstraintDeclarationException;
import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintTarget;
import javax.validation.ConstraintValidator;
import javax.validation.OverridesAttribute;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import javax.validation.constraintvalidation.SupportedValidationTarget;
import javax.validation.constraintvalidation.ValidationTarget;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Description: helper class that builds a {@link ConstraintValidation} or its
 * composite constraint validations by parsing the jsr-annotations and
 * providing information (e.g. for @OverridesAttributes) <br/>
 */
final class AnnotationConstraintBuilder<A extends Annotation> {
    private static final Logger log = Logger.getLogger(AnnotationConstraintBuilder.class.getName());

    private final ConstraintValidation<?> constraintValidation;
    private List<ConstraintOverrides> overrides;

    /**
     * Create a new AnnotationConstraintBuilder instance.
     *
     * @param validatorClasses
     * @param annotation
     * @param owner
     * @param access
     */
    public AnnotationConstraintBuilder(
            Class<? extends ConstraintValidator<A, ?>>[] validatorClasses, A annotation, Class<?> owner,
            AccessStrategy access, ConstraintTarget target) {
        final boolean reportFromComposite =
            annotation != null && annotation.annotationType().isAnnotationPresent(ReportAsSingleViolation.class);
        constraintValidation =
            new ConstraintValidation<A>(validatorClasses, annotation, owner, access, reportFromComposite,
                target);
        buildFromAnnotation();
    }

    /** build attributes, payload, groups from 'annotation' */
    @Privileged
    private void buildFromAnnotation() {
        if (constraintValidation.getAnnotation() == null) {
            return;
        }
        final Class<? extends Annotation> annotationType = constraintValidation.getAnnotation().annotationType();

        boolean foundPayload = false;
        boolean foundGroups = false;
        Method validationAppliesTo = null;
        boolean foundMessage = false;

        for (final Method method : AnnotationProxyBuilder.findMethods(annotationType)) {
            // groups + payload must also appear in attributes (also
            // checked by TCK-Tests)
            if (method.getParameterTypes().length == 0) {
                try {
                    final String name = method.getName();
                    if (ConstraintAnnotationAttributes.PAYLOAD.getAttributeName().equals(name)) {
                        buildPayload(method);
                        foundPayload = true;
                    } else if (ConstraintAnnotationAttributes.GROUPS.getAttributeName().equals(name)) {
                        buildGroups(method);
                        foundGroups = true;
                    } else if (ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO.getAttributeName().equals(name)) {
                        buildValidationAppliesTo(method);
                        validationAppliesTo = method;
                    } else if (name.startsWith("valid")) {
                        throw new ConstraintDefinitionException("constraints parameters can't start with valid: " + name);
                    } else {
                        if (ConstraintAnnotationAttributes.MESSAGE.getAttributeName().equals(name)) {
                            foundMessage = true;
                            if (!TypeUtils.isAssignable(method.getReturnType(), ConstraintAnnotationAttributes.MESSAGE.getType())) {
                                throw new ConstraintDefinitionException("Return type for message() must be of type " + ConstraintAnnotationAttributes.MESSAGE.getType());
                            }
                        }
                        constraintValidation.getAttributes().put(name, method.invoke(constraintValidation.getAnnotation()));
                    }
                } catch (final ConstraintDefinitionException cde) {
                    throw cde;
                } catch (final Exception e) { // do nothing
                    log.log(Level.WARNING, String.format("Error processing annotation: %s ", constraintValidation.getAnnotation()), e);
                }
            }
        }

        if (!foundMessage) {
            throw new ConstraintDefinitionException("Annotation " + annotationType.getName() + " has no message method");
        }
        if (!foundPayload) {
            throw new ConstraintDefinitionException("Annotation " + annotationType.getName() + " has no payload method");
        }
        if (!foundGroups) {
            throw new ConstraintDefinitionException("Annotation " + annotationType.getName() + " has no groups method");
        }
        if (validationAppliesTo != null && !ConstraintTarget.IMPLICIT.equals(validationAppliesTo.getDefaultValue())) {
            throw new ConstraintDefinitionException("validationAppliesTo default value should be IMPLICIT");
        }

        // valid validationAppliesTo
        final Constraint annotation = annotationType.getAnnotation(Constraint.class);
        if (annotation == null) {
            return;
        }

        final Pair validationTarget = computeValidationTarget(annotation.validatedBy());
        for (final Annotation a : annotationType.getAnnotations()) {
            final Class<? extends Annotation> aClass = a.annotationType();
            if (aClass.getName().startsWith("java.lang.annotation.")) {
                continue;
            }

            final Constraint inheritedConstraint = aClass.getAnnotation(Constraint.class);
            if (inheritedConstraint != null && !aClass.getName().startsWith("javax.validation.constraints.")) {
                final Pair validationTargetInherited = computeValidationTarget(inheritedConstraint.validatedBy());
                if ((validationTarget.a > 0 && validationTargetInherited.b > 0 && validationTarget.b == 0)
                        || (validationTarget.b > 0 && validationTargetInherited.a > 0 && validationTarget.a == 0)) {
                    throw new ConstraintDefinitionException("Parent and child constraint have different targets");
                }
            }
        }
    }

    private Pair computeValidationTarget(final Class<?>[] validators) {
        int param = 0;
        int annotatedElt = 0;

        for (final Class<?> validator : validators) {
            final SupportedValidationTarget supportedAnnotationTypes = validator.getAnnotation(SupportedValidationTarget.class);
            if (supportedAnnotationTypes != null) {
                final List<ValidationTarget> values = Arrays.asList(supportedAnnotationTypes.value());
                if (values.contains(ValidationTarget.PARAMETERS)) {
                    param++;
                }
                if (values.contains(ValidationTarget.ANNOTATED_ELEMENT)) {
                    annotatedElt++;
                }
            } else {
                annotatedElt++;
            }
        }

        if (annotatedElt == 0 && param >= 1 && constraintValidation.getValidationAppliesTo() != null) { // pure cross param
            throw new ConstraintDefinitionException("pure cross parameter constraints shouldn't get validationAppliesTo attribute");
        }
        if (param >= 1 && annotatedElt >= 1 && constraintValidation.getValidationAppliesTo() == null) { // generic and cross param
            throw new ConstraintDefinitionException("cross parameter AND generic constraints should get validationAppliesTo attribute");
        }
        if (param == 0 && constraintValidation.getValidationAppliesTo() != null) { // pure generic
            throw new ConstraintDefinitionException("pure generic constraints shouldn't get validationAppliesTo attribute");
        }

        return new Pair(annotatedElt, param);
    }

    private void buildValidationAppliesTo(final Method method) throws InvocationTargetException, IllegalAccessException {
        if (!TypeUtils.isAssignable(method.getReturnType(), ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO.getType())) {
            throw new ConstraintDefinitionException("Return type for validationAppliesTo() must be of type "
                + ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO.getType());
        }
        final Object validationAppliesTo = method.invoke(constraintValidation.getAnnotation());
        if (!ConstraintTarget.class.isInstance(validationAppliesTo)) {
            throw new ConstraintDefinitionException("validationAppliesTo type is " + ConstraintTarget.class.getName());
        }
        constraintValidation.setValidationAppliesTo(ConstraintTarget.class.cast(validationAppliesTo));
    }

    private void buildGroups(final Method method) throws IllegalAccessException, InvocationTargetException {
        if (!TypeUtils.isAssignable(method.getReturnType(), ConstraintAnnotationAttributes.GROUPS.getType())) {
            throw new ConstraintDefinitionException("Return type for groups() must be of type "
                + ConstraintAnnotationAttributes.GROUPS.getType());
        }

        final Object raw = method.invoke(constraintValidation.getAnnotation());
        Class<?>[] garr;
        if (raw instanceof Class<?>) {
            garr = new Class[] { (Class<?>) raw };
        } else if (raw instanceof Class<?>[]) {
            garr = (Class<?>[]) raw;
            if (Object[].class.cast(method.getDefaultValue()).length > 0) {
                throw new ConstraintDefinitionException("Default value for groups() must be an empty array");
            }
        } else {
            garr = null;
        }

        if (garr == null || garr.length == 0) {
            garr = GroupsComputer.DEFAULT_GROUP;
        }
        constraintValidation.setGroups(garr);
    }

    @SuppressWarnings("unchecked")
    private void buildPayload(final Method method) throws IllegalAccessException, InvocationTargetException {
        if (!TypeUtils.isAssignable(method.getReturnType(), ConstraintAnnotationAttributes.PAYLOAD.getType())) {
            throw new ConstraintDefinitionException("Return type for payload() must be of type "
                + ConstraintAnnotationAttributes.PAYLOAD.getType());
        }
        if (Object[].class.cast(method.getDefaultValue()).length > 0) {
            throw new ConstraintDefinitionException("Default value for payload() must be an empty array");
        }

        final Class<? extends Payload>[] payload_raw =
            (Class<? extends Payload>[]) method.invoke(constraintValidation.getAnnotation());

        final Set<Class<? extends Payload>> payloadSet;
        if (payload_raw == null) {
            payloadSet = Collections.<Class<? extends Payload>> emptySet();
        } else {
            payloadSet = new HashSet<Class<? extends Payload>>(payload_raw.length);
            Collections.addAll(payloadSet, payload_raw);
        }
        constraintValidation.setPayload(payloadSet);
    }

    /**
     * Get the configured {@link ConstraintValidation}.
     *
     * @return {@link ConstraintValidation}
     */
    public ConstraintValidation<?> getConstraintValidation() {
        return constraintValidation;
    }

    /**
     * initialize a child composite 'validation' with @OverridesAttribute from
     * 'constraintValidation' and add to composites.
     */
    public void addComposed(ConstraintValidation<?> composite) {
        applyOverridesAttributes(composite);

        if (constraintValidation.getValidationAppliesTo() != null) {
            composite.setValidationAppliesTo(constraintValidation.getValidationAppliesTo());
        }

        constraintValidation.addComposed(composite); // add AFTER apply()
    }

    private void applyOverridesAttributes(ConstraintValidation<?> composite) {
        if (null == overrides) {
            buildOverridesAttributes();
        }
        if (!overrides.isEmpty()) {
            final int index = computeIndex(composite);

            // Search for the overrides to apply
            final ConstraintOverrides generalOverride = findOverride(composite.getAnnotation().annotationType(), -1);
            if (generalOverride != null) {
                if (index > 0) {
                    throw new ConstraintDeclarationException("Wrong OverridesAttribute declaration for "
                        + generalOverride.constraintType
                        + ", it needs a defined index when there is a list of constraints");
                }
                generalOverride.applyOn(composite);
            }

            final ConstraintOverrides override = findOverride(composite.getAnnotation().annotationType(), index);
            if (override != null) {
                override.applyOn(composite);
            }
        }
    }

    /**
     * Calculates the index of the composite constraint. The index represents
     * the order in which it is added in reference to other constraints of the
     * same type.
     *
     * @param composite
     *            The composite constraint (not yet added).
     * @return An integer index always >= 0
     */
    private int computeIndex(ConstraintValidation<?> composite) {
        int idx = 0;
        for (ConstraintValidation<?> each : constraintValidation.getComposingValidations()) {
            if (each.getAnnotation().annotationType() == composite.getAnnotation().annotationType()) {
                idx++;
            }
        }
        return idx;
    }

    /** read overridesAttributes from constraintValidation.annotation */
    private void buildOverridesAttributes() {
        overrides = new LinkedList<ConstraintOverrides>();
        for (Method method : constraintValidation.getAnnotation().annotationType().getDeclaredMethods()) {
            final OverridesAttribute.List overridesAttributeList = method.getAnnotation(OverridesAttribute.List.class);
            if (overridesAttributeList != null) {
                for (OverridesAttribute overridesAttribute : overridesAttributeList.value()) {
                    parseConstraintOverride(method.getName(), overridesAttribute);
                }
            }
            final OverridesAttribute overridesAttribute = method.getAnnotation(OverridesAttribute.class);
            if (overridesAttribute != null) {
                parseConstraintOverride(method.getName(), overridesAttribute);
            }
        }
    }

    private void parseConstraintOverride(String methodName, OverridesAttribute oa) {
        ConstraintOverrides target = findOverride(oa.constraint(), oa.constraintIndex());
        if (target == null) {
            target = new ConstraintOverrides(oa.constraint(), oa.constraintIndex());
            overrides.add(target);
        }
        target.values.put(oa.name(), constraintValidation.getAttributes().get(methodName));
    }

    private ConstraintOverrides findOverride(Class<? extends Annotation> constraint, int constraintIndex) {
        for (ConstraintOverrides each : overrides) {
            if (each.constraintType == constraint && each.constraintIndex == constraintIndex) {
                return each;
            }
        }
        return null;
    }

    /**
     * Holds the values to override in a composed constraint during creation of
     * a composed ConstraintValidation
     */
    private static final class ConstraintOverrides {
        final Class<? extends Annotation> constraintType;
        final int constraintIndex;

        /** key = attributeName, value = overridden value */
        final Map<String, Object> values;

        private ConstraintOverrides(Class<? extends Annotation> constraintType, int constraintIndex) {
            this.constraintType = constraintType;
            this.constraintIndex = constraintIndex;
            values = new HashMap<String, Object>();
        }

        @SuppressWarnings("unchecked")
        private void applyOn(ConstraintValidation<?> composite) {
            // Update the attributes
            composite.getAttributes().putAll(values);

            // And the annotation
            final Annotation originalAnnot = composite.getAnnotation();
            final AnnotationProxyBuilder<Annotation> apb = new AnnotationProxyBuilder<Annotation>(originalAnnot);
            for (String key : values.keySet()) {
                apb.putValue(key, values.get(key));
            }
            final Annotation newAnnot = apb.createAnnotation();
            ((ConstraintValidation<Annotation>) composite).setAnnotation(newAnnot);
        }
    }

    private static class Pair {
        private int a;
        private int b;

        private Pair(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }
}
