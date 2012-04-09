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

import org.apache.bval.util.AccessStrategy;

/**
 * Description: Interface of the context that holds all state information
 * during the validation process<br/>
 */
public interface ValidationContext<T extends ValidationListener> {
    /**
     * Get the property value.
     * @return {@link Object}
     */
    Object getPropertyValue();

    /**
     * Get the value by using the given access strategy.
     * @param access
     * @return {@link Object}
     */
    Object getPropertyValue(AccessStrategy access);

    /**
     * Get the property name.
     * @return {@link String}
     */
    String getPropertyName();

    /**
     * Get the {@link ValidationListener}.
     * @return T
     */
    T getListener();

    /**
     * Get the bean.
     * @return {@link Object}
     */
    Object getBean();

    /**
     * Get the model meta-bean.
     * @return {@link MetaBean}
     */
    MetaBean getMetaBean();

    /**
     * Set the model meta-bean.
     * @param metaBean
     */
    void setMetaBean(MetaBean metaBean);

    /**
     * Get the model meta-property.
     * @return {@link MetaProperty}
     */
    MetaProperty getMetaProperty();

    /**
     * Set the bean.
     * @param bean
     */
    void setBean(Object bean);

    /**
     * Avoid recursion by recording the current state of this context as having been validated.
     * <p/>
     *
     * @return true when this state had not already been recorded
     */
    boolean collectValidated();

    /**
     * Set the current bean/metabean.
     * @param aBean
     * @param aMetaBean
     */
    void setBean(Object aBean, MetaBean aMetaBean);

    /**
     * Set the current meta-property.
     * @param metaProperty
     */
    void setMetaProperty(MetaProperty metaProperty);

    /**
     * Step deeper into association at 'prop' 
     * @param prop
     * @param access
     */
    void moveDown(MetaProperty prop, AccessStrategy access);

    /**
     * Step out from a validation of associated objects.
     * @param bean
     * @param metaBean
     */
    void moveUp(Object bean, MetaBean metaBean);

    /**
     * Set the index of the object currently validated into the context.
     * used to create the propertyPath with [index] information for collections.
     * @param index
     */
    void setCurrentIndex(Integer index);

    /**
     * set the key of the object in a map currently validated into the context.
     * used to create the propertyPath with [key] information for maps.
     * @param key
     */
    void setCurrentKey(Object key);

    /**
     * Get the current access strategy.
     * @return {@link AccessStrategy}
     */
    AccessStrategy getAccess();
}
