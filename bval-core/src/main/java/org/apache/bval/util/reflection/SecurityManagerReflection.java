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
package org.apache.bval.util.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

public class SecurityManagerReflection extends DefaultReflection implements Reflection {
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
    private static <T> T run(final PrivilegedExceptionAction<T> action) throws Exception {
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
    public Class<?> getClass(final ClassLoader classLoader, final String className) throws Exception {
        return run(new PrivilegedExceptionAction<Class<?>>() {
            public Class<?> run() throws Exception {
                return SecurityManagerReflection.super.getClass(classLoader, className);
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
    public Object getAnnotationValue(final Annotation annotation, final String name) throws IllegalAccessException, InvocationTargetException {
        return run(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    return SecurityManagerReflection.super.getAnnotationValue(annotation, name);
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
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
    public ClassLoader getClassLoader(final Class<?> clazz) {
        return run(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return SecurityManagerReflection.super.getClassLoader(clazz);
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
    public final String getProperty(final String name) {
        return run(new PrivilegedAction<String>() {
            public String run() {
                return SecurityManagerReflection.super.getProperty(name);
            }
        });
    }

    public Field getDeclaredField(final Class<?> clazz, final String fieldName) {
        return run(new PrivilegedAction<Field>() {
            public Field run() {
                return SecurityManagerReflection.super.getDeclaredField(clazz, fieldName);
            }
        });
    }

    public Field[] getDeclaredFields(final Class<?> clazz) {
        return run(new PrivilegedAction<Field[]>() {
            public Field[] run() {
                return SecurityManagerReflection.super.getDeclaredFields(clazz);
            }
        });
    }

    public Constructor<?> getDeclaredConstructor(final Class<?> clazz, final Class<?>... parameters) {
        return run(new PrivilegedAction<Constructor<?>>() {
            public Constructor<?> run() {
                return SecurityManagerReflection.super.getDeclaredConstructor(clazz, parameters);
            }
        });
    }

    public Method getDeclaredMethod(final Class<?> clazz, final String name, final Class<?>... parameters) {
        return run(new PrivilegedAction<Method>() {
            public Method run() {
                return SecurityManagerReflection.super.getDeclaredMethod(clazz, name, parameters);
            }
        });
    }

    public Method[] getDeclaredMethods(final Class<?> clazz) {
        return run(new PrivilegedAction<Method[]>() {
            public Method[] run() {
                return SecurityManagerReflection.super.getDeclaredMethods(clazz);
            }
        });
    }

    public Constructor<?>[] getDeclaredConstructors(final Class<?> clazz) {
        return run(new PrivilegedAction<Constructor<?>[]>() {
            public Constructor<?>[] run() {
                return SecurityManagerReflection.super.getDeclaredConstructors(clazz);
            }
        });
    }

    public Method getPublicMethod(final Class<?> clazz, final String methodName) {
        return run(new PrivilegedAction<Method>() {
            public Method run() {
                return SecurityManagerReflection.super.getPublicMethod(clazz, methodName);
            }
        });
    }

    public <T> T newInstance(final Class<T> cls) {
        return run(new PrivilegedAction<T>() {
            public T run() {
                return SecurityManagerReflection.super.newInstance(cls);
            }
        });
    }
}
