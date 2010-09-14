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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Description: MetaData class<br/>
 */
public abstract class ElementDescriptorImpl implements ElementDescriptor {

    /**
     * Get a set of {@link ConstraintDescriptor}s from the specified array of
     * {@link Validation}s.
     * 
     * @param validations
     * @return {@link ConstraintDescriptor} set
     */
    protected static Set<ConstraintDescriptor<?>> getConstraintDescriptors(Validation[] validations) {
        final Set<ConstraintDescriptor<?>> result = new HashSet<ConstraintDescriptor<?>>(validations.length);
        for (Validation validation : validations) {
            if (validation instanceof ConstraintValidation<?>) {
                result.add((ConstraintValidation<?>) validation);
            }
        }
        return result;
    }

    /** the MetaBean of this element */
    protected final MetaBean metaBean;

    /** the raw type of this element */
    protected final Class<?> elementClass;

    private Set<ConstraintDescriptor<?>> constraintDescriptors;

    /**
     * Create a new ElementDescriptorImpl instance.
     * 
     * @param metaBean
     * @param elementClass
     * @param validations
     */
    protected ElementDescriptorImpl(MetaBean metaBean, Class<?> elementClass, Validation[] validations) {
        this.metaBean = metaBean;
        this.elementClass = elementClass;
        setConstraintDescriptors(getConstraintDescriptors(validations));
    }

    /**
     * Create a new ElementDescriptorImpl instance.
     * 
     * @param elementClass
     * @param validations
     */
    protected ElementDescriptorImpl(Class<?> elementClass, Validation[] validations) {
        this(null, elementClass, validations);
    }

    /**
     * {@inheritDoc}
     * 
     * @return Statically defined returned type.
     */
    public Class<?> getElementClass() {
        return elementClass;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public ElementDescriptor.ConstraintFinder findConstraints() {
        return new ConstraintFinderImpl(metaBean, new HashSet((Set) constraintDescriptors));
    }

    /**
     * {@inheritDoc}
     */
    public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        return constraintDescriptors.isEmpty() ? Collections.<ConstraintDescriptor<?>> emptySet() : Collections
            .unmodifiableSet(constraintDescriptors);
    }

    /**
     * Get the mutable {@link ConstraintDescriptor} {@link Set}.
     * 
     * @return Set of {@link ConstraintDescriptor}
     */
    protected Set<ConstraintDescriptor<?>> getMutableConstraintDescriptors() {
        return constraintDescriptors;
    }

    /**
     * {@inheritDoc} return true if at least one constraint declaration is
     * present on the element.
     */
    public boolean hasConstraints() {
        return !getConstraintDescriptors().isEmpty();
    }

    /**
     * Set the constraintDescriptors for this element.
     * 
     * @param constraintDescriptors
     *            to set
     */
    public void setConstraintDescriptors(Set<ConstraintDescriptor<?>> constraintDescriptors) {
        this.constraintDescriptors = constraintDescriptors;
    }

    /**
     * Get the model {@link MetaBean} used.
     * 
     * @return MetaBean
     */
    public MetaBean getMetaBean() {
        return metaBean;
    }
}
