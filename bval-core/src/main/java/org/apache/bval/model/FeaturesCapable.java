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

import org.apache.commons.collections.FastHashMap;
import org.apache.commons.lang.ArrayUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * Description: abstract superclass of meta objects that support a map of features.<br/>
 */
public abstract class FeaturesCapable implements Serializable {
    private static final long serialVersionUID = 1L;

    private FastHashMap features = new FastHashMap();
    /** key = validation id, value = the validation */
    private Validation[] validations = new Validation[0];

    /**
     * Create a new FeaturesCapable instance.
     */
    public FeaturesCapable() {
        features.setFast(true);
    }

    /**
     * Get the (live) map of features.
     * @return Map<String, Object>
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFeatures() {
        return features;
    }

    /**
     * Set whether to optimize read operations by accessing the
     * features map in an unsynchronized manner.
     * @param fast
     */
    public void optimizeRead(boolean fast) {
        features.setFast(fast);
    }

    /**
     * Get the specified feature.
     * @param <T>
     * @param key
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T> T getFeature(String key) {
        return (T) features.get(key);
    }

    /**
     * Get the specified feature, returning <code>defaultValue</code> if undeclared.
     * @param <T>
     * @param key
     * @param defaultValue
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T> T getFeature(String key, T defaultValue) {
        final T v = (T) features.get(key);
        if (v == null) {
            return (features.containsKey(key)) ? null : defaultValue;
        } else {
            return v;
        }
    }

    /**
     * Convenience method to set a particular feature value.
     * @param <T>
     * @param key
     * @param value
     */
    public <T> void putFeature(String key, T value) {
        features.put(key, value);
    }

    /**
     * Create a deep copy (copy receiver and copy properties).
     * @param <T>
     * @return new T instance
     */
    @SuppressWarnings("unchecked")
    public <T extends FeaturesCapable> T copy() {
        try {
            T self = (T) clone();
            copyInto(self);
            return self;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("cannot clone() " + this, e);
        }
    }

    /**
     * Copy this {@link FeaturesCapable} into another {@link FeaturesCapable} instance.
     * @param <T>
     * @param target
     */
    protected <T extends FeaturesCapable> void copyInto(T target) {
        target.features = (FastHashMap) features.clone();
        if (validations != null) {
            target.validations = validations.clone();
        }
    }

    /**
     * Get any validations set for this {@link FeaturesCapable}.
     * @return Validation array
     */
    public Validation[] getValidations() {
        return validations;
    }

    /**
     * Add a validation to this {@link FeaturesCapable}.
     * @param validation to add
     */
    public void addValidation(Validation validation) {
        validations = (Validation[]) ArrayUtils.add(validations, validation);
    }

    /**
     * Search for an equivalent validation among those configured.
     * @param aValidation
     * @return true if found
     */
    public boolean hasValidation(Validation aValidation) {
        if (validations == null) return false;
        for (Validation validation : validations) {
            if (validation.equals(aValidation)) return true;
        }
        return false;
    }
}
