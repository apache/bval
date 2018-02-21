/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval.jsr.metadata;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.ConstraintValidator;

import org.apache.bval.util.Validate;

public class ValidatorMapping<A extends Annotation> implements HasAnnotationBehavior {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final ValidatorMapping EMPTY = new ValidatorMapping("empty", Collections.emptyList());

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> ValidatorMapping<A> empty() {
        return EMPTY;
    }

    public static <A extends Annotation> ValidatorMapping<A> merge(
        List<? extends ValidatorMapping<A>> validatorMappings,
        AnnotationBehaviorMergeStrategy annotationBehaviorMergeStrategy) {

        final AnnotationBehavior behavior = annotationBehaviorMergeStrategy.apply(validatorMappings);

        final List<? extends ValidatorMapping<A>> nonEmpty =
            validatorMappings.stream().filter(m -> !m.isEmpty()).collect(Collectors.toList());

        if (nonEmpty.size() <= 1) {
            // avoid creating the composite instance if behavior matches:
            final ValidatorMapping<A> simpleResult = nonEmpty.isEmpty() ? empty() : nonEmpty.get(0);

            if (simpleResult.hasBehavior(behavior)) {
                return simpleResult;
            }
        }
        final String source =
            nonEmpty.stream().map(ValidatorMapping::getSource).collect(Collectors.joining(";", "[", "]"));

        return new ValidatorMapping<>(source, nonEmpty.stream().map(ValidatorMapping::getValidatorTypes)
            .flatMap(Collection::stream).distinct().collect(Collectors.toList()), behavior);
    }

    private final String source;
    private final List<Class<? extends ConstraintValidator<A, ?>>> validatorTypes;
    private final AnnotationBehavior annotationBehavior;

    public ValidatorMapping(String source, List<Class<? extends ConstraintValidator<A, ?>>> validatorTypes) {
        this(source, validatorTypes, AnnotationBehavior.ABSTAIN);
    }

    public ValidatorMapping(String source, List<Class<? extends ConstraintValidator<A, ?>>> validatorTypes,
        AnnotationBehavior annotationBehavior) {
        this.source = Objects.toString(source, "unspecified");
        this.validatorTypes = Collections.unmodifiableList(Validate.notNull(validatorTypes, "validatorTypes"));
        this.annotationBehavior = Validate.notNull(annotationBehavior, "annotationBehavior");
    }

    public List<Class<? extends ConstraintValidator<A, ?>>> getValidatorTypes() {
        return validatorTypes;
    }

    public AnnotationBehavior getAnnotationBehavior() {
        return annotationBehavior;
    }

    public boolean isEmpty() {
        return validatorTypes.isEmpty();
    }

    public String getSource() {
        return source;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!getClass().isInstance(obj)) {
            return false;
        }
        final ValidatorMapping<?> other = (ValidatorMapping<?>) obj;
        return getSource().equals(other.getSource()) && getAnnotationBehavior() == other.getAnnotationBehavior()
            && getValidatorTypes().equals(other.getValidatorTypes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSource(), getAnnotationBehavior(), getValidatorTypes());
    }

    @Override
    public String toString() {
        return String.format("%s[source: %s; annotationBehavior: %s; validatorTypes: %s]",
            ValidatorMapping.class.getSimpleName(), getSource(), getAnnotationBehavior(), getValidatorTypes());
    }

    public boolean hasBehavior(AnnotationBehavior annotationBehavior) {
        return getAnnotationBehavior() == annotationBehavior;
    }
}