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
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.weaver.privilizer.Privilizing;

/**
 * Security-agnostic "blueprint" class for reflection-related operations. Intended for use by Apache BVal code.
 */
public class Reflection {
    /**
     * Get the named {@link Class} from the specified {@link ClassLoader}.
     * @param classLoader
     * @param className
     * @return Class
     * @throws Exception
     */
    public static Class<?> getClass(final ClassLoader classLoader, final String className) throws Exception {
        return ClassUtils.getClass(classLoader, className, true);
    }

    /**
     * Get the named value from the specified {@link Annotation}.
     * @param annotation
     * @param name
     * @return Object value
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Object getAnnotationValue(final Annotation annotation, final String name)
        throws IllegalAccessException, InvocationTargetException {
        final Method valueMethod;
        try {
            valueMethod = annotation.annotationType().getDeclaredMethod(name);
        } catch (final NoSuchMethodException ex) {
            // do nothing
            return null;
        }
        final boolean mustUnset = setAccessible(valueMethod, true);
        try {
            return valueMethod.invoke(annotation);
        } finally {
            if (mustUnset) {
                setAccessible(valueMethod, false);
            }
        }
    }

    /**
     * Get a usable {@link ClassLoader}: that of {@code clazz} if {@link Thread#getContextClassLoader()} returns {@code null}.
     * @param clazz
     * @return {@link ClassLoader}
     */
    public static ClassLoader getClassLoader(final Class<?> clazz) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl == null ? clazz.getClassLoader() : cl;
    }

    /**
     * Convenient point for {@link Privilizing} {@link System#getProperty(String)}.
     * @param name
     * @return String
     */
    public static String getProperty(final String name) {
        return System.getProperty(name);
    }

    /**
     * Get the declared field from {@code clazz}.
     * @param clazz
     * @param fieldName
     * @return {@link Field} or {@code null}
     */
    public static Field getDeclaredField(final Class<?> clazz, final String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (final NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * Convenient point for {@link Privilizing} {@link Class#getDeclaredFields()}.
     * @param clazz
     * @return {@link Field} array
     */
    public static Field[] getDeclaredFields(final Class<?> clazz) {
        return clazz.getDeclaredFields();
    }

    /**
     * Get the declared constructor from {@code clazz}.
     * @param T generic type
     * @param clazz
     * @param parameters
     * @return {@link Constructor} or {@code null}
     */
    public static <T> Constructor<T> getDeclaredConstructor(final Class<T> clazz, final Class<?>... parameters) {
        try {
            return clazz.getDeclaredConstructor(parameters);
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Get the declared method from {@code clazz}.
     * @param clazz
     * @param name
     * @param parameters
     * @return {@link Method} or {@code null}
     */
    public static Method getDeclaredMethod(final Class<?> clazz, final String name, final Class<?>... parameters) {
        try {
            return clazz.getDeclaredMethod(name, parameters);
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Convenient point for {@link Privilizing} {@link Class#getDeclaredMethods()}.
     * @param clazz
     * @return {@link Method} array
     */
    public static Method[] getDeclaredMethods(final Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }

    /**
     * Convenient point for {@link Privilizing} {@link Class#getDeclaredConstructors()}.
     * @param clazz
     * @return {@link Constructor} array
     */
    public static Constructor<?>[] getDeclaredConstructors(final Class<?> clazz) {
        return clazz.getDeclaredConstructors();
    }

    /**
     * Get the specified {@code public} {@link Method} from {@code clazz}.
     * @param clazz
     * @param methodName
     * @return {@link Method} or {@code null}
     */
    public static Method getPublicMethod(final Class<?> clazz, final String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Construct a new instance of {@code cls} using its default constructor.
     * @param cls
     * @return T
     */
    public static <T> T newInstance(final Class<T> cls) {
        try {
            return cls.newInstance();
        } catch (final Exception ex) {
            throw new RuntimeException("Cannot instantiate : " + cls, ex);
        }
    }

    /**
     * Set the accessibility of {@code o} to {@code accessible}. If running without a {@link SecurityManager}
     * and {@code accessible == false}, this call is ignored (because any code could reflectively make any
     * object accessible at any time).
     * @param o
     * @param accessible
     * @return whether a change was made.
     */
    public static boolean setAccessible(final AccessibleObject o, boolean accessible) {
        if (o == null || o.isAccessible() == accessible) {
            return false;
        }
        if (!accessible && System.getSecurityManager() == null) {
            return false;
        }
        final Member m = (Member) o;

        // For public members whose declaring classes are public, we need do nothing:
        if (Modifier.isPublic(m.getModifiers()) && Modifier.isPublic(m.getDeclaringClass().getModifiers())) {
            return false;
        }
        o.setAccessible(accessible);
        return true;
    }

}
