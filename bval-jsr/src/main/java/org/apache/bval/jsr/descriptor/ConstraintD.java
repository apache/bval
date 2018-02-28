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
package org.apache.bval.jsr.descriptor;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.validation.ConstraintDeclarationException;
import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintTarget;
import javax.validation.ConstraintValidator;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import javax.validation.UnexpectedTypeException;
import javax.validation.ValidationException;
import javax.validation.groups.Default;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.Scope;
import javax.validation.metadata.ValidateUnwrappedValue;
import javax.validation.valueextraction.UnwrapByDefault;
import javax.validation.valueextraction.Unwrapping;
import javax.validation.valueextraction.Unwrapping.Skip;
import javax.validation.valueextraction.Unwrapping.Unwrap;
import javax.validation.valueextraction.ValueExtractor;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.ConstraintAnnotationAttributes;
import org.apache.bval.jsr.ConstraintAnnotationAttributes.Worker;
import org.apache.bval.jsr.metadata.ContainerElementKey;
import org.apache.bval.jsr.metadata.Meta;
import org.apache.bval.jsr.util.AnnotationsManager;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.jsr.valueextraction.ValueExtractors;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.TypeUtils;

public class ConstraintD<A extends Annotation> implements ConstraintDescriptor<A> {
    private enum Optionality {
        OPTIONAL, REQUIRED;

        public boolean isOptional() {
            return this == Optionality.OPTIONAL;
        }
    }

    private static <T> Set<T> set(Supplier<T[]> array) {
        return Stream.of(array.get()).collect(ToUnmodifiable.set());
    }

    private final A annotation;
    private final Scope scope;
    private final Meta<?> meta;

    private final Set<Class<? extends Payload>> payload;
    private final Class<?> validatedType;

    private final Set<Class<?>> groups;
    private final boolean reportAsSingle;
    private final ValidateUnwrappedValue valueUnwrapping;
    private final Map<String, Object> attributes;

    private final Set<ConstraintDescriptor<?>> composingConstraints;
    private final List<Class<? extends ConstraintValidator<A, ?>>> constraintValidatorClasses;
    private final Class<? extends ConstraintValidator<A, ?>> constraintValidatorClass;

    public ConstraintD(A annotation, Scope scope, Meta<?> meta, ApacheValidatorFactory validatorFactory) {
        this.annotation = Validate.notNull(annotation, "annotation");
        this.scope = Validate.notNull(scope, "scope");
        this.meta = Validate.notNull(meta, "meta");

        payload = computePayload();
        validatedType = computeValidatedType(validatorFactory);

        groups = computeGroups();
        reportAsSingle = annotation.annotationType().isAnnotationPresent(ReportAsSingleViolation.class);
        valueUnwrapping = computeValidateUnwrappedValue();
        attributes = AnnotationsManager.readAttributes(annotation);

        Validate.notNull(validatorFactory, "validatorFactory");
        composingConstraints = computeComposingConstraints(validatorFactory);
        constraintValidatorClasses = computeConstraintValidatorClasses(validatorFactory);
        constraintValidatorClass = new ComputeConstraintValidatorClass<>(validatorFactory, meta.getValidationTarget(),
            annotation, validatedType).get();
    }

    @Override
    public A getAnnotation() {
        return annotation;
    }

    @Override
    public Set<Class<?>> getGroups() {
        return groups;
    }

    @Override
    public Set<Class<? extends Payload>> getPayload() {
        return payload;
    }

