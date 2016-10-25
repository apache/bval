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

import javax.validation.GroupDefinitionException;
import javax.validation.GroupSequence;
import javax.validation.ValidationException;
import javax.validation.groups.Default;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description: compute group order, based on the RI behavior as to guarantee
 * compatibility with interpretations of the spec.<br/>
 * Implementation is thread-safe.
 */
public class GroupsComputer {
    public static final Class<?>[] DEFAULT_GROUP = new Class<?>[] { Default.class };

    /** The default group array used in case any of the validate methods is called without a group. */
    private static final Groups DEFAULT_GROUPS;
    static {
        DEFAULT_GROUPS = new GroupsComputer().computeGroups(Arrays.asList(DEFAULT_GROUP));
    }

    /** caching resolved groups in a thread-safe map. */
    private final Map<Class<?>, List<Group>> resolvedSequences = new ConcurrentHashMap<Class<?>, List<Group>>();

    /**
     * Compute groups from an array of group classes.
     * @param groups
     * @return {@link Groups}
     */
    public Groups computeGroups(Class<?>[] groups) {
        if (groups == null) {
            throw new IllegalArgumentException("null passed as group");
        }

        // if no groups is specified use the default
        if (groups.length == 0) {
            return DEFAULT_GROUPS;
        }

        return computeGroups(Arrays.asList(groups));
    }

    /**
     * Main compute implementation.
     * @param groups
     * @return {@link Groups}
     */
    protected Groups computeGroups(Collection<Class<?>> groups) {
        if (groups == null || groups.size() == 0) {
            throw new IllegalArgumentException("At least one group has to be specified.");
        }

        for (final Class<?> clazz : groups) {
            if (clazz == null) {
                throw new IllegalArgumentException("At least one group has to be specified.");
            }

            if (!clazz.isInterface()) {
                throw new ValidationException("A group has to be an interface. " + clazz.getName() + " is not.");
            }
        }

        Groups chain = new Groups();
        for (Class<?> clazz : groups) {
            GroupSequence anno = clazz.getAnnotation(GroupSequence.class);
            if (anno == null) {
                Group group = new Group(clazz);
                chain.insertGroup(group);
                insertInheritedGroups(clazz, chain);
            } else {
                insertSequence(clazz, anno, chain);
            }
        }

        return chain;
    }

    private void insertInheritedGroups(Class<?> clazz, Groups chain) {
        for (Class<?> extendedInterface : clazz.getInterfaces()) {
            Group group = new Group(extendedInterface);
            chain.insertGroup(group);
            insertInheritedGroups(extendedInterface, chain);
        }
    }

    private void insertSequence(Class<?> clazz, GroupSequence anno, Groups chain) {
        List<Group> sequence;
        if (resolvedSequences.containsKey(clazz)) {
            sequence = resolvedSequences.get(clazz);
        } else {
            sequence = resolveSequence(clazz, anno, new HashSet<Class<?>>());
        }
        chain.insertSequence(sequence);
    }

    private List<Group> resolveSequence(Class<?> group, GroupSequence sequenceAnnotation,
        Set<Class<?>> processedSequences) {
        if (processedSequences.contains(group)) {
            throw new GroupDefinitionException("Cyclic dependency in groups definition");
        } else {
            processedSequences.add(group);
        }
        List<Group> resolvedGroupSequence = new LinkedList<Group>();
        Class<?>[] sequenceArray = sequenceAnnotation.value();
        for (Class<?> clazz : sequenceArray) {
            GroupSequence anno = clazz.getAnnotation(GroupSequence.class);
            if (anno == null) {
                resolvedGroupSequence.add(new Group(clazz)); // group part of sequence
            } else {
                List<Group> tmpSequence = resolveSequence(clazz, anno, processedSequences); // recursion!
                resolvedGroupSequence.addAll(tmpSequence);
            }
        }
        resolvedSequences.put(group, resolvedGroupSequence);
        return resolvedGroupSequence;
    }
}
