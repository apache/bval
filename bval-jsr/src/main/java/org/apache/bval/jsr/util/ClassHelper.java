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
package org.apache.bval.jsr.util;

import java.io.Serializable;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Common operations on classes that do not require an {@link AccessController}.
 * 
 * @author Carlos Vara
 */
public class ClassHelper {
    private static final Set<Class<?>> IGNORED_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(null,Object.class,Serializable.class,Cloneable.class)));

    private ClassHelper() {
        // No instances please
    }

    /**
     * Fill the list with the full class/interface hierarchy of the given class.
     * List is ordered from the most to less specific.
     *
     * @param allClasses
     *            The current list of classes in the hierarchy.
     * @param clazz
     */
    public static List<Class<?>> fillFullClassHierarchyAsList(List<Class<?>> allClasses, Class<?> clazz) {
        if (IGNORED_TYPES.contains(clazz) || allClasses.contains(clazz)) {
            return allClasses;
        }
        allClasses.add(clazz);
        fillFullClassHierarchyAsList(allClasses, clazz.getSuperclass());
        for (Class<?> subClass : clazz.getInterfaces()) {
            fillFullClassHierarchyAsList(allClasses, subClass);
        }
        return allClasses;
    }

}
