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
package org.apache.bval;

import java.util.IdentityHashMap;

import org.apache.bval.model.FeaturesCapable;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.PropertyAccess;

/**
 * Description: Context during validation to help the {@link org.apache.bval.model.Validation}
 * and the {@link BeanValidator} do their jobs.
 * Used to bundle {@link BeanValidationContext} and {@link ValidationListener}
 * together <br/>
 * <b>This class is NOT thread-safe: a new instance will be created for each
 * validation
 * processing per thread.<br/></b>
 */
public class BeanValidationContext<T extends ValidationListener>
      implements ValidationContext<T> {
    /** represent an unknown propertyValue. */
    private static final Object UNKNOWN = new Object();

    /** metainfo of current object. */
    private MetaBean metaBean;
    /** current object. */
    private Object bean;
    /** metainfo of current property. */
    private MetaProperty metaProperty;
    /**
     * cached value of current property.
     * Cached because of potential redundant access for different Validations
     */
    private Object propertyValue = UNKNOWN;

    /** access strategy used for previous access */
    private AccessStrategy access;

    /** set of objects already validated to avoid endless loops. */
    protected IdentityHashMap validatedObjects = new IdentityHashMap();

    /**
     * true when value is fixed, so that it will NOT be dynamically
     * determined from the annotated element or the metaProperty.
     * <b><br>Note: When value is UNKNOWN, it will be determined THE FIRST TIME
     * IT IS ACCESSED.</b>
     */
    private boolean fixed;

    /** listener notified of validation constraint violations. */
    private T listener;

    public BeanValidationContext(T listener) {
        this.listener = listener;
    }

    public T getListener() {
        return listener;
    }

    public void setListener(T listener) {
        this.listener = listener;
    }

    /**
     * add the object to the collection of validated objects to keep
     * track of them to avoid endless loops during validation.
     *
     * @return true when the object was not already validated in this context
     */
    public boolean collectValidated() {
        return validatedObjects.put(getBean(), Boolean.TRUE) == null;
    }

    /** @return true when the object has already been validated in this context */
    public boolean isValidated(Object object) {
        return validatedObjects.containsKey(object);
    }

    /**
     * Clear map of validated objects (invoke when you want to 'reuse' the
     * context for different validations)
     */
    public void resetValidated() {
        validatedObjects.clear();
    }

    public void setBean(Object aBean, MetaBean aMetaBean) {
        bean = aBean;
        metaBean = aMetaBean;
        metaProperty = null;
        unknownValue();
    }

    /**
     * get the cached value or access it somehow (via field or method)<br>
     * <b>you should prefer getPropertyValue(AccessStrategy) instead of this method</b>
     *
     * @return the current value of the property accessed by reflection
     * @throws IllegalArgumentException - error accessing attribute (config error, reflection problem)
     * @throws IllegalStateException    - when no property is currently set in the context (application logic bug)
     */
    public Object getPropertyValue() {
        if (access == null) { // undefined access strategy
            return getPropertyValue(
                  new PropertyAccess(bean.getClass(), metaProperty.getName()));
        } else {
            return getPropertyValue(access);
        }
    }

    /** get the value by using the given access strategy and cache it */
    public Object getPropertyValue(AccessStrategy access)
          throws IllegalArgumentException, IllegalStateException {
        if (propertyValue == UNKNOWN || (this.access != access && !fixed)) {
            propertyValue = access.get(bean);
            this.access = access;
        }
        return propertyValue;
    }

    /**
     * convenience method to access metaProperty.name
     *
     * @return null or the name of the current property
     */
    public String getPropertyName() {
        return metaProperty == null ? null : metaProperty.getName();
    }

    public void setPropertyValue(Object propertyValue) {
        this.propertyValue = propertyValue;
    }

    public void setFixedValue(Object value) {
        setPropertyValue(value);
        fixed = true;
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    /**
     * depending on whether we have a metaProperty or not,
     * this returns the metaProperty or otherwise the metaBean.
     * This is used to have a simple way to request features
     * in the Validation for both bean- and property-level validations.
     *
     * @return something that is capable to deliver features
     */
    public FeaturesCapable getMeta() {
        return (metaProperty == null) ? metaBean : metaProperty;
    }

    /**
     * drop cached value.
     * mark the internal cachedValue as UNKNOWN.
     * This forces the BeanValidationContext to recompute the value
     * the next time it is accessed.
     * Use this method inside tests or when the propertyValue has been
     * changed during validation.
     */
    public void unknownValue() {
        propertyValue = UNKNOWN;
        access = null;
    }

    public MetaBean getMetaBean() {
        return metaBean;
    }

    public Object getBean() {
        return bean;
    }

    public MetaProperty getMetaProperty() {
        return metaProperty;
    }

    public void setMetaBean(MetaBean metaBean) {
        this.metaBean = metaBean;
    }

    public void setBean(Object bean) {
        this.bean = bean;
        unknownValue();
    }

    public void setMetaProperty(MetaProperty metaProperty) {
        this.metaProperty = metaProperty;
        unknownValue();
    }

    public String toString() {
        return "BeanValidationContext{ bean=" + bean + ", metaProperty=" + metaProperty +
              ", propertyValue=" + propertyValue + '}';
    }

    public void moveDown(MetaProperty prop, AccessStrategy access) {
        setMetaProperty(prop);
        setBean(getPropertyValue(access), prop.getMetaBean());
    }

    public void moveUp(Object bean, MetaBean aMetaBean) {
        setBean(bean, aMetaBean); // reset context state
    }

    public void setCurrentIndex(int index) {
        // do nothing
    }

    public void setCurrentKey(Object key) {
        // do nothing
    }

}
