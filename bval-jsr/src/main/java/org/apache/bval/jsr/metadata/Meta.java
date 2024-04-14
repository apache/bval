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
package org.apache.bval.jsr.metadata;

import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Objects;

import org.apache.bval.util.EmulatedAnnotatedType;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.TypeUtils;

/**
 * Validation class model.
 *
 * @param <E>
 */
public abstract class Meta<E extends AnnotatedElement> {

    public static class ForClass<T> extends Meta<Class<T>> {
        private final AnnotatedType annotatedType;

        public ForClass(Class<T> host) {
            super(host, ElementType.TYPE);
            this.annotatedType = EmulatedAnnotatedType.wrap(host);
        }

        @Override
        public final Class<T> getDeclaringClass() {
            return getHost();
        }

        @Override
        public Type getType() {
            return getHost();
        }

        @Override
        public AnnotatedType getAnnotatedType() {
            return annotatedType;
        }

        @Override
        public String getName() {
            return getHost().getName();
        }

        @Override
        public Meta<?> getParent() {
            return null;
        }
    }

    public static abstract class ForMember<M extends Member & AnnotatedElement> extends Meta<M> {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        private final Lazy<Meta<Class<?>>> parent = new Lazy<>(() -> new Meta.ForClass(getDeclaringClass()));

        protected ForMember(M host, ElementType elementType) {
            super(host, elementType);
        }

        @Override
        public Class<?> getDeclaringClass() {
            return getHost().getDeclaringClass();
        }

        @Override
        public Meta<Class<?>> getParent() {
            return parent.get();
        }
    }

    public static class ForField extends ForMember<Field> {

        public ForField(Field host) {
            super(host, ElementType.FIELD);
        }

        @Override
        public Type getType() {
            return getHost().getGenericType();
        }

        @Override
        public AnnotatedType getAnnotatedType() {
            return getHost().getAnnotatedType();
        }

        @Override
        public String getName() {
            return getHost().getName();
        }
    }

    public static abstract class ForExecutable<E extends Executable> extends ForMember<E> {

        protected ForExecutable(E host, ElementType elementType) {
            super(host, elementType);
        }

        @Override
        public AnnotatedType getAnnotatedType() {
            return getHost().getAnnotatedReturnType();
        }
    }

    public static class ForConstructor<T> extends ForExecutable<Constructor<? extends T>> {

        public ForConstructor(Constructor<? extends T> host) {
            super(host, ElementType.CONSTRUCTOR);
        }

        @Override
        public Type getType() {
            return getHost().getDeclaringClass();
        }

        @Override
        public String getName() {
            return getHost().getDeclaringClass().getSimpleName();
        }
    }

    public static class ForMethod extends ForExecutable<Method> {

        public ForMethod(Method host) {
            super(host, ElementType.METHOD);
        }

        @Override
        public Type getType() {
            return getHost().getGenericReturnType();
        }

        @Override
        public String getName() {
            return getHost().getName();
        }
    }

    public static class ForCrossParameter<E extends Executable> extends Meta<E> {

        private final Meta<E> parent;

        public ForCrossParameter(Meta<E> parent) {
            super(parent.getHost(), parent.getElementType());
            this.parent = parent;
        }

        @Override
        public Type getType() {
            return Object[].class;
        }

        @Override
        public String getName() {
            return "<cross parameter>";
        }

        @Override
        public String describeHost() {
            return String.format("%s of %s", getName(), getHost());
        }

        @Override
        public Meta<E> getParent() {
            return parent;
        }

        @Override
        public Class<?> getDeclaringClass() {
            return getHost().getDeclaringClass();
        }

        @Override
        public AnnotatedType getAnnotatedType() {
            return getHost().getAnnotatedReturnType();
        }
    }

    public static class ForParameter extends Meta<Parameter> {

        private final String name;
        private final Lazy<Meta<? extends Executable>> parent = new Lazy<>(this::computeParent);

        public ForParameter(Parameter host, String name) {
            super(host, ElementType.PARAMETER);
            this.name = Validate.notNull(name, "name");
        }

        @Override
        public Type getType() {
            return getHost().getParameterizedType();
        }

        @Override
        public Class<?> getDeclaringClass() {
            return getHost().getDeclaringExecutable().getDeclaringClass();
        }

        @Override
        public AnnotatedType getAnnotatedType() {
            return getHost().getAnnotatedType();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String describeHost() {
            return String.format("%s of %s", getName(), getHost().getDeclaringExecutable());
        }

        @Override
        public Meta<? extends Executable> getParent() {
            return parent.get();
        }

        private Meta<? extends Executable> computeParent() {
            final Executable exe = getHost().getDeclaringExecutable();
            return exe instanceof Method ? new Meta.ForMethod((Method) exe)
                : new Meta.ForConstructor<>((Constructor<?>) exe);
        }
    }

    public static class ForContainerElement extends Meta<AnnotatedType> {

        private final Meta<?> parent;
        private final ContainerElementKey key;

        public ForContainerElement(Meta<?> parent, ContainerElementKey key) {
            super(key.getAnnotatedType(), ElementType.TYPE_USE);
            this.parent = Validate.notNull(parent, "parent");
            this.key = Validate.notNull(key, "key");
        }

        @Override
        public Type getType() {
            Type result = getHost().getType();
            if (result instanceof TypeVariable<?>) {
                final Type parentType = parent.getType();
                if (parentType instanceof ParameterizedType) {
                    final Map<TypeVariable<?>, Type> typeArguments =
                        TypeUtils.getTypeArguments((ParameterizedType) parentType);
                    if (typeArguments.containsKey(result)) {
                        return typeArguments.get(result);
                    }
                }
            }
            return result;
        }

        @Override
        public Class<?> getDeclaringClass() {
            return parent.getDeclaringClass();
        }

        @Override
        public AnnotatedType getAnnotatedType() {
            return key.getAnnotatedType();
        }

        public Integer getTypeArgumentIndex() {
            return key.getTypeArgumentIndex();
        }

        @Override
        public String getName() {
            return key.toString();
        }

        @Override
        public String describeHost() {
            return String.format("%s of %s", key, parent);
        }

        @Override
        public Meta<?> getParent() {
            return parent;
        }
    }

    private final E host;
    private final ElementType elementType;

    protected Meta(E host, ElementType elementType) {
        super();
        this.host = Validate.notNull(host, "host");
        this.elementType = Validate.notNull(elementType, "elementType");
    }

    public E getHost() {
        return host;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public abstract Type getType();

    public abstract Class<?> getDeclaringClass();

    public abstract AnnotatedType getAnnotatedType();

    public abstract String getName();

    public abstract Meta<?> getParent();

    @Override
    public final String toString() {
        return String.format("%s.%s(%s)", Meta.class.getSimpleName(), getClass().getSimpleName(), describeHost());
    }

    public String describeHost() {
        return host.toString();
    }
}
