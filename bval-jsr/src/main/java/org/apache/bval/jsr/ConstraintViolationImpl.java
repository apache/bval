/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.bval.jsr;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.ValidationException;
import javax.validation.metadata.ConstraintDescriptor;

import org.apache.bval.util.Exceptions;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.Objects;

/**
 * Description: Describe a constraint validation defect.<br/>
 * From rootBean and propertyPath, it is possible to rebuild the context of the
 * failure
 */
public class ConstraintViolationImpl<T> implements ConstraintViolation<T>, Serializable {
    /** Serialization version */
    private static final long serialVersionUID = 1L;

    private final String messageTemplate;
    private final String message;
    /** root bean validation was invoked on. */
    private final T rootBean;
    private final Class<T> rootBeanClass;
    /** last bean validated. */
    private final Object leafBean;
    private final Object value;
    private final Path propertyPath;
    private final ElementType elementType;
    private final ConstraintDescriptor<?> constraintDescriptor;
    private final Object returnValue;
    private final Object[] parameters;
    private final int hashCode;

    /**
     * Create a new ConstraintViolationImpl instance.
     * 
     * @param messageTemplate
     *            - message reason (raw message)
     * @param message
     *            - interpolated message (locale specific)
     * @param rootBean
     * @param leafBean
     * @param propertyPath
     * @param value
     * @param constraintDescriptor
     * @param rootBeanClass
     * @param elementType
     * @param returnValue
     * @param parameters
     */
    public ConstraintViolationImpl(String messageTemplate, String message, T rootBean, Object leafBean,
        Path propertyPath, Object value, ConstraintDescriptor<?> constraintDescriptor, Class<T> rootBeanClass,
        ElementType elementType, Object returnValue, Object[] parameters) {
        this.messageTemplate = messageTemplate;
        this.message = message;
        this.rootBean = rootBean;
        this.rootBeanClass = rootBeanClass;
        this.propertyPath = propertyPath;
        this.leafBean = leafBean;
        this.value = value;
        this.constraintDescriptor = constraintDescriptor;
        this.elementType = elementType;
        this.returnValue = returnValue;
        this.parameters = parameters;
        this.hashCode = Arrays.deepHashCode(new Object[] { messageTemplate, message, rootBean, rootBeanClass, leafBean,
            value, propertyPath, elementType, constraintDescriptor, returnValue, parameters });
    }

    /**
     * {@inheritDoc} former name getInterpolatedMessage()
     * 
     * @return The interpolated error message for this constraint violation.
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessageTemplate() {
        return messageTemplate;
    }

    /**
     * {@inheritDoc}
     * 
     * @return Root bean being validated
     */
    @Override
    public T getRootBean() {
        return rootBean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getRootBeanClass() {
        return rootBeanClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getLeafBean() {
        return leafBean;
    }

    @Override
    public Object[] getExecutableParameters() {
        return parameters;
    }

    @Override
    public Object getExecutableReturnValue() {
        return returnValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @return The value failing to pass the constraint
     */
    @Override
    public Object getInvalidValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     * 
     * @return the property path to the value from <code>rootBean</code> Null if
     *         the value is the rootBean itself
     */
    @Override
    public Path getPropertyPath() {
        return propertyPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return constraintDescriptor;
    }

    @Override
    public <U> U unwrap(Class<U> type) {
        if (!type.isInstance(this)) {
            Exceptions.raise(ValidationException::new, "Type %s is not supported", type);
        }
        return type.cast(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s{rootBean=%s, propertyPath='%s', message='%s', leafBean=%s, value=%s}",
            ConstraintViolationImpl.class.getSimpleName(), rootBean, propertyPath, message, leafBean, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }

        @SuppressWarnings("rawtypes")
        final ConstraintViolationImpl that = (ConstraintViolationImpl) o;

        return Objects.equals(constraintDescriptor, that.constraintDescriptor) && elementType == that.elementType
            && Objects.equals(leafBean, that.leafBean) && Objects.equals(message, that.message)
            && Objects.equals(messageTemplate, that.messageTemplate) && Arrays.equals(parameters, that.parameters)
            && Objects.equals(propertyPath, that.propertyPath) && Objects.equals(returnValue, that.returnValue)
            && Objects.equals(rootBean, that.rootBean) && Objects.equals(rootBeanClass, that.rootBeanClass)
            && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
