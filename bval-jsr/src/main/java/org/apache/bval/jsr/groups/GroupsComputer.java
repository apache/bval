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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.validation.GroupDefinitionException;
import javax.validation.GroupSequence;
import javax.validation.ValidationException;
import javax.validation.groups.Default;
import javax.validation.metadata.GroupConversionDescriptor;

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
    private static final Groups DEFAULT_GROUPS;
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
    public final Groups computeCascadingGroups(Set<GroupConversionDescriptor> groupConversions, Class<?> group) {
        final Groups preliminaryResult = computeGroups(group);

        final Map<Class<?>, Class<?>> gcMap = groupConversions.stream()
            .collect(Collectors.toMap(GroupConversionDescriptor::getFrom, GroupConversionDescriptor::getTo));

        final boolean simpleGroup = preliminaryResult.getSequences().isEmpty();

        // conversion of a simple (non-sequence) group:
        if (simpleGroup && gcMap.containsKey(group)) {
            return computeGroups(gcMap.get(group));
        }

        final Groups result = new Groups();

        if (simpleGroup) {
            // ignore group inheritance from initial argument as that is handled elsewhere:
            result.insertGroup(preliminaryResult.getGroups().get(0));
        } else {
            // expand group sequence conversions in place:

            for (List<Group> seq : preliminaryResult.getSequences()) {
                final List<Group> converted = new ArrayList<>();
                for (Group gg : seq) {
                    final Class<?> c = gg.getGroup();
                    if (gcMap.containsKey(c)) {
                        final Groups convertedGroupExpansion = computeGroups(gcMap.get(c));
                        if (convertedGroupExpansion.getSequences().isEmpty()) {
                            converted.add(gg);
                        } else {
                            convertedGroupExpansion.getSequences().stream().flatMap(Collection::stream)
                                .forEach(converted::add);
                        }
                    }
                }
                result.insertSequence(converted);
            }
        }
        return result;
    }

    /**
     * Main compute implementation.
     * 
     * @param groups
     * @return {@link Groups}
     */
    protected Groups computeGroups(Collection<Class<?>> groups) {
        Validate.notNull(groups, "groups");

        if (groups.isEmpty() || Arrays.asList(DEFAULT_GROUP).equals(new ArrayList<>(groups))) {
            return DEFAULT_GROUPS;
        }
        Exceptions.raiseIf(groups.stream().anyMatch(Objects::isNull), IllegalArgumentException::new,
            "Null group specified");

        for (final Class<?> clazz : groups) {
            Exceptions.raiseUnless(clazz.isInterface(), ValidationException::new,
                "A group must be an interface. %s is not.", clazz);
        }
        final Groups chain = new Groups();
        for (Class<?> clazz : groups) {
            final GroupSequence anno = clazz.getAnnotation(GroupSequence.class);
            if (anno == null) {
                chain.insertGroup(new Group(clazz));
                insertInheritedGroups(clazz, chain);
                continue;
            }
            chain.insertSequence(
                resolvedSequences.computeIfAbsent(clazz, g -> resolveSequence(g, anno, new HashSet<>())));
        }
        return chain;
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
