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
package org.apache.bval.jsr.job;

import java.util.HashSet;
import java.util.Set;

import javax.validation.ClockProvider;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Path;
import javax.validation.ValidationException;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.CrossParameterDescriptor;

import org.apache.bval.jsr.descriptor.ComposedD;
import org.apache.bval.jsr.descriptor.ConstraintD;
import org.apache.bval.jsr.descriptor.CrossParameterD;
import org.apache.bval.jsr.util.ContainerElementNodeBuilderCustomizableContextImpl;
import org.apache.bval.jsr.util.LeafNodeBuilderCustomizableContextImpl;
import org.apache.bval.jsr.util.NodeBuilderCustomizableContextImpl;
import org.apache.bval.jsr.util.NodeBuilderDefinedContextImpl;
import org.apache.bval.jsr.util.NodeImpl;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.Validate;

public class ConstraintValidatorContextImpl<T> implements ConstraintValidatorContext, MessageInterpolator.Context {
    private class ConstraintViolationBuilderImpl implements ConstraintValidatorContext.ConstraintViolationBuilder {
        private final String template;
        private final PathImpl path;

        ConstraintViolationBuilderImpl(String template, PathImpl path) {
            this.template = template;
            this.path = path;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NodeBuilderDefinedContext addNode(String name) {
            PathImpl p;
            if (path.isRootPath()) {
                p = PathImpl.create();
                p.getLeafNode().setName(name);
            } else {
                p = PathImpl.copy(path);
                p.addNode(new NodeImpl.PropertyNodeImpl(name));
            }
            return new NodeBuilderDefinedContextImpl(ConstraintValidatorContextImpl.this, template, p);
        }

        @Override
        public NodeBuilderCustomizableContext addPropertyNode(String name) {
            return new NodeBuilderCustomizableContextImpl(ConstraintValidatorContextImpl.this, template, path, name);
        }

        @Override
        public LeafNodeBuilderCustomizableContext addBeanNode() {
            return new LeafNodeBuilderCustomizableContextImpl(ConstraintValidatorContextImpl.this, template, path);
        }

        @Override
        public NodeBuilderDefinedContext addParameterNode(int index) {
            Exceptions.raiseUnless(frame.descriptor instanceof CrossParameterDescriptor, ValidationException::new,
                "Cannot add parameter node for %s", frame.descriptor.getClass().getName());

            final CrossParameterD<?, ?> crossParameter =
                ComposedD.unwrap(frame.descriptor, CrossParameterD.class).findFirst().get();

            final String parameterName = crossParameter.getParent().getParameterDescriptors().get(index).getName();

            final NodeImpl node = new NodeImpl.ParameterNodeImpl(parameterName, index);
            if (!path.isRootPath()) {
                path.removeLeafNode();
            }
            path.addNode(node);
            return new NodeBuilderDefinedContextImpl(ConstraintValidatorContextImpl.this, template, path);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ConstraintValidatorContext addConstraintViolation() {
            addError(template, path);
            return ConstraintValidatorContextImpl.this;
        }

        @Override
        public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name,
            Class<?> containerType, Integer typeArgumentIndex) {
            return new ContainerElementNodeBuilderCustomizableContextImpl(ConstraintValidatorContextImpl.this, template,
                path, name, containerType, typeArgumentIndex);
        }
    }

    private final ValidationJob<T>.Frame<?> frame;
    private final ConstraintD<?> constraint;
    private final Lazy<Set<ConstraintViolation<T>>> violations = new Lazy<>(HashSet::new);
    private boolean defaultConstraintViolationDisabled;

    /**
     * Temporary for code migration
     */
    // TODO delete
    @Deprecated
    protected ConstraintValidatorContextImpl() {
        this.frame = null;
        this.constraint = null;
    }

    ConstraintValidatorContextImpl(ValidationJob<T>.Frame<?> frame, ConstraintD<?> constraint) {
        super();
        this.frame = Validate.notNull(frame, "frame");
        this.constraint = Validate.notNull(constraint, "constraint");
    }

    @Override
    public void disableDefaultConstraintViolation() {
        this.defaultConstraintViolationDisabled = true;
    }

    @Override
    public String getDefaultConstraintMessageTemplate() {
        return constraint.getMessageTemplate();
    }

    @Override
    public ConstraintViolationBuilder buildConstraintViolationWithTemplate(String messageTemplate) {
        return new ConstraintViolationBuilderImpl(messageTemplate, frame.context.getPath());
    }

    @Override
    public ClockProvider getClockProvider() {
        return frame.getJob().validatorContext.getClockProvider();
    }

    @Override
    public <U> U unwrap(Class<U> type) {
        try {
            return type.cast(this);
        } catch (ClassCastException e) {
            throw new ValidationException(e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addError(String messageTemplate, Path propertyPath) {
        violations.get().add(((ValidationJob) frame.getJob()).createViolation(messageTemplate, this, propertyPath));
    }

    ValidationJob<T>.Frame<?> getFrame() {
        return frame;
    }

    Set<ConstraintViolation<T>> getRequiredViolations() {
        if (!violations.optional().isPresent()) {
            Exceptions.raiseIf(defaultConstraintViolationDisabled, ValidationException::new,
                "Expected custom constraint violation(s)");

            addError(getDefaultConstraintMessageTemplate(), frame.context.getPath());
        }
        return violations.get();
    }

    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return constraint;
    }

    @Override
    public Object getValidatedValue() {
        return frame.context.getValue();
    }
}
