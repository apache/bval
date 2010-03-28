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
import javax.validation.Path;

import org.apache.bval.jsr303.ConstraintValidatorContextImpl;

/**
 * Description: <br/>
 * User: roman <br/>
 * Date: 28.09.2009 <br/>
 * Time: 15:30:38 <br/>
 * Copyright: Agimatec GmbH
 */
final class NodeContextBuilderImpl
      implements ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder {
    private final ConstraintValidatorContextImpl parent;
    private final String messageTemplate;
    private final PathImpl propertyPath;

    NodeContextBuilderImpl(ConstraintValidatorContextImpl contextImpl,
                                    String template, PathImpl path) {
        parent = contextImpl;
        messageTemplate = template;
        propertyPath = path;
        propertyPath.getLeafNode().setInIterable(true);
    }

    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext atKey(
          Object key) {
        propertyPath.getLeafNode().setKey(key);
        return new NodeBuilderDefinedContextImpl(parent, messageTemplate, propertyPath);
    }

    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext atIndex(
          Integer index) {
        propertyPath.getLeafNode().setIndex(index);
        return new NodeBuilderDefinedContextImpl(parent, messageTemplate, propertyPath);
    }

    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext addNode(
          String name) {
        Path.Node node = new NodeImpl(name);
        propertyPath.addNode(node);
        return new NodeBuilderCustomizableContextImpl(parent, messageTemplate, propertyPath);
    }

    public ConstraintValidatorContext addConstraintViolation() {
        parent.addError(messageTemplate, propertyPath);
        return parent;
    }
}