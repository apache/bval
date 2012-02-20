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
import java.lang.reflect.Type;

/**
 * Description: abstract class to encapsulate different strategies
 * to get the value of a Property.  This class is designed such that
 * subclasses are intended to know internally to which property they refer,
 * with only the particular target instance being externally required
 * to calculate the property's value.  One intent of this design is
 * that the notion of the very definition of a property is abstracted
 * along with the mechanism for accessing that property.<br/>
 */
public abstract class AccessStrategy {
    /**
     * Get the value from the given instance.
     * @param instance
     * @return the value
     * @throws IllegalArgumentException in case of an error
     */
    public abstract Object get(Object instance);

    /**
     * Get the Java program {@link ElementType} used by this {@link AccessStrategy}
     * to determine property values.
     * @return ElementType
     */
    public abstract ElementType getElementType();

    /**
     * Get the type of the property
     * @return Type
     */
    public abstract Type getJavaType();

    /**
     * Get a name representative of this property.
     * @return String
     */
    public abstract String getPropertyName();
}
