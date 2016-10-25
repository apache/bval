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
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.util.Arrays;

/**
 * Description: Describe a constraint validation defect.<br/>
 * From rootBean and propertyPath, it is possible to rebuild the context of the failure
 */
class ConstraintViolationImpl<T> implements ConstraintViolation<T>, Serializable {
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
     * @param messageTemplate - message reason (raw message)
     * @param message - interpolated message (locale specific)
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
        this.hashCode = computeHashCode();
    }

    /**
     * {@inheritDoc}
     * former name getInterpolatedMessage()
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
     * @return The value failing to pass the constraint
     */
    @Override
    public Object getInvalidValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     * @return the property path to the value from <code>rootBean</code>
     *         Null if the value is the rootBean itself
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
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new ValidationException("Type " + type + " is not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ConstraintViolationImpl{" + "rootBean=" + rootBean + ", propertyPath='" + propertyPath + '\''
            + ", message='" + message + '\'' + ", leafBean=" + leafBean + ", value=" + value + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ConstraintViolationImpl that = (ConstraintViolationImpl) o;

        if (constraintDescriptor != null ? !constraintDescriptor.equals(that.constraintDescriptor)
            : that.constraintDescriptor != null)
            return false;
        if (elementType != that.elementType)
            return false;
        if (leafBean != null ? !leafBean.equals(that.leafBean) : that.leafBean != null)
            return false;
        if (message != null ? !message.equals(that.message) : that.message != null)
            return false;
        if (messageTemplate != null ? !messageTemplate.equals(that.messageTemplate) : that.messageTemplate != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(parameters, that.parameters))
            return false;
        if (propertyPath != null ? !propertyPath.equals(that.propertyPath) : that.propertyPath != null)
            return false;
        if (returnValue != null ? !returnValue.equals(that.returnValue) : that.returnValue != null)
            return false;
        if (rootBean != null ? !rootBean.equals(that.rootBean) : that.rootBean != null)
            return false;
        if (rootBeanClass != null ? !rootBeanClass.equals(that.rootBeanClass) : that.rootBeanClass != null)
            return false;
        if (value != null ? !value.equals(that.value) : that.value != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public int computeHashCode() {
        int result = messageTemplate != null ? messageTemplate.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (rootBean != null ? rootBean.hashCode() : 0);
        result = 31 * result + (rootBeanClass != null ? rootBeanClass.hashCode() : 0);
        result = 31 * result + (leafBean != null ? leafBean.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (propertyPath != null ? propertyPath.hashCode() : 0);
        result = 31 * result + (elementType != null ? elementType.hashCode() : 0);
        result = 31 * result + (constraintDescriptor != null ? constraintDescriptor.hashCode() : 0);
        result = 31 * result + (returnValue != null ? returnValue.hashCode() : 0);
        result = 31 * result + (parameters != null ? Arrays.hashCode(parameters) : 0);
        return result;
    }
}
