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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.GroupDefinitionException;
import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.ElementDescriptor;
import jakarta.validation.metadata.ElementDescriptor.ConstraintFinder;
import jakarta.validation.metadata.Scope;

import org.apache.bval.jsr.groups.Group;
import org.apache.bval.jsr.groups.Groups;
import org.apache.bval.jsr.groups.GroupsComputer;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.Validate;

class Finder implements ConstraintFinder {
    private static Stream<Group> allGroups(Groups groups) {
        return Stream.concat(groups.getGroups().stream(),
            groups.getSequences().stream().map(Group.Sequence::getGroups).flatMap(Collection::stream));
    }

    private volatile Predicate<ConstraintD<?>> groups = c -> true;
    private volatile Predicate<ConstraintD<?>> scope;
    private volatile Predicate<ConstraintD<?>> elements;

    private final GroupsComputer groupsComputer;
    private final ElementDescriptor owner;
    private final Lazy<Groups> getDefaultSequence = new Lazy<>(this::computeDefaultSequence);
    private final Lazy<Class<?>> beanClass;

    Finder(GroupsComputer groupsComputer, ElementDescriptor owner) {
        this.groupsComputer = Validate.notNull(groupsComputer, "groupsComputer");
        this.owner = Validate.notNull(owner, "owner");
        this.beanClass = new Lazy<>(() -> firstAtomicElementDescriptor().getBean().getElementClass());
    }

    @Override
    public ConstraintFinder unorderedAndMatchingGroups(Class<?>... groups) {
        final Set<Class<?>> allGroups = computeAll(groups);
        this.groups = c -> !Collections.disjoint(allGroups, c.getGroups());
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
        return getConstraints().filter(filter()).collect(ToUnmodifiable.set());
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

    private ElementD<?,?> firstAtomicElementDescriptor() {
        return ComposedD.unwrap(owner, ElementD.class).findFirst().orElseThrow(IllegalStateException::new);
    }

    private Groups computeDefaultSequence() {
        final ElementD<?, ?> element = firstAtomicElementDescriptor();
        Collection<Class<?>> redef = 
        element.getGroupStrategy().getGroups().stream().map(Group::getGroup).collect(Collectors.toList());
        
        if (redef == null) {
            return GroupsComputer.DEFAULT_GROUPS;
        }
        final Class<?> t = this.beanClass.get();
        if (redef.contains(Default.class)) {
            Exceptions.raise(GroupDefinitionException::new, "%s for %s cannot include %s.class",
                GroupSequence.class.getSimpleName(), t, Default.class.getSimpleName());
        }
        redef = redef.stream()
                .map(substituteDefaultGroup())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return groupsComputer.computeGroups(redef);
    }

    private Set<Class<?>> computeAll(Class<?>[] groups) {
        final Groups preliminaryGroups = groupsComputer.computeGroups(Stream.of(groups).map(substituteDefaultGroup()));
        return allGroups(preliminaryGroups)
            .flatMap(g -> g.isDefault() ? allGroups(getDefaultSequence.get()) : Stream.of(g)).map(Group::getGroup)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private UnaryOperator<Class<?>> substituteDefaultGroup() {
        return t -> t.isAssignableFrom(beanClass.get()) ? Default.class : t;
    }
}
