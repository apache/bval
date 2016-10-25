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
import org.apache.bval.model.ValidationListener;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.ValidationException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Description: Short-lived {@link ConstraintValidatorContext} implementation passed by
 * a {@link ConstraintValidation} to its adapted {@link ConstraintValidator}. <br/>
 */
public class ConstraintValidatorContextImpl implements ConstraintValidatorContext {
    private final List<ValidationListener.Error> errorMessages = new LinkedList<ValidationListener.Error>();

    private final ConstraintValidation<?> constraintDescriptor;
    private final GroupValidationContext<?> validationContext;

    private boolean defaultDisabled;

    /**
     * Create a new ConstraintValidatorContextImpl instance.
     * @param validationContext
     * @param aConstraintValidation
     */
    public ConstraintValidatorContextImpl(GroupValidationContext<?> validationContext,
        ConstraintValidation<?> aConstraintValidation) {
        this.validationContext = validationContext;
        this.constraintDescriptor = aConstraintValidation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableDefaultConstraintViolation() {
        defaultDisabled = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultConstraintMessageTemplate() {
        return constraintDescriptor.getMessageTemplate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintViolationBuilder buildConstraintViolationWithTemplate(String messageTemplate) {
        return new ConstraintViolationBuilderImpl(this, messageTemplate, validationContext.getPropertyPath());
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new ValidationException("Type " + type + " not supported");
    }

    private static final class ConstraintViolationBuilderImpl
        implements ConstraintValidatorContext.ConstraintViolationBuilder {
        private final ConstraintValidatorContextImpl parent;
        private final String messageTemplate;
        private final PathImpl propertyPath;

        /**
         * Create a new ConstraintViolationBuilderImpl instance.
         * @param contextImpl
         * @param template
         * @param path
         */
        ConstraintViolationBuilderImpl(ConstraintValidatorContextImpl contextImpl, String template, PathImpl path) {
            parent = contextImpl;
            messageTemplate = template;
            propertyPath = path;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NodeBuilderDefinedContext addNode(String name) {
            PathImpl path;
            if (propertyPath.isRootPath()) {
                path = PathImpl.create();
                path.getLeafNode().setName(name);
            } else {
                path = PathImpl.copy(propertyPath);
                path.addNode(new NodeImpl(name));
            }
            return new NodeBuilderDefinedContextImpl(parent, messageTemplate, path);
        }

        @Override
        public NodeBuilderCustomizableContext addPropertyNode(String name) {
            final NodeImpl node;
            if (!propertyPath.isRootPath()) {
                if (propertyPath.getLeafNode().getKind() != null) {
                    node = new NodeImpl.PropertyNodeImpl(name);
                    propertyPath.addNode(node);
                } else {
                    node = propertyPath.getLeafNode();
                }
            } else {
                node = new NodeImpl.PropertyNodeImpl(name);
                propertyPath.addNode(node);
            }
            node.setName(name);
            node.setKind(ElementKind.PROPERTY); // enforce it
            return new NodeBuilderCustomizableContextImpl(parent, messageTemplate, propertyPath);
        }

        @Override
        public LeafNodeBuilderCustomizableContext addBeanNode() {
            final NodeImpl node = new NodeImpl.BeanNodeImpl();
            node.setKind(ElementKind.BEAN);
            propertyPath.addNode(node);
            return new LeafNodeBuilderCustomizableContextImpl(parent, messageTemplate, propertyPath);
        }

        @Override
        public NodeBuilderDefinedContext addParameterNode(int index) {
            final Method method = parent.validationContext.getMethod();
            final List<String> parameters =
                parent.validationContext.getParameterNameProvider().getParameterNames(method);
            final NodeImpl node = new NodeImpl.ParameterNodeImpl(parameters.get(index), index);
            node.setParameterIndex(index);
            node.setKind(ElementKind.PARAMETER);
            if (!propertyPath.isRootPath()) {
                propertyPath.removeLeafNode();
            }
            propertyPath.addNode(node);
            return new NodeBuilderDefinedContextImpl(parent, messageTemplate, propertyPath);
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

    /**
     * Get the queued error messages.
     * @return List
     */
    public List<ValidationListener.Error> getErrorMessages() {
        if (defaultDisabled && errorMessages.isEmpty()) {
            throw new ValidationException(
                "At least one custom message must be created if the default error message gets disabled.");
        }

        List<ValidationListener.Error> returnedErrorMessages = new ArrayList<ValidationListener.Error>(errorMessages);
        if (!defaultDisabled) {
            returnedErrorMessages.add(new ValidationListener.Error(getDefaultConstraintMessageTemplate(),
                validationContext.getPropertyPath(), null));
        }
        return returnedErrorMessages;
    }

    /**
     * Get this {@link ConstraintValidatorContext}'s {@link GroupValidationContext}.
     * @return {@link GroupValidationContext}
     */
    public GroupValidationContext<?> getValidationContext() {
        return validationContext;
    }

    /**
     * Add an error message to this {@link ConstraintValidatorContext}.
     * @param messageTemplate
     * @param propertyPath
     */
    public void addError(String messageTemplate, Path propertyPath) {
        errorMessages.add(new ValidationListener.Error(messageTemplate, propertyPath, null));
    }
}
