/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval.jsr;

import org.apache.bval.model.MetaBean;
import org.apache.bval.model.Validation;

import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.CrossParameterDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.ParameterDescriptor;
import javax.validation.metadata.ReturnValueDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class InvocableElementDescriptor extends ElementDescriptorImpl implements ProcedureDescriptor {
    private static final CopyOnWriteArraySet<ConstraintValidation<?>> NO_CONSTRAINTS =
        new CopyOnWriteArraySet<ConstraintValidation<?>>();

    private ReturnValueDescriptor returnValueDescriptor;
    private CrossParameterDescriptor crossParameterDescriptor;
    private final List<ParameterDescriptor> parameterDescriptors = new ArrayList<ParameterDescriptor>();

    protected InvocableElementDescriptor(final MetaBean metaBean, final Class<?> elementClass,
        final Validation[] validations) {
        super(metaBean, elementClass, validations);
    }

    protected InvocableElementDescriptor(final Class<?> elementClass, final Validation[] validations) {
        super(elementClass, validations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ParameterDescriptor> getParameterDescriptors() {
        // index aligned
        return parameterDescriptors;
    }

    public void setReturnValueDescriptor(final ReturnValueDescriptor returnValueDescriptor) {
        this.returnValueDescriptor = returnValueDescriptor;
    }

    public CrossParameterDescriptor getCrossParameterDescriptor() {
        return crossParameterDescriptor;
    }

    public void setCrossParameterDescriptor(final CrossParameterDescriptor crossParameterDescriptor) {
        this.crossParameterDescriptor = crossParameterDescriptor;
    }

    /**
     * Add the specified validations to this {@link org.apache.bval.jsr.MethodDescriptorImpl}.
     * @param validations
     */
    void addValidations(Collection<ConstraintValidation<?>> validations) {
        getMutableConstraintDescriptors().addAll(validations);
    }

    protected boolean hasConstrainedParameters() {
        for (final ParameterDescriptor pd : getParameterDescriptors()) {
            if (pd.isCascaded() || !pd.getConstraintDescriptors().isEmpty()) {
                return true;
            }
        }
        return getCrossParameterDescriptor().hasConstraints();
    }

    public ReturnValueDescriptor getReturnValueDescriptor() {
        return returnValueDescriptor;
    }

    protected boolean hasConstrainedReturnValue() {
        return getReturnValueDescriptor().isCascaded()
            || !getReturnValueDescriptor().getConstraintDescriptors().isEmpty();
    }

    @Override
    public ElementDescriptor.ConstraintFinder findConstraints() {
        return new ConstraintFinderImpl(metaBean, NO_CONSTRAINTS);
    }

    @Override
    public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        return Set.class.cast(NO_CONSTRAINTS);
    }
}
