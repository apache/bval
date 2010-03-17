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
package com.agimatec.validation.example;

import com.agimatec.validation.model.Features;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/** Copyright: Agimatec GmbH 2008 */
public class BusinessObjectBeanInfo extends SimpleBeanInfo {
    Class targetClass = BusinessObject.class;

    @Override
    public BeanInfo[] getAdditionalBeanInfo() {
        ExplicitBeanInfo bi = new ExplicitBeanInfo();
        bi.setPropertyDescriptors(_getPropertyDescriptors());
        return new BeanInfo[]{bi};
    }

    public PropertyDescriptor[] _getPropertyDescriptors() {
        try {
            PropertyDescriptor numericValue = new PropertyDescriptor("numericValue",
                    targetClass, "getNumericValue", "setNumericValue");
            numericValue.setValue(Features.Property.MAX_VALUE, new Integer(100));
            numericValue.setValue(Features.Property.MIN_VALUE, new Integer(-100));
            return new PropertyDescriptor[]{numericValue};
        } catch (IntrospectionException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}

class ExplicitBeanInfo extends SimpleBeanInfo {
    private PropertyDescriptor[] propertyDescriptors;

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public void setPropertyDescriptors(PropertyDescriptor[] propertyDescriptors) {
        this.propertyDescriptors = propertyDescriptors;
    }
}