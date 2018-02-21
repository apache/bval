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
import java.lang.reflect.Method;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.bval.util.Exceptions;

/**
 * Description: <br/>
 * InvocationHandler implementation of <code>Annotation</code> that pretends it is a "real" source code annotation.
 * <p/>
 */
class AnnotationProxy implements Annotation, InvocationHandler, Serializable {

    /** Serialization version */
    private static final long serialVersionUID = 1L;

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
        return values.entrySet().stream().map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
            .collect(Collectors.joining(", ", String.format("@%s(", annotationType().getName()), ")"));
    }
}
