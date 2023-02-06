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

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.ContainerElementNodeBuilderCustomizableContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.ContainerElementNodeBuilderDefinedContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.ContainerElementNodeContextBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderCustomizableContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

import org.apache.bval.jsr.job.ConstraintValidatorContextImpl;

public class ContainerElementNodeContextBuilderImpl implements ContainerElementNodeContextBuilder {
    private final PathImpl path;
    private ConstraintValidatorContextImpl<?>.ConstraintViolationBuilderImpl builder;

    ContainerElementNodeContextBuilderImpl(PathImpl path,
        ConstraintValidatorContextImpl<?>.ConstraintViolationBuilderImpl builder) {
        super();
        this.builder = builder.ofLegalState();
        this.path = path;
        path.getLeafNode().inIterable();
    }

    @Override
    public ContainerElementNodeBuilderDefinedContext atKey(Object key) {
        path.getLeafNode().setKey(key);
        return new ContainerElementNodeBuilderDefinedContextImpl(path, builder);
    }

    @Override
    public ContainerElementNodeBuilderDefinedContext atIndex(Integer index) {
        path.getLeafNode().setIndex(index);
        return new ContainerElementNodeBuilderDefinedContextImpl(path, builder);
    }

    @Override
    public NodeBuilderCustomizableContext addPropertyNode(String name) {
        return new NodeBuilderCustomizableContextImpl(path, name, builder);
    }

    @Override
    public LeafNodeBuilderCustomizableContext addBeanNode() {
        return new LeafNodeBuilderCustomizableContextImpl(path, builder);
    }

    @Override
    public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name, Class<?> containerType,
        Integer typeArgumentIndex) {
        return new ContainerElementNodeBuilderCustomizableContextImpl(path, name, containerType, typeArgumentIndex,
            builder);
    }

    @Override
    public ConstraintValidatorContext addConstraintViolation() {
        return builder.addConstraintViolation(path);
    }
}
