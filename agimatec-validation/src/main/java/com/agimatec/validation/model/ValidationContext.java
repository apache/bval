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
package com.agimatec.validation.model;

import com.agimatec.validation.util.AccessStrategy;

/**
 * Description: Interface of the context that holds all state information
 * during the validation process<br/>
 * User: roman.stumm <br/>
 * Date: 28.04.2008 <br/>
 * Time: 09:36:02 <br/>
 * Copyright: Agimatec GmbH
 */
public interface ValidationContext<T extends ValidationListener> {
    Object getPropertyValue();

    /** get the value by using the given access strategy and cache it */
    Object getPropertyValue(AccessStrategy access);

    String getPropertyName();

    T getListener();

    Object getBean();

    MetaBean getMetaBean();

    void setMetaBean(MetaBean metaBean);

    MetaProperty getMetaProperty();

    void setBean(Object bean);

    boolean collectValidated();

    void setBean(Object aBean, MetaBean aMetaBean);

    void setMetaProperty(MetaProperty metaProperty);

    /** step deeper into association at 'prop' */
    void moveDown(MetaProperty prop, AccessStrategy access);

    /** step out from a validation of associated objects. */
    void moveUp(Object bean, MetaBean metaBean);

    /**
     * set the index of the object currently validated into the context.
     * used to create the propertyPath with [index] information for collections.
     */
    void setCurrentIndex(int index);

    /**
     * set the key of the object in a map currently validated into the context.
     * used to create the propertyPath with [key] information for maps.
     */
    void setCurrentKey(Object key);
}
