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
package org.apache.bval.model;

import java.lang.reflect.Type;

import org.apache.bval.util.reflection.TypeUtils;

/**
 * Description: the meta description of a property of a bean. It supports a map
 * of features and multiple validations.<br/>
 *
 * @see Validation
 * @see MetaBean
 */
public class MetaProperty extends Meta implements Cloneable, Features.Property {
    private static final long serialVersionUID = 1L;

    private String name;

    private Type type;
    private MetaBean metaBean;

    /**
     * Create a new MetaProperty instance.
     */
    public MetaProperty() {
    }

    /**
     * Get the metabean of the target bean (mainly for relationships).
     * @return MetaBean (may be null).
     */
    public MetaBean getMetaBean() {
        return metaBean;
    }

    /**
     * Set the MetaBean of this {@link MetaProperty}.
     * @param metaBean to set
     */
    public void setMetaBean(MetaBean metaBean) {
        this.metaBean = metaBean;
    }

    /**
     * Set the metabean that owns this property (usually called by MetaBean.putProperty())
     * @param parentMetaBean
     */
    void setParentMetaBean(MetaBean parentMetaBean) {
        this.parentMetaBean = parentMetaBean;
    }

    /**
     * Learn whether this property is considered a relationship.
     * @return <code>true</code> if it has a MetaBean of its own
     */
    public boolean isRelationship() {
        return metaBean != null;
    }

    /**
     * Set the type of this property.
     * @param type to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Get the type of this property.
     * @return
     */
    public Type getType() {
        return type;
    }

    /**
     * Resolve the type of this property to a class.
     * @return Class, <code>null</code> if cannot be determined
     */
    public Class<?> getTypeClass() {
        Type targetType = type instanceof DynaType ? ((DynaType) type).getRawType() : type;
        if (targetType == null) {
            return null;
        }
        Type assigningType = getParentMetaBean() == null ? null : getParentMetaBean().getBeanClass();
        return TypeUtils.getRawType(targetType, assigningType);
    }

    /**
     * Get the name of this property.
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Learn whether this property is considered mandatory.
     * @return <code>true</code> if the <code>MANDATORY</code> feature is set to <code>true</code>.
     * @see {@link Features.Property#MANDATORY}
     */
    public boolean isMandatory() {
        return getFeature(MANDATORY, Boolean.FALSE).booleanValue();
    }

    /**
     * Set this property as being mandatory (or not).
     * @param mandatory
     * @see {@link Features.Property#MANDATORY}
     */
    public void setMandatory(boolean mandatory) {
        putFeature(MANDATORY, Boolean.valueOf(mandatory));
    }

    /**
     * Get javascript validations of this property.
     * @return String[]
     * @deprecated
     */
    @Deprecated // remove this method?
    public String[] getJavaScriptValidations() {
        return getFeature(JAVASCRIPT_VALIDATION_FUNCTIONS);
    }

    /**
     * Set the name of this property.
     * @param name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaProperty clone() throws CloneNotSupportedException {
        return (MetaProperty) super.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "MetaProperty{" + "name='" + name + '\'' + ", type=" + type + '}';
    }
}
