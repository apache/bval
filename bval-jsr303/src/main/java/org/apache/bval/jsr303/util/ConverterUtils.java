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
package org.apache.bval.jsr303.util;

import javax.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Description: method for value conversion<br/>
 * User: roman <br/>
 * Date: 25.11.2009 <br/>
 * Time: 09:32:44 <br/>
 * Copyright: Agimatec GmbH
 */
public final class ConverterUtils {
    private static interface Converter {
        Object fromString(String value) throws Exception;
    }

    private static final Map<String, Converter> converterMap;

    static {
        converterMap = new HashMap<String, Converter>();
        converterMap.put(byte.class.getName(), new Converter() {
            public Byte fromString(String value) {
                return Byte.valueOf(value);
            }
        });
        converterMap.put(short.class.getName(), new Converter() {
            public Short fromString(String value) {
                return Short.valueOf(value);
            }
        });
        converterMap.put(int.class.getName(), new Converter() {
            public Integer fromString(String value) {
                return Integer.valueOf(value);
            }
        });
        converterMap.put(long.class.getName(), new Converter() {
            public Long fromString(String value) {
                return Long.valueOf(value);
            }
        });
        converterMap.put(float.class.getName(), new Converter() {
            public Float fromString(String value) {
                return Float.valueOf(value);
            }
        });
        converterMap.put(double.class.getName(), new Converter() {
            public Double fromString(String value) {
                return Double.valueOf(value);
            }
        });
        converterMap.put(boolean.class.getName(), new Converter() {
            public Boolean fromString(String value) {
                return Boolean.valueOf(value);
            }
        });
        converterMap.put(char.class.getName(), new Converter() {
            public Character fromString(String value) {
                if (value.length() != 1) {
                    throw new ValidationException("Invalid char value: " + value);
                }
                return value.charAt(0);
            }
        });
        converterMap.put(String.class.getName(), new Converter() {
            public String fromString(String value) {
                return value;
            }
        });
        converterMap.put(Class.class.getName(), new Converter() {
            public Class<?> fromString(String value) {
                return SecureActions.loadClass(value, getClass());
            }
        });

    }


    /** implementation of spec: 7.1.3. Converting the string representation of a value */
    public static Object fromStringToType(String value, Class<?> type) {
        Converter converter = converterMap.get(type.getName());
        if (converter != null) {
            try {
                return converter.fromString(value);
            } catch (Exception e) {
                throw new ValidationException(
                      "Invalid " + type.getSimpleName() + " format: " + value, e);
            }
        } else if (type.isEnum()) {
            try {
                @SuppressWarnings("unchecked")
                final Class<Enum> enumClass = (Class<Enum>) type;
                return Enum.valueOf(enumClass, value);
            } catch (Exception e) { // IllegalArgumentException
                throw new ValidationException(
                      "Invalid type: " + type + ". There is no enum member: " + value);
            }
        } else {
            // spec: If any of the string representation does not match its type counterpart,
            // a ValidationException is raised.
            throw new ValidationException("Cannot convert " + value + " to " + type.getName());
        }

    }
}
