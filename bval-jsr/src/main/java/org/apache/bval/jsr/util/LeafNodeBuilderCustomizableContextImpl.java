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

public class LeafNodeBuilderCustomizableContextImpl implements ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderCustomizableContext {
    private final ConstraintValidatorContextImpl context;
    private final PathImpl path;
    private final String template;
    private final NodeImpl node;

    public LeafNodeBuilderCustomizableContextImpl(final ConstraintValidatorContextImpl parent, String messageTemplate, PathImpl propertyPath) {
        context = parent;
        template = messageTemplate;
        path = propertyPath;
        node = new NodeImpl((String) null);
        node.setKind(ElementKind.BEAN);
    }

    @Override
    public ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeContextBuilder inIterable() {
        path.getLeafNode().setInIterable(true);
        return new LeafNodeContextBuilderImpl();
    }

    @Override
    public ConstraintValidatorContext addConstraintViolation() {
        context.addError(template, path);
        return context;
    }

    private class LeafNodeContextBuilderImpl implements ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeContextBuilder {
        @Override
        public ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderDefinedContext atKey(Object key) {
            path.getLeafNode().setKey(key);
            return new LeafNodeBuilderDefinedContextImpl(context, template, path);
        }

        @Override
        public ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderDefinedContext atIndex(Integer index) {
            path.getLeafNode().setIndex(index);
            return new LeafNodeBuilderDefinedContextImpl(context, template, path);
        }

        @Override
        public ConstraintValidatorContext addConstraintViolation() {
            context.addError(template, path);
            return context;
        }
    }
}
