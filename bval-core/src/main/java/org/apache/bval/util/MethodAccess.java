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
package org.apache.bval.util;

import java.beans.Introspector;
import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Description: invoke a zero-argument method (getter)<br/>
 */
@Privilizing(@CallTo(Reflection.class))
public class MethodAccess extends AccessStrategy {
    private final Method method;
    private final String propertyName;

    /**
     * Create a new MethodAccess instance.
     * @param method
     */
    public MethodAccess(Method method) {
        this(getPropertyName(method), method);
    }

    /**
     * Create a new MethodAccess instance.
     * @param propertyName
     * @param method
     */
    public MethodAccess(String propertyName, final Method method) {
        this.method = method;
        this.propertyName = propertyName;
    }

    /**
     * Process bean properties getter by applying the JavaBean naming conventions.
     *
     * @param member the member for which to get the property name.
     * @return The bean method name with the "is" or "get" prefix stripped off, <code>null</code>
     *         the method name id not according to the JavaBeans standard.
     */
    public static String getPropertyName(Method member) {
        final String methodName = member.getName();
        if (methodName.startsWith("is")) {
            return Introspector.decapitalize(methodName.substring(2));
        }
        if (methodName.startsWith("get")) {
            return Introspector.decapitalize(methodName.substring(3));
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * normally the propertyName of the getter method, e.g.<br>
     * method: getName() -> propertyName: name<br>
     * method: isValid() -> propertyName: valid<br>
     */
    @Override
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final Object instance) {
        final boolean mustUnset = Reflection.setAccessible(method, true);
        try {
            return method.invoke(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (mustUnset) {
                Reflection.setAccessible(method, false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ElementType getElementType() {
        return ElementType.METHOD;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getJavaType() {
        return method.getGenericReturnType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return method.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MethodAccess that = (MethodAccess) o;

        return method.equals(that.method);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return method.hashCode();
    }
}
