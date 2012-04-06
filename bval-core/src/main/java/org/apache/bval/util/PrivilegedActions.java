/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.apache.commons.lang3.ClassUtils;

/**
 * Description: utility methods to perform actions with AccessController or without. <br/>
 */
public class PrivilegedActions {
    private static String lineSeparator = null;
    private static String pathSeparator = null;

    /**
     * Return the value of the "line.separator" system property.
     * 
     * Requires security policy: 
     *   'permission java.util.PropertyPermission "read";'
     */
    @Deprecated // unused method - will remove in future release
    public static final String getLineSeparator() {
        if (lineSeparator == null) {
            lineSeparator =
                AccessController.doPrivileged(new PrivilegedAction<String>() {
                    public String run() {
                        return System.getProperty("line.separator");
                    }
                });
        }
        return lineSeparator;
    }

    /**
     * Return the value of the "path.separator" system property.
     * 
     * Requires security policy:
     *   'permission java.util.PropertyPermission "read";'
     */
    @Deprecated // unused method - will remove in future release
    public static final String getPathSeparator() {
        if (pathSeparator == null) {
            pathSeparator =
                AccessController.doPrivileged(new PrivilegedAction<String>() {
                    public String run() {
                        return System.getProperty("path.separator");
                    }
                });
        }
        return pathSeparator;
    }

    /**
     * Perform action with AccessController.doPrivileged() if security if enabled.
     *
     * @param action - the action to run
     * @return result of running the action
     */
    // should not be called by just anyone; do not increase access
    private static <T> T run(PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
    }

    /**
     * Perform action with AccessController.doPrivileged() if security if enabled.
     *
     * @param action - the action to run
     * @return result of running the action
     */
    // should not be called by just anyone; do not increase access
    private static <T> T run(final PrivilegedExceptionAction<T> action) throws PrivilegedActionException, Exception {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
    }

    /**
     * Perform AccessController.doPrivileged() action for ClassUtil.getClass()
     * 
     * @return Class
     * @exception Exception
     */
    public static Class<?> getClass(final ClassLoader classLoader, final String className) throws Exception {
        return run(new PrivilegedExceptionAction<Class<?>>() {
            public Class<?> run() throws Exception {
                return ClassUtils.getClass(classLoader, className, true);
            }
        });
    }

    /**
     * Return a PrivilegedAction object for clazz.getDeclaredMethod().invoke().
     * 
     * Requires security policy
     *  'permission java.lang.RuntimePermission "accessDeclaredMembers";'
     *  'permission java.lang.reflect.ReflectPermission "suppressAccessChecks";'
     *   
     * @return Object
     * @exception IllegalAccessException, InvocationTargetException
     */
    public static Object getAnnotationValue(final Annotation annotation, final String name)
          throws IllegalAccessException, InvocationTargetException {
        return run(new PrivilegedAction<Object>() {
            public Object run() {
                Method valueMethod;
                try {
                    valueMethod = annotation.annotationType().getDeclaredMethod(name);
                } catch (NoSuchMethodException ex) {
                    // do nothing
                    valueMethod = null;
                }
                if (null != valueMethod) {
                    try {
                        valueMethod.setAccessible(true);
                        return valueMethod.invoke(annotation);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }
        });
    }

    /**
     * Return a PrivilegeAction object for clazz.getClassloader().
     * 
     * Requires security policy:
     *   'permission java.lang.RuntimePermission "getClassLoader";'
     *   
     * @return Classloader
     */
    public static ClassLoader getClassLoader(final Class<?> clazz) {
        return run(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = clazz.getClassLoader();
                }
                return cl;
            }
        });
    }

    /**
     * Return a PrivilegeAction object for System.getProperty().
     * 
     * Requires security policy:
     *   'permission java.util.PropertyPermission "read";'
     *   
     * @return String
     */
    public static final String getProperty(final String name) {
        return run(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty(name);
            }
        });
    }

}

