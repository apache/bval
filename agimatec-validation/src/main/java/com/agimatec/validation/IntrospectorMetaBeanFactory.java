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

import static com.agimatec.validation.model.Features.Property.*;
import com.agimatec.validation.model.MetaBean;
import com.agimatec.validation.model.MetaProperty;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Enumeration;

/**
 * Description: use information from java.beans.Introspector in MetaBeans.
 * The PropertyDescriptor can contain info about HIDDEN, PREFERRED, READONLY
 * and other features<br/>
 * User: roman <br/>
 * Date: 07.10.2009 <br/>
 * Time: 11:43:19 <br/>
 * Copyright: Agimatec GmbH
 */
public final class IntrospectorMetaBeanFactory implements MetaBeanFactory {

    public void buildMetaBean(MetaBean meta) throws Exception {
        if(meta.getBeanClass() == null) return; // handle only, when local class exists

        BeanInfo info = Introspector.getBeanInfo(meta.getBeanClass());
        if (info.getBeanDescriptor() != null) {
            meta.setName(
                  info.getBeanDescriptor().getName()); // (display?)name = simple class name!
        }
        for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
            if (!pd.getName().equals("class")) { // except this one!
                MetaProperty metaProp = buildMetaProperty(pd);
                meta.putProperty(pd.getName(), metaProp);
            }
        }
    }

    protected MetaProperty buildMetaProperty(PropertyDescriptor pd) {
        MetaProperty meta = new MetaProperty();
        meta.setName(pd.getName());
//        meta.setDisplayName(pd.getDisplayName());
        meta.setType(pd.getPropertyType());
        if (pd.isHidden()) meta.putFeature(HIDDEN, Boolean.TRUE);
        if (pd.isPreferred()) meta.putFeature(PREFERRED, Boolean.TRUE);
        if (pd.isConstrained()) meta.putFeature(READONLY, Boolean.TRUE);

        Enumeration<String> enumeration = pd.attributeNames();
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement();
            Object value = pd.getValue(key);
            meta.putFeature(key, value);
        }
        return meta;
    }
}
