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

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Description: direct field access strategy.<br/>
 */
public class FieldAccess extends AccessStrategy {

    private final Field field;

    /**
     * Create a new FieldAccess instance.
     * @param field
     */
    public FieldAccess(final Field field) {
        this.field = field;
        if (!field.isAccessible()) {
            run(new PrivilegedAction<Void>() {
                public Void run() {
                    field.setAccessible(true);
                    return null;
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object get(final Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ElementType getElementType() {
        return ElementType.FIELD;
    }

    /**
     * {@inheritDoc}
     */
    public Type getJavaType() {
        return field.getGenericType();
    }

    /**
     * {@inheritDoc}
     */
    public String getPropertyName() {
        return field.getName();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return field.toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldAccess that = (FieldAccess) o;

        return field.equals(that.field);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return field.hashCode();
    }

    private static <T> T run(PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
    }
}
