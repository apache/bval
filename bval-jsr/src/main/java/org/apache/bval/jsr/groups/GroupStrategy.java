/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bval.jsr.groups;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Validate;

/**
 * Group strategy interface.
 */
public interface GroupStrategy {
    public static class Simple implements GroupStrategy {
        private final Set<Group> groups;

        private Simple(Set<Group> groups) {
            this.groups = groups;
        }

        @Override
        public Set<Group> getGroups() {
            return groups;
        }

        @Override
        public GroupStrategy redefining(Map<Group, ? extends GroupStrategy> redefinitions) {
            if (Collections.disjoint(redefinitions.keySet(), groups)) {
                return this;
            }
            return groups.stream().map(g -> redefinitions.containsKey(g) ? redefinitions.get(g) : g)
                .collect(Collectors.collectingAndThen(Collectors.toList(), GroupStrategy::composite));
        }

        @Override
        public int hashCode() {
            return groups.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this
                || obj != null && obj.getClass().equals(getClass()) && ((Simple) obj).groups.equals(groups);
        }

        @Override
        public String toString() {
            return groups.toString();
        }
    }

    public static class Composite implements GroupStrategy {
        private final Set<? extends GroupStrategy> components;
        protected final boolean ordered;

        public Composite(Collection<? extends GroupStrategy> components, boolean ordered) {
            Validate.isTrue(Validate.notNull(components).stream().noneMatch(Objects::isNull),
                "null component not permitted");
            this.components = new LinkedHashSet<>(components);
            this.ordered = ordered;
        }

        @Override
        public Set<Group> getGroups() {
            return components.stream().map(GroupStrategy::getGroups).flatMap(Collection::stream)
                .collect(ToUnmodifiable.set());
        }

        @Override
        public GroupStrategy redefining(Map<Group, ? extends GroupStrategy> redefinitions) {
            if (!components.isEmpty()) {
                final Set<GroupStrategy> redef =
                    components.stream().map(cmp -> cmp.redefining(redefinitions)).collect(Collectors.toSet());
                if (!redef.equals(components)) {
                    return new Composite(redef, ordered);
                }
            }
            return this;
        }

        @Override
        public boolean applyTo(Predicate<GroupStrategy> operation) {
            if (components.isEmpty()) {
                return true;
            }
            final boolean applyAll = !ordered;
            boolean result = true;
            for (GroupStrategy gs : components) {
                result = gs.applyTo(operation) && result;
                if (!(applyAll || result)) {
                    return false;
                }
            }
            return result;
        }

        @Override
        public int hashCode() {
            return Objects.hash(components, ordered);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !obj.getClass().equals(getClass())) {
                return false;
            }
            final Composite other = (Composite) obj;
            return other.components.equals(components) && other.ordered == ordered;
        }

        @Override
        public String toString() {
            return String.format("%sordered: %s", ordered ? "" : "un", components);
        }
    }

    public static final GroupStrategy EMPTY = new GroupStrategy() {

        @Override
        public GroupStrategy redefining(Map<Group, ? extends GroupStrategy> redefinitions) {
            return this;
        }

        @Override
        public Set<Group> getGroups() {
            return Collections.emptySet();
        }
    };

    public static GroupStrategy redefining(GroupStrategy source, Map<Group, ? extends GroupStrategy> redefinitions) {
        Validate.notNull(source, "source");

        if (!(redefinitions == null || redefinitions.isEmpty())) {
            if (redefinitions.containsValue(null)) {
                redefinitions = redefinitions.entrySet().stream().filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            if (!redefinitions.isEmpty()) {
                return source.redefining(redefinitions);
            }
        }
        return source;
    }

    public static GroupStrategy simple(Group... groups) {
        return simple(Arrays.asList(groups));
    }

    public static GroupStrategy simple(Collection<? extends Group> coll) {
        Validate.notNull(coll);
        if (coll.size() == 1) {
            return coll.iterator().next();
        }
        final Set<Group> groups = Collections.unmodifiableSet(new LinkedHashSet<>(coll));
        return new Simple(groups);
    }

    public static GroupStrategy composite(GroupStrategy... components) {
        return composite(Arrays.asList(components));
    }

    public static GroupStrategy composite(Collection<? extends GroupStrategy> components) {
        if (components.isEmpty()) {
            return EMPTY;
        }
        if (components.size() == 1) {
            return components.iterator().next();
        }
        final Set<GroupStrategy> compressedComponents = new LinkedHashSet<>();

        final Consumer<Set<Group>> addGroups = s -> {
            if (!s.isEmpty()) {
                compressedComponents.add(simple(s));
                s.clear();
            }
        };
        final Set<Group> unorderedGroups = new HashSet<>();
        for (GroupStrategy component : components) {
            if (component instanceof Composite && ((Composite) component).ordered) {
                addGroups.accept(unorderedGroups);
                compressedComponents.add(component);
                continue;
            }
            unorderedGroups.addAll(component.getGroups());
        }
        addGroups.accept(unorderedGroups);
        if (compressedComponents.size() == 1) {
            return compressedComponents.iterator().next();
        }
        return new Composite(compressedComponents, false);
    }

    /**
     * Get the associated groups.
     * @return {@link Set} of {@link Group}
     */
    Set<Group> getGroups();

    /**
     * Get an equivalent strategy making group substitutions specified by {@code redefinitions}.
     * @param redefinitions
     * @return {@link GroupStrategy}
     */
    GroupStrategy redefining(Map<Group, ? extends GroupStrategy> redefinitions);

    /**
     * Apply the specified {@code boolean}-returning {@code operation}.
     * @param operation
     * @return {@code boolean}
     */
    default boolean applyTo(Predicate<GroupStrategy> operation) {
        return operation.test(this);
    }
}
