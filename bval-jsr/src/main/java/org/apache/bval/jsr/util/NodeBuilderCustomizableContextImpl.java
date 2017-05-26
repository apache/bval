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

import org.apache.bval.jsr.ConstraintValidatorContextImpl;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ElementKind;

/**
 * Description: implementation of {@link javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext}.<br/>
 */
public final class NodeBuilderCustomizableContextImpl
    implements ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext {
    private final ConstraintValidatorContextImpl parent;
    private final String messageTemplate;
    private final PathImpl propertyPath;
    private NodeImpl node;

    /**
     * Create a new NodeBuilderCustomizableContextImpl instance.
     * @param contextImpl
     * @param template
     * @param path
     * @param name
     */
    public NodeBuilderCustomizableContextImpl(ConstraintValidatorContextImpl contextImpl, String template, PathImpl path,
        String name) {
        parent = contextImpl;
        messageTemplate = template;
        propertyPath = path;

        if (propertyPath.isRootPath() || propertyPath.getLeafNode().getKind() != null) {
            node = new NodeImpl.PropertyNodeImpl(name);
        } else {
            node = propertyPath.removeLeafNode();
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
        return new NodeContextBuilderImpl(parent, messageTemplate, propertyPath, node);
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
        propertyPath.addNode(node);
        node = new NodeImpl.PropertyNodeImpl(name);
        return this;
    }

    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderCustomizableContext addBeanNode() {
        propertyPath.addNode(node);
        return new LeafNodeBuilderCustomizableContextImpl(parent, messageTemplate, propertyPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintValidatorContext addConstraintViolation() {
        propertyPath.addNode(node);
        node = null;
        parent.addError(messageTemplate, propertyPath);
        return parent;
    }

}
