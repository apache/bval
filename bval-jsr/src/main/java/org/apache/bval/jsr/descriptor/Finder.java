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

import java.lang.annotation.ElementType;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.groups.Default;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.ElementDescriptor.ConstraintFinder;
import javax.validation.metadata.Scope;

import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Validate;

class Finder implements ConstraintFinder, Supplier<Stream<ConstraintD<?>>> {
    private Predicate<ConstraintD<?>> groups = c -> true;
    private Predicate<ConstraintD<?>> scope;
    private Predicate<ConstraintD<?>> elements;

    private final ElementDescriptor owner;

    Finder(ElementDescriptor owner) {
        this.owner = Validate.notNull(owner, "owner");
    }

    @Override
    public ConstraintFinder unorderedAndMatchingGroups(Class<?>... groups) {
        this.groups = c -> Stream.of(groups).anyMatch(t -> {
            final Set<Class<?>> constraintGroups = c.getGroups();
            return constraintGroups.contains(t)
                || constraintGroups.contains(Default.class) && c.getDeclaringClass().isAssignableFrom(t);
        });
        return this;
    }

    @Override
    public ConstraintFinder lookingAt(Scope scope) {
        this.scope = scope == Scope.HIERARCHY ? null : c -> c.getScope() == scope;
        return this;
    }

    @Override
    public ConstraintFinder declaredOn(ElementType... types) {
        this.elements = c -> Stream.of(types).filter(Objects::nonNull)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(ElementType.class))).contains(c.getDeclaredOn());

        return this;
    }

    @Override
    public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        return get().collect(ToUnmodifiable.set());
    }

    @Override
    public Stream<ConstraintD<?>> get() {
        return getConstraints().filter(filter());
    }

    @Override
    public boolean hasConstraints() {
        return getConstraints().anyMatch(filter());
    }

    private Stream<ConstraintD<?>> getConstraints() {
        return owner.getConstraintDescriptors().stream().<ConstraintD<?>> map(c -> c.unwrap(ConstraintD.class));
    }

    private Predicate<ConstraintD<?>> filter() {
        Predicate<ConstraintD<?>> result = groups;
        if (scope != null) {
            result = result.and(scope);
        }
        if (elements != null) {
            result = result.and(elements);
        }
        return result;
    }
}
