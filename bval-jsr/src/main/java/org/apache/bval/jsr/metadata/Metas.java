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

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import javax.validation.constraintvalidation.ValidationTarget;

import org.apache.bval.util.Validate;

/**
 * Validation class model.
 *
 * @param <E>
 */
// TODO rename to Meta; delete old type of that name
public abstract class Metas<E extends AnnotatedElement> {

    public static class ForClass extends Metas<Class<?>> {

        public ForClass(Class<?> host) {
            super(host, ElementType.TYPE);
        }

        @Override
        public final Class<?> getDeclaringClass() {
            return getHost();
        }

        @Override
        public Type getType() {
            return getHost();
        }

        @Override
        public AnnotatedType getAnnotatedType() {
            return new AnnotatedType() {

                @Override
                public Annotation[] getDeclaredAnnotations() {
                    return getHost().getDeclaredAnnotations();
                }

                @Override
                public Annotation[] getAnnotations() {
                    return getHost().getAnnotations();
                }

                @Override
                public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                    return getHost().getAnnotation(annotationClass);
                }

                @Override
                public Type getType() {
                    return getHost();
                }
            };
        }

        @Override
        public String getName() {
            return getHost().getName();
        }
    }

    public static abstract class ForMember<M extends Member & AnnotatedElement> extends Metas<M> {

        protected ForMember(M host, ElementType elementType) {
            super(host, elementType);
        }

        @Override
        public Class<?> getDeclaringClass() {
            return getHost().getDeclaringClass();
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

    public static class ForConstructor extends ForExecutable<Constructor<?>> {

        public ForConstructor(Constructor<?> host) {
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

    public static class ForCrossParameter<E extends Executable> extends Metas.ForExecutable<E> {

        public ForCrossParameter(Metas<E> parent) {
            super(parent.getHost(), parent.getElementType());
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
        public ValidationTarget getValidationTarget() {
            return ValidationTarget.PARAMETERS;
        }

        @Override
        public String toString() {
            return String.format("%s(%s of %s)", getStringPrefix(), getName(), getHost());
        }
    }

    public static class ForParameter extends Metas<Parameter> {

        private final String name;

        public ForParameter(Parameter host, String name) {
            super(host, ElementType.PARAMETER);
            this.name = Validate.notNull(name, "name");
        }

        @Override
        public Collection<ValidationTarget> getValidationTargets() {
            return asList(ValidationTarget.values());
        }

        @Override
        public Type getType() {
            return getHost().getType();
        }

        @Override
        public Class<?> getDeclaringClass() {
            return getHost().getDeclaringExecutable().getDeclaringClass();
        }

        @Override
        public AnnotatedType getAnnotatedType() {
            return getHost().getAnnotatedType();
        }

        public String getName() {
            return name;
        }
    }

    public static class ForContainerElement extends Metas<AnnotatedType> {

        private final Metas<?> parent;
        private final ContainerElementKey key;

        public ForContainerElement(Metas<?> parent, ContainerElementKey key) {
            super(key.getAnnotatedType(), ElementType.TYPE_USE);
            this.parent = Validate.notNull(parent, "parent");
            this.key = Validate.notNull(key, "key");
        }

        @Override
        public Type getType() {
            return getHost().getType();
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
            return Integer.valueOf(key.getTypeArgumentIndex());
        }

        @Override
        public String getName() {
            return key.toString();
        }

        @Override
        public String toString() {
            return String.format("%s(%s of %s)", getStringPrefix(), key, getHost());
        }
    }

    private final E host;
    private final ElementType elementType;

    protected Metas(E host, ElementType elementType) {
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

    public Collection<ValidationTarget> getValidationTargets() {
        // todo: cache for perf?
        return singleton(getValidationTarget());
    }

    public ValidationTarget getValidationTarget() {
        return ValidationTarget.ANNOTATED_ELEMENT;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getStringPrefix(), host);
    }

    protected String getStringPrefix() {
        return Metas.class.getSimpleName() + '.' + getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!obj.getClass().equals(getClass())) {
            return false;
        }
        return Objects.equals(((Metas<?>) obj).getHost(), getHost());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHost());
    }
}
