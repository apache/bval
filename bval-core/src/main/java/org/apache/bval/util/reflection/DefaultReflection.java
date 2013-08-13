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

import org.apache.commons.lang3.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class DefaultReflection implements Reflection {
    public Class<?> getClass(final ClassLoader classLoader, final String className) throws Exception {
        return ClassUtils.getClass(classLoader, className, true);
    }

    public Object getAnnotationValue(final Annotation annotation, final String name) throws IllegalAccessException, InvocationTargetException {
        Method valueMethod;
        try {
            valueMethod = annotation.annotationType().getDeclaredMethod(name);
        } catch (final NoSuchMethodException ex) {
            // do nothing
            valueMethod = null;
        }
        if (null != valueMethod) {
            if (!valueMethod.isAccessible()) {
                valueMethod.setAccessible(true);
            }
            return valueMethod.invoke(annotation);
        }
        return null;
    }

    public ClassLoader getClassLoader(final Class<?> clazz) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return cl;
        }
        return clazz.getClassLoader();
    }

    public String getProperty(final String name) {
        return System.getProperty(name);
    }

    public Field getDeclaredField(final Class<?> clazz, final String fieldName) {
        final Field f;
        try {
            f = clazz.getDeclaredField(fieldName);
        } catch (final NoSuchFieldException e) {
            return null;
        }
        setAccessibility(f);
        return f;
    }

    public Field[] getDeclaredFields(final Class<?> clazz) {
        final Field[] fields = clazz.getDeclaredFields();
        if (fields.length > 0) {
            for (final Field f : fields) {
                if (!f.isAccessible()) {
                    f.setAccessible(true);
                }
            }
        }
        return fields;
    }

    public Constructor<?> getDeclaredConstructor(final Class<?> clazz, final Class<?>... parameters) {
        try {
            return clazz.getDeclaredConstructor(parameters);
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    public Method getDeclaredMethod(final Class<?> clazz, final String name, final Class<?>... parameters) {
        try {
            return clazz.getDeclaredMethod(name, parameters);
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    public Method[] getDeclaredMethods(final Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }

    public Constructor<?>[] getDeclaredConstructors(final Class<?> clazz) {
        return clazz.getDeclaredConstructors();
    }

    public Method getPublicMethod(final Class<?> clazz, final String methodName) {
        try {
            return clazz.getMethod(methodName);
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    public <T> T newInstance(final Class<T> cls) {
        try {
            return cls.newInstance();
        } catch (final Exception ex) {
            throw new RuntimeException("Cannot instantiate : " + cls, ex);
        }
    }

    private static void setAccessibility(final Field field) {
        // FIXME 2011-03-27 jw:
        // - Why not simply call field.setAccessible(true)?
        // - Fields can not be abstract.
        if (!Modifier.isPublic(field.getModifiers()) || (
            Modifier.isPublic(field.getModifiers()) &&
                Modifier.isAbstract(field.getModifiers()))) {
            field.setAccessible(true);
        }
    }
}
