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
package org.apache.bval.jsr303.util;

import org.apache.commons.lang3.ClassUtils;

import java.security.AccessController;
import java.util.List;

/**
 * Common operations on classes that do not require an {@link AccessController}.
 * 
 * @author Carlos Vara
 */
public class ClassHelper {

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
     *            The current class, root of the hierarchy to traverse.
     */
    static public void fillFullClassHierarchyAsList(List<Class<?>> allClasses, Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        if (allClasses.contains(clazz)) {
            return;
        }
        allClasses.add(clazz);
        fillFullClassHierarchyAsList(allClasses, clazz.getSuperclass());
        for (Class<?> subClass : clazz.getInterfaces()) {
            fillFullClassHierarchyAsList(allClasses, subClass);
        }
    }

    /**
     * @deprecated Will be removed for security reasons.
     *
     * Perform ClassUtils.getClass functions with Java 2 Security enabled.
     */
    @Deprecated
    public static Class<?> getClass(String className) throws ClassNotFoundException {
        return getClass(className, true);
    }

    /**
     * @deprecated Will be removed for security reasons.
     *
     * Perform ClassUtils.getClass functions with Java 2 Security enabled.
     */
    @Deprecated
    public static Class<?> getClass(String className, boolean initialize) throws ClassNotFoundException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null)
          loader = ClassHelper.class.getClassLoader();
        return ClassUtils.getClass(loader, className, initialize);
    }
}
