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

import java.lang.reflect.AnnotatedType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.validation.metadata.ContainerElementTypeDescriptor;

import org.apache.bval.jsr.GraphContext;
import org.apache.bval.jsr.metadata.ContainerElementKey;
import org.apache.bval.util.Validate;

public class ContainerElementTypeD extends CascadableContainerD<CascadableContainerD<?, ?>, AnnotatedType>
    implements ContainerElementTypeDescriptor {

    private final ContainerElementKey key;
    /** {@link GraphContext#runtimeKey(ContainerElementKey)} of {@link #key} by runtime container class. */
    private final ConcurrentMap<Class<?>, ContainerElementKey> runtimeKeys = new ConcurrentHashMap<>();

    ContainerElementTypeD(ContainerElementKey key, MetadataReader.ForContainer<AnnotatedType> reader,
        CascadableContainerD<?, ?> parent) {
        super(reader, parent);
        this.key = Validate.notNull(key, "key");
    }

    @Override
    public Class<?> getContainerClass() {
        return key.getContainerClass();
    }

    @Override
    public Integer getTypeArgumentIndex() {
        return key.getTypeArgumentIndex();
    }

    public ContainerElementKey getKey() {
        return key;
    }

    /**
     * Get {@link #getKey()} in terms of the runtime type of the container held by {@code context}.
     *
     * @param context
     * @return {@link ContainerElementKey}
     * @see GraphContext#runtimeKey(ContainerElementKey)
     */
    public ContainerElementKey getRuntimeKey(GraphContext context) {
        final Object value = context.getValue();
        if (value == null || value.getClass() == key.getContainerClass()) {
            return key;
        }
        ContainerElementKey result = runtimeKeys.get(value.getClass());
        if (result == null) {
            result = context.runtimeKey(key);
            runtimeKeys.putIfAbsent(value.getClass(), result);
        }
        return result;
    }
}
