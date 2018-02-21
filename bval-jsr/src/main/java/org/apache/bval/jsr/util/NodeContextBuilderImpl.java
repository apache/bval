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

import org.apache.bval.jsr.job.ConstraintValidatorContextImpl;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.ContainerElementNodeBuilderCustomizableContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder;

/**
 * Description: Implementation of {@link NodeContextBuilder}.<br/>
 */
public final class NodeContextBuilderImpl implements ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder {
    private final ConstraintValidatorContextImpl<?> context;
    private final String template;
    private final PathImpl path;
    // The name of the last "added" node, it will only be added if it has a non-null name
    // The actual incorporation in the path will take place when the definition of the current leaf node is complete
    private final NodeImpl node;

    /**
     * Create a new NodeContextBuilderImpl instance.
     * @param contextImpl
     * @param template
     * @param path
     */
    NodeContextBuilderImpl(ConstraintValidatorContextImpl<?> contextImpl, String template, PathImpl path, NodeImpl node) {
        this.context = contextImpl;
        this.template = template;
        this.path = path;
        this.node = node;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext atKey(Object key) {
        node.setKey(key);
        path.addNode(node);
        return new NodeBuilderDefinedContextImpl(context, template, path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext atIndex(Integer index) {
        node.setIndex(index);
        path.addNode(node);
        return new NodeBuilderDefinedContextImpl(context, template, path);
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
        path.addNode(node);
        return new NodeBuilderCustomizableContextImpl(context, template, path, name);
    }

    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderCustomizableContext addBeanNode() {
        path.addNode(node);
        return new LeafNodeBuilderCustomizableContextImpl(context, template, path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintValidatorContext addConstraintViolation() {
        path.addNode(node);
        context.addError(template, path);
        return context;
    }

    @Override
    public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name, Class<?> containerType,
        Integer typeArgumentIndex) {
        final NodeImpl node = new NodeImpl.ContainerElementNodeImpl(name, containerType, typeArgumentIndex);
        path.addNode(node);
        return new ContainerElementNodeBuilderCustomizableContextImpl(context, template, path, name, containerType,
            typeArgumentIndex);
    }

}
