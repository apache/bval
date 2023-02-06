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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import jakarta.validation.ValidationException;
import jakarta.validation.metadata.PropertyDescriptor;

import org.apache.bval.jsr.GraphContext;
import org.apache.bval.jsr.util.Methods;
import org.apache.bval.jsr.util.PathImpl;
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
            Reflection.makeAccessible(getTarget());
            try {
                return getTarget().get(parent);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
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
            Reflection.makeAccessible(getTarget());
            try {
                return getTarget().invoke(parent);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    protected PropertyD(MetadataReader.ForContainer<E> reader, BeanD<?> parent) {
        super(reader, parent);
    }

    public final Stream<GraphContext> read(GraphContext context) {
        if (context.getValue() == null) {
            return Stream.empty();
        }
        try {
            final Object value = getValue(context.getValue());
            final PathImpl p = context.getPath();
            p.addProperty(getPropertyName());
            return Stream.of(context.child(p, value));
        } catch (Exception e) {
            throw e instanceof ValidationException ? (ValidationException) e : new ValidationException(e);
        }
    }

    public abstract Object getValue(Object parent) throws Exception;
}
