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
package com.agimatec.validation;

import com.agimatec.validation.model.MetaBean;

import java.util.Map;

/**
 * Description: Interface to find BeanInfos <br/>
 * User: roman.stumm <br/>
 * Date: 05.07.2007 <br/>
 * Time: 16:17:20 <br/>
 * Copyright: Agimatec GmbH 2008
 */
public interface MetaBeanFinder {
    /**
     * @param beanInfoId - symbolic unique name of Meta Info
     * @return BeanInfo
     * @throws IllegalArgumentException - when MetaBean not found
     */
    MetaBean findForId(String beanInfoId);

    /**
     * @param clazz - bean class
     * @return BeanInfo (never null)
     */
    MetaBean findForClass(Class clazz);

    /**
     * @return all MetaBeans for classes that have a xml descriptor:
     *         key = bean.id, value = MetaBean
     */
    public Map<String, MetaBean> findAll();
}
