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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.util.Arrays;
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
import javax.validation.ValidationException;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.Scope;
import javax.validation.metadata.ValidateUnwrappedValue;
import javax.validation.valueextraction.Unwrapping;
import javax.validation.valueextraction.Unwrapping.Skip;
import javax.validation.valueextraction.Unwrapping.Unwrap;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.ConstraintAnnotationAttributes;
import org.apache.bval.jsr.ConstraintAnnotationAttributes.Worker;
import org.apache.bval.jsr.metadata.Meta;
import org.apache.bval.jsr.util.AnnotationsManager;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.Validate;

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
    private final Set<Class<?>> groups;
    private final boolean reportAsSingle;
    private final ValidateUnwrappedValue valueUnwrapping;
    private final Map<String, Object> attributes;
    private final ConstraintTarget validationAppliesTo;

    private final Set<ConstraintDescriptor<?>> composingConstraints;
    private final List<Class<? extends ConstraintValidator<A, ?>>> constraintValidatorClasses;
    private final Lazy<String> toString =
        new Lazy<>(() -> String.format("%s: %s", ConstraintD.class.getSimpleName(), getAnnotation()));

    public ConstraintD(A annotation, Scope scope, Meta<?> meta, ApacheValidatorFactory validatorFactory) {
        this.annotation = Validate.notNull(annotation, "annotation");
        this.scope = Validate.notNull(scope, "scope");
        this.meta = Validate.notNull(meta, "meta");

        payload = computePayload();
        groups = set(() -> read(ConstraintAnnotationAttributes.GROUPS, Optionality.REQUIRED));
        reportAsSingle = annotation.annotationType().isAnnotationPresent(ReportAsSingleViolation.class);
        valueUnwrapping = computeValidateUnwrappedValue();
        attributes = AnnotationsManager.readAttributes(annotation);
        validationAppliesTo = computeValidationAppliesTo(meta.getElementType());

        Validate.notNull(validatorFactory, "validatorFactory");
        composingConstraints = computeComposingConstraints(validatorFactory);
        constraintValidatorClasses = computeConstraintValidatorClasses(validatorFactory);
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
        return validationAppliesTo;
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

    @Override
    public String toString() {
        return toString.get();
    }

    private <T> T read(ConstraintAnnotationAttributes attr) {
        return read(attr, Optionality.OPTIONAL);
    }

    private <T> T read(ConstraintAnnotationAttributes attr, Optionality optionality) {
        final Class<? extends Annotation> constraintType = annotation.annotationType();
        final Optional<T> result =
            Optional.of(constraintType).map(attr::analyze).filter(Worker::isValid).map(w -> w.<T> read(annotation));

        Exceptions.raiseUnless(optionality.isOptional() || result.isPresent(), ConstraintDefinitionException::new,
            "Required attribute %s missing from constraint type %s",
            f -> f.args(attr.getAttributeName(), constraintType));

        return result.orElse(null);
    }

    private Set<ConstraintDescriptor<?>> computeComposingConstraints(ApacheValidatorFactory validatorFactory) {
        return Stream.of(validatorFactory.getAnnotationsManager().getComposingConstraints(annotation))
            .map(c -> new ConstraintD<>(c, scope, meta, validatorFactory)).collect(ToUnmodifiable.set());
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

    private Set<Class<? extends Payload>> computePayload() {
        final Set<Class<? extends Payload>> result =
            set(() -> read(ConstraintAnnotationAttributes.PAYLOAD, Optionality.REQUIRED));
        if (result.containsAll(Arrays.asList(Unwrapping.Unwrap.class, Unwrapping.Skip.class))) {
            Exceptions.raise(ConstraintDeclarationException::new,
                "Constraint %s declared at %s specifies conflicting value unwrapping hints", annotation,
                meta.getHost());
        }
        return result;
    }

    private ConstraintTarget computeValidationAppliesTo(ElementType elementType) {
        final ConstraintTarget result = read(ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO);
        if (result != null && result != ConstraintTarget.IMPLICIT) {
            final AnnotatedElement host = meta.getHost();
            Exceptions.raiseUnless(host instanceof Executable, ConstraintDeclarationException::new, "Illegal %s on %s",
                result, host);

            switch (result) {
            case PARAMETERS:
                Exceptions.raiseIf(((Executable) host).getParameterCount() == 0, ConstraintDeclarationException::new,
                    "Illegal specification of %s on %s with no parameters", result, elementType);
                break;
            case RETURN_VALUE:
                Exceptions.raiseIf(Void.TYPE.equals(meta.getType()), ConstraintDeclarationException::new,
                    "Illegal %s on %s method %s", result, Void.TYPE, host);
                break;
            default:
                Exceptions.raise(IllegalStateException::new, "Unknown %s %s", ConstraintTarget.class.getSimpleName(),
                    result);
            }
        }
        return result;
    }
}
