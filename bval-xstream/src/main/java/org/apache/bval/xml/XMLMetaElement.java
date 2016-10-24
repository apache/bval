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

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.util.reflection.Reflection;

import static org.apache.bval.model.Features.Property.DENIED;
import static org.apache.bval.model.Features.Property.HIDDEN;
import static org.apache.bval.model.Features.Property.MANDATORY;
import static org.apache.bval.model.Features.Property.MAX_LENGTH;
import static org.apache.bval.model.Features.Property.MIN_LENGTH;
import static org.apache.bval.model.Features.Property.READONLY;

/**
 * Description: <br/>
 */
public class XMLMetaElement extends XMLFeaturesCapable {
    /** Serialization version */
    private static final long serialVersionUID = 1L;

    @XStreamAsAttribute()
    private String name;
    @XStreamAsAttribute()
    private String mandatory;

    @XStreamAsAttribute()
    private Integer minLength;
    @XStreamAsAttribute()
    private Integer maxLength;
    @XStreamAsAttribute()
    private Boolean readonly;
    @XStreamAsAttribute()
    private Boolean hidden;
    @XStreamAsAttribute()
    private Boolean denied;
    /**
     * normally the type is determined by the implementation class.
     * in case, no implementation class is given, the xml can
     * contain the type directly.
     */
    @XStreamAsAttribute()
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMandatory() {
        return mandatory;
    }

    public void setMandatory(String mandatory) {
        this.mandatory = mandatory;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Boolean getReadonly() {
        return readonly;
    }

    public void setReadonly(Boolean readonly) {
        this.readonly = readonly;
    }

    public Boolean getDenied() {
        return denied;
    }

    public void setDenied(Boolean denied) {
        this.denied = denied;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void mergeInto(MetaProperty prop) throws ClassNotFoundException {
        mergeFeaturesInto(prop);
        if (getType() != null && getType().length() > 0) {
            prop.setType(Reflection.toClass(getType())); // enhancement: or use getGenericType() ?
        }
        if (getHidden() != null) {
            prop.putFeature(HIDDEN, getHidden().booleanValue());
        }
        if (getMandatory() != null) {
            prop.putFeature(MANDATORY, getMandatory().equals("true"));
        }
        if (getMaxLength() != null) {
            prop.putFeature(MAX_LENGTH, getMaxLength());
        }
        if (getMinLength() != null) {
            prop.putFeature(MIN_LENGTH, getMinLength());
        }
        if (getReadonly() != null) {
            prop.putFeature(READONLY, getReadonly());
        }
        if (getDenied() != null) {
            prop.putFeature(DENIED, getDenied());
        }
    }
}
