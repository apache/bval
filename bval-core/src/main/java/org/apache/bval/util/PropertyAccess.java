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

import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Description: Undefined dynamic strategy (FIELD or METHOD access). Uses Apache
 * Commons BeanUtils (if present) to support its {@code DynaBean} type. Otherwise the
 * {@code java.beans} APIs are used for Java bean property methods and we fall
 * back to accessing field values directly.
 */
@Privilizing(@CallTo(Reflection.class))
public class PropertyAccess extends AccessStrategy {
    private static final Logger log = Logger.getLogger(PropertyAccess.class.getName());
    private static final String BEANUTILS = "org.apache.commons.beanutils.BeanUtils";
    private static final String BEANUTILS_PROPERTY_ACCESS = "org.apache.bval.util.BeanUtilsPropertyAccess";
    private static final Constructor<? extends PropertyAccess> BEANUTILS_PROPERTY_ACCESS_CTOR;
    private static final ConcurrentMap<Class<?>, Map<String, PropertyDescriptor>> PROPERTY_DESCRIPTORS =
        new ConcurrentHashMap<Class<?>, Map<String, PropertyDescriptor>>();

    static {
        final ClassLoader cl = Reflection.getClassLoader(PropertyAccess.class);
        boolean useBeanUtils;
        try {
            Reflection.toClass(BEANUTILS, cl);
            useBeanUtils = true;
        } catch (Exception e) {
            useBeanUtils = false;
        }
        Constructor<? extends PropertyAccess> ctor;
        if (useBeanUtils) {
            try {
                final Class<?> beanUtilsPropertyAccess = Reflection.toClass(BEANUTILS_PROPERTY_ACCESS, cl);

                ctor = Reflection.getDeclaredConstructor(beanUtilsPropertyAccess.asSubclass(PropertyAccess.class),
                    Class.class, String.class);

            } catch (Exception e) {
                ctor = null;
            }
        } else {
            ctor = null;
        }
        BEANUTILS_PROPERTY_ACCESS_CTOR = ctor;
    }

    /**
     * Obtain a {@link PropertyAccess} instance.
     * @param clazz
     * @param propertyName
     * @return PropertyAccess
     * @since 1.1.2
     */
    public static PropertyAccess getInstance(Class<?> clazz, String propertyName) {
        if (BEANUTILS_PROPERTY_ACCESS_CTOR != null) {
            try {
                return BEANUTILS_PROPERTY_ACCESS_CTOR.newInstance(clazz, propertyName);
            } catch (Exception e) {
                log.log(Level.WARNING, String.format("Exception encountered attempting to instantiate %s(%s, %s)",
                    BEANUTILS_PROPERTY_ACCESS_CTOR, clazz, propertyName), e);
            }
        }
        return new PropertyAccess(clazz, propertyName);
    }

    private final Class<?> beanClass;
    private final String propertyName;
    private Field rememberField;

    /**
     * Create a new PropertyAccess instance.
     * 
     * @param clazz
     * @param propertyName
     */
    @Deprecated
    // keep as protected
    public PropertyAccess(Class<?> clazz, String propertyName) {
        this.beanClass = clazz;
        this.propertyName = propertyName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ElementType getElementType() {
        return rememberField != null ? ElementType.FIELD : ElementType.METHOD;
    }

    protected Object getPublicProperty(Object bean)
        throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (bean instanceof Map<?, ?>) {
            return ((Map<?, ?>) bean).get(propertyName);
        }
        final Method readMethod = getPropertyReadMethod(propertyName, bean.getClass());
        if (readMethod == null) {
            throw new NoSuchMethodException(toString());
        }
        final boolean unset = Reflection.setAccessible(readMethod, true);
        try {
            return readMethod.invoke(bean);
        } finally {
            if (unset) {
                Reflection.setAccessible(readMethod, false);
            }
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
    public static Object getProperty(Object bean, String propertyName)
        throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return getInstance(bean.getClass(), propertyName).get(bean);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Property{" + beanClass.getName() + '.' + propertyName + '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getJavaType() {
        Type result = getTypeInner();
        return result == null ? Object.class : result;
    }

    /**
     * Learn whether this {@link PropertyAccess} references a known property.
     * 
     * @return boolean
     */
    public boolean isKnown() {
        return getTypeInner() != null;
    }

    /**
     * Find out what, if any, type can be calculated.
     * 
     * @return type found or <code>null</code>
     */
    private Type getTypeInner() {
        if (rememberField != null) {
            return rememberField.getGenericType();
        }
        Method readMethod = getPropertyReadMethod(propertyName, beanClass);
        if (readMethod != null) {
            return readMethod.getGenericReturnType();
        }
        Field fld = getField(propertyName, beanClass);
        if (fld != null) {
            cacheField(fld);
            return rememberField.getGenericType();
        }
        return null;
    }

    private static Method getPropertyReadMethod(String propertyName, Class<?> beanClass) {
        final Map<String, PropertyDescriptor> propertyDescriptors = getPropertyDescriptors(beanClass);
        if (propertyDescriptors.containsKey(propertyName)) {
            return propertyDescriptors.get(propertyName).getReadMethod();
        }
        return null;
    }

    private static Field getField(String propertyName, Class<?> beanClass) {
        try { // try public field
            return beanClass.getField(propertyName);
        } catch (NoSuchFieldException ex2) {
            // search for private/protected field up the hierarchy
            Class<?> theClass = beanClass;
            while (theClass != null) {
                try {
                    return theClass.getDeclaredField(propertyName);
                } catch (NoSuchFieldException ex3) {
                    // do nothing
                }
                theClass = theClass.getSuperclass();
            }
        }
        return null;
    }

    private static Object readField(Field field, Object bean) throws IllegalAccessException {
        final boolean mustUnset = Reflection.setAccessible(field, true);
        try {
            return field.get(bean);
        } finally {
            if (mustUnset) {
                Reflection.setAccessible(field, false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(Object bean) {
        try {
            if (rememberField != null) { // cache field of previous access
                return readField(rememberField, bean);
            }
            try { // try public method
                return getPublicProperty(bean);
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
        Field field = getField(propertyName, beanClass);
        if (field != null) {
            cacheField(field);
            return readField(rememberField, bean);
        }
        throw new IllegalArgumentException("cannot access field " + propertyName);
    }

    private void cacheField(Field field) {
        this.rememberField = field;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PropertyAccess that = (PropertyAccess) o;

        return beanClass.equals(that.beanClass) && propertyName.equals(that.propertyName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result;
        result = beanClass.hashCode();
        result = 31 * result + propertyName.hashCode();
        return result;
    }

    private static Map<String, PropertyDescriptor> getPropertyDescriptors(Class<?> type) {
        if (PROPERTY_DESCRIPTORS.containsKey(type)) {
            return PROPERTY_DESCRIPTORS.get(type);
        }
        Map<String, PropertyDescriptor> m;
        try {
            final PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(type).getPropertyDescriptors();
            if (propertyDescriptors == null) {
                m = Collections.emptyMap();
            } else {
                m = new HashMap<String, PropertyDescriptor>();
                for (PropertyDescriptor pd : propertyDescriptors) {
                    m.put(pd.getName(), pd);
                }
            }
        } catch (IntrospectionException e) {
            log.log(Level.SEVERE, String.format("Cannot locate %s for ", BeanInfo.class.getSimpleName(), type), e);
            m = Collections.emptyMap();
        }
        final Map<String, PropertyDescriptor> faster = PROPERTY_DESCRIPTORS.putIfAbsent(type, m);
        return faster == null ? m : faster;
    }
}