    @Override
    public List<Class<? extends ConstraintValidator<A, ?>>> getConstraintValidatorClasses() {
        return constraintValidatorClasses;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Set<ConstraintDescriptor<?>> getComposingConstraints() {
        return composingConstraints;
    }

    @Override
    public boolean isReportAsSingleViolation() {
        return reportAsSingle;
    }

    @Override
    public String getMessageTemplate() {
        return read(ConstraintAnnotationAttributes.MESSAGE, Optionality.REQUIRED);
    }

    @Override
    public ConstraintTarget getValidationAppliesTo() {
        return read(ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO);
    }

    @Override
    public ValidateUnwrappedValue getValueUnwrapping() {
        return valueUnwrapping;
    }

    @Override
    public <U> U unwrap(Class<U> type) throws ValidationException {
        try {
            return type.cast(this);
        } catch (ClassCastException e) {
            throw new ValidationException(e);
        }
    }

    public Scope getScope() {
        return scope;
    }

    public Class<?> getDeclaringClass() {
        return meta.getDeclaringClass();
    }

    public ElementType getDeclaredOn() {
        return meta.getElementType();
    }

    public Class<?> getValidatedType() {
        return validatedType;
    }

    public Class<? extends ConstraintValidator<A, ?>> getConstraintValidatorClass() {
        return constraintValidatorClass;
    }

    private <T> T read(ConstraintAnnotationAttributes attr) {
        return read(attr, Optionality.OPTIONAL);
    }

    private <T> T read(ConstraintAnnotationAttributes attr, Optionality optionality) {
        final Class<? extends Annotation> constraintType = annotation.annotationType();
        final Optional<T> result =
            Optional.of(constraintType).map(attr::analyze).filter(Worker::isValid).map(w -> w.<T> read(annotation));

        Exceptions.raiseUnless(optionality.isOptional() || result.isPresent(), ConstraintDefinitionException::new,
            "Required attribute %s missing from constraint type %s", attr.getAttributeName(), constraintType);

        return result.orElse(null);
    }

    private Set<ConstraintDescriptor<?>> computeComposingConstraints(ApacheValidatorFactory validatorFactory) {
        return Stream.of(validatorFactory.getAnnotationsManager().getComposingConstraints(annotation))
            .map(c -> new ConstraintD<>(c, scope, meta, validatorFactory))
            .collect(ToUnmodifiable.set(LinkedHashSet::new));
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends ConstraintValidator<A, ?>>> computeConstraintValidatorClasses(
        ApacheValidatorFactory validatorFactory) {
        return validatorFactory.getConstraintsCache()
            .getConstraintValidatorClasses((Class<A>) annotation.annotationType());
    }

    private ValidateUnwrappedValue computeValidateUnwrappedValue() {
        final Set<Class<? extends Payload>> p = getPayload();
        final boolean unwrap = p.contains(Unwrap.class);
        final boolean skip = p.contains(Skip.class);
        if (unwrap) {
            Validate.validState(!skip, "Cannot specify both %s and %s", Unwrap.class.getSimpleName(),
                Skip.class.getSimpleName());
            return ValidateUnwrappedValue.UNWRAP;
        }
        return skip ? ValidateUnwrappedValue.SKIP : ValidateUnwrappedValue.DEFAULT;
    }

    private Set<Class<?>> computeGroups() {
        final Class<?>[] groups = read(ConstraintAnnotationAttributes.GROUPS, Optionality.REQUIRED);
        if (groups.length == 0) {
            return Collections.singleton(Default.class);
        }
        return set(() -> groups);
    }

    private Set<Class<? extends Payload>> computePayload() {
        final Set<Class<? extends Payload>> result =
            set(() -> read(ConstraintAnnotationAttributes.PAYLOAD, Optionality.REQUIRED));
        Exceptions.raiseIf(result.containsAll(Arrays.asList(Unwrapping.Unwrap.class, Unwrapping.Skip.class)),
            ConstraintDeclarationException::new,
            "Constraint %s declared at %s specifies conflicting value unwrapping hints", annotation, meta.getHost());
        return result;
    }

    private Class<?> computeValidatedType(ApacheValidatorFactory validatorFactory) {
        final Class<?> rawType = TypeUtils.getRawType(meta.getType(), null);

        Exceptions.raiseIf(rawType == null, UnexpectedTypeException::new, "Could not calculate validated type from %s",
            meta.getType());

        if (payload.contains(Unwrapping.Skip.class)) {
            return rawType;
        }
        final ValueExtractor<?> valueExtractor =
            validatorFactory.getValueExtractors().find(new ContainerElementKey(meta.getAnnotatedType(), null));

        final boolean unwrap = payload.contains(Unwrapping.Unwrap.class);

        if (valueExtractor == null) {
            Exceptions.raiseIf(unwrap, ConstraintDeclarationException::new, "No compatible %s found for %s",
                ValueExtractor.class.getSimpleName(), meta.getType());
        } else {
            @SuppressWarnings("unchecked")
            final Class<? extends ValueExtractor<?>> extractorClass =
                (Class<? extends ValueExtractor<?>>) valueExtractor.getClass();
            if (unwrap || extractorClass.isAnnotationPresent(UnwrapByDefault.class)) {
                return ValueExtractors.getExtractedType(valueExtractor, meta.getType());
            }
        }
        return rawType;
    }
}
