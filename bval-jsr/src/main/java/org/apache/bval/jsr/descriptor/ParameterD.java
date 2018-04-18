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

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import javax.validation.metadata.ParameterDescriptor;

import org.apache.bval.jsr.metadata.Meta;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.TypeUtils;

public class ParameterD<P extends ExecutableD<?, ?, P>> extends CascadableContainerD<P, Parameter>
    implements ParameterDescriptor {

    private final int index;
    private final String name;
    private final Class<?> type;

    ParameterD(Meta.ForParameter meta, int index, MetadataReader.ForContainer<Parameter> reader, P parent) {
        super(reader, parent);

        Validate.isTrue(index >= 0 && index < meta.getHost().getDeclaringExecutable().getParameterCount(),
            "Invalid parameter index %d", index);

        this.index = index;

        name = reader.meta.getName();
        type = resolveType();
    }

    @Override
    public Class<?> getElementClass() {
        return type;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getName() {
        return name;
    }

    private Class<?> resolveType() {
        final Class<?> declaringClass = getTarget().getDeclaringExecutable().getDeclaringClass();

        Type t = getTarget().getParameterizedType();

        int arrayDepth = 0;
        while (t instanceof GenericArrayType) {
            arrayDepth++;
            t = ((GenericArrayType) t).getGenericComponentType();
        }

        Class<?> result = null;

        while (result == null) {
            result = TypeUtils.getRawType(t, declaringClass);
            if (result != null) {
                break;
            }
            if (t instanceof TypeVariable<?>) {
                final TypeVariable<?> tv = (TypeVariable<?>) t;
                t = tv.getBounds()[0];
            }
        }
        return arrayDepth > 0 ? Array.newInstance(result, new int[arrayDepth]).getClass() : result;
    }
}
