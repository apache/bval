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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.bval.jsr.metadata.Signature;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.ObjectUtils;
import org.apache.bval.util.StringUtils;
import org.apache.bval.util.reflection.Reflection;

/**
 * Description: <br/>
 * InvocationHandler implementation of <code>Annotation</code> that pretends it is a "real" source code annotation.
 * <p/>
 */
class AnnotationProxy implements Annotation, InvocationHandler, Serializable {

    /** Serialization version */
    private static final long serialVersionUID = 1L;
    
    private Signature EQUALS = new Signature("equals", Object.class);

    private final Class<? extends Annotation> annotationType;
    private final SortedMap<String, Object> values;

    /**
     * Create a new AnnotationProxy instance.
     * 
     * @param <A>
     * @param descriptor
     */
    <A extends Annotation> AnnotationProxy(AnnotationProxyBuilder<A> descriptor) {
        this.annotationType = descriptor.getType();
        values = new TreeMap<>();
        int processedValuesFromDescriptor = 0;
        for (final Method m : descriptor.getMethods()) {
            if (descriptor.contains(m.getName())) {
                values.put(m.getName(), descriptor.getValue(m.getName()));
                processedValuesFromDescriptor++;
            } else {
                Exceptions.raiseIf(m.getDefaultValue() == null, IllegalArgumentException::new,
                    "No value provided for %s", m.getName());
                values.put(m.getName(), m.getDefaultValue());
            }
        }
        Exceptions.raiseUnless(processedValuesFromDescriptor == descriptor.size() || Valid.class.equals(annotationType),
            IllegalArgumentException::new, "Trying to instantiate %s with unknown parameters.",
            annotationType.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (values.containsKey(method.getName())) {
            return values.get(method.getName());
        }
        if (EQUALS.equals(Signature.of(method))) {
            return equalTo(args[0]);
        }
        return method.invoke(this, args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends Annotation> annotationType() {
        return annotationType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return values.entrySet().stream()
            .map(e -> String.format("%s=%s", e.getKey(), StringUtils.valueOf(e.getValue())))
            .collect(Collectors.joining(", ", String.format("@%s(", annotationType().getName()), ")"));
    }

    @Override
    public int hashCode() {
        return values.entrySet().stream().mapToInt(e -> {
            return (127 * e.getKey().hashCode()) ^ ObjectUtils.hashCode(e.getValue());
        }).sum();
    }

    private boolean equalTo(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Annotation) {
            final Annotation other = (Annotation) obj;
            return other.annotationType().equals(annotationType)
                && values.entrySet().stream().allMatch(e -> memberEquals(other, e.getKey(), e.getValue()));
        }
        return false;
    }

    private boolean memberEquals(Annotation other, String name, Object value) {
        final Method member = Reflection.getDeclaredMethod(annotationType, name);
        final Object otherValue;
        try {
            otherValue = member.invoke(other);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        Exceptions.raiseIf(otherValue == null || !otherValue.getClass().equals(value.getClass()),
            IllegalStateException::new, "Unexpected value %s for member %s of %s", otherValue, name, other);

        if (value instanceof Object[]) {
            return Arrays.equals((Object[]) value, (Object[]) otherValue);
        }
        if (value instanceof byte[]) {
            return Arrays.equals((byte[]) value, (byte[]) otherValue);
        }
        if (value instanceof short[]) {
            return Arrays.equals((short[]) value, (short[]) otherValue);
        }
        if (value instanceof int[]) {
            return Arrays.equals((int[]) value, (int[]) otherValue);
        }
        if (value instanceof char[]) {
            return Arrays.equals((char[]) value, (char[]) otherValue);
        }
        if (value instanceof long[]) {
            return Arrays.equals((long[]) value, (long[]) otherValue);
        }
        if (value instanceof float[]) {
            return Arrays.equals((float[]) value, (float[]) otherValue);
        }
        if (value instanceof double[]) {
            return Arrays.equals((double[]) value, (double[]) otherValue);
        }
        if (value instanceof boolean[]) {
            return Arrays.equals((boolean[]) value, (boolean[]) otherValue);
        }
        return value.equals(otherValue);
    }
}
