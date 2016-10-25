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
package org.apache.bval.model;

/**
 * Description: ("artificial" enum with custom values).<br/>
 */
public class DynaTypeEnum implements DynaType {
    private final Class<?> enumClass;
    private Value[] enumConstants;

    /**
     * Create a new DynaTypeEnum instance.
     * @param enumClass
     */
    public DynaTypeEnum(Class<?> enumClass) {
        this.enumClass = enumClass;
    }

    /**
     * Create a new DynaTypeEnum instance.
     * @param enumClass
     * @param names
     */
    public DynaTypeEnum(Class<?> enumClass, String... names) {
        this(enumClass);
        setEnumNames(names);
    }

    /**
     * Set the enumeration value names.
     * @param names
     */
    public void setEnumNames(String[] names) {
        enumConstants = new Value[names.length];
        int i = 0;
        for (String each : names) {
            enumConstants[i++] = new Value(each);
        }
    }

    /**
     * Get the name of the enum class.
     * @return String
     */
    public String getName() {
        return enumClass.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getRawType() {
        return enumClass;
    }

    /**
     * Learn whether the referred class is, in fact, an enum class.
     * used by freemarker-template "bean-infos-json.ftl"
     */
    public boolean isEnum() {
        return enumClass.isEnum();
    }

    /**
     * Get the emulated constants.
     * used by freemarker-template "bean-infos-json.ftl"
     * @return Value[]
     */
    public Value[] getEnumConstants() {
        return enumConstants != null ? enumConstants.clone() : null;
    }

    /**
     * Learn whether the wrapped class is assignable from <code>cls</code>.
     * @param cls
     * @return boolean
     */
    public boolean isAssignableFrom(Class<?> cls) {
        return enumClass.isAssignableFrom(cls);
    }

    /**
     * Represents a single "enum" instance (= the value).
     */
    public static final class Value {
        final String name;

        /**
         * Create a new Value instance.
         * @param name
         */
        Value(String name) {
            this.name = name;
        }

        /**
         * used by freemarker-template "bean-infos-json.ftl"
         * @return the name of this constant
         */
        public String name() {
            return name;
        }

    }
}
