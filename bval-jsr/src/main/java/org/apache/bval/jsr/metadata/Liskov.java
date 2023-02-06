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

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.ConstraintDeclarationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Valid;
import jakarta.validation.executable.ValidateOnExecution;

import org.apache.bval.jsr.metadata.HierarchyBuilder.ContainerDelegate;
import org.apache.bval.jsr.metadata.HierarchyBuilder.ElementDelegate;
import org.apache.bval.jsr.metadata.HierarchyBuilder.HierarchyDelegate;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;

class Liskov {
    //@formatter:off
    private enum ValidationElement {
        constraints, cascades, groupConversions, validateOnExecution;
    }

    private enum StrengtheningIssue implements Predicate<Map<Meta<?>, Set<ValidationElement>>> {
        overriddenHierarchy("overridden %s in inheritance hierarchy: %s") {

            @Override
            public boolean test(Map<Meta<?>, Set<ValidationElement>> detectedValidationElements) {
                Class<?> declaringType = null;

                for (Map.Entry<Meta<?>, Set<ValidationElement>> e : detectedValidationElements.entrySet()){
                    final Class<?> t = e.getKey().getDeclaringClass();
                    if (declaringType != null) {
                        if (declaringType.isAssignableFrom(t)) {
                            continue;
                        }
                        return false;
                    }
                    if (!e.getValue().isEmpty()){
                        declaringType = t;
                    }
                }
                return true;
            }
        },
        unrelatedInheritance("declared %s in unrelated inheritance hierarchies: %s") {

            @Override
            public boolean test(Map<Meta<?>, Set<ValidationElement>> detectedValidationElements) {
                if (detectedValidationElements.size() < 2) {
                    // no unrelated hierarchy possible
                    return true;
                }
                final Map<Class<?>, Set<ValidationElement>> interfaceValidation = new LinkedHashMap<>();
                detectedValidationElements.forEach((k,v)->{
                    final Class<?> t = k.getDeclaringClass();
                    if (t.isInterface()){
                        interfaceValidation.put(t, v);
                    }
                });
                if (interfaceValidation.isEmpty()) {
                    // if all are classes, there can be no unrelated types in the hierarchy:
                    return true;
                }
                // verify that all types can be assigned to the constrained interfaces:
                for (Meta<?> meta : detectedValidationElements.keySet()) {
                    final Class<?> t = meta.getDeclaringClass();
                    for (Map.Entry<Class<?>, Set<ValidationElement>> e : interfaceValidation.entrySet()) {
                        if (t.equals(e.getKey()) || e.getValue().isEmpty()) {
                            continue;
                        }
                        if (!e.getKey().isAssignableFrom(t)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        };
        //@formatter:on

        final String format;

        private StrengtheningIssue(String format) {
            this.format = "Illegal strengthening: " + format;
        }

        Supplier<String> messageFor(Map<Meta<?>, Set<ValidationElement>> detectedValidationElements) {
            return () -> {
                final Set<ValidationElement> validationElements =
                    detectedValidationElements.values().stream().flatMap(Collection::stream)
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(ValidationElement.class)));

                final String describeHierarchy = detectedValidationElements.keySet().stream().map(Meta::describeHost)
                    .collect(Collectors.joining(", ", "[", "]"));

                return String.format(format, validationElements, describeHierarchy);
            };
        }

        void check(Map<Meta<?>, Set<ValidationElement>> detectedValidationElements) {
            Exceptions.raiseUnless(test(detectedValidationElements), ConstraintDeclarationException::new,
                messageFor(detectedValidationElements));
        }
    }

    static void validateContainerHierarchy(Collection<? extends ContainerDelegate<?>> delegates, ElementKind elementKind) {
        if (Validate.notNull(delegates, "delegates").isEmpty()) {
            return;
        }
        if (Validate.notNull(elementKind, "elementKind") == ElementKind.CONTAINER_ELEMENT) {
            elementKind = getContainer(delegates.iterator().next().getHierarchyElement());
        }
        switch (elementKind) {
        case RETURN_VALUE:
            noRedeclarationOfReturnValueCascading(delegates);

            final Map<Meta<?>, Set<ValidationElement>> detectedValidationElements =
                detectValidationElements(delegates, ElementDelegate::getHierarchyElement, detectGroupConversion());

            // pre-check return value overridden hierarchy:
            Stream.of(StrengtheningIssue.values())
                .filter(si -> !(si == StrengtheningIssue.overriddenHierarchy
                    && detectedValidationElements.values().stream().filter(s -> !s.isEmpty()).count() < 2))
                .forEach(si -> si.check(detectedValidationElements));

            break;
        case PARAMETER:
            noStrengtheningOfPreconditions(delegates, detectConstraints(), detectCascading(), detectGroupConversion());
            break;
        default:
            break;
        }
    }

    static void validateCrossParameterHierarchy(Collection<? extends ElementDelegate<?, ?>> delegates) {
        if (Validate.notNull(delegates, "delegates").isEmpty()) {
            return;
        }
        noStrengtheningOfPreconditions(delegates, detectConstraints());
    }

    static void validateValidateOnExecution(Collection<? extends HierarchyDelegate<?, ?>> delegates) {
        noStrengtheningOfPreconditions(delegates, detectValidateOnExecution());
    }

    private static ElementKind getContainer(Meta<?> meta) {
        Meta<?> m = meta;
        while (m.getElementType() == ElementType.TYPE_USE) {
            m = m.getParent();
        }
        switch (m.getElementType()) {
        case METHOD:
            return ElementKind.RETURN_VALUE;
        case PARAMETER:
            return ElementKind.PARAMETER;
        default:
            return ElementKind.PROPERTY;
        }
    }

    private static void noRedeclarationOfReturnValueCascading(Collection<? extends ContainerDelegate<?>> delegates) {
        final Map<Class<?>, Meta<?>> cascadedReturnValues =
            delegates.stream().filter(ContainerDelegate::isCascade).map(HierarchyDelegate::getHierarchyElement)
                .collect(Collectors.toMap(Meta::getDeclaringClass, Function.identity()));

        final boolean anyRelated = cascadedReturnValues.keySet().stream().anyMatch(t -> cascadedReturnValues.keySet()
            .stream().filter(Predicate.isEqual(t).negate()).anyMatch(t2 -> related(t, t2)));

        Exceptions.raiseIf(anyRelated, ConstraintDeclarationException::new,
            "Multiple method return values marked @%s in hierarchy %s",
            f -> f.args(Valid.class.getSimpleName(), cascadedReturnValues.values()));
    }

    @SafeVarargs
    private static <D extends HierarchyDelegate<?, ?>> void noStrengtheningOfPreconditions(Collection<? extends D> delegates,
        Function<? super D, ValidationElement>... detectors) {

        final Map<Meta<?>, Set<ValidationElement>> detectedValidationElements = 
                detectValidationElements(delegates, HierarchyDelegate::getHierarchyElement, detectors);

        if (detectedValidationElements.isEmpty()) {
            return;
        }
        for (StrengtheningIssue s : StrengtheningIssue.values()) {
            s.check(detectedValidationElements);
        }
    }

    @SafeVarargs
    private static <T> Map<Meta<?>, Set<ValidationElement>> detectValidationElements(Collection<? extends T> delegates,
        Function<? super T, Meta<?>> toMeta, Function<? super T, ValidationElement>... detectors) {
        final Map<Meta<?>, Set<ValidationElement>> detectedValidationElements = new LinkedHashMap<>();
        delegates.forEach(d -> {
            detectedValidationElements.put(toMeta.apply(d),
                Stream.of(detectors).map(dt -> dt.apply(d)).filter(Objects::nonNull)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(ValidationElement.class))));
        });
        if (detectedValidationElements.values().stream().allMatch(Collection::isEmpty)) {
            // nothing declared
            return Collections.emptyMap();
        }
        return detectedValidationElements;
    }

    private static boolean related(Class<?> c1, Class<?> c2) {
        return c1.isAssignableFrom(c2) || c2.isAssignableFrom(c1);
    }

    private static Function<ElementDelegate<?, ?>, ValidationElement> detectConstraints() {
        return d -> d.getDeclaredConstraints().length > 0 ? ValidationElement.constraints : null;
    }

    private static Function<ContainerDelegate<?>, ValidationElement> detectCascading() {
        return d -> d.isCascade() ? ValidationElement.cascades : null;
    }

    private static Function<ContainerDelegate<?>, ValidationElement> detectGroupConversion() {
        return d -> d.getGroupConversions().isEmpty() ? null : ValidationElement.groupConversions;
    }

    private static Function<HierarchyDelegate<?, ?>, ValidationElement> detectValidateOnExecution() {
        return d -> d.getHierarchyElement().getHost().isAnnotationPresent(ValidateOnExecution.class)
            ? ValidationElement.validateOnExecution : null;
    }

    private Liskov() {
    }
}
