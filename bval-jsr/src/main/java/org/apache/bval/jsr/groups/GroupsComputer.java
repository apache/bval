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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.GroupDefinitionException;
import jakarta.validation.GroupSequence;
import jakarta.validation.ValidationException;
import jakarta.validation.groups.Default;
import jakarta.validation.metadata.GroupConversionDescriptor;

import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;

/**
 * Description: compute group order, based on the RI behavior as to guarantee
 * compatibility with interpretations of the spec.<br/>
 * Implementation is thread-safe.
 */
public class GroupsComputer {
    public static final Class<?>[] DEFAULT_GROUP = { Default.class };

    /**
     * The default group array used in case any of the validate methods is
     * called without a group.
     */
    public static final Groups DEFAULT_GROUPS;
    static {
        DEFAULT_GROUPS = new Groups();
        for (Class<?> g : DEFAULT_GROUP) {
            DEFAULT_GROUPS.insertGroup(new Group(g));
        }
    }

    /** caching resolved groups in a thread-safe map. */
    private final Map<Class<?>, List<Group>> resolvedSequences = new ConcurrentHashMap<>();

    /**
     * Compute groups from an array of group classes.
     * 
     * @param groups
     * @return {@link Groups}
     */
    @SafeVarargs
    public final Groups computeGroups(Class<?>... groups) {
        Exceptions.raiseIf(groups == null, IllegalArgumentException::new, "null validation groups specified");

        if (groups.length == 0 || (groups.length == 1 && groups[0] == Default.class)) {
            return DEFAULT_GROUPS;
        }
        return computeGroups(Arrays.asList(groups));
    }

    /**
     * Compute groups for a single cascading validation taking into account the specified set of
     * {@link GroupConversionDescriptor}s.
     * 
     * @param groupConversions
     * @param group
     * @return {@link Groups}
     */
    @Deprecated
    public final Groups computeCascadingGroups(Set<GroupConversionDescriptor> groupConversions, Class<?> group) {
        final Groups preliminaryResult = computeGroups(Stream.of(group));

        final Map<Class<?>, Class<?>> gcMap = groupConversions.stream()
            .collect(Collectors.toMap(GroupConversionDescriptor::getFrom, GroupConversionDescriptor::getTo));

        final boolean simpleGroup = preliminaryResult.getSequences().isEmpty();

        // conversion of a simple (non-sequence) group:
        if (simpleGroup && gcMap.containsKey(group)) {
            return computeGroups(Stream.of(gcMap.get(group)));
        }
        final Groups result = new Groups();

        if (simpleGroup) {
            // ignore group inheritance from initial argument as that is handled elsewhere:
            result.insertGroup(preliminaryResult.getGroups().iterator().next());
        } else {
            // expand group sequence conversions in place:

            for (Group.Sequence seq : preliminaryResult.getSequences()) {
                final List<Group> converted = new ArrayList<>();
                for (Group gg : seq.getGroups()) {
                    final Class<?> c = gg.getGroup();
                    if (gcMap.containsKey(c)) {
                        final Groups convertedGroupExpansion = computeGroups(Stream.of(gcMap.get(c)));
                        if (convertedGroupExpansion.getSequences().isEmpty()) {
                            converted.add(gg);
                        } else {
                            convertedGroupExpansion.getSequences().stream().map(Group.Sequence::getGroups)
                                .flatMap(Collection::stream).forEach(converted::add);
                        }
                    }
                }
                result.insertSequence(converted);
            }
        }
        return result;
    }

    /**
     * Compute groups from a {@link Collection}.
     * 
     * @param groups
     * @return {@link Groups}
     */
    public Groups computeGroups(Collection<Class<?>> groups) {
        Validate.notNull(groups, "groups");

        if (groups.isEmpty() || (groups.size() == 1 && groups.contains(Default.class))) {
            return DEFAULT_GROUPS;
        }
        return computeGroups(groups.stream());
    }

    /**
     * Compute groups from a {@link Stream}.
     * 
     * @param groups
     * @return {@link Groups}
     */
    public Groups computeGroups(Stream<Class<?>> groups) {
        final Groups result = new Groups();

        groups.peek(g -> {
            Exceptions.raiseIf(g == null, IllegalArgumentException::new, "Null group specified");

            Exceptions.raiseUnless(g.isInterface(), ValidationException::new,
                "A group must be an interface. %s is not.", g);

        }).forEach(g -> {
            final GroupSequence anno = g.getAnnotation(GroupSequence.class);
            if (anno == null) {
                result.insertGroup(new Group(g));
                insertInheritedGroups(g, result);
            } else {
                result.insertSequence(
                    resolvedSequences.computeIfAbsent(g, gg -> resolveSequence(gg, anno, new HashSet<>())));
            }
        });
        if (Arrays.asList(DEFAULT_GROUP).equals(result.getGroups()) && result.getSequences().isEmpty()) {
            return DEFAULT_GROUPS;
        }
        return result;
    }

    private void insertInheritedGroups(Class<?> clazz, Groups chain) {
        for (Class<?> extendedInterface : clazz.getInterfaces()) {
            chain.insertGroup(new Group(extendedInterface));
            insertInheritedGroups(extendedInterface, chain);
        }
    }

    private List<Group> resolveSequence(Class<?> group, GroupSequence sequenceAnnotation,
        Set<Class<?>> processedSequences) {
        Exceptions.raiseUnless(processedSequences.add(group), GroupDefinitionException::new,
            "Cyclic dependency in groups definition");

        final List<Group> resolvedGroupSequence = new ArrayList<>();
        for (Class<?> clazz : sequenceAnnotation.value()) {
            final GroupSequence anno = clazz.getAnnotation(GroupSequence.class);
            if (anno == null) {
                // group part of sequence
                resolvedGroupSequence.add(new Group(clazz));
            } else {
                // recursion!
                resolvedGroupSequence.addAll(resolveSequence(clazz, anno, processedSequences));
            }
        }
        return resolvedGroupSequence;
    }
}
