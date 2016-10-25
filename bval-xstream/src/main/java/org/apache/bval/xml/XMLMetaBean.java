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
package org.apache.bval.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;

/**
 * Description: <br/>
 */
@XStreamAlias("bean")
public class XMLMetaBean extends XMLFeaturesCapable {
    /** Serialization version */
    private static final long serialVersionUID = 1L;

    @XStreamAsAttribute()
    private String id;
    @XStreamAsAttribute()
    private String name;
    @XStreamAsAttribute()
    private String impl;
    @XStreamImplicit
    private List<XMLMetaProperty> properties;
    @XStreamImplicit
    private List<XMLMetaBeanReference> beanRelations;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImpl() {
        return impl;
    }

    public void setImpl(String impl) {
        this.impl = impl;
    }

    public List<XMLMetaProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<XMLMetaProperty> properties) {
        this.properties = properties;
    }

    public void addProperty(XMLMetaProperty property) {
        if (properties == null)
            properties = new ArrayList<XMLMetaProperty>();
        properties.add(property);
    }

    public void putProperty(XMLMetaProperty property) {
        if (property.getName() != null) {
            XMLMetaProperty prop = findProperty(property.getName());
            if (prop != null) {
                properties.remove(prop);
            }
        }
        addProperty(property);
    }

    public XMLMetaProperty removeProperty(String name) {
        XMLMetaProperty prop = findProperty(name);
        if (prop != null) {
            properties.remove(prop);
        }
        return prop;
    }

    public XMLMetaProperty getProperty(String name) {
        return findProperty(name);
    }

    private XMLMetaProperty findProperty(String name) {
        if (properties == null)
            return null;
        for (XMLMetaProperty prop : properties) {
            if (name.equals(prop.getName()))
                return prop;
        }
        return null;
    }

    public List<XMLMetaBeanReference> getBeanRefs() {
        return beanRelations;
    }

    public void setBeanRefs(List<XMLMetaBeanReference> beanRelations) {
        this.beanRelations = beanRelations;
    }

    public void addBeanRef(XMLMetaBeanReference beanRelation) {
        if (beanRelations == null)
            beanRelations = new ArrayList<XMLMetaBeanReference>();
        beanRelations.add(beanRelation);
    }

    public void putBeanRef(XMLMetaBeanReference beanRelation) {
        if (beanRelation.getName() != null) {
            XMLMetaBeanReference relation = findBeanRef(beanRelation.getName());
            if (relation != null) {
                beanRelations.remove(relation);
            }
        }
        addBeanRef(beanRelation);
    }

    public XMLMetaBeanReference removeBeanRef(String name) {
        XMLMetaBeanReference relation = findBeanRef(name);
        if (relation != null) {
            beanRelations.remove(relation);
        }
        return relation;
    }

    public XMLMetaBeanReference getBeanRef(String name) {
        return findBeanRef(name);
    }

    private XMLMetaBeanReference findBeanRef(String name) {
        if (beanRelations == null)
            return null;
        for (XMLMetaBeanReference relation : beanRelations) {
            if (name.equals(relation.getName()))
                return relation;
        }
        return null;
    }

}
