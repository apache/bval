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
package org.apache.bval.jsr.xml;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.util.AnnotationLiteral;
import javax.validation.ConstraintTarget;
import javax.validation.Payload;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.groups.ConvertGroup;

import org.apache.bval.cdi.EmptyAnnotationLiteral;
import org.apache.bval.jsr.ConstraintAnnotationAttributes;
import org.apache.bval.jsr.util.AnnotationsManager;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privileged;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Description: Holds the information and creates an annotation proxy during xml
 * parsing of validation mapping constraints. <br/>
 */
@Privilizing(@CallTo(Reflection.class))
public final class AnnotationProxyBuilder<A extends Annotation> {
    private static final ConcurrentMap<Class<?>, Method[]> METHODS_CACHE = new ConcurrentHashMap<>();

    public static <A> Method[] findMethods(final Class<A> annotationType) {
        // cache only built-in constraints to avoid memory leaks:
        // TODO use configurable cache size property?
        if (annotationType.getName().startsWith("javax.validation.constraints.")) {
            return METHODS_CACHE.computeIfAbsent(annotationType, Reflection::getDeclaredMethods);
        }
        return Reflection.getDeclaredMethods(annotationType);
    }

    private final Class<A> type;
    private final Map<String, Object> elements = new HashMap<>();
    private final Method[] methods;
    private boolean changed;

    /**
     * Create a new AnnotationProxyBuilder instance.
     *
     * @param annotationType
     */
    public AnnotationProxyBuilder(final Class<A> annotationType) {
        this.type = annotationType;
        this.methods = findMethods(annotationType);
    }

    /**
     * Create a new AnnotationProxyBuilder instance.
     *
     * @param annotationType
     * @param elements
     */
    public AnnotationProxyBuilder(Class<A> annotationType, Map<String, Object> elements) {
        this(annotationType);
        elements.forEach(this.elements::put);
    }

    /**
     * Create a builder initially configured to create an annotation equivalent
     * to {@code annot}.
     * 
     * @param annot
     *            Annotation to be replicated.
     */
    @SuppressWarnings("unchecked")
    public AnnotationProxyBuilder(A annot) {
        this((Class<A>) annot.annotationType());
        elements.putAll(AnnotationsManager.readAttributes(annot));
    }

    public Method[] getMethods() {
        return methods;
    }

    /**
     * Add an element to the configuration.
     *
     * @param elementName
     * @param value
     */
    @Deprecated
    public Object putValue(String elementName, Object value) {
        return elements.put(elementName, value);
    }

    /**
     * Add an element to the configuration.
     *
     * @param elementName
     * @param value
     * @return whether any change occurred
     */
    public boolean setValue(String elementName, Object value) {
        final boolean result = !Objects.equals(elements.put(elementName, value), value);
        changed |= result;
        return result;
    }

    /**
     * Get the specified element value from the current configuration.
     *
     * @param elementName
     * @return Object value
     */
    public Object getValue(String elementName) {
        return elements.get(elementName);
    }

    /**
     * Learn whether a given element has been configured.
     *
     * @param elementName
     * @return <code>true</code> if an <code>elementName</code> element is found
     *         on this annotation
     */
    public boolean contains(String elementName) {
        return elements.containsKey(elementName);
    }

    /**
     * Get the number of configured elements.
     *
     * @return int
     */
    public int size() {
        return elements.size();
    }

    /**
     * Get the configured Annotation type.
     *
     * @return Class<A>
     */
    public Class<A> getType() {
        return type;
    }

    /**
     * Configure the well-known JSR303 "message" element.
     *
     * @param message
     * @return
     */
    public boolean setMessage(String message) {
        return setValue(ConstraintAnnotationAttributes.MESSAGE.getAttributeName(), message);
    }

    /**
     * Configure the well-known JSR303 "groups" element.
     *
     * @param groups
     * @return
     */
    public boolean setGroups(Class<?>[] groups) {
        return setValue(ConstraintAnnotationAttributes.GROUPS.getAttributeName(), groups);
    }

    /**
     * Configure the well-known JSR303 "payload" element.
     * 
     * @param payload
     * @return
     */
    public boolean setPayload(Class<? extends Payload>[] payload) {
        return setValue(ConstraintAnnotationAttributes.PAYLOAD.getAttributeName(), payload);
    }

    /**
     * Configure the well-known "validationAppliesTo" element.
     * 
     * @param constraintTarget
     */
    public boolean setValidationAppliesTo(ConstraintTarget constraintTarget) {
        return setValue(ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO.getAttributeName(), constraintTarget);
    }

    public boolean isChanged() {
        return changed;
    }

    /**
     * Create the annotation represented by this builder.
     *
     * @return {@link Annotation}
     */
    public A createAnnotation() {
        final ClassLoader classLoader = Reflection.getClassLoader(getType());
        @SuppressWarnings("unchecked")
        final Class<A> proxyClass = (Class<A>) Proxy.getProxyClass(classLoader, getType());
        return doCreateAnnotation(proxyClass, new AnnotationProxy(this));
    }

    @Privileged
    private A doCreateAnnotation(final Class<A> proxyClass, final InvocationHandler handler) {
        try {
            final Constructor<A> constructor = proxyClass.getConstructor(InvocationHandler.class);
            final boolean mustUnset = Reflection.setAccessible(constructor, true); // java
                                                                                   // 8
            try {
                return constructor.newInstance(handler);
            } finally {
                if (mustUnset) {
                    Reflection.setAccessible(constructor, false);
                }
            }
        } catch (Exception e) {
            throw new ValidationException("Unable to create annotation for configured constraint", e);
        }
    }

    public static final class ValidAnnotation extends EmptyAnnotationLiteral<Valid> implements Valid {
        private static final long serialVersionUID = 1L;

        public static final ValidAnnotation INSTANCE = new ValidAnnotation();
    }

    public static final class ConvertGroupAnnotation extends AnnotationLiteral<ConvertGroup> implements ConvertGroup {
        private static final long serialVersionUID = 1L;

        private final Class<?> from;
        private final Class<?> to;

        public ConvertGroupAnnotation(final Class<?> from, final Class<?> to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public Class<?> from() {
            return from;
        }

        @Override
        public Class<?> to() {
            return to;
        }
    }
}
