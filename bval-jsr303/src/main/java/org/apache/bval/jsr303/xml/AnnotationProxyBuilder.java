/**
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
package org.apache.bval.jsr303.xml;


import javax.validation.Payload;
import javax.validation.ValidationException;

import org.apache.bval.jsr303.util.SecureActions;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Description: Holds the information and creates an annotation proxy
 * during xml parsing of validation mapping constraints. <br/>
 */
final class AnnotationProxyBuilder<A extends Annotation> {
    private static final String ANNOTATION_PAYLOAD = "payload";
    private static final String ANNOTATION_GROUPS = "groups";
    private static final String ANNOTATION_MESSAGE = "message";

    private final Class<A> type;

    private final Map<String, Object> elements = new HashMap<String, Object>();

    public AnnotationProxyBuilder(Class<A> annotationType) {
        this.type = annotationType;
    }

    public AnnotationProxyBuilder(Class<A> annotationType, Map<String, Object> elements) {
        this.type = annotationType;
        for (Map.Entry<String, Object> entry : elements.entrySet()) {
            this.elements.put(entry.getKey(), entry.getValue());
        }
    }

    public void putValue(String elementName, Object value) {
        elements.put(elementName, value);
    }

    public Object getValue(String elementName) {
        return elements.get(elementName);
    }

    public boolean contains(String elementName) {
        return elements.containsKey(elementName);
    }

    public int size() {
        return elements.size();
    }

    public Class<A> getType() {
        return type;
    }

    public void setMessage(String message) {
        putValue(ANNOTATION_MESSAGE, message);
    }

    public void setGroups(Class<?>[] groups) {
        putValue(ANNOTATION_GROUPS, groups);
    }

    public void setPayload(Class<? extends Payload>[] payload) {
        putValue(ANNOTATION_PAYLOAD, payload);
    }

    public A createAnnotation() {
        ClassLoader classLoader = SecureActions.getClassLoader(getClass());
        Class<A> proxyClass = (Class<A>) Proxy.getProxyClass(classLoader, getType());
        InvocationHandler handler = new AnnotationProxy(this);
        try {
            return SecureActions.getConstructor(proxyClass, InvocationHandler.class)
                  .newInstance(handler);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException(
                  "Unable to create annotation for configured constraint", e);
        }
    }
}
