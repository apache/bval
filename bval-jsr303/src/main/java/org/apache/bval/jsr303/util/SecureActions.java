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

import org.apache.bval.util.PrivilegedActions;
import org.apache.commons.lang.StringUtils;

import javax.validation.ValidationException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.PrivilegedAction;

/**
 * Description: utility methods to perform actions with AccessController or without.<br/>
 * User: roman <br/>
 * Date: 01.10.2009 <br/>
 * Time: 16:44:09 <br/>
 * Copyright: Agimatec GmbH
 */
public class SecureActions extends PrivilegedActions {

    /**
     * create a new instance of the class using the default no-arg constructor.
     * perform newInstance() call with AccessController.doPrivileged() if possible.
     *
     * @param cls - the class (no interface, non-abstract, has accessible default no-arg-constructor)
     * @return a new instance
     */
    public static <T> T newInstance(final Class<T> cls) {
        return newInstance(cls, ValidationException.class);
    }

    public static <T> T newInstance(final Class<T> cls, final Class<?>[] paramTypes,
                                    final Object[] values) {
        return newInstance(cls, ValidationException.class, paramTypes, values);
    }

    public static Class<?> loadClass(final String className, final Class<?> caller) {
        return run(new PrivilegedAction<Class<?>>() {
            public Class<?> run() {
                try {
                    ClassLoader contextClassLoader =
                          Thread.currentThread().getContextClassLoader();
                    if (contextClassLoader != null) {
                        return contextClassLoader.loadClass(className);
                    }
                } catch (Throwable e) {
                    // ignore
                }
                try {
                    return Class.forName(className, true, caller.getClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new ValidationException("Unable to load class: " + className, e);
                }
            }
        });
    }

    public static Field getDeclaredField(final Class<?> clazz, final String fieldName) {
        return run(new PrivilegedAction<Field>() {
            public Field run() {
                try {
                    Field f = clazz.getDeclaredField(fieldName);
                    setAccessibility(f);
                    return f;
                } catch (NoSuchFieldException e) {
                    return null;
                }
            }
        });
    }

    private static void setAccessibility(Field field) {
        if (!Modifier.isPublic(field.getModifiers()) || (
              Modifier.isPublic(field.getModifiers()) &&
                    Modifier.isAbstract(field.getModifiers()))) {
            field.setAccessible(true);
        }
    }

    /**
     * Returns the <b>public method</b> with the specified name or null if it does not exist.
     *
     * @return Returns the method or null if not found.
     */
    public static Method getGetter(final Class<?> clazz, final String methodName) {
        return run(new PrivilegedAction<Method>() {
            public Method run() {
                try {
                    String methodName0 = StringUtils.capitalize(methodName);
                    try {
                        return clazz.getMethod("get" + methodName0);
                    } catch (NoSuchMethodException e) {
                        return clazz.getMethod("is" + methodName0);
                    }
                } catch (NoSuchMethodException e) {
                    return null;
                }
            }
        });

    }

    public static Method getMethod(final Class<?> clazz, final String methodName) {
        return run(new PrivilegedAction<Method>() {
            public Method run() {
                try {
                    return clazz.getMethod(methodName);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            }
        });
    }

    public static Method[] getDeclaredMethods(final Class<?> clazz) {
        return run(new PrivilegedAction<Method[]>() {
            public Method[] run() {
                return clazz.getDeclaredMethods();
            }
        });
    }

    public static <T> Constructor<T> getConstructor(final Class<T> clazz,
                                                    final Class<?>... params) {
        return run(new PrivilegedAction<Constructor<T>>() {
            public Constructor<T> run() {
                try {
                    return clazz.getConstructor(params);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            }
        });
    }

}
