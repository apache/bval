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
package org.apache.bval.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Optional;
import java.util.stream.Stream;

public class EmulatedAnnotatedType<T extends Type> implements AnnotatedType {
    private static class Parameterized extends EmulatedAnnotatedType<ParameterizedType>
        implements AnnotatedParameterizedType {

        Parameterized(ParameterizedType wrapped) {
            super(wrapped);
        }

        @Override
        public AnnotatedType[] getAnnotatedActualTypeArguments() {
            return wrapArray(wrapped.getActualTypeArguments());
        }
    }

    private static class Variable extends EmulatedAnnotatedType<TypeVariable<?>> implements AnnotatedTypeVariable {

        Variable(TypeVariable<?> wrapped) {
            super(wrapped);
        }

        @Override
        public AnnotatedType[] getAnnotatedBounds() {
            return wrapped.getAnnotatedBounds();
        }
    }

    private static class Wildcard extends EmulatedAnnotatedType<WildcardType> implements AnnotatedWildcardType {

        Wildcard(WildcardType wrapped) {
            super(wrapped);
        }

        @Override
        public AnnotatedType[] getAnnotatedLowerBounds() {
            return wrapArray(wrapped.getLowerBounds());
        }

        @Override
        public AnnotatedType[] getAnnotatedUpperBounds() {
            return wrapArray(wrapped.getUpperBounds());
        }
    }

    public static EmulatedAnnotatedType<?> wrap(Type type) {
        if (type instanceof ParameterizedType) {
            return new EmulatedAnnotatedType.Parameterized((ParameterizedType) type);
        }
        if (type instanceof TypeVariable<?>) {
            return new EmulatedAnnotatedType.Variable((TypeVariable<?>) type);
        }
        if (type instanceof WildcardType) {
            return new EmulatedAnnotatedType.Wildcard((WildcardType) type);
        }
        return new EmulatedAnnotatedType<>(type);
    }

    private static EmulatedAnnotatedType<?>[] wrapArray(Type[] types) {
        return Stream.of(types).map(EmulatedAnnotatedType::wrap).toArray(EmulatedAnnotatedType[]::new);
    }

    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = {};

    protected final T wrapped;
    private final Optional<AnnotatedElement> annotated;

    private EmulatedAnnotatedType(T wrapped) {
        super();
        this.wrapped = Validate.notNull(wrapped);
        this.annotated =
            Optional.of(wrapped).filter(AnnotatedElement.class::isInstance).map(AnnotatedElement.class::cast);
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return annotated.map(e -> e.getAnnotation(annotationClass)).orElse(null);
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotated.map(AnnotatedElement::getAnnotations).orElse(EMPTY_ANNOTATION_ARRAY);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return annotated.map(AnnotatedElement::getDeclaredAnnotations).orElse(EMPTY_ANNOTATION_ARRAY);
    }

    @Override
    public Type getType() {
        return wrapped;
    }
}
