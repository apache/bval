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
package org.apache.bval.jsr.groups;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.validation.GroupDefinitionException;
import javax.validation.groups.Default;

import org.apache.bval.util.Exceptions;
import org.apache.bval.util.ObjectWrapper;

/**
 * Immutable object that wraps an interface representing a single group.
 */
public final class Group implements GroupStrategy {
    /**
     * Models a group sequence.
     */
    public static final class Sequence extends GroupStrategy.Composite {
        private static Set<Group> validGroups(Collection<Group> groups, Function<Group, ? extends GroupStrategy> mapper) {
            final Set<Group> result = new LinkedHashSet<>();
            final ObjectWrapper<Group> prev = new ObjectWrapper<>();

            groups.stream().map(g -> Optional.of(g).<GroupStrategy> map(mapper).orElse(g)).map(GroupStrategy::getGroups)
                .flatMap(Collection::stream).forEach(g -> {
                    // only permit duplicates if they are contiguous:
                    if (result.add(g)) {
                        prev.accept(g);
                        return;
                    }
                    if (!g.equals(prev.get())) {
                        Exceptions.raise(GroupDefinitionException::new, "Invalid group sequence %s specified", groups);
                    }
                });
            return result;
        }

        private final Set<Group> groups;

        private Sequence(Collection<Group> groups) {
            super(groups, true);
            this.groups = Collections.unmodifiableSet(validGroups(groups, Function.identity()));
        }

        @Override
        public Set<Group> getGroups() {
            return groups;
        }

        @Override
        public String toString() {
            return String.format("Group sequence: %s", groups);
        }

        @Override
        public GroupStrategy redefining(Map<Group, ? extends GroupStrategy> redefinitions) {
            if (Collections.disjoint(redefinitions.keySet(), groups)) {
                return this;
            }
            final Set<GroupStrategy> components = new LinkedHashSet<>();

            final Set<Group> mappedGroups;
            try {
                mappedGroups = validGroups(groups, g -> {
                    final GroupStrategy result = Optional.of(g).<GroupStrategy> map(redefinitions::get).orElse(g);
                    components.add(result);
                    return result;
                });
            } catch (GroupDefinitionException e) {
                throw Exceptions.create(GroupDefinitionException::new, "Could not expand %s using %s", this,
                    redefinitions);
            }
            if (components.equals(mappedGroups)) {
                return new Sequence(mappedGroups);
            }
            return new GroupStrategy.Composite(components, ordered);
        }
    }

    /**
     * the Default Group
     */
    public static final Group DEFAULT = new Group(Default.class);

    public static final Group of(Class<?> group) {
        return new Group(group);
    }

    public static final Sequence sequence(Group... groups) {
        return sequence(Arrays.asList(groups));
    }

    public static final Sequence sequence(Collection<Group> groups) {
        return new Sequence(groups);
    }

    private final Class<?> group;

    /**
     * Create a new Group instance.
     * @param group
     */
    public Group(Class<?> group) {
        this.group = group;
    }

    /**
     * Get the actual group class.
     * @return
     */
    public Class<?> getGroup() {
        return group;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s{group=%s}", Group.class.getSimpleName(), group);
    }

    /**
     * Learn whether the group represented is the default group.
     * @return boolean
     */
    public boolean isDefault() {
        return Default.class.equals(group);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass().equals(o.getClass()) && Objects.equals(group, ((Group) o).group);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return group.hashCode();
    }

    @Override
    public Set<Group> getGroups() {
        return Collections.singleton(this);
    }

    @Override
    public boolean applyTo(Predicate<GroupStrategy> operation) {
        return operation.test(this);
    }

    @Override
    public GroupStrategy redefining(Map<Group, ? extends GroupStrategy> redefinitions) {
        final GroupStrategy redefined = redefinitions.get(this);
        return redefined == null ? this : redefined;
    }
}
