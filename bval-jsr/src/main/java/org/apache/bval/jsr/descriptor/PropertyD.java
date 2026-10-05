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
package org.apache.bval.jsr.descriptor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import jakarta.validation.ValidationException;
import jakarta.validation.metadata.PropertyDescriptor;

import org.apache.bval.jsr.GraphContext;
import org.apache.bval.jsr.util.Methods;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

@Privilizing(@CallTo(Reflection.class))
public abstract class PropertyD<E extends AnnotatedElement> extends CascadableContainerD<BeanD<?>, E>
    implements PropertyDescriptor {

    static class ForField extends PropertyD<Field> {

        ForField(MetadataReader.ForContainer<Field> reader, BeanD<?> parent) {
            super(reader, parent);
        }

        @Override
        public String getPropertyName() {
            return getTarget().getName();
        }

        @Override
        public Object getValue(Object parent) throws Exception {
            final MethodHandle getter = getter();
            if (getter != null) {
                try {
                    return (Object) getter.invokeExact(parent);
                } catch (Exception | Error e) {
                    throw e;
                } catch (Throwable t) {
                    throw new IllegalArgumentException(t);
                }
            }
            Reflection.makeAccessible(getTarget());
            try {
                return getTarget().get(parent);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        MethodHandle unreflect(Lookup lookup) throws IllegalAccessException {
            return lookup.unreflectGetter(getTarget());
        }
    }

    static class ForMethod extends PropertyD<Method> {

        ForMethod(MetadataReader.ForContainer<Method> reader, BeanD<?> parent) {
            super(reader, parent);
        }

        @Override
        public String getPropertyName() {
            return Methods.propertyName(getTarget());
        }

        @Override
        public Object getValue(Object parent) throws Exception {
            final MethodHandle getter = getter();
            if (getter != null) {
                try {
                    return (Object) getter.invokeExact(parent);
                } catch (Throwable t) {
                    // same wrapping as Method#invoke followed by the catch below
                    throw new IllegalArgumentException(new InvocationTargetException(t));
                }
            }
            Reflection.makeAccessible(getTarget());
            try {
                return getTarget().invoke(parent);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        MethodHandle unreflect(Lookup lookup) throws IllegalAccessException {
            return lookup.unreflect(getTarget());
        }
    }

    private static final MethodType GETTER_TYPE = MethodType.methodType(Object.class, Object.class);

    /**
     * Reading through a cached {@link MethodHandle} avoids the slow path that {@link Field#get(Object)} and
     * {@link Method#invoke(Object, Object...)} take when the reflective object is not a JIT constant. Holds
     * {@link #NO_GETTER} when no handle can be created, in which case reflection is used.
     */
    private volatile MethodHandle getter;

    private static final MethodHandle NO_GETTER = MethodHandles.constant(Object.class, null);

    protected PropertyD(MetadataReader.ForContainer<E> reader, BeanD<?> parent) {
        super(reader, parent);
    }

    public final Stream<GraphContext> read(GraphContext context) {
        if (context.getValue() == null) {
            return Stream.empty();
        }
        try {
            final Object value = getValue(context.getValue());
            return Stream.of(context.child(p -> p.addProperty(getPropertyName()), value));
        } catch (Exception e) {
            throw e instanceof ValidationException ? (ValidationException) e : new ValidationException(e);
        }
    }

    public abstract Object getValue(Object parent) throws Exception;

    abstract MethodHandle unreflect(Lookup lookup) throws IllegalAccessException;

    /**
     * Get a {@code (Object)Object} handle reading this property, or {@code null} if none could be created.
     */
    final MethodHandle getter() {
        MethodHandle result = getter;
        if (result == null) {
            try {
                Reflection.makeAccessible((AccessibleObject) getTarget());
                result = unreflect(MethodHandles.lookup()).asType(GETTER_TYPE);
            } catch (RuntimeException | IllegalAccessException e) {
                result = NO_GETTER;
            }
            getter = result;
        }
        return result == NO_GETTER ? null : result;
    }
}
