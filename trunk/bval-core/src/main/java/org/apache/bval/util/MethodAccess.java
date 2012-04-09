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
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Description: invoke a zero-argument method (getter)<br/>
 */
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
        if (!method.isAccessible()) {
            run( new PrivilegedAction<Void>() {
                public Void run() {
                    method.setAccessible(true);
                    return null;
                }
            });
        }
    }

    /**
     * Process bean properties getter by applying the JavaBean naming conventions.
     *
     * @param member the member for which to get the property name.
     * @return The bean method name with the "is" or "get" prefix stripped off, <code>null</code>
     *         the method name id not according to the JavaBeans standard.
     */
    public static String getPropertyName(Method member) {
        String name = null;
        String methodName = member.getName();
        if (methodName.startsWith("is")) {
            name = Introspector.decapitalize(methodName.substring(2));
        } /* else if ( methodName.startsWith("has")) {
				name = Introspector.decapitalize( methodName.substring( 3 ) );
			} */
        // setter annotation is NOT supported in the spec
        /*  else if (method.getName().startsWith("set") && method.getParameterTypes().length == 1) {
           propName = Introspector.decapitalize(method.getName().substring(3));
       } */
        else if (methodName.startsWith("get")) {
            name = Introspector.decapitalize(methodName.substring(3));
        }
        return name;
    }

    /**
     * {@inheritDoc}
     * normally the propertyName of the getter method, e.g.<br>
     * method: getName() -> propertyName: name<br>
     * method: isValid() -> propertyName: valid<br>
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * {@inheritDoc}
     */
    public Object get(final Object instance) {
        try {
            return method.invoke(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ElementType getElementType() {
        return ElementType.METHOD;
    }

    /**
     * {@inheritDoc}
     */
    public Type getJavaType() {
        return method.getGenericReturnType();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return method.toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodAccess that = (MethodAccess) o;

        return method.equals(that.method);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return method.hashCode();
    }

    private static <T> T run(PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
    }
}
