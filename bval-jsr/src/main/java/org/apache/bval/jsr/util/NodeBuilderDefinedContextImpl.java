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

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.ContainerElementNodeBuilderCustomizableContext;

import org.apache.bval.jsr.job.ConstraintValidatorContextImpl;

/**
 * Description: Implementation of {@link NodeBuilderDefinedContext}.<br/>
 */
public final class NodeBuilderDefinedContextImpl
    implements ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext {
    private final PathImpl path;
    private final ConstraintValidatorContextImpl<?>.ConstraintViolationBuilderImpl builder;

    /**
     * Create a new NodeBuilderDefinedContextImpl instance.
     * 
     * @param path
     * @param builder
     */
    public NodeBuilderDefinedContextImpl(PathImpl path,
        ConstraintValidatorContextImpl<?>.ConstraintViolationBuilderImpl builder) {
        this.path = path;
        this.builder = builder.ofLegalState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext addNode(String name) {
        return addPropertyNode(name);
    }

    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext addPropertyNode(
        String name) {
        return new NodeBuilderCustomizableContextImpl(path, name, builder);
    }

    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderCustomizableContext addBeanNode() {
        return new LeafNodeBuilderCustomizableContextImpl(path, builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintValidatorContext addConstraintViolation() {
        return builder.addConstraintViolation(path);
    }

    @Override
    public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name, Class<?> containerType,
        Integer typeArgumentIndex) {
        return new ContainerElementNodeBuilderCustomizableContextImpl(path, name, containerType, typeArgumentIndex,
            builder);
    }
}
