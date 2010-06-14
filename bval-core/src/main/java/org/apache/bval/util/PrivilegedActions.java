/**
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

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
     * create a new instance.
     *
     * @param cls - the class (no interface, non-abstract, has accessible default no-arg-constructor)
     * @return a new instance
     * @throws IllegalArgumentException on any error to wrap target exceptions.
     */
    public static <T> T newInstance(final Class<T> cls) {
        return newInstance(cls, IllegalArgumentException.class);
    }

    public static <T, E extends RuntimeException> T newInstance(final Class<T> cls,
                                                                final Class<E> exception,
                                                                final Class<?>[] paramTypes,
                                                                final Object[] values) {
        return run(new PrivilegedAction<T>() {
            public T run() {
                try {
                    Constructor<T> cons = cls.getConstructor(paramTypes);
                    if (!cons.isAccessible()) {
                        cons.setAccessible(true);
                    }
                    return cons.newInstance(values);
                } catch (Exception e) {
                    throw newException("Cannot instantiate : " + cls, exception, e);
                }
            }
        });
    }

    /**
     * create a new instance of the class using the default no-arg constructor.
     * perform newInstance() call with AccessController.doPrivileged() if possible.
     *
     * @param cls       - the type to create a new instance from
     * @param exception - type of exception to throw when newInstance() call fails
     * @return the new instance of 'cls'
     */
    public static <T, E extends RuntimeException> T newInstance(final Class<T> cls,
                                                                final Class<E> exception) {
        return run(new PrivilegedAction<T>() {
            public T run() {
                try {
                    return cls.newInstance();
                } catch (Exception e) {
                    throw newException("Cannot instantiate : " + cls, exception, e);
                }
            }


        });
    }

    private static <E extends RuntimeException> RuntimeException newException(String msg,
                                                                              final Class<E> exception,
                                                                              Throwable e) {
        try {
            Constructor<E> co = exception.getConstructor(String.class, Throwable.class);
            try {
                return co.newInstance(msg, e);
            } catch (Exception e1) {
                //noinspection ThrowableInstanceNeverThrown
                return new RuntimeException(msg, e); // fallback
            }
        } catch (NoSuchMethodException e1) {
            //noinspection ThrowableInstanceNeverThrown
            return new RuntimeException(msg, e); // fallback
        }
    }

    /**
     * perform action with AccessController.doPrivileged() if possible.
     *
     * @param action - the action to run
     * @return result of running the action
     */
    public static <T> T run(PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
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
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty(name);
            }
        });
    }

}

