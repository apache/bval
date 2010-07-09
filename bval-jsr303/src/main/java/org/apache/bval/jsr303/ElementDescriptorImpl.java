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


import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;

import org.apache.bval.model.MetaBean;
import org.apache.bval.model.Validation;

import java.util.HashSet;
import java.util.Set;

/**
 * Description: MetaData class<br/>
 */
public abstract class ElementDescriptorImpl implements ElementDescriptor {
    protected final MetaBean metaBean;
    protected final Class<?> elementClass;
    private Set<ConstraintDescriptor<?>> constraintDescriptors;

    protected ElementDescriptorImpl(MetaBean metaBean, Class<?> elementClass, 
                                 Validation[] validations) {
        this.metaBean = metaBean;
        this.elementClass = elementClass;
        createConstraintDescriptors(validations);
    }

    protected ElementDescriptorImpl(Class<?> elementClass, Validation[] validations) {
        this.metaBean = null;
        this.elementClass = elementClass;
        createConstraintDescriptors(validations);
    }

    /**
     * {@inheritDoc}
     *  @return Statically defined returned type.
     */
    public Class<?> getElementClass() {
        return elementClass;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public ElementDescriptor.ConstraintFinder findConstraints() {
        return new ConstraintFinderImpl(metaBean, (Set) constraintDescriptors);
    }

    /**
     * {@inheritDoc}
     */
    public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        return constraintDescriptors;
    }

    /**
     * {@inheritDoc}
     * return true if at least one constraint declaration is present on the element.
     */
    public boolean hasConstraints() {
        return !constraintDescriptors.isEmpty();
    }

    private void createConstraintDescriptors(Validation[] validations) {
        final Set<ConstraintDescriptor<?>> cds = new HashSet<ConstraintDescriptor<?>>(validations.length);
        for (Validation validation : validations) {
            if (validation instanceof ConstraintValidation<?>) {
                ConstraintValidation<?> cval = (ConstraintValidation<?>) validation;
                cds.add(cval);
            }
        }
        setConstraintDescriptors(cds);
    }

    /**
     * Set the constraintDescriptors for this element.
     * @param constraintDescriptors to set
     */
    public void setConstraintDescriptors(Set<ConstraintDescriptor<?>> constraintDescriptors) {
        this.constraintDescriptors = constraintDescriptors;
    }

    /**
     * Get the model {@link MetaBean} used.
     * @return MetaBean
     */
    public MetaBean getMetaBean() {
        return metaBean;
    }
}
