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
package org.apache.bval.util;

import java.lang.annotation.ElementType;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

import org.apache.bval.util.reflection.TypeUtils;

/**
 * {@link AccessStrategy} to get an indexed member of an {@link Iterable} or
 * array object.
 */
public class IndexedAccess extends AccessStrategy {
    private static final TypeVariable<?> ITERABLE_TYPE = Iterable.class.getTypeParameters()[0];

    /**
     * Get the Java element type of a particular container type.
     * 
     * @param containerType
     * @return Type or <code>null</code> if <code>containerType</code> is not
     *         some type of {@link Iterable} or array
     */
    public static Type getJavaElementType(Type containerType) {
        if (TypeUtils.isArrayType(containerType)) {
            return TypeUtils.getArrayComponentType(containerType);
        }
        if (TypeUtils.isAssignable(containerType, Iterable.class)) {
            Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(containerType, Iterable.class);
            Type type = TypeUtils.unrollVariables(typeArguments, ITERABLE_TYPE);
            return type != null ? type : Object.class;
        }
        return null;
    }

    private final Type containerType;
    private final Integer index;

    /**
     * Create a new IndexedAccessStrategy instance.
     * 
     * @param containerType
     * @param index
     */
    public IndexedAccess(Type containerType, Integer index) {
        super();
        this.containerType = containerType;
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(Object instance) {
        if (index == null) {
            throw new UnsupportedOperationException("Cannot read null index");
        }
        if (instance != null && instance.getClass().isArray()) {
            if (Array.getLength(instance) - index > 0) {
                return Array.get(instance, index);
            }
        } else if (instance instanceof List<?>) {
            List<?> list = (List<?>) instance;
            if (list.size() - index > 0) {
                return list.get(index);
            }
        } else if (instance instanceof Iterable<?>) {
            int i = 0;
            for (Object o : (Iterable<?>) instance) {
                if (++i == index) {
                    return o;
                }
            }
        }
        return null;
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
        return getJavaElementType(containerType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPropertyName() {
        return String.format("[%d]", index);
    }

}
