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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintDeclarationException;
import javax.validation.ElementKind;
import javax.validation.Valid;

import org.apache.bval.jsr.metadata.HierarchyBuilder.ContainerDelegate;
import org.apache.bval.jsr.metadata.HierarchyBuilder.HierarchyDelegate;
import org.apache.bval.jsr.metadata.HierarchyBuilder.ElementDelegate;
import org.apache.bval.util.Exceptions;
import org.apache.commons.lang3.Validate;

class Liskov {
    //@formatter:off
    private enum ValidationElement {
        constraints, cascades, groupConversions;
    }

    private enum StrengtheningIssue implements Predicate<Map<Meta<?>, Set<ValidationElement>>> {
        overriddenHierarchy("overridden %s in inheritance hierarchy: %s") {

            @Override
            public boolean test(Map<Meta<?>, Set<ValidationElement>> detectedValidationElements) {
                boolean lowestFound = false;

                for (Set<ValidationElement> validated : detectedValidationElements.values()) {
                    if (lowestFound) {
                        return false;
                    }
                    lowestFound = !validated.isEmpty();
                }
                return true;
            }
        },
        unrelatedInheritance("declared %s in unrelated inheritance hierarchies: %s") {

            @Override
            public boolean test(Map<Meta<?>, Set<ValidationElement>> detectedValidationElements) {
                final Set<Class<?>> interfaces = detectedValidationElements.keySet().stream().map(Meta::getDeclaringClass)
                        .filter(Class::isInterface).collect(Collectors.toSet());
                if (interfaces.isEmpty()) {
                    return true;
                }
                final boolean allRelated =
                    detectedValidationElements.keySet().stream().map(Meta::getDeclaringClass).allMatch(ifc -> interfaces
                        .stream().filter(Predicate.isEqual(ifc).negate()).allMatch(ifc2 -> related(ifc, ifc2)));

                return allRelated;
            }
        };
        //@formatter:on

        final String format;

        private StrengtheningIssue(String format) {
            this.format = "Illegal strengthening: " + format;
        }

        Supplier<String> messageFor(Map<Meta<?>, Set<ValidationElement>> detectedValidationElements) {
            return () -> {
                final Set<ValidationElement> validationElements = detectedValidationElements.values().stream()
                    .flatMap(Collection::stream).collect(Collectors.toSet());

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

    static void validateContainerHierarchy(List<? extends ContainerDelegate<?>> delegates, ElementKind elementKind) {
        if (Validate.notNull(delegates, "delegates").isEmpty()) {
            return;
        }
        if (elementKind == ElementKind.CONTAINER_ELEMENT) {
            elementKind = getContainer(delegates.get(0).getHierarchyElement());
        }
        switch (Validate.notNull(elementKind, "elementKind")) {
        case PROPERTY:
            break;
        case RETURN_VALUE:
            noRedeclarationOfReturnValueCascading(delegates);
            break;
        case PARAMETER:
            noStrengtheningOfPreconditions(delegates, detectConstraints(), detectCascading(), detectGroupConversion());
            break;
        default:
            Exceptions.raise(IllegalArgumentException::new, "Cannot validate %s.%s as %s",
                ElementKind.class.getSimpleName(), elementKind, ContainerDelegate.class.getSimpleName());
        }
    }

    static void validateCrossParameterHierarchy(List<? extends ElementDelegate<?, ?>> delegates) {
        if (Validate.notNull(delegates, "delegates").isEmpty()) {
            return;
        }
        noStrengtheningOfPreconditions(delegates, detectConstraints());
    }

    private static ElementKind getContainer(Meta<?> meta) {
        Meta<?> m = meta;
        while (m.getElementType() == ElementType.TYPE_USE) {
            m = ((Meta.ForContainerElement) m).getParent();
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

    private static void noRedeclarationOfReturnValueCascading(List<? extends ContainerDelegate<?>> delegates) {
        final Set<Meta<?>> markedForCascade = delegates.stream().filter(ContainerDelegate::isCascade)
            .map(HierarchyDelegate::getHierarchyElement).collect(Collectors.toCollection(LinkedHashSet::new));

        Exceptions.raiseIf(markedForCascade.size() > 1, ConstraintDeclarationException::new,
            "Multiple return values marked @%s in same hierarchy: %s", Valid.class.getSimpleName(), markedForCascade);
    }

    @SafeVarargs
    private static <D extends ElementDelegate<?, ?>> void noStrengtheningOfPreconditions(List<? extends D> delegates,
        Function<? super D, ValidationElement>... detectors) {

        final Map<Meta<?>, Set<ValidationElement>> detectedValidationElements = new LinkedHashMap<>();
        delegates.forEach(d -> {
            detectedValidationElements.put(d.getHierarchyElement(),
                Stream.of(detectors).map(dt -> dt.apply(d)).filter(Objects::nonNull)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(ValidationElement.class))));
        });
        if (detectedValidationElements.values().stream().allMatch(Collection::isEmpty)) {
            // nothing declared
            return;
        }
        for (StrengtheningIssue s : StrengtheningIssue.values()) {
            s.check(detectedValidationElements);
        }
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

    private Liskov() {
    }
}
