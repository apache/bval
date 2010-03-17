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
package com.agimatec.validation.jsr303;

import com.agimatec.validation.model.MetaBean;
import com.agimatec.validation.model.Validation;

import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;
import java.util.HashSet;
import java.util.Set;

/**
 * Description: MetaData class<br/>
 * User: roman.stumm <br/>
 * Date: 02.04.2008 <br/>
 * Time: 12:23:45 <br/>
 * Copyright: Agimatec GmbH 2008
 */
public abstract class ElementDescriptorImpl implements ElementDescriptor {
    protected final MetaBean metaBean;
    protected final Class elementClass;
    private Set<ConstraintDescriptor<?>> constraintDescriptors;

    protected ElementDescriptorImpl(MetaBean metaBean,
                                 Validation[] validations) {
        this.metaBean = metaBean;
        this.elementClass = metaBean.getBeanClass();
        createConstraintDescriptors(validations);
    }

    protected ElementDescriptorImpl(Class elementClass, Validation[] validations) {
        this.metaBean = null;
        this.elementClass = elementClass;
        createConstraintDescriptors(validations);
    }

    /** @return Statically defined returned type. */
    public Class getElementClass() {
        return elementClass;
    }

    public ElementDescriptor.ConstraintFinder findConstraints() {
        return new ConstraintFinderImpl(metaBean, constraintDescriptors);
    }

    public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        return constraintDescriptors;
    }

    /** return true if at least one constraint declaration is present on the element. */
    public boolean hasConstraints() {
        return !constraintDescriptors.isEmpty();
    }

    private void createConstraintDescriptors(Validation[] validations) {
        final Set<ConstraintDescriptor<?>> cds = new HashSet(validations.length);
        for (Validation validation : validations) {
            if (validation instanceof ConstraintValidation) {
                ConstraintValidation cval = (ConstraintValidation) validation;
                cds.add(cval);
            }
        }
        setConstraintDescriptors(cds);
    }

    public void setConstraintDescriptors(Set<ConstraintDescriptor<?>> constraintDescriptors) {
        this.constraintDescriptors = constraintDescriptors;
    }

    public MetaBean getMetaBean() {
        return metaBean;
    }
}
