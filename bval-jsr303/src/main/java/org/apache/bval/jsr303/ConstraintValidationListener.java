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
import javax.validation.MessageInterpolator;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;

import org.apache.bval.jsr303.util.PathImpl;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;

import java.util.HashSet;
import java.util.Set;

/**
 * Description: <br/>
 * User: roman.stumm <br/>
 * Date: 01.04.2008 <br/>
 * Time: 14:52:19 <br/>
 * Copyright: Agimatec GmbH 2008
 */
public final class ConstraintValidationListener<T> implements ValidationListener {
    private final Set<ConstraintViolation<T>> constaintViolations = new HashSet();
    private final T rootBean;

    public ConstraintValidationListener(T aRootBean) {
        this.rootBean = aRootBean;
    }

    @SuppressWarnings({"ManualArrayToCollectionCopy"})
    public void addError(String reason, ValidationContext context) {
        addError(reason, null, context);
    }

    public void addError(Error error, ValidationContext context) {
        if (error.getOwner() instanceof Path) {
            addError(error.getReason(), (Path) error.getOwner(), context);
        } else {
            addError(error.getReason(), null, context);
        }
    }

    private void addError(String messageTemplate, Path propPath,
                          ValidationContext context) {
        final Object value;

        final ConstraintDescriptor constraint;
        final String message;
        if (context instanceof GroupValidationContext) {
            GroupValidationContext gcontext = (GroupValidationContext) context;
            value = gcontext.getValidatedValue();
            if (gcontext instanceof MessageInterpolator.Context) {
                message = gcontext.getMessageResolver()
                      .interpolate(messageTemplate,
                            (MessageInterpolator.Context) gcontext);
            } else {
                message =
                      gcontext.getMessageResolver().interpolate(messageTemplate, null);
            }
            constraint = gcontext.getConstraintDescriptor();
            if (propPath == null) propPath = gcontext.getPropertyPath();
        } else {
            if (context.getMetaProperty() == null) value = context.getBean();
            else value = context.getPropertyValue();                        
            message = messageTemplate;
            if (propPath == null)
                propPath = PathImpl.createPathFromString(context.getPropertyName());
            constraint = null;
        }
        ConstraintViolationImpl<T> ic = new ConstraintViolationImpl<T>(messageTemplate,
              message, rootBean, context.getBean(), propPath, value, constraint);
        constaintViolations.add(ic);
    }

    public Set<ConstraintViolation<T>> getConstaintViolations() {
        return constaintViolations;
    }

    public boolean isEmpty() {
        return constaintViolations.isEmpty();
    }

    public T getRootBean() {
        return rootBean;
    }
}
