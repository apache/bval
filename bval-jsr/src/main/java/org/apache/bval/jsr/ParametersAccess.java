/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.bval.jsr;

import org.apache.bval.util.AccessStrategy;

import java.lang.annotation.ElementType;
import java.lang.reflect.Type;

public class ParametersAccess extends AccessStrategy {
    @Override
    public Object get(final Object instance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ElementType getElementType() {
        return ElementType.PARAMETER;
    }

    @Override
    public Type getJavaType() {
        return Object[].class;
    }

    @Override
    public String getPropertyName() {
        return null;
    }
}
