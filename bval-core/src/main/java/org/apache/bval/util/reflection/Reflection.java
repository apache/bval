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

public interface Reflection {
    static final Reflection INSTANCE = ReflectionFactory.newInstance();

    Class<?> getClass(final ClassLoader classLoader, final String className) throws Exception;

    Object getAnnotationValue(final Annotation annotation, final String name) throws IllegalAccessException, InvocationTargetException;

    ClassLoader getClassLoader(final Class<?> clazz);

    String getProperty(final String name);

    Field getDeclaredField(final Class<?> clazz, final String fieldName);

    Field[] getDeclaredFields(final Class<?> clazz);

    Constructor<?> getDeclaredConstructor(final Class<?> clazz, final Class<?>... parameters);

    Method getDeclaredMethod(final Class<?> clazz, final String name, final Class<?>... parameters);

    Method[] getDeclaredMethods(final Class<?> clazz);

    Constructor<?>[] getDeclaredConstructors(final Class<?> clazz);

    Method getPublicMethod(final Class<?> clazz, final String methodName);

    <T> T newInstance(final Class<T> cls);

    public static class ReflectionFactory {
        public static Reflection newInstance() {
            if (System.getSecurityManager() != null) {
                return new SecurityManagerReflection();
            }
            return new DefaultReflection();
        }

        private ReflectionFactory() {
            // no-op
        }
    }
}
