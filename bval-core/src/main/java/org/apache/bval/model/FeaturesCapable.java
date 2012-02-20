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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Description: abstract superclass of meta objects that support a map of
 * features.<br/>
 */
public abstract class FeaturesCapable implements Serializable {
    private static final long serialVersionUID = -4045110242904814218L;

    private ConcurrentMap<String, Object> features = createFeaturesMap();

    /** key = validation id, value = the validation */
    private Validation[] validations = new Validation[0];

    private volatile boolean locking;
    private ReentrantLock lock = new ReentrantLock(true);

    /**
     * Create a new FeaturesCapable instance.
     */
    public FeaturesCapable() {
        super();
    }

    /**
     * Get the (live) map of features.
     * 
     * @return Map<String, Object>
     */
    public Map<String, Object> getFeatures() {
        return features;
    }

    /**
     * Set whether to optimize read operations by accessing the features map in
     * an unsynchronized manner.
     * 
     * @param fast
     */
    public void optimizeRead(boolean fast) {
        lock.lock();
        try {
            this.locking = !fast;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the specified feature.
     * 
     * @param <T>
     * @param key
     * @return T
     */
    public <T> T getFeature(String key) {
        return getFeature(key, (T) null);
    }

    /**
     * Get the specified feature, returning <code>defaultValue</code> if
     * undeclared.
     * 
     * @param <T>
     * @param key
     * @param defaultValue
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T> T getFeature(String key, T defaultValue) {
        boolean release = acquireLockIfNeeded();
        try {
            return features.containsKey(key) ? (T) features.get(key) : defaultValue;
        } finally {
            if (release) {
                lock.unlock();
            }
        }
    }

    /**
     * Convenience method to set a particular feature value.
     * 
     * @param <T>
     * @param key
     * @param value
     */
    public <T> void putFeature(String key, T value) {
        boolean release = acquireLockIfNeeded();
        try {
            features.put(key, value);
        } finally {
            if (release) {
                lock.unlock();
            }
        }
    }

    /**
     * Create a deep copy (copy receiver and copy properties).
     * 
     * @param <T>
     * @return new T instance
     */
    public <T extends FeaturesCapable> T copy() {
        try {
            @SuppressWarnings("unchecked")
            final T self = (T) clone();
            copyInto(self);
            return self;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("cannot clone() " + this, e);
        }
    }

    /**
     * Copy this {@link FeaturesCapable} into another {@link FeaturesCapable}
     * instance.
     * 
     * @param <T>
     * @param target
     */
    protected <T extends FeaturesCapable> void copyInto(T target) {
        target.features = target.createFeaturesMap();
        target.features.putAll(features);
        if (validations != null) {
            target.validations = validations.clone();
        }
    }

    /**
     * Get any validations set for this {@link FeaturesCapable}.
     * 
     * @return Validation array
     */
    public Validation[] getValidations() {
        return ArrayUtils.clone(validations);
    }

    /**
     * Set the validations for this {@link FeaturesCapable}.
     * 
     * @param validations
     */
    public void setValidations(Validation[] validations) {
        this.validations = ArrayUtils.clone(validations);
    }

    /**
     * Add a validation to this {@link FeaturesCapable}.
     * 
     * @param validation
     *            to add
     */
    public void addValidation(Validation validation) {
        validations = ArrayUtils.add(validations, validation);
    }

    /**
     * Search for an equivalent validation among those configured.
     * 
     * @param aValidation
     * @return true if found
     */
    public boolean hasValidation(Validation aValidation) {
        if (validations == null)
            return false;
        for (Validation validation : validations) {
            if (validation.equals(aValidation))
                return true;
        }
        return false;
    }

    /**
     * Create a features map for this {@link FeaturesCapable} object.
     * @return ConcurrentMap
     */
    protected ConcurrentMap<String, Object> createFeaturesMap() {
        return new ConcurrentHashMap<String, Object>();
    }

    private boolean acquireLockIfNeeded() {
        if (locking) {
            lock.lock();
            // double read
            if (locking) {
                return true;
            }
            lock.unlock();
        }
        return false;
    }

}
