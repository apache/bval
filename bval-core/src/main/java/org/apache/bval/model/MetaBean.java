/**
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

import org.apache.commons.lang.ArrayUtils;

/**
 * Description: the meta description of a bean or class.
 * the class/bean itself can have a map of features and an array of metaproperties.<br/>
 *
 * @see MetaProperty
 */
public class MetaBean extends FeaturesCapable implements Cloneable, Features.Bean {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private Class<?> beanClass;
    private MetaProperty[] properties = new MetaProperty[0];

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    public MetaProperty[] getProperties() {
        return properties;
    }

    public void setProperties(MetaProperty[] properties) {
        this.properties = properties;
    }

    public MetaProperty getProperty(String name) {
        for (MetaProperty p : properties) {
            if (name.equals(p.getName())) return p;
        }
        return null;
    }

    /** @return true when at least one of the properties is a relationship */
    public boolean hasRelationships() {
        for (MetaProperty p : properties) {
            if (p.isRelationship()) return true;
        }
        return false;
    }

    public boolean hasProperties() {
        return properties.length > 0;
    }

  /**
   * bidirectional - set the relationship between a MetaProperty and its parentMetaBean
   * @param name
   * @param property
   */
    public void putProperty(String name, MetaProperty property) {
        final MetaProperty oldProperty = getProperty(name);
        if(property != null) property.setParentMetaBean(this);
        if (oldProperty == null) { // add
            if (properties.length == 0) {
                properties = new MetaProperty[1];
            } else {
                MetaProperty[] newproperties = new MetaProperty[properties.length + 1];
                System.arraycopy(properties, 0, newproperties, 0, properties.length);
                properties = newproperties;
            }
            properties[properties.length - 1] = property;
        } else { // replace
            int idx = ArrayUtils.indexOf(properties, oldProperty);
            properties[idx] = property;
        }
    }

    public String toString() {
        return "MetaBean{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", beanClass=" +
                beanClass + '}';
    }

    @Override
    protected <T extends FeaturesCapable> void copyInto(T target) {
        super.copyInto(target);
        final MetaBean copy = (MetaBean) target;
        if (properties != null) {
            copy.properties = properties.clone();
            for (int i = copy.properties.length - 1; i >= 0; i--) {
                copy.properties[i] = copy.properties[i].copy();
            }
        }
    }

    public MetaBean resolveMetaBean(Object bean) {
        return bean == null || bean == beanClass || beanClass.isInstance(bean) ? this : null;
    }

}
