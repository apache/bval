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

import java.util.Map;
import java.util.TreeMap;

/**
 * Description: the meta description of a bean or class. the class/bean itself can have a map of features and an array
 * of metaproperties.<br/>
 * 
 * @see MetaProperty
 */
public class MetaBean extends FeaturesCapable implements Cloneable, Features.Bean {
    private static final long serialVersionUID = 2L;

    private String id;
    private String name;
    private Class<?> beanClass;
    private Map<String, MetaProperty> properties = new TreeMap<String, MetaProperty>();

    /**
     * Get the id.
     * 
     * @return String
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id.
     * 
     * @param id
     *            the String to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the name.
     * 
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name.
     * 
     * @param name
     *            the String to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the beanClass.
     * 
     * @return Class<?>
     */
    public Class<?> getBeanClass() {
        return beanClass;
    }

    /**
     * Set the beanClass.
     * 
     * @param beanClass
     *            the Class<?> to set
     */
    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    /**
     * Get the properties.
     * 
     * @return MetaProperty[]
     */
    public MetaProperty[] getProperties() {
        return properties.values().toArray(new MetaProperty[this.properties.size()]);
    }

    /**
     * Set the properties.
     * 
     * @param properties
     *            the MetaProperty[] to set
     */
    public void setProperties(MetaProperty[] properties) {
        this.properties.clear();
        for (MetaProperty property : properties) {
            this.properties.put(property.getName(), property);
        }
    }

    /**
     * Get the specified {@link MetaProperty}.
     * 
     * @param name
     * @return MetaProperty found or <code>null</code>
     */
    public MetaProperty getProperty(String name) {
        return this.properties.get(name);
    }

    /**
     * Learn whether any known property is a relationship.
     * 
     * @see MetaProperty#isRelationship()
     * @return true when at least one of the properties is a relationship
     */
    public boolean hasRelationships() {
        for (MetaProperty property : this.properties.values()) {
            if (property.isRelationship()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Learn whether there are any known properties.
     * 
     * @return boolean
     */
    public boolean hasProperties() {
        return this.properties.size() > 0;
    }

    /**
     * bidirectional - set the relationship between a MetaProperty and its parentMetaBean
     * 
     * @param name
     * @param property
     *            if <code>null</code>, remove
     */
    public void putProperty(String name, MetaProperty property) {
        if (property == null) {
            this.properties.remove(name);
        } else {        
            property.setParentMetaBean(this);
            this.properties.put(name, property);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "MetaBean{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", beanClass=" + beanClass + '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void copyInto(FeaturesCapable target) {
        super.copyInto(target);
        final MetaBean copy = (MetaBean) target;
        if (properties != null) {
            copy.properties = new TreeMap<String, MetaProperty>();        
            for (Map.Entry<String, MetaProperty> entry : properties.entrySet()) {
                copy.properties.put(entry.getKey(), (MetaProperty) entry.getValue().copy());
            }
        }
    }

    /**
     * <p>
     * If this {@link MetaBean} is compatible with <code>bean</code>, return <code>this</code>, else <code>null</code>.
     * </p>
     * <p>
     * Compatibility is satisfied in one of the following ways:
     * <ul>
     * <li><code>bean</code> is null</li>
     * <li><code>bean</code> is an instance of our <code>beanClass</code></li>
     * <li><code>bean</code> <em>is</em> our <code>beanClass</code> itself</li>
     * </ul>
     * </p>
     * 
     * @param bean
     * @return <code>this</code> or <code>null</code>
     */
    public MetaBean resolveMetaBean(Object bean) {
        return bean == null || bean == beanClass || beanClass.isInstance(bean) ? this : null;
    }

}
