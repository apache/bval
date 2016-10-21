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
package org.apache.bval.cdi;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.util.AnnotationLiteral;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class BValAnnotatedType<A> implements AnnotatedType<A> {
    private final AnnotatedType<A> delegate;
    private final Set<Annotation> annotations;

    public BValAnnotatedType(final AnnotatedType<A> annotatedType) {
        delegate = annotatedType;

        annotations = new HashSet<Annotation>(annotatedType.getAnnotations().size());
        annotations.addAll(annotatedType.getAnnotations());
        annotations.add(BValBindingLiteral.INSTANCE);
    }

    @Override
    public Class<A> getJavaClass() {
        return delegate.getJavaClass();
    }

    @Override
    public Set<AnnotatedConstructor<A>> getConstructors() {
        return delegate.getConstructors();
    }

    @Override
    public Set<AnnotatedMethod<? super A>> getMethods() {
        return delegate.getMethods();
    }

    @Override
    public Set<AnnotatedField<? super A>> getFields() {
        return delegate.getFields();
    }

    @Override
    public Type getBaseType() {
        return delegate.getBaseType();
    }

    @Override
    public Set<Type> getTypeClosure() {
        return delegate.getTypeClosure();
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
        for (final Annotation ann : annotations) {
            if (ann.annotationType().equals(annotationType)) {
                @SuppressWarnings("unchecked")
                final T result = (T) ann;
                return result;
            }
        }
        return null;
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        for (final Annotation ann : annotations) {
            if (ann.annotationType().equals(annotationType)) {
                return true;
            }
        }
        return false;
    }

    public static class BValBindingLiteral extends EmptyAnnotationLiteral<BValBinding> implements BValBinding {
        private static final long serialVersionUID = 1L;

        public static final Annotation INSTANCE = new BValBindingLiteral();

        @Override
        public String toString() {
            return "@org.apache.bval.cdi.BValBinding()";
        }

    }
}
