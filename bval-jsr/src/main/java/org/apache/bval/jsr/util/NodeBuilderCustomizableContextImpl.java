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
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import javax.validation.ElementKind;

import org.apache.bval.jsr.job.ConstraintValidatorContextImpl;

/**
 * Description: implementation of {@link javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext}.<br/>
 */
public final class NodeBuilderCustomizableContextImpl
    implements ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext {
    private final ConstraintValidatorContextImpl<?> context;
    private final String template;
    private final PathImpl path;
    private NodeImpl node;

    /**
     * Create a new NodeBuilderCustomizableContextImpl instance.
     * @param context
     * @param template
     * @param path
     * @param name
     */
    public NodeBuilderCustomizableContextImpl(ConstraintValidatorContextImpl<?> context, String template, PathImpl path,
        String name) {
        this.context = context;
        this.template = template;
        this.path = path;

        if (path.isRootPath() || path.getLeafNode().getKind() != null) {
            node = new NodeImpl.PropertyNodeImpl(name);
        } else {
            node = path.removeLeafNode();
            node.setName(name);
            node.setKind(ElementKind.PROPERTY); // enforce it
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder inIterable() {
        node.setInIterable(true);
        return new NodeContextBuilderImpl(context, template, path, node);
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
        node = new NodeImpl.PropertyNodeImpl(name);
        return this;
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
        node = null;
        context.addError(template, path);
        return context;
    }

    @Override
    public NodeBuilderCustomizableContext inContainer(Class<?> containerClass, Integer typeArgumentIndex) {
        path.getLeafNode().inContainer(containerClass, typeArgumentIndex);
        return this;
    }

    @Override
    public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name, Class<?> containerType,
        Integer typeArgumentIndex) {
        path.addNode(node);
        node = new NodeImpl.ContainerElementNodeImpl(name, containerType, typeArgumentIndex);
        return new ContainerElementNodeBuilderCustomizableContextImpl(context, template, path, name, containerType,
            typeArgumentIndex);
    }

}
