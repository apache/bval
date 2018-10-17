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
import javax.validation.ElementKind;
import javax.validation.ValidationException;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.CrossParameterDescriptor;

import org.apache.bval.jsr.ApacheMessageContext;
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

public class ConstraintValidatorContextImpl<T> implements ConstraintValidatorContext, ApacheMessageContext {
    public class ConstraintViolationBuilderImpl implements ConstraintValidatorContext.ConstraintViolationBuilder {
        private final String template;
        private final PathImpl path;

        private boolean complete;

        ConstraintViolationBuilderImpl(String template, PathImpl path) {
            this.template = template;
            this.path = path;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NodeBuilderDefinedContext addNode(String name) {
            return new NodeBuilderDefinedContextImpl(extensiblePath().addProperty(name), this);
        }

        @Override
        public NodeBuilderCustomizableContext addPropertyNode(String name) {
            return new NodeBuilderCustomizableContextImpl(extensiblePath(), name, this);
        }

        @Override
        public LeafNodeBuilderCustomizableContext addBeanNode() {
            return new LeafNodeBuilderCustomizableContextImpl(extensiblePath(), this);
        }

        @Override
        public NodeBuilderDefinedContext addParameterNode(int index) {
            ofLegalState();
            Exceptions.raiseUnless(frame.descriptor instanceof CrossParameterDescriptor, ValidationException::new,
                "Cannot add parameter node for %s", f -> f.args(frame.descriptor.getClass().getName()));

            final CrossParameterD<?, ?> crossParameter =
                ComposedD.unwrap(frame.descriptor, CrossParameterD.class).findFirst().get();

            final String parameterName = crossParameter.getParent().getParameterDescriptors().get(index).getName();

            path.removeLeafNode();
            return new NodeBuilderDefinedContextImpl(path.addNode(new NodeImpl.ParameterNodeImpl(parameterName, index)),
                this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ConstraintValidatorContext addConstraintViolation() {
            return addConstraintViolation(path);
        }

        public synchronized ConstraintViolationBuilderImpl ofLegalState() {
            Validate.validState(!complete, "#addConstraintViolation() already called");
            return this;
        }

        public ConstraintValidatorContext addConstraintViolation(PathImpl p) {
            synchronized (this) {
                ofLegalState();
                complete = true;
            }
            addError(template, p);
            return ConstraintValidatorContextImpl.this;
        }

        @Override
        public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name,
            Class<?> containerType, Integer typeArgumentIndex) {
            ofLegalState();
            return new ContainerElementNodeBuilderCustomizableContextImpl(extensiblePath(), name,
                containerType, typeArgumentIndex, this);
        }

        private PathImpl extensiblePath() {
            if (path.isRootPath()) {
                return PathImpl.create();
            }
            final PathImpl result = PathImpl.copy(path);
            final NodeImpl leafNode = result.getLeafNode();
            if (leafNode.getKind() == ElementKind.BEAN
                && !(leafNode.isInIterable() || leafNode.getContainerClass() != null)) {
                result.removeLeafNode();
            }
            return result;
        }
    }

    private final ValidationJob<T>.Frame<?> frame;
    private final ConstraintD<?> constraint;
    private final Lazy<Set<ConstraintViolation<T>>> violations = new Lazy<>(HashSet::new);
    private boolean defaultConstraintViolationDisabled;

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

    ValidationJob<T>.Frame<?> getFrame() {
        return frame;
    }

    Set<ConstraintViolation<T>> getRequiredViolations() {
        if (!violations.optional().isPresent()) {
            if (defaultConstraintViolationDisabled) {
                Exceptions.raise(ValidationException::new, "Expected custom constraint violation(s)");
            }
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addError(String messageTemplate, PathImpl propertyPath) {
        violations.get().add(((ValidationJob) frame.getJob()).createViolation(messageTemplate, this, propertyPath));
    }

    @Override
    public String getConfigurationProperty(String propertyKey) {
        return frame.context.getValidatorContext().getFactory().getProperties().get(propertyKey);
    }
}
