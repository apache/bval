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
package com.agimatec.validation.util;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.PrivilegedAction;

/**
 * Description: direct field access.<br/>
 * User: roman <br/>
 * Date: 29.10.2009 <br/>
 * Time: 12:13:08 <br/>
 * Copyright: Agimatec GmbH
 */
public class FieldAccess extends AccessStrategy {

    private final Field field;

    public Type getJavaType() {
        return field.getGenericType();
    }

    public FieldAccess(Field field) {
        this.field = field;
        if(!field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    public Object get(final Object instance) {
        return PrivilegedActions.run(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    return field.get(instance);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });
    }

    public ElementType getElementType() {
        return ElementType.FIELD;
    }

    public String getPropertyName() {
        return field.getName();
    }

    public String toString() {
        return field.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldAccess that = (FieldAccess) o;

        return field.equals(that.field);
    }

    public int hashCode() {
        return field.hashCode();
    }
}
