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
package org.apache.bval.util;

import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Description: Undefined dynamic strategy (FIELD or METHOD access) Uses
 * PropertyUtils or tries to determine field to access the value<br/>
 */
public class PropertyAccess extends AccessStrategy {
    private final Class<?> beanClass;
    private final String propertyName;
    private Field rememberField;

    /**
     * Create a new PropertyAccess instance.
     * 
     * @param clazz
     * @param propertyName
     */
    public PropertyAccess(Class<?> clazz, String propertyName) {
        this.beanClass = clazz;
        this.propertyName = propertyName;
    }

    /**
     * {@inheritDoc}
     */
    public ElementType getElementType() {
        return (rememberField != null) ? ElementType.FIELD : ElementType.METHOD;
    }

    private static Object getPublicProperty(Object bean, String property) throws InvocationTargetException,
        NoSuchMethodException, IllegalAccessException {
        if (bean instanceof Map<?, ?>) {
            return ((Map<?, ?>) bean).get(property);
        } else { // supports DynaBean and standard Objects
            return PropertyUtils.getSimpleProperty(bean, property);
        }
    }

    /**
     * Get a named property from <code>bean</code>.
     * 
     * @param bean
     * @param propertyName
     * @return Object found
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     */
    public static Object getProperty(Object bean, String propertyName) throws InvocationTargetException,
        NoSuchMethodException, IllegalAccessException {
        return new PropertyAccess(bean.getClass(), propertyName).get(bean);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Property{" + beanClass.getName() + '.' + propertyName + '}';
    }

    /**
     * {@inheritDoc}
     */
    public Type getJavaType() {
        /*
         * if(Map.class.isAssignableFrom(beanClass)) { return beanClass. }
         */
        if (rememberField != null) { // use cached field of previous access
            return rememberField.getGenericType();
        }
        for (PropertyDescriptor each : PropertyUtils.getPropertyDescriptors(beanClass)) {
            if (each.getName().equals(propertyName) && each.getReadMethod() != null) {
                return each.getReadMethod().getGenericReturnType();
            }
        }
        try { // try public field
            return beanClass.getField(propertyName).getGenericType();
        } catch (NoSuchFieldException ex2) {
            // search for private/protected field up the hierarchy
            Class<?> theClass = beanClass;
            while (theClass != null) {
                try {
                    return theClass.getDeclaredField(propertyName).getGenericType();
                } catch (NoSuchFieldException ex3) {
                    // do nothing
                }
                theClass = theClass.getSuperclass();
            }
        }
        return Object.class; // unknown type: allow any type??
    }

    /**
     * {@inheritDoc}
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * {@inheritDoc}
     */
    public Object get(Object bean) {
        try {
            if (rememberField != null) { // cache field of previous access
                return rememberField.get(bean);
            }

            try { // try public method
                return getPublicProperty(bean, propertyName);
            } catch (NoSuchMethodException ex) {
                return getFieldValue(bean);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot access " + propertyName, e);
        }
    }

    private Object getFieldValue(Object bean) throws IllegalAccessException {
        Object value;
        try { // try public field
            Field aField = bean.getClass().getField(propertyName);
            value = aField.get(bean);
            rememberField = aField;
            return value;
        } catch (NoSuchFieldException ex2) {
            // search for private/protected field up the hierarchy
            Class<?> theClass = bean.getClass();
            while (theClass != null) {
                try {
                    Field aField = theClass.getDeclaredField(propertyName);
                    if (!aField.isAccessible()) {
                        aField.setAccessible(true);
                    }
                    value = aField.get(bean);
                    rememberField = aField;
                    return value;
                } catch (NoSuchFieldException ex3) {
                    // do nothing
                }
                theClass = theClass.getSuperclass();
            }
            throw new IllegalArgumentException("cannot access field " + propertyName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        PropertyAccess that = (PropertyAccess) o;

        return beanClass.equals(that.beanClass) && propertyName.equals(that.propertyName);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int result;
        result = beanClass.hashCode();
        result = 31 * result + propertyName.hashCode();
        return result;
    }
}
