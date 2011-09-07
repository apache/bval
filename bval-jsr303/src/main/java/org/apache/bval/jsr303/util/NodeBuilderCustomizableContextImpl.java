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
package org.apache.bval.jsr303.util;


import org.apache.bval.jsr303.ConstraintValidatorContextImpl;

import javax.validation.ConstraintValidatorContext;

/**
 * Description: implementation of {@link NodeBuilderCustomizableContext}.<br/>
 */
final class NodeBuilderCustomizableContextImpl
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
    NodeBuilderCustomizableContextImpl(ConstraintValidatorContextImpl contextImpl, String template,
                              PathImpl path, String name) {
        parent = contextImpl;
        messageTemplate = template;
        propertyPath = path;
        node = new NodeImpl(name);
    }

    /**
     * {@inheritDoc}
     */
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder inIterable() {
        // Modifies the "previous" node in the path
        node.setInIterable(true);
        return new NodeContextBuilderImpl(parent, messageTemplate, propertyPath, node);
    }

    /**
     * {@inheritDoc}
     */
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext addNode(
          String name) {
        propertyPath.addNode(node);
        node = new NodeImpl(name);
        return this; // Re-use this instance
    }

    /**
     * {@inheritDoc}
     */
    public ConstraintValidatorContext addConstraintViolation() {
        propertyPath.addNode(node);
        node = null;
        parent.addError(messageTemplate, propertyPath);
        return parent;
    }
    
}
