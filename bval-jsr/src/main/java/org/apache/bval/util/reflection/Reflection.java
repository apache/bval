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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.weaver.privilizer.Privilizing;

/**
 * Security-agnostic "blueprint" class for reflection-related operations. Intended for use by Apache BVal code.
 */
public class Reflection {
    /**
     * Inclusivity literals for {@link #hierarchy(Class, Interfaces)}.
     * Taken from commons-lang3.
     */
    public enum Interfaces {
        INCLUDE, EXCLUDE
    }

    private static final Object[][] NATIVE_CODES = new Object[][]{
            { byte.class, "byte", "B" },
            { char.class, "char", "C" },
            { double.class, "double", "D" },
            { float.class, "float", "F" },
            { int.class, "int", "I" },
            { long.class, "long", "J" },
            { short.class, "short", "S" },
            { boolean.class, "boolean", "Z" },
            { void.class, "void", "V" }
    };

    /**
     * Maps primitive {@code Class}es to their corresponding wrapper {@code Class}.
     */
    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP;
    static {
        final Map<Class<?>, Class<?>> m = new HashMap<>();
        m.put(Boolean.TYPE, Boolean.class);
        m.put(Byte.TYPE, Byte.class);
        m.put(Character.TYPE, Character.class);
        m.put(Short.TYPE, Short.class);
        m.put(Integer.TYPE, Integer.class);
        m.put(Long.TYPE, Long.class);
        m.put(Double.TYPE, Double.class);
        m.put(Float.TYPE, Float.class);
        m.put(Void.TYPE, Void.TYPE);
        PRIMITIVE_WRAPPER_MAP = Collections.unmodifiableMap(m);
    }

    /**
     * <p>Converts the specified primitive Class object to its corresponding
     * wrapper Class object.</p>
     *
     * <p>NOTE: From v2.2, this method handles {@code Void.TYPE},
     * returning {@code Void.TYPE}.</p>
     *
     * @param cls  the class to convert, may be null
     * @return the wrapper class for {@code cls} or {@code cls} if
     * {@code cls} is not a primitive. {@code null} if null input.
     * @since 2.1
     */
    public static Class<?> primitiveToWrapper(final Class<?> cls) {
        Class<?> convertedClass = cls;
        if (cls != null && cls.isPrimitive()) {
            convertedClass = PRIMITIVE_WRAPPER_MAP.get(cls);
        }
        return convertedClass;
    }

    public static Class<?> wrapperToPrimitive(final Class<?> cls) {
        for (Map.Entry<Class<?>, Class<?>> primitiveEntry : PRIMITIVE_WRAPPER_MAP.entrySet()) {
            if (primitiveEntry.getValue().equals(cls)) {
                return primitiveEntry.getKey();
            }
        }
        return null;
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

    public static Class<?> toClass(String className) throws ClassNotFoundException {
        ClassLoader cl = getClassLoader(Reflection.class);
        return toClass(className, cl);
    }

    /**
     * Return the class for the given string, correctly handling
     * primitive types. If the given class loader is null, the context
     * loader of the current thread will be used.
     *
     * @throws RuntimeException on load error
     */
    public static Class<?> toClass(String className, ClassLoader loader) throws ClassNotFoundException {
        return toClass(className, false, loader);
    }

    /**
     * Return the class for the given string, correctly handling
     * primitive types. If the given class loader is null, the context
     * loader of the current thread will be used.
     *
     * @throws RuntimeException on load error
     */
    public static Class<?> toClass(String className, boolean resolve, ClassLoader loader) throws ClassNotFoundException {
        if (className == null) {
            throw new NullPointerException("className == null");
        }

        // array handling
        int dims = 0;
        while (className.endsWith("[]")) {
            dims++;
            className = className.substring(0, className.length() - 2);
        }

        // check against primitive types
        boolean primitive = false;
        if (className.indexOf('.') == -1) {
            for (int i = 0; !primitive && (i < NATIVE_CODES.length); i++) {
                if (NATIVE_CODES[i][1].equals(className)) {
                    if (dims == 0) {
                        return (Class<?>) NATIVE_CODES[i][0];
                    }
                    className = (String) NATIVE_CODES[i][2];
                    primitive = true;
                }
            }
        }

        if (dims > 0) {
            StringBuilder buf = new StringBuilder(className.length() + dims + 2);
            for (int i = 0; i < dims; i++) {
                buf.append('[');
            }
            if (!primitive) {
                buf.append('L');
            }
            buf.append(className);
            if (!primitive) {
                buf.append(';');
            }
            className = buf.toString();
        }

        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        return Class.forName(className, resolve, loader);
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
     * Perform a search against the class hierarchy.
     * @param clazz
     * @param search
     * @return T or {@code null}
     */
    public static <T> T find(final Class<?> clazz, Function<Class<?>, T> search) {
        for (Class<?> t : hierarchy(clazz, Interfaces.INCLUDE)) {
            final T value = search.apply(t);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Construct a new instance of {@code cls} using its default constructor.
     * @param cls
     * @return T
     */
    public static <T> T newInstance(final Class<T> cls) {
        try {
            return cls.getConstructor().newInstance();
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

    /**
     * Get an {@link Iterable} that can iterate over a class hierarchy in ascending (subclass to superclass) order.
     * Taken from commons-lang3.
     *
     * @param type the type to get the class hierarchy from
     * @param interfacesBehavior switch indicating whether to include or exclude interfaces
     * @return Iterable an Iterable over the class hierarchy of the given class
     */
    public static Iterable<Class<?>> hierarchy(final Class<?> type, final Interfaces interfacesBehavior) {
        if (type == null) {
            return Collections.emptySet();
        }
        final Iterable<Class<?>> classes = new Iterable<Class<?>>() {

            @Override
            public Iterator<Class<?>> iterator() {
                return new Iterator<Class<?>>() {
                    Optional<Class<?>> next = Optional.of(type);

                    @Override
                    public boolean hasNext() {
                        return next.isPresent();
                    }

                    @Override
                    public Class<?> next() {
                        final Class<?> result = next.orElseThrow(NoSuchElementException::new);
                        next = Optional.ofNullable(result.getSuperclass());
                        return result;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
        if (interfacesBehavior != Interfaces.INCLUDE) {
            return classes;
        }
        return new Iterable<Class<?>>() {

            @Override
            public Iterator<Class<?>> iterator() {
                final Set<Class<?>> seenInterfaces = new HashSet<Class<?>>();
                final Iterator<Class<?>> wrapped = classes.iterator();

                return new Iterator<Class<?>>() {
                    Iterator<Class<?>> interfaces = Collections.emptyIterator();

                    @Override
                    public boolean hasNext() {
                        return interfaces.hasNext() || wrapped.hasNext();
                    }

                    @Override
                    public Class<?> next() {
                        if (interfaces.hasNext()) {
                            final Class<?> nextInterface = interfaces.next();
                            seenInterfaces.add(nextInterface);
                            return nextInterface;
                        }
                        final Class<?> nextSuperclass = wrapped.next();
                        final Set<Class<?>> currentInterfaces = new LinkedHashSet<>();
                        walkInterfaces(currentInterfaces, nextSuperclass);
                        interfaces = currentInterfaces.iterator();
                        return nextSuperclass;
                    }

                    private void walkInterfaces(final Set<Class<?>> addTo, final Class<?> c) {
                        for (final Class<?> iface : c.getInterfaces()) {
                            if (!seenInterfaces.contains(iface)) {
                                addTo.add(iface);
                            }
                            walkInterfaces(addTo, iface);
                        }
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
