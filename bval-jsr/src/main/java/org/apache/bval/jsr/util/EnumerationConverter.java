/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bval.jsr.util;

import org.apache.commons.beanutils.Converter;

/**
 * A {@code org.apache.commons.beanutils.Converter} implementation to handle
 * Enumeration type.
 */
@Deprecated
public final class EnumerationConverter implements Converter {

    /**
     * The static converter instance.
     */
    private static final EnumerationConverter INSTANCE = new EnumerationConverter();

    /**
     * Returns this converter instance.
     *
     * @return this converter instance.
     */
    public static EnumerationConverter getInstance() {
        return INSTANCE;
    }

    /**
     * This class can't be instantiated.
     */
    private EnumerationConverter() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object convert(Class type, Object value) {
        if (!type.isEnum()) {
            throw new RuntimeException("Only enum types supported in this version!");
        }

        if (value == null) {
            throw new RuntimeException("Null values not supported in this version!");
        }

        if (String.class != value.getClass()) {
            throw new RuntimeException("Only java.lang.String values supported in this version!");
        }

        String stringValue = (String) value;

        final Class<Enum> enumClass = (Class<Enum>) type;
        return Enum.valueOf(enumClass, stringValue);
    }

}
