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
import org.apache.bval.model.MetaProperty;

import static org.apache.bval.model.Features.Property.MAX_VALUE;
import static org.apache.bval.model.Features.Property.MIN_VALUE;
import static org.apache.bval.model.Features.Property.REG_EXP;
import static org.apache.bval.model.Features.Property.TIME_LAG;

/**
 * Description: <br/>
 */
@XStreamAlias("property")
public class XMLMetaProperty extends XMLMetaElement {
    /** Serialization version */
    private static final long serialVersionUID = 1L;

    /**
     * if you need other types (Long, double, String) for maxValue and
     * minValue, configure via
     * <pre>
     * &lt;feature key="maxValue">
     *   &lt;value class="java.lang.Long">1000&lt;/value>
     * &lt;/feature>
     * </pre>
     * instead with explicit typing.
     */
    @XStreamAsAttribute()
    private Integer maxValue; // XStream requires a non-abstract type to parse XML
    @XStreamAsAttribute()
    private Integer minValue; // XStream requires a non-abstract type to parse XML

    private String regexp;

    @XStreamAsAttribute()
    private String timeLag;

    public Integer getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }

    public Integer getMinValue() {
        return minValue;
    }

    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    public String getTimeLag() {
        return timeLag;
    }

    public void setTimeLag(String timeLag) {
        this.timeLag = timeLag;
    }

    @Override
    public void mergeInto(MetaProperty prop) throws ClassNotFoundException {
        super.mergeInto(prop); // call super!
        if (getMaxValue() != null) {
            prop.putFeature(MAX_VALUE, getMaxValue());
        }
        if (getMinValue() != null) {
            prop.putFeature(MIN_VALUE, getMinValue());
        }
        if (getRegexp() != null) {
            prop.putFeature(REG_EXP, getRegexp());
        }
        if (getTimeLag() != null) {
            prop.putFeature(TIME_LAG, getTimeLag());
        }
    }
}
