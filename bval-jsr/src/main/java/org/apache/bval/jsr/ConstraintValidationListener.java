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

import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;

import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.MessageInterpolator;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Description: JSR-303 {@link ValidationListener} implementation; provides {@link ConstraintViolation}s.<br/>
 * 
 * @version $Rev: 1503686 $ $Date: 2013-07-16 14:38:56 +0200 (mar., 16 juil. 2013) $
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
    @Override
    public <VL extends ValidationListener> void addError(String reason, ValidationContext<VL> context) {
        addError(reason, null, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <VL extends ValidationListener> void addError(Error error, ValidationContext<VL> context) {
        if (error.getOwner() instanceof Path) {
            addError(error.getReason(), (Path) error.getOwner(), context);
        } else {
            addError(error.getReason(), null, context);
        }
    }

    private void addError(String messageTemplate, Path propPath, ValidationContext<?> context) {
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
                message =
                    gcontext.getMessageResolver().interpolate(messageTemplate, (MessageInterpolator.Context) gcontext);
            } else {
                message = gcontext.getMessageResolver().interpolate(messageTemplate, null);
            }
            descriptor = gcontext.getConstraintValidation().asSerializableDescriptor();
            if (propPath == null)
                propPath = gcontext.getPropertyPath();
        } else {
            if (context.getMetaProperty() == null)
                value = context.getBean();
            else
                value = context.getPropertyValue();
            message = messageTemplate;
            if (propPath == null)
                propPath = PathImpl.createPathFromString(context.getPropertyName());
            descriptor = null;
        }
        ElementType elementType = (context.getAccess() != null) ? context.getAccess().getElementType() : null;

        final Object[] parameters;
        Object leaf;
        Object returnValue;
        T rootBean;
        if (GroupValidationContext.class.isInstance(context)) { // TODO: clean up it but it would need to rework completely our context - get rid of it would be the best
            final GroupValidationContext<T> ctx = GroupValidationContext.class.cast(context);
            final ElementKind elementKind = ctx.getElementKind();
            final Iterator<Path.Node> it = propPath.iterator();
            final ElementKind kind = propPath.iterator().next().getKind();

            returnValue = ctx.getReturnValue();

            if (ElementKind.CONSTRUCTOR.equals(kind)) {
                rootBean = null;
                leaf = context.getBean();
                returnValue = this.rootBean; // switch back return value and rootBean
            } else if (ElementKind.METHOD.equals(kind)) {
                if (ElementKind.RETURN_VALUE.equals(elementKind)) { // switch back return value and rootBean
                    rootBean = (T) returnValue;
                    if (kindOf(propPath, ElementKind.RETURN_VALUE)) {
                        leaf = returnValue;
                        returnValue = this.rootBean;
                    } else {
                        leaf = this.rootBean;
                        returnValue = this.rootBean;
                    }
                } else {
                    rootBean = this.rootBean;
                    if (kindOf(propPath, ElementKind.PARAMETER, ElementKind.CROSS_PARAMETER)) {
                        leaf = rootBean;
                    } else {
                        leaf = context.getBean();
                    }
                }
            } else {
                rootBean = this.rootBean;
                leaf = context.getBean();
            }

            if (ElementKind.CONSTRUCTOR.equals(kind)
                && (ElementKind.CROSS_PARAMETER.equals(elementKind) || ElementKind.PARAMETER.equals(elementKind))
                && (it.hasNext() && it.next() != null && it.hasNext() && it.next() != null && !it.hasNext())) { // means inherited validation use real value
                leaf = null;
            }

            parameters = ctx.getParameters();
        } else {
            leaf = context.getBean();
            returnValue = null;
            parameters = null;
            rootBean = this.rootBean;
        }

        constraintViolations.add(new ConstraintViolationImpl<T>(messageTemplate, message, rootBean, leaf, propPath,
            value, descriptor, rootBeanType, elementType, returnValue, parameters));
    }

    private static boolean kindOf(final Path propPath, final ElementKind... kinds) {
        final Iterator<Path.Node> node = propPath.iterator();
        boolean isParam = false;
        while (node.hasNext()) {
            final ElementKind current = node.next().getKind();
            isParam = false;
            for (final ElementKind k : kinds) {
                if (k.equals(current)) {
                    isParam = true;
                    break;
                }
            }
        }
        return isParam;
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
        boolean endOutMostReportAsSingle = (--compositeDepth == 0);
        if (endOutMostReportAsSingle) {
            hasCompositeError = false;
        }
        return endOutMostReportAsSingle;
    }
}
