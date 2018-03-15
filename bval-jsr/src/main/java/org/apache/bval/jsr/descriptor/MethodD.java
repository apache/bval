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
package org.apache.bval.jsr.descriptor;

import java.lang.reflect.Method;

import javax.validation.metadata.MethodDescriptor;
import javax.validation.metadata.MethodType;

import org.apache.bval.jsr.util.Methods;

class MethodD extends ExecutableD<Method, MetadataReader.ForMethod, MethodD> implements MethodDescriptor {
    private final MethodType methodType;

    MethodD(MetadataReader.ForMethod reader, BeanD<?> parent) {
        super(reader, parent);
        methodType = Methods.isGetter(reader.meta.getHost()) ? MethodType.GETTER : MethodType.NON_GETTER;
    }

    @Override
    public Class<?> getElementClass() {
        return getTarget().getReturnType();
    }

    MethodType getMethodType() {
        return methodType;
    }
}
