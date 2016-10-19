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

import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Description: direct field access strategy.<br/>
 */
@Privilizing(@CallTo(Reflection.class))
public class FieldAccess extends AccessStrategy {

    private final Field field;

    /**
     * Create a new FieldAccess instance.
     * @param field
     */
    public FieldAccess(final Field field) {
        this.field = field;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final Object instance) {
        final boolean mustUnset = Reflection.setAccessible(field, true);
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (mustUnset) {
                Reflection.setAccessible(field, false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ElementType getElementType() {
        return ElementType.FIELD;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getJavaType() {
        return field.getGenericType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPropertyName() {
        return field.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return field.toString();
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

        FieldAccess that = (FieldAccess) o;

        return field.equals(that.field);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return field.hashCode();
    }
}
