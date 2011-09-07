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


import org.apache.bval.jsr303.util.PathImpl;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;

import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Set;

/**
 * Description: JSR-303 {@link ValidationListener} implementation; provides {@link ConstraintViolation}s.<br/>
 * 
 * @version $Rev$ $Date$
 */
public final class ConstraintValidationListener<T> implements ValidationListener {
    private final Set<ConstraintViolation<T>> constraintViolations = new HashSet<ConstraintViolation<T>>();
    private final T rootBean;
    private final Class<T> rootBeanType;
    // the validation process is single-threaded and it's unlikely to change in the near future (otherwise use AtomicInteger).
    private int compositeDepth = 0;
    private boolean hasCompositeError;

    /**
     * Create a new ConstraintValidationListener instance.
     * @param aRootBean
     * @param rootBeanType
     */
    public ConstraintValidationListener(T aRootBean, Class<T> rootBeanType) {
        this.rootBean = aRootBean;
        this.rootBeanType = rootBeanType;
    }

    /**
     * {@inheritDoc}
     */
    public <VL extends ValidationListener> void addError(String reason, ValidationContext<VL> context) {
        addError(reason, null, context);
    }

    /**
     * {@inheritDoc}
     */
    public <VL extends ValidationListener> void addError(Error error, ValidationContext<VL> context) {
        if (error.getOwner() instanceof Path) {
            addError(error.getReason(), (Path) error.getOwner(), context);
        } else {
            addError(error.getReason(), null, context);
        }
    }

    private void addError(String messageTemplate, Path propPath,
                          ValidationContext<?> context) {
        if (compositeDepth > 0) {
            hasCompositeError |= true;
            return;
        }
        final Object value;

        final ConstraintDescriptor<?> descriptor;
        final String message;
        if (context instanceof GroupValidationContext<?>) {
            GroupValidationContext<?> gcontext = (GroupValidationContext<?>) context;
            value = gcontext.getValidatedValue();
            if (gcontext instanceof MessageInterpolator.Context) {
                message = gcontext.getMessageResolver()
                      .interpolate(messageTemplate,
                            (MessageInterpolator.Context) gcontext);
            } else {
                message =
                      gcontext.getMessageResolver().interpolate(messageTemplate, null);
            }
            descriptor = gcontext.getConstraintValidation().asSerializableDescriptor();
            if (propPath == null) propPath = gcontext.getPropertyPath();
        } else {
            if (context.getMetaProperty() == null) value = context.getBean();
            else value = context.getPropertyValue();
            message = messageTemplate;
            if (propPath == null)
                propPath = PathImpl.createPathFromString(context.getPropertyName());
            descriptor = null;
        }
        ElementType elementType = (context.getAccess() != null) ? context.getAccess().getElementType() : null;
        ConstraintViolationImpl<T> ic = new ConstraintViolationImpl<T>(messageTemplate,
              message, rootBean, context.getBean(), propPath, value, descriptor, rootBeanType, elementType);
        constraintViolations.add(ic);
    }

    /**
     * Get the {@link ConstraintViolation}s accumulated by this {@link ConstraintValidationListener}.
     * @return {@link Set} of {@link ConstraintViolation}
     */
    public Set<ConstraintViolation<T>> getConstraintViolations() {
        return constraintViolations;
    }

    /**
     * Learn whether no violations were found. 
     * @return boolean
     */
    public boolean isEmpty() {
        return constraintViolations.isEmpty();
    }

    /**
     * Get the root bean.
     * @return T
     */
    public T getRootBean() {
        return rootBean;
    }

    /**
     * Get the root bean type of this {@link ConstraintValidationListener}.
     * @return Class<T>
     */
    public Class<T> getRootBeanType() {
        return rootBeanType;
    }
    
    /**
     * Get the count of encountered violations.
     * @return int
     */
    public int violationsSize() {
        return constraintViolations.size();
    }

    /**
     * Learn whether there are violations available.
     * If in report-as-single-violation mode, the result is scoped accordingly.
     * Note that this means you must check before exiting report-as-single-violation mode
     * @return boolean
     */
    public boolean hasViolations() {
        return compositeDepth == 0 ? !constraintViolations.isEmpty() : hasCompositeError;
    }

    /**
     * Signify the beginning of a report-as-single-violation composite validation.
     * @return <code>true</code> as this call caused the listener to enter report-as-single-violation mode
     */
    public boolean beginReportAsSingle() {
        return ++compositeDepth == 1;
    }

    /**
     * Signify the end of a report-as-single-violation composite validation.
     * @return <code>true</code> as this call caused the listener to exit report-as-single-violation mode
     */
    public boolean endReportAsSingle() {
        return --compositeDepth == 0;
    }
}
