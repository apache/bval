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
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderDefinedContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeContextBuilder;

public class LeafNodeBuilderCustomizableContextImpl
    implements ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderCustomizableContext {

    private final class LeafNodeContextBuilderImpl implements LeafNodeContextBuilder {
        private final LeafNodeBuilderDefinedContext definedContext = new LeafNodeBuilderDefinedContext() {

            @Override
            public ConstraintValidatorContext addConstraintViolation() {
                return LeafNodeBuilderCustomizableContextImpl.this.addConstraintViolation();
            }
        };

        @Override
        public LeafNodeBuilderDefinedContext atKey(Object key) {
            node.setKey(key);
            return definedContext;
        }

        @Override
        public LeafNodeBuilderDefinedContext atIndex(
            Integer index) {
            node.setIndex(index);
            return definedContext;
        }

        @Override
        public ConstraintValidatorContext addConstraintViolation() {
            return LeafNodeBuilderCustomizableContextImpl.this.addConstraintViolation();
        }
    }

    private final ConstraintValidatorContextImpl context;
    private final PathImpl path;
    private final String template;
    private final NodeImpl node;

    public LeafNodeBuilderCustomizableContextImpl(final ConstraintValidatorContextImpl parent, String messageTemplate,
        PathImpl propertyPath) {
        context = parent;
        template = messageTemplate;
        path = propertyPath;
        node = new NodeImpl.BeanNodeImpl();
    }

    @Override
    public LeafNodeContextBuilder inIterable() {
        node.setInIterable(true);
        return new LeafNodeContextBuilderImpl();
    }

    @Override
    public ConstraintValidatorContext addConstraintViolation() {
        path.addNode(node);
        context.addError(template, path);
        return context;
    }

}
