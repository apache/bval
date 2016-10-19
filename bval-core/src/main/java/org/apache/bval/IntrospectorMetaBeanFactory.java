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
package org.apache.bval;

import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Enumeration;

import static org.apache.bval.model.Features.Property.HIDDEN;
import static org.apache.bval.model.Features.Property.PREFERRED;
import static org.apache.bval.model.Features.Property.READONLY;

/**
 * Description: use information from java.beans.Introspector in MetaBeans. The PropertyDescriptor can contain info about
 * HIDDEN, PREFERRED, READONLY and other features<br/>
 * NOTE: THIS IS AN OPTIONAL CLASS, TO ENABLE IT, SET Factory Property apache.bval.enable-introspector="true"
 */
public class IntrospectorMetaBeanFactory implements MetaBeanFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildMetaBean(MetaBean meta) throws Exception {
        if (meta.getBeanClass() == null) {
            return; // handle only, when local class exists
        }
        BeanInfo info = Introspector.getBeanInfo(meta.getBeanClass());
        if (meta.getName() == null && info.getBeanDescriptor() != null) {
            meta.setName(info.getBeanDescriptor().getName()); // (display?)name = simple class name!
        }
        for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
            if (!(pd instanceof IndexedPropertyDescriptor || pd.getName().equals("class"))) {
                MetaProperty metaProp = buildMetaProperty(pd, meta.getProperty(pd.getName()));
                meta.putProperty(pd.getName(), metaProp);
            }
        }
    }

    /**
     * Create a {@link MetaProperty} from the specified {@link PropertyDescriptor}.
     * 
     * @param pd
     * @return MetaProperty
     */
    @Deprecated
    protected MetaProperty buildMetaProperty(PropertyDescriptor pd) {
        return buildMetaProperty(pd, null);
    }

    /**
     * Create a {@link MetaProperty} from the specified {@link PropertyDescriptor}.
     * 
     * @param pd
     * @param existing
     * @return MetaProperty
     */
    protected MetaProperty buildMetaProperty(PropertyDescriptor pd, MetaProperty existing) {
        MetaProperty meta = new MetaProperty();
        meta.setName(pd.getName());
        meta.setType(determineGenericPropertyType(pd));
        if (pd.isHidden()) {
            meta.putFeature(HIDDEN, Boolean.TRUE);
        }
        if (pd.isPreferred()) {
            meta.putFeature(PREFERRED, Boolean.TRUE);
        }
        if (pd.isConstrained()) {
            meta.putFeature(READONLY, Boolean.TRUE);
        }
        Enumeration<String> enumeration = pd.attributeNames();
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement();
            Object value = pd.getValue(key);
            meta.putFeature(key, value);
        }
        return meta;
    }

    private Type determineGenericPropertyType(PropertyDescriptor pd) {
        Method m = pd.getReadMethod();
        if (m != null) {
            return m.getGenericReturnType();
        }
        m = pd.getWriteMethod();
        if (m != null && m.getParameterTypes().length == 1) {
            return m.getGenericParameterTypes()[0];
        }
        return pd.getPropertyType();
    }
}
