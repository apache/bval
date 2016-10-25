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
 * Description: Implementation of {@link NodeBuilderDefinedContext}.<br/>
 */
public final class NodeBuilderDefinedContextImpl
    implements ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext {
    private final ConstraintValidatorContextImpl parent;
    private final String messageTemplate;
    private final PathImpl propertyPath;

    /**
     * Create a new NodeBuilderDefinedContextImpl instance.
     * @param contextImpl
     * @param template
     * @param path
     */
    public NodeBuilderDefinedContextImpl(ConstraintValidatorContextImpl contextImpl, String template, PathImpl path) {
        parent = contextImpl;
        messageTemplate = template;
        propertyPath = path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext addNode(String name) {
        // Node not yet added, wait until more information is provided
        return new NodeBuilderCustomizableContextImpl(parent, messageTemplate, propertyPath, name);
    }

    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext addPropertyNode(
        String name) {
        return new NodeBuilderCustomizableContextImpl(parent, messageTemplate, propertyPath, name);
    }

    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderCustomizableContext addBeanNode() {
        final NodeImpl node = new NodeImpl((String) null);
        node.setKind(ElementKind.BEAN);
        propertyPath.addNode(node);
        return new LeafNodeBuilderCustomizableContextImpl(parent, messageTemplate, propertyPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintValidatorContext addConstraintViolation() {
        parent.addError(messageTemplate, propertyPath);
        return parent;
    }
}
