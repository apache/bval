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


import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder;

import org.apache.bval.jsr303.ConstraintValidatorContextImpl;

/**
 * Description: Implementation of {@link NodeContextBuilder}.<br/>
 */
final class NodeContextBuilderImpl
      implements ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder {
    private final ConstraintValidatorContextImpl parent;
    private final String messageTemplate;
    private final PathImpl propertyPath;
    // The name of the last "added" node, it will only be added if it has a non-null name
    // The actual incorporation in the path will take place when the definition of the current leaf node is complete
    private final String lastNodeName;

    /**
     * Create a new NodeContextBuilderImpl instance.
     * @param contextImpl
     * @param template
     * @param path
     * @param name
     */
    NodeContextBuilderImpl(ConstraintValidatorContextImpl contextImpl,
                                    String template, PathImpl path, String name) {
        parent = contextImpl;
        messageTemplate = template;
        propertyPath = path;
        lastNodeName = name;
    }

    /**
     * {@inheritDoc}
     */
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext atKey(
          Object key) {
        // Modifies the "previous" node in the path
        propertyPath.getLeafNode().setKey(key);
        addLastNodeIfNeeded();
        return new NodeBuilderDefinedContextImpl(parent, messageTemplate, propertyPath);
    }

    /**
     * {@inheritDoc}
     */
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext atIndex(
          Integer index) {
        // Modifies the "previous" node in the path
        propertyPath.getLeafNode().setIndex(index);
        addLastNodeIfNeeded();
        return new NodeBuilderDefinedContextImpl(parent, messageTemplate, propertyPath);
    }

    /**
     * {@inheritDoc}
     */
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext addNode(
          String name) {
        addLastNodeIfNeeded();
        return new NodeBuilderCustomizableContextImpl(parent, messageTemplate, propertyPath, lastNodeName);
    }

    /**
     * {@inheritDoc}
     */
    public ConstraintValidatorContext addConstraintViolation() {
        addLastNodeIfNeeded();
        parent.addError(messageTemplate, propertyPath);
        return parent;
    }
    
    private void addLastNodeIfNeeded() {
        if (lastNodeName != null) {
            NodeImpl node = new NodeImpl(lastNodeName);
            propertyPath.addNode(node);
        }
    }
}