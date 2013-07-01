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
package org.apache.bval;

import org.apache.bval.util.AccessStrategy;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class ConstructorAccess extends AccessStrategy {

    private final Constructor<?> constructor;

    public ConstructorAccess(final Constructor<?> constructor) {
        this.constructor = constructor;
        if (!constructor.isAccessible()) {
            run(new PrivilegedAction<Void>() {
                public Void run() {
                    constructor.setAccessible(true);
                    return null;
                }
            });
        }
    }

    @Override
    public Object get(final Object instance) {
        throw new UnsupportedOperationException();
    }

    public ElementType getElementType() {
        return ElementType.CONSTRUCTOR;
    }

    public Type getJavaType() {
        return constructor.getDeclaringClass();
    }

    /**
     * {@inheritDoc}
     */
    public String getPropertyName() {
        return constructor.getDeclaringClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return constructor.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ConstructorAccess that = (ConstructorAccess) o;
        return constructor.equals(that.constructor);
    }

    public int hashCode() {
        return constructor.hashCode();
    }

    private static <T> T run(PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
    }
}
