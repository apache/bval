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
package org.apache.bval.jsr303;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import java.io.Serializable;
import java.lang.annotation.ElementType;

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
     */
    public ConstraintViolationImpl(String messageTemplate, String message, T rootBean, Object leafBean,
                                   Path propertyPath, Object value,
                                   ConstraintDescriptor<?> constraintDescriptor, Class<T> rootBeanClass, ElementType elementType) {
        this.messageTemplate = messageTemplate;
        this.message = message;
        this.rootBean = rootBean;
        this.rootBeanClass = rootBeanClass;
        this.propertyPath = propertyPath;
        this.leafBean = leafBean;
        this.value = value;
        this.constraintDescriptor = constraintDescriptor;
        this.elementType = elementType;
    }

    /**
     * {@inheritDoc}
     * former name getInterpolatedMessage()
     * @return The interpolated error message for this constraint violation.
     */
    public String getMessage() {
        return message;
    }

    /**
     * {@inheritDoc}
     */
    public String getMessageTemplate() {
        return messageTemplate;
    }

    /**
     * {@inheritDoc}
     * @return Root bean being validated
     */
    public T getRootBean() {
        return rootBean;
    }

    /**
     * {@inheritDoc}
     */
    public Class<T> getRootBeanClass() {
        return rootBeanClass;
    }

    /**
     * {@inheritDoc}
     */
    public Object getLeafBean() {
        return leafBean;
    }

    /**
     * {@inheritDoc}
     * @return The value failing to pass the constraint
     */
    public Object getInvalidValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     * @return the property path to the value from <code>rootBean</code>
     *         Null if the value is the rootBean itself
     */
    public Path getPropertyPath() {
        return propertyPath;
    }

    /**
     * {@inheritDoc}
     */
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return constraintDescriptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ConstraintViolationImpl{" + "rootBean=" + rootBean + ", propertyPath='" +
              propertyPath + '\'' + ", message='" + message + '\'' + ", leafBean=" +
              leafBean + ", value=" + value + '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((this.leafBean == null) ? 0 : this.leafBean.hashCode());
        result = prime * result
                + ((this.message == null) ? 0 : this.message.hashCode());
        result = prime
                * result
                + ((this.propertyPath == null) ? 0 : this.propertyPath
                        .hashCode());
        result = prime * result
                + ((this.rootBean == null) ? 0 : this.rootBean.hashCode());
        result = prime * result
                + ((this.value == null) ? 0 : this.value.hashCode());
        result = prime * result
                + ((this.elementType == null) ? 0 : this.elementType.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     * NOTE: Needed to avoid duplication in the reported violations.
     * 
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ConstraintViolationImpl<?>)) {
            return false;
        }

        ConstraintViolationImpl<?> other = (ConstraintViolationImpl<?>) obj;

        if (this.leafBean == null) {
            if (other.leafBean != null) {
                return false;
            }
        } else if (!this.leafBean.equals(other.leafBean)) {
            return false;
        }
        
        if (this.message == null) {
            if (other.message != null) {
                return false;
            }
        } else if (!this.message.equals(other.message)) {
            return false;
        }
        
        if (this.propertyPath == null) {
            if (other.propertyPath != null) {
                return false;
            }
        } else if (!this.propertyPath.equals(other.propertyPath)) {
            return false;
        }
        
        if (this.rootBean == null) {
            if (other.rootBean != null) {
                return false;
            }
        } else if (!this.rootBean.equals(other.rootBean)) {
            return false;
        }
        
        if (this.rootBeanClass != other.rootBeanClass) {
            return false;
        }
        
        if (this.value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!this.value.equals(other.value)) {
            return false;
        }
        
        if (this.elementType != other.elementType) {
            return false;
        }
        
        return true;
    }
    
    
}
