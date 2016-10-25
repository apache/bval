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

import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.apache.bval.model.FeaturesCapable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Description: <br/>
 */
public class XMLFeaturesCapable implements Serializable {
    /** Serialization version */
    private static final long serialVersionUID = 1L;

    @XStreamImplicit
    private List<XMLMetaFeature> features;
    @XStreamImplicit(itemFieldName = "validator")
    private List<XMLMetaValidatorReference> validators;

    public List<XMLMetaFeature> getFeatures() {
        return features;
    }

    public void setFeatures(List<XMLMetaFeature> features) {
        this.features = features;
    }

    public void putFeature(String key, Object value) {
        XMLMetaFeature anno = findFeature(key);
        if (features == null)
            features = new ArrayList<XMLMetaFeature>();
        if (anno == null) {
            features.add(new XMLMetaFeature(key, value));
        } else {
            anno.setValue(value);
        }
    }

    public void removeFeature(String key) {
        XMLMetaFeature anno = findFeature(key);
        if (anno != null) {
            getFeatures().remove(anno);
        }
    }

    public Object getFeature(String key) {
        XMLMetaFeature anno = findFeature(key);
        return anno == null ? null : anno.getValue();
    }

    private XMLMetaFeature findFeature(String key) {
        if (features == null)
            return null;
        for (XMLMetaFeature anno : features) {
            if (key.equals(anno.getKey()))
                return anno;
        }
        return null;
    }

    public List<XMLMetaValidatorReference> getValidators() {
        return validators;
    }

    public void setValidators(List<XMLMetaValidatorReference> validators) {
        this.validators = validators;
    }

    public void addValidator(String validatorId) {
        if (validators == null)
            validators = new ArrayList<XMLMetaValidatorReference>();
        validators.add(new XMLMetaValidatorReference(validatorId));
    }

    public void mergeFeaturesInto(FeaturesCapable fc) {
        if (getFeatures() != null) {
            for (XMLMetaFeature each : getFeatures()) {
                fc.putFeature(each.getKey(), each.getValue());
            }
        }
    }
}
