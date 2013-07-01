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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Description: the meta description of a bean or class. the class/bean itself can have a map of features and an array
 * of metaproperties.<br/>
 * 
 * @see MetaProperty
 */
public class MetaBean extends FeaturesCapable implements Cloneable, Features.Bean {
    private static final long serialVersionUID = 2L;

    private String id;
    private String name;
    private Class<?> beanClass;

    // TODO: optimize sortings

    private Map<String, MetaProperty> properties = new TreeMap<String, MetaProperty>(new Comparator<String>() { // order of fields to ensure correct failling order
        public int compare(final String o1, final String o2) {
            return fieldIndex(o1) - fieldIndex(o2);
        }

        private int fieldIndex(final String o2) {
            final Class<?> clazz = getBeanClass();

            int i = 0;
            Class<?> beanClass1 = clazz;
            while (beanClass1 != null && beanClass1 != Object.class) {
                for (final Field f : beanClass1.getDeclaredFields()) {
                    i++;
                    if (f.getName().equals(o2)) {
                        return i;
                    }
                }
                beanClass1 = beanClass1.getSuperclass();
            }

            if (clazz != null) {
                final String getter = "get" + Character.toUpperCase(o2.charAt(0)) + o2.substring(1);
                for (final Method m : clazz.getMethods()) {
                    i++;
                    if (m.getName().equals(getter) && m.getParameterTypes().length == 0) {
                        return i;
                    }
                }
            }

            return Integer.MIN_VALUE; // to avoid collision and false positive in get() due to equals
        }
    });
    private Map<Method, MetaMethod> methods = new TreeMap<Method, MetaMethod>(new Comparator<Method>() {
        public int compare(final Method o1, final Method o2) {
            final int i = o1.getName().compareTo(o2.getName());
            if (i != 0) {
                return i;
            }
            return Arrays.hashCode(o1.getParameterTypes()) - Arrays.hashCode(o2.getParameterTypes());
        }
    });
    private Map<Constructor<?>, MetaConstructor> constructors = new TreeMap<Constructor<?>, MetaConstructor>(new Comparator<Constructor<?>>() {
        public int compare(final Constructor<?> o1, final Constructor<?> o2) {
            return Arrays.hashCode(o1.getParameterTypes()) - Arrays.hashCode(o2.getParameterTypes());
        }
    });

    /**
     * Get the id.
     * 
     * @return String
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id.
     * 
     * @param id
     *            the String to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the name.
     * 
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name.
     * 
     * @param name
     *            the String to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the beanClass.
     * 
     * @return Class<?>
     */
    public Class<?> getBeanClass() {
        return beanClass;
    }

    /**
     * Set the beanClass.
     * 
     * @param beanClass
     *            the Class<?> to set
     */
    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    /**
     * Get the properties.
     * 
     * @return MetaProperty[]
     */
    public MetaProperty[] getProperties() {
        return properties.values().toArray(new MetaProperty[this.properties.size()]);
    }

    public MetaMethod[] getMethods() {
        return methods.values().toArray(new MetaMethod[this.methods.size()]);
    }

    public void addMethod(final Method method, final MetaMethod meta) {
        methods.put(method, meta);
    }

    public MetaConstructor[] getConstructors() {
        return constructors.values().toArray(new MetaConstructor[this.constructors.size()]);
    }

    public void addConstructor(final Constructor<?> constructor, final MetaConstructor meta) {
        constructors.put(constructor, meta);
    }

    /**
     * Set the properties.
     * 
     * @param properties
     *            the MetaProperty[] to set
     */
    public void setProperties(MetaProperty[] properties) {
        this.properties.clear();
        for (MetaProperty property : properties) {
            this.properties.put(property.getName(), property);
        }
    }

    /**
     * Get the specified {@link MetaProperty}.
     * 
     * @param name
     * @return MetaProperty found or <code>null</code>
     */
    public MetaProperty getProperty(String name) {
        return this.properties.get(name);
    }

    /**
     * Learn whether any known property is a relationship.
     * 
     * @see MetaProperty#isRelationship()
     * @return true when at least one of the properties is a relationship
     */
    public boolean hasRelationships() {
        for (MetaProperty property : this.properties.values()) {
            if (property.isRelationship()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Learn whether there are any known properties.
     * 
     * @return boolean
     */
    public boolean hasProperties() {
        return this.properties.size() > 0;
    }

    /**
     * bidirectional - set the relationship between a MetaProperty and its parentMetaBean
     * 
     * @param name
     * @param property
     *            if <code>null</code>, remove
     */
    public void putProperty(String name, MetaProperty property) {
        if (property == null) {
            this.properties.remove(name);
        } else {        
            property.setParentMetaBean(this);
            this.properties.put(name, property);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "MetaBean{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", beanClass=" + beanClass + '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void copyInto(FeaturesCapable target) {
        super.copyInto(target);
        final MetaBean copy = (MetaBean) target;
        if (properties != null) {
            copy.properties = new TreeMap<String, MetaProperty>();        
            for (Map.Entry<String, MetaProperty> entry : properties.entrySet()) {
                copy.properties.put(entry.getKey(), (MetaProperty) entry.getValue().copy());
            }
        }
    }

    /**
     * <p>
     * If this {@link MetaBean} is compatible with <code>bean</code>, return <code>this</code>, else <code>null</code>.
     * </p>
     * <p>
     * Compatibility is satisfied in one of the following ways:
     * <ul>
     * <li><code>bean</code> is null</li>
     * <li><code>bean</code> is an instance of our <code>beanClass</code></li>
     * <li><code>bean</code> <em>is</em> our <code>beanClass</code> itself</li>
     * </ul>
     * </p>
     * 
     * @param bean
     * @return <code>this</code> or <code>null</code>
     */
    public MetaBean resolveMetaBean(Object bean) {
        return bean == null || bean == beanClass || beanClass.isInstance(bean) ? this : null;
    }

    public MetaMethod getMethod(final Method method) {
        return methods.get(method);
    }

    public MetaConstructor getConstructor(final Constructor<?> constructor) {
        return constructors.get(constructor);
    }
}
