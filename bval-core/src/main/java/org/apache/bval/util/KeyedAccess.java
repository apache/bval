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

import org.apache.bval.util.reflection.TypeUtils;

import java.lang.annotation.ElementType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

/**
 * {@link AccessStrategy} to get a keyed value from a {@link Map}. Contains
 * special handling when a string key is used against a container type whose key
 * parameter is not assignable from {@link String}: against a map whose key type
 * is an enum class, it will be interpreted as a named enum constant; other key
 * types will be compared via {@link Object#toString()}.
 */
public class KeyedAccess extends AccessStrategy {
    private static final TypeVariable<?>[] MAP_TYPEVARS = Map.class.getTypeParameters();

    /**
     * Get the Java element type of a particular container type.
     * 
     * @param containerType
     * @return Type or <code>null</code> if <code>containerType</code> is not
     *         some kind of {@link Map}
     */
    public static Type getJavaElementType(Type containerType) {
        if (TypeUtils.isAssignable(containerType, Map.class)) {
            Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(containerType, Map.class);
            return ObjectUtils.defaultIfNull(TypeUtils.unrollVariables(typeArguments, MAP_TYPEVARS[1]), Object.class);
        }
        return null;
    }

    private final Type containerType;
    private final Object key;

    /**
     * Create a new KeyedAccess instance.
     * 
     * @param containerType
     * @param key
     */
    public KeyedAccess(Type containerType, Object key) {
        super();
        this.containerType = containerType;
        this.key = key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(Object instance) {
        if (instance instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) instance;
            Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(containerType, Map.class);
            Type keyType = TypeUtils.unrollVariables(typeArguments, MAP_TYPEVARS[0]);
            if (key == null || keyType == null || TypeUtils.isInstance(key, keyType)) {
                return map.get(key);
            }
            if (key instanceof String) {
                String name = (String) key;
                Class<?> rawKeyType = TypeUtils.getRawType(keyType, containerType);
                if (rawKeyType.isEnum()) {
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    final Object result = map.get(Enum.valueOf((Class<? extends Enum>) rawKeyType, name));
                    return result;
                }
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    if (name.equals(e.getKey())) {
                        return e.getValue();
                    }
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
        final Type result = getJavaElementType(containerType);
        return result == null ? Object.class : result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPropertyName() {
        return String.format("[%s]", key);
    }

}
