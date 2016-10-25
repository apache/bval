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

import org.apache.bval.jsr.groups.Group;
import org.apache.bval.jsr.groups.Groups;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.ValidationContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ElementKind;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.Path;
import javax.validation.TraversableResolver;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Description: JSR-303 {@link ValidationContext} extension. <br/>
 */
public interface GroupValidationContext<T> extends ValidationContext<ConstraintValidationListener<T>> {

    /**
     * Get the groups of this {@link GroupValidationContext}.
     * @return the groups in their sequence for validation
     */
    Groups getGroups();

    void setCurrentGroups(Groups groups);

    /**
     * Set the current {@link Group}.
     * @param group to set
     */
    void setCurrentGroup(Group group);

    /**
     * Get the current {@link Group}.
     * @return Group
     */
    Group getCurrentGroup();

    /**
     * Get the property path.
     * @return {@link PathImpl}
     */
    PathImpl getPropertyPath();

    /**
     * Get the root {@link MetaBean}.
     * @return {@link MetaBean}
     */
    MetaBean getRootMetaBean();

    /**
     * Set the {@link ConstraintValidation}.
     * @param constraint to set
     */
    void setConstraintValidation(ConstraintValidation<?> constraint);

    /**
     * Get the {@link ConstraintValidation}.
     * @return {@link ConstraintValidation}
     */
    ConstraintValidation<?> getConstraintValidation();

    /**
     * Get the value being validated.
     * @return Object
     */
    Object getValidatedValue();

    /**
     * Set a fixed value for the context.
     * @param value to set
     */
    void setFixedValue(Object value);

    /**
     * Get the message resolver.
     * @return {@link MessageInterpolator}
     */
    MessageInterpolator getMessageResolver();

    /**
     * Get the {@link TraversableResolver}.
     * @return {@link TraversableResolver}
     */
    TraversableResolver getTraversableResolver();

    /**
     * Get the {@link ConstraintValidatorFactory}.
     * @return {@link ConstraintValidatorFactory}
     */
    ConstraintValidatorFactory getConstraintValidatorFactory();

    /**
     * Accumulate a validated constraint.
     * @param constraint
     * @return true when the constraint for the object in this path was not
     *         already validated in this context
     */
    boolean collectValidated(ConstraintValidator<?, ?> constraint);

    /**
     * Get the current owning class.
     * @return Class
     */
    Class<?> getCurrentOwner();

    /**
     * Set the current owning class.
     * @param currentOwner to set
     */
    void setCurrentOwner(Class<?> currentOwner);

    void setKind(ElementKind type);

    ElementKind getElementKind();

    Object getReturnValue();

    Object[] getParameters();

    void setParameters(Object[] parameters);

    void setReturnValue(Object returnValue);

    ParameterNameProvider getParameterNameProvider();

    void setMethod(Method method);

    Method getMethod();

    void setConstructor(Constructor<?> method);

    Constructor<?> getConstructor();

    void moveDown(Path.Node node);
}
