/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.bval.jsr303;


import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.util.PropertyAccess;
import org.apache.commons.lang.StringUtils;

import javax.validation.ValidationException;
import java.lang.reflect.*;
import java.util.*;

/**
 * Description: handle nested property paths <br/>
 */
final class NestedMetaProperty {
    private MetaBean metaBean;
    private MetaProperty metaProperty;
    private final String propertyPath;
    private boolean nested;
    private Object value;

    public NestedMetaProperty(String path, Object value) {
        this.propertyPath = path;
        this.value = value;
    }

     void parse() {
        try {
            StringTokenizer tokens = new StringTokenizer(propertyPath, ".[]", true);
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();
                if ("[".equals(token)) {
                    String sindex = tokens.nextToken();
                    int idx = Integer.parseInt(sindex);
                    token = tokens.nextToken();
                    if (!"]".equals(token)) {
                        throw new ValidationException(
                                "']' missing, invalid property format: " + propertyPath);
                    }
                    useIndexedValue(idx);
                    resolveMetaBean();
                } else if (!".".equals(token)) { // it is a property name
                    MetaProperty mp = getMetaBean().getProperty(token);
                    if (mp == null) {
                        throw new IllegalArgumentException(
                                "unknown property '" + token + "' in " + getMetaBean().getId());
                    }
                    if (getValue() != null) {
                        setValue(
                              PropertyAccess.getProperty(getValue(), token));
                    }
                    setMetaProperty(mp);
                    resolveMetaBean();
                }
            }
        } catch (ValidationException ex) {
            throw ex; // route exception
        } catch (IllegalArgumentException ex) {
            throw ex; // route exception
        } catch (Exception ex) { // wrap exception
            throw new ValidationException(
                    "invalid property format: " + propertyPath, ex);

        }
    }

    public MetaProperty getMetaProperty() {
        return metaProperty;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public boolean isNested() {
        return nested;
    }

    public void setMetaProperty(MetaProperty aMetaProperty) {
        if (this.metaProperty != null) {
            this.nested = true;
        }
        this.metaProperty = aMetaProperty;
    }

    public MetaBean getMetaBean() {
        return metaBean;
    }

    public void setMetaBean(MetaBean metaBean) {
        this.metaBean = metaBean;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    private void useIndexedValue(int idx) {
        setValue(getAtIndex(getValue(), idx));
    }

    private Object getAtIndex(Object value, int idx) {
        if (value == null) return null;
        if (value instanceof Iterable<?>) {
            Iterator<?> iter = ((Iterable<?>) value).iterator();
            for (int i = 0; i <= idx; i++) {
                value = iter.next();
            }
            return value;
        } else if (value.getClass().isArray()) {
            return getAtIndex(Arrays.asList(value), idx);
        } else {
            throw new ValidationException("cannot access indexed value from " + value);
        }
    }

    static Type typeOf(Member member) {
        if (member instanceof Field) {
            return ((Field) member).getGenericType();
        }
        if (member instanceof Method) {
            return ((Method) member).getGenericReturnType();
        }
        throw new IllegalArgumentException("Member " + member + " is neither a field nor a method");
    }

    static Type getIndexedType(Type type) {
        Type indexedType = type;
        if (isCollection(type) && type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Class<?> collectionClass = getCollectionClass(type);
            if (Collection.class.isAssignableFrom(collectionClass)) {
                indexedType = paramType.getActualTypeArguments()[0];
            } else if (Map.class.isAssignableFrom(collectionClass)) {
                indexedType = paramType.getActualTypeArguments()[1];
            }
        } else if (isArray(type) && type instanceof GenericArrayType) {
            GenericArrayType arrayTye = (GenericArrayType) type;
            indexedType = arrayTye.getGenericComponentType();
        }
        return indexedType;
    }

    static Class<? extends Collection<?>> getCollectionClass(Type type) {
        if (type instanceof Class<?> && isCollectionClass((Class<?>) type)) {
            return (Class<? extends Collection<?>>) type;
        }
        if (type instanceof ParameterizedType) {
            return getCollectionClass(((ParameterizedType) type).getRawType());
        }
        if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            if (upperBounds.length == 0) {
                return null;
            }
            return getCollectionClass(upperBounds[0]);
        }
        return null;
    }

    static boolean isArray(Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>) type).isArray();
        }
        return type instanceof GenericArrayType;
    }

    /**
     * @param type the type to check.
     * @return Returns <code>true</code> if <code>type</code> is a collection type or <code>false</code> otherwise.
     */
    static boolean isCollection(Type type) {
        return getCollectionClass(type) != null;
    }

    static boolean isCollectionClass(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
    }

    // enhancement: HACK ALERT! improve support for nested property types, this is not correct in all cases
    private void resolveMetaBean() {
        if (metaProperty.getMetaBean() == null) {
            return;
        }

        if (getValue() != null) {
            metaBean = metaProperty.getMetaBean().resolveMetaBean(getValue());
        } else {
            Member member = null;
            try {
                member = metaBean.getBeanClass().getDeclaredField(metaProperty.getName());
            } catch (NoSuchFieldException e) {
                String getter = "get" + StringUtils.capitalize(metaProperty.getName());
                try {
                    member = metaBean.getBeanClass().getDeclaredMethod(getter);
                } catch (NoSuchMethodException e1) {
                    try {
                        member = metaBean.getBeanClass().getField(metaProperty.getName());
                    } catch (NoSuchFieldException e2) {
                        try {
                            member = metaBean.getBeanClass().getMethod(getter);
                        } catch (NoSuchMethodException e3) {
                        }
                    }
                }
            }
            if (member != null) {
                Type type = getIndexedType(typeOf(member));
                if (type != null) {
                    metaBean = metaProperty.getMetaBean().resolveMetaBean(type);
                }
            }
        }
    }
}
