/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bval.jsr;

import org.apache.bval.model.Features;
import org.apache.bval.model.MetaProperty;

import javax.validation.metadata.PropertyDescriptor;

/**
 * Description: {@link PropertyDescriptor} implementation.<br/>
 *
 * TODO: use it instead of MetaProperty!
 */
class PropertyDescriptorImpl extends ElementDescriptorImpl implements PropertyDescriptor {
    private String propertyPath;

    /**
     * Create a new PropertyDescriptorImpl instance.
     * 
     * @param property
     */
    PropertyDescriptorImpl(MetaProperty property) {
        super(property.getParentMetaBean(), property.getTypeClass(), property.getValidations());
        setCascaded(property.getMetaBean() != null || property.getFeature(Features.Property.REF_CASCADE) != null);
        setPropertyPath(property.getName());
    }

    /**
     * Set the referenced property path.
     * 
     * @param propertyPath
     */
    public void setPropertyPath(String propertyPath) {
        this.propertyPath = propertyPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPropertyName() {
        return propertyPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "PropertyDescriptorImpl{" + "returnType=" + elementClass + ", propertyPath='" + propertyPath + '\''
            + '}';
    }
}
