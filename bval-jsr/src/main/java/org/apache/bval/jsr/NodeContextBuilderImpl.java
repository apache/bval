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
package org.apache.bval.jsr;

import org.apache.bval.jsr.util.LeafNodeBuilderCustomizableContextImpl;
import org.apache.bval.jsr.util.NodeBuilderDefinedContextImpl;
import org.apache.bval.jsr.util.NodeImpl;
import org.apache.bval.jsr.util.PathImpl;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ElementKind;

public class NodeContextBuilderImpl implements ConstraintValidatorContext.ConstraintViolationBuilder.NodeContextBuilder {
    private final PathImpl path;
    private final String template;
    private final ConstraintValidatorContextImpl context;

    public NodeContextBuilderImpl(final ConstraintValidatorContextImpl context, final String template, final PathImpl path) {
        this.context = context;
        this.template = template;
        this.path = path;
    }

    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext atKey(Object key) {
        path.getLeafNode().setKey(key);
        return new NodeBuilderDefinedContextImpl(context, template, path);
    }

    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderDefinedContext atIndex(Integer index) {
        path.getLeafNode().setIndex(index);
        return new NodeBuilderDefinedContextImpl(context, template, path);
    }

    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext addNode(String name) {
        return new NodeBuilderCustomizableContextImpl(context, template, path).addNode(name);
    }

    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext addPropertyNode(String name) {
        return new NodeBuilderCustomizableContextImpl(context, template, path).addPropertyNode(name);
    }

    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderCustomizableContext addBeanNode() {
        final NodeImpl node = new NodeImpl.BeanNodeImpl();
        node.setKind(ElementKind.BEAN);
        path.addNode(node);
        return new LeafNodeBuilderCustomizableContextImpl(context, template, path);
    }

    @Override
    public ConstraintValidatorContext addConstraintViolation() {
        context.addError(template, path);
        return context;
    }
}
