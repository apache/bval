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
package org.apache.bval.jsr303.groups;


import javax.validation.GroupSequence;
import javax.validation.ValidationException;
import javax.validation.groups.Default;
import java.util.*;

/**
 * Description: compute group order, based on the hibernate validator algorithm
 * to guarantee compatibility with interpretation of spec by reference implementation <br/>
 * Implementation is thread-safe.
 * User: roman <br/>
 * Date: 09.04.2009 <br/>
 * Time: 09:15:50 <br/>
 * Copyright: Agimatec GmbH
 */
public class GroupsComputer {
    /** The default group array used in case any of the validate methods is called without a group. */
    public static final Class<?>[] DEFAULT_GROUP_ARRAY = new Class<?>[]{Default.class};
    private static final Groups DEFAULT_GROUPS;

    static {
        DEFAULT_GROUPS =
              new GroupsComputer().computeGroups(Arrays.asList(DEFAULT_GROUP_ARRAY));
    }

    /** caching resolved groups in a thread-safe map. */
    private final Map<Class<?>, List<Group>> resolvedSequences =
          Collections.synchronizedMap(new HashMap<Class<?>, List<Group>>());

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


    protected Groups computeGroups(Collection<Class<?>> groups) {
        if (groups == null || groups.size() == 0) {
            throw new IllegalArgumentException("At least one group has to be specified.");
        }

        for (Class<?> clazz : groups) {
            if (!clazz.isInterface()) {
                throw new ValidationException(
                      "A group has to be an interface. " + clazz.getName() + " is not.");
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
            throw new ValidationException("Cyclic dependency in groups definition");
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
                List<Group> tmpSequence =
                      resolveSequence(clazz, anno, processedSequences);  // recursion!
                resolvedGroupSequence.addAll(tmpSequence);
            }
        }
        resolvedSequences.put(group, resolvedGroupSequence);
        return resolvedGroupSequence;
    }
}
