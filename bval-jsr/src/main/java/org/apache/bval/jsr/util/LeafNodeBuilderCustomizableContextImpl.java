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
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderCustomizableContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeBuilderDefinedContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.LeafNodeContextBuilder;

import org.apache.bval.jsr.job.ConstraintValidatorContextImpl;

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
            path.getLeafNode().setKey(key);
            return definedContext;
        }

        @Override
        public LeafNodeBuilderDefinedContext atIndex(Integer index) {
            path.getLeafNode().setIndex(index);
            return definedContext;
        }

        @Override
        public ConstraintValidatorContext addConstraintViolation() {
            return LeafNodeBuilderCustomizableContextImpl.this.addConstraintViolation();
        }
    }

    private final ConstraintValidatorContextImpl<?>.ConstraintViolationBuilderImpl builder;
    private final PathImpl path;

    public LeafNodeBuilderCustomizableContextImpl(
        PathImpl path, ConstraintValidatorContextImpl<?>.ConstraintViolationBuilderImpl builder) {
        this.builder = builder.ofLegalState();
        this.path = path.addBean();
    }

    @Override
    public LeafNodeContextBuilder inIterable() {
        builder.ofLegalState();
        path.getLeafNode().setInIterable(true);
        return new LeafNodeContextBuilderImpl();
    }

    @Override
    public ConstraintValidatorContext addConstraintViolation() {
        return builder.addConstraintViolation(path);
    }

    @Override
    public LeafNodeBuilderCustomizableContext inContainer(Class<?> containerType, Integer typeArgumentIndex) {
        builder.ofLegalState();
        path.getLeafNode().inContainer(containerType, typeArgumentIndex);
        return this;
    }
}
