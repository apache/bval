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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintValidator;
import javax.validation.constraintvalidation.SupportedValidationTarget;
import javax.validation.constraintvalidation.ValidationTarget;

import org.apache.bval.jsr.metadata.AnnotationDeclaredValidatorMappingProvider;
import org.apache.bval.jsr.metadata.CompositeValidatorMappingProvider;
import org.apache.bval.jsr.metadata.DualValidationMappingProvider;
import org.apache.bval.jsr.metadata.ValidatorMappingProvider;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.Validate;

/**
 * Description: hold the relationship annotation->validatedBy[]
 * ConstraintValidator classes that are already parsed in a cache.<br/>
 */
public class ConstraintCached {

    /**
     * Describes a {@link ConstraintValidator} implementation type.
     * 
     * @since 2.0
     */
    public static final class ConstraintValidatorInfo<T extends Annotation> {
        private static final Set<ValidationTarget> DEFAULT_VALIDATION_TARGETS =
            Collections.singleton(ValidationTarget.ANNOTATED_ELEMENT);

        private final Class<? extends ConstraintValidator<T, ?>> type;
        private Set<ValidationTarget> supportedTargets;

        ConstraintValidatorInfo(Class<? extends ConstraintValidator<T, ?>> type) {
            super();
            this.type = Validate.notNull(type);
            final SupportedValidationTarget svt = type.getAnnotation(SupportedValidationTarget.class);
            
            supportedTargets = svt == null ? DEFAULT_VALIDATION_TARGETS
                : Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(svt.value())));

            if (supportedTargets.isEmpty()) {
                Exceptions.raise(ConstraintDefinitionException::new, "Illegally specified 0-length %s value on %s",
                    SupportedValidationTarget.class.getSimpleName(), type);
            }
        }

        public Class<? extends ConstraintValidator<T, ?>> getType() {
            return type;
        }

        public Set<ValidationTarget> getSupportedTargets() {
            return supportedTargets;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this
                || obj instanceof ConstraintValidatorInfo<?> && ((ConstraintValidatorInfo<?>) obj).type.equals(type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }
    }

    private final Map<Class<? extends Annotation>, Set<ConstraintValidatorInfo<?>>> constraintValidatorInfo =
        new HashMap<>();

    private final List<ValidatorMappingProvider> customValidatorMappingProviders = new ArrayList<>();
    private final Lazy<ValidatorMappingProvider> validatorMappingProvider =
        new Lazy<>(this::createValidatorMappingProvider);

    public void add(ValidatorMappingProvider validatorMappingProvider) {
        customValidatorMappingProviders.add(validatorMappingProvider);
        this.validatorMappingProvider.reset(this::createValidatorMappingProvider);
    }

    public <A extends Annotation> List<Class<? extends ConstraintValidator<A, ?>>> getConstraintValidatorClasses(
        Class<A> constraintType) {
        final Set<ConstraintValidatorInfo<A>> infos = infos(constraintType);
        return infos == null ? Collections.emptyList()
            : infos.stream().map(ConstraintValidatorInfo::getType).collect(ToUnmodifiable.list());
    }

    public <A extends Annotation> Set<ConstraintValidatorInfo<A>> getConstraintValidatorInfo(Class<A> constraintType) {
        return Collections.unmodifiableSet(infos(constraintType));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <A extends Annotation> Set<ConstraintValidatorInfo<A>> infos(Class<A> constraintType) {
        return (Set) constraintValidatorInfo.computeIfAbsent(constraintType,
            c -> validatorMappingProvider.get().getValidatorMapping(c).getValidatorTypes().stream()
                .map(ConstraintValidatorInfo::new).collect(Collectors.toSet()));
    }

    private ValidatorMappingProvider createValidatorMappingProvider() {
        final ValidatorMappingProvider configured;
        if (customValidatorMappingProviders.isEmpty()) {
            configured = AnnotationDeclaredValidatorMappingProvider.INSTANCE;
        } else {
            final ValidatorMappingProvider custom;
            if (customValidatorMappingProviders.size() == 1) {
                custom = customValidatorMappingProviders.get(0);
            } else {
                custom = new CompositeValidatorMappingProvider(customValidatorMappingProviders);
            }
            configured = new DualValidationMappingProvider(AnnotationDeclaredValidatorMappingProvider.INSTANCE, custom);
        }
        // interpret spec as saying that default constraint validators are
        // always present even when annotation-based validators
        // have been excluded by custom (i.e. XML) config:
        return new DualValidationMappingProvider(configured, ConstraintDefaults.INSTANCE);
    }
}
