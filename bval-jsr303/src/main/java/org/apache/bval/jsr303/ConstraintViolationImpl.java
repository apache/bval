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

/**
 * Description: Describe a constraint validation defect<br/>
 * From rootBean and propertyPath, it is possible to rebuild the context of the failure
 */
class ConstraintViolationImpl<T> implements ConstraintViolation<T> {
    private final String messageTemplate;
    private final String message;
    /** root bean validation was invoked on. */
    private final T rootBean;
    private final Class<T> rootBeanClass;
    /** last bean validated. */
    private final Object leafBean;
    private final Object value;
    private final Path propertyPath;
    private final ConstraintDescriptor constraintDescriptor;
    

    /**
     * @param messageTemplate - message reason (raw message) 
     * @param message - interpolated message (locale specific)
     * @param rootBean
     * @param leafBean
     * @param propertyPath
     * @param value
     * @param constraintDescriptor
     */
    public ConstraintViolationImpl(String messageTemplate, String message, T rootBean, Object leafBean,
                                   Path propertyPath, Object value,
                                   ConstraintDescriptor constraintDescriptor, Class<T> rootBeanClass) {
        this.messageTemplate = messageTemplate;
        this.message = message;
        this.rootBean = rootBean;
        this.rootBeanClass = rootBeanClass;
        this.propertyPath = propertyPath;
        this.leafBean = leafBean;
        this.value = value;
        this.constraintDescriptor = constraintDescriptor;
    }

    /**
     * former name getInterpolatedMessage()
     * @return The interpolated error message for this constraint violation.
     **/
    public String getMessage() {
        return message;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    /** Root bean being validated validated */
    public T getRootBean() {
        return rootBean;
    }

    public Class<T> getRootBeanClass() {
        return rootBeanClass;
    }

    public Object getLeafBean() {
        return leafBean;
    }

    /** The value failing to pass the constraint */
    public Object getInvalidValue() {
        return value;
    }

    /**
     * the property path to the value from <code>rootBean</code>
     * Null if the value is the rootBean itself
     */
    public Path getPropertyPath() {
        return propertyPath;
    }

    public ConstraintDescriptor getConstraintDescriptor() {
        return constraintDescriptor;
    }

    public String toString() {
        return "ConstraintViolationImpl{" + "rootBean=" + rootBean + ", propertyPath='" +
              propertyPath + '\'' + ", message='" + message + '\'' + ", leafBean=" +
              leafBean + ", value=" + value + '}';
    }
}
