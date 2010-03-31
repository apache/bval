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


import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.bval.jsr303.util.SecureActions;


/**
 * Description: <br/>
 * InvocationHandler implementation of <code>Annotation</code> that pretends it is a
 * "real" source code annotation.
 * <p/>
 */
public class AnnotationProxy implements Annotation, InvocationHandler {

    private final Class<? extends Annotation> annotationType;
    private final Map<String, Object> values;

    public AnnotationProxy(AnnotationProxyBuilder descriptor) {
        this.annotationType = descriptor.getType();
        values = getAnnotationValues(descriptor);
    }

    private Map<String, Object> getAnnotationValues(AnnotationProxyBuilder descriptor) {
        Map<String, Object> result = new HashMap();
        int processedValuesFromDescriptor = 0;
        final Method[] declaredMethods = SecureActions.getDeclaredMethods(annotationType);
        for (Method m : declaredMethods) {
            if (descriptor.contains(m.getName())) {
                result.put(m.getName(), descriptor.getValue(m.getName()));
                processedValuesFromDescriptor++;
            } else if (m.getDefaultValue() != null) {
                result.put(m.getName(), m.getDefaultValue());
            } else {
                throw new IllegalArgumentException("No value provided for " + m.getName());
            }
        }
        if (processedValuesFromDescriptor != descriptor.size()) {
            throw new RuntimeException(
                  "Trying to instanciate " + annotationType + " with unknown paramters.");
        }
        return result;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (values.containsKey(method.getName())) {
            return values.get(method.getName());
        }
        return method.invoke(this, args);
    }

    public Class<? extends Annotation> annotationType() {
        return annotationType;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append('@').append(annotationType().getName()).append('(');
        boolean comma = false;
        for (String m : getMethodsSorted()) {
            if (comma) result.append(", ");
            result.append(m).append('=').append(values.get(m));
            comma = true;
        }
        result.append(")");
        return result.toString();
    }

    private SortedSet<String> getMethodsSorted() {
        SortedSet<String> result = new TreeSet();
        result.addAll(values.keySet());
        return result;
    }
}

