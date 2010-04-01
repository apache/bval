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
package org.apache.bval.jsr303;


import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.apache.bval.model.Features;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.Validation;

import java.util.HashSet;
import java.util.Set;

/**
 * Description: <br/>
 */
public class BeanDescriptorImpl extends ElementDescriptorImpl implements BeanDescriptor {
    protected final ApacheFactoryContext factoryContext;

    protected BeanDescriptorImpl(ApacheFactoryContext factoryContext, MetaBean metaBean,
                                 Validation[] validations) {
        super(metaBean, validations);
        this.factoryContext = factoryContext;
    }

    /**
     * Returns true if the bean involves validation:
     * - a constraint is hosted on the bean itself
     * - a constraint is hosted on one of the bean properties
     * - or a bean property is marked for cascade (@Valid)
     *
     * @return true if the bean nvolves validation
     */
    public boolean isBeanConstrained() {
        if (hasAnyConstraints()) return true;
        for (MetaProperty mprop : metaBean.getProperties()) {
            if (mprop.getMetaBean() != null ||
                  mprop.getFeature(Features.Property.REF_CASCADE) != null) return true;
        }
        return false;
    }

    private boolean hasAnyConstraints() {
        if (hasConstraints()) return true;
        if (metaBean.getValidations().length > 0) return true;
        for (MetaProperty mprop : metaBean.getProperties()) {
            if (mprop.getValidations().length > 0) return true;
        }
        return false;
    }

    /**
     * Return the property level constraints for a given propertyName
     * or null if either the property does not exist or has no constraint
     * The returned object (and associated objects including ConstraintDescriptors)
     * are immutable.
     *
     * @param propertyName property evaludated
     */
    public PropertyDescriptor getConstraintsForProperty(String propertyName) {
        MetaProperty prop = metaBean.getProperty(propertyName);
        if (prop == null) return null;
        return getPropertyDescriptor(prop);
    }

    private PropertyDescriptor getPropertyDescriptor(MetaProperty prop) {
        PropertyDescriptorImpl edesc =
              prop.getFeature(Jsr303Features.Property.PropertyDescriptor);
        if (edesc == null) {
            Class<?> targetClass =
                  prop.getFeature(Features.Property.REF_BEAN_TYPE, prop.getTypeClass());
            if (targetClass.isPrimitive()) {
                // optimization: do not create MetaBean for primitives
                // enhancement: do not create MetaBean for classes without any metadata?
                edesc = new PropertyDescriptorImpl(targetClass, prop.getValidations());
            } else {
                edesc = new PropertyDescriptorImpl(
                      factoryContext.getMetaBeanFinder().findForClass(targetClass),
                      prop.getValidations());
            }
            edesc.setCascaded((prop.getMetaBean() != null ||
                  prop.getFeature(Features.Property.REF_CASCADE) != null));
            edesc.setPropertyPath(prop.getName());
            prop.putFeature(Jsr303Features.Property.PropertyDescriptor, edesc);
        }
        return edesc;
    }

    /** return the property descriptors having at least a constraint defined */
    public Set<PropertyDescriptor> getConstrainedProperties() {
        Set<PropertyDescriptor> validatedProperties = new HashSet<PropertyDescriptor>();
        for (MetaProperty prop : metaBean.getProperties()) {
            if (prop.getValidations().length > 0 || (prop.getMetaBean() != null ||
                  prop.getFeature(Features.Property.REF_CASCADE) != null)) {
                validatedProperties.add(getPropertyDescriptor(prop));
            }
        }
        return validatedProperties;
    }

    public String toString() {
        return "BeanDescriptorImpl{" + "returnType=" + elementClass + '}';
    }
}
