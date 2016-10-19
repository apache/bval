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

import java.beans.Introspector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Description: the meta description of a bean or class. the class/bean itself can have a map of features and an array
 * of metaproperties.<br/>
 * 
 * @see MetaProperty
 */
@Privilizing(@CallTo(Reflection.class))
public class MetaBean extends FeaturesCapable implements Cloneable, Features.Bean {
    private static final long serialVersionUID = 2L;

    private String id;
    private String name;
    private Class<?> beanClass;

    private Map<String, MetaProperty> properties = null;
    private Map<Method, MetaMethod> methods = null;
    private Map<Constructor<?>, MetaConstructor> constructors = null;

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
     * @return Class
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
        if (beanClass != null) {
            // order of fields to ensure correct failling order
            final Map<String, MetaProperty> oldProperties = properties;
            final Map<Method, MetaMethod> oldMethods = methods;
            final Map<Constructor<?>, MetaConstructor> oldConstructors = constructors;

            properties = new TreeMap<String, MetaProperty>(new FieldComparator(beanClass));
            if (oldProperties != null) {
                properties.putAll(oldProperties);
            }
            methods = new TreeMap<Method, MetaMethod>(new MethodComparator(beanClass));
            if (oldMethods != null) {
                methods.putAll(oldMethods);
            }
            constructors = new TreeMap<Constructor<?>, MetaConstructor>(new ConstructorComparator(beanClass));
            if (oldConstructors != null) {
                constructors.putAll(oldConstructors);
            }
        }
    }

    /**
     * Get the properties.
     * 
     * @return MetaProperty[]
     */
    public MetaProperty[] getProperties() {
        if (properties == null) {
            return new MetaProperty[0];
        }
        return properties.values().toArray(new MetaProperty[this.properties.size()]);
    }

    public MetaMethod[] getMethods() {
        if (methods == null) {
            return new MetaMethod[0];
        }
        return methods.values().toArray(new MetaMethod[this.methods.size()]);
    }

    public void addMethod(final Method method, final MetaMethod meta) {
        if (methods == null) {
            methods = new HashMap<Method, MetaMethod>();
        }
        methods.put(method, meta);
    }

    public void addConstructor(final Constructor<?> constructor, final MetaConstructor meta) {
        if (constructors == null) {
            constructors = new HashMap<Constructor<?>, MetaConstructor>();
        }
        constructors.put(constructor, meta);
    }

    /**
     * Set the properties.
     * 
     * @param properties
     *            the MetaProperty[] to set
     */
    public void setProperties(MetaProperty[] properties) {
        this.properties = new HashMap<String, MetaProperty>();
        for (final MetaProperty property : properties) {
            this.properties.put(property.getName(), property);
        }
    }

    /**
     * Get the specified {@link MetaProperty}.
     * 
     * @param name property name
     * @return MetaProperty found or <code>null</code>
     */
    public MetaProperty getProperty(String name) {
        if (properties == null) {
            return null;
        }
        return this.properties.get(name);
    }

    /**
     * Learn whether any known property is a relationship.
     * 
     * @see MetaProperty#isRelationship()
     * @return true when at least one of the properties is a relationship
     */
    public boolean hasRelationships() {
        if (properties == null) {
            return false;
        }
        for (MetaProperty property : this.properties.values()) {
            if (property.isRelationship()) {
                return true;
            }
        }
        return false;
    }

    /**
     * bidirectional - set the relationship between a MetaProperty and its parentMetaBean
     * 
     * @param name property name
     * @param property
     *            if <code>null</code>, remove
     */
    public void putProperty(String name, MetaProperty property) {
        if (properties == null) {
            properties = new HashMap<String, MetaProperty>();
        }
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
    @Override
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
     * @param bean instance
     * @return <code>this</code> or <code>null</code>
     */
    public MetaBean resolveMetaBean(Object bean) {
        return bean == null || bean == beanClass || beanClass.isInstance(bean) ? this : null;
    }

    public MetaMethod getMethod(final Method method) {
        return methods == null ? null : methods.get(method);
    }

    public MetaConstructor getConstructor(final Constructor<?> constructor) {
        return constructors == null ? null : constructors.get(constructor);
    }

    protected static class FieldComparator implements Comparator<String> {
        private final Map<String, Integer> fields = new HashMap<String, Integer>();

        protected FieldComparator(final Class<?> beanClass) {
            int i = 0;
            Class<?> clazz = beanClass;
            while (clazz != null && clazz != Object.class) {
                for (final Field f : Reflection.getDeclaredFields(clazz)) {
                    final String name = f.getName();
                    if (!fields.containsKey(name)) {
                        fields.put(name, Integer.valueOf(++i));
                    }
                }
                for (final Method m : clazz.getDeclaredMethods()) {
                    final String name = getPropertyName(m);
                    if (StringUtils.isNotEmpty(name)) {
                        if (!fields.containsKey(name)) {
                            fields.put(name, Integer.valueOf(++i));
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }

        private String getPropertyName(Method potentialAccessor) {
            if (potentialAccessor.getParameterTypes().length == 0) {
                final String name = potentialAccessor.getName();
                if (Boolean.TYPE.equals(potentialAccessor.getReturnType())
                    && potentialAccessor.getName().startsWith("is")) {
                    return Introspector.decapitalize(name.substring(2));
                }
                if (!Void.TYPE.equals(potentialAccessor.getReturnType())
                    && potentialAccessor.getName().startsWith("get")) {
                    return Introspector.decapitalize(name.substring(3));
                }
            }
            return null;
        }

        @Override
        public int compare(final String o1, final String o2) {
            final Integer i1 = fields.get(o1);
            final Integer i2 = fields.get(o2);
            if (i1 == null) {
                if (i2 == null) {
                    // java.util.TreeMap requires that the comparator be consistent with #equals(),
                    // therefore we must not incorrectly report 0 comparison for different property names
                    return StringUtils.compare(o1, o2);
                }
                return -1;
            }
            if (i2 == null) {
                return 1;
            }
            return i1.intValue() - i2.intValue();
        }

    }

    protected static class MethodComparator implements Comparator<Method> {
        private final Map<Method, Integer> methods = new HashMap<Method, Integer>();

        protected MethodComparator(final Class<?> beanClass) {
            Class<?> clazz = beanClass;
            while (clazz != null && clazz != Object.class) {
                for (final Method m : Reflection.getDeclaredMethods(clazz)) {
                    methods.put(m, Arrays.hashCode(m.getParameterTypes()));
                }
                clazz = clazz.getSuperclass();
            }
        }

        @Override
        public int compare(final Method o1, final Method o2) {
            if (o1 == o2) {
                return 0;
            }

            final int i = o1.getName().compareTo(o2.getName());
            return i == 0 ? methods.get(o1) - methods.get(o2) : i;
        }
    }

    protected static class ConstructorComparator implements Comparator<Constructor<?>> {
        private final Map<Constructor<?>, Integer> constructors = new HashMap<Constructor<?>, Integer>();

        protected ConstructorComparator(final Class<?> beanClass) {
            for (final Constructor<?> c : Reflection.getDeclaredConstructors(beanClass)) {
                constructors.put(c, Arrays.hashCode(c.getParameterTypes()));
            }
        }

        @Override
        public int compare(final Constructor<?> o1, final Constructor<?> o2) {
            if (o1 == o2) {
                return 0;
            }

            final int i = o1.getName().compareTo(o2.getName());
            return i == 0 ? constructors.get(o1) - constructors.get(o2) : i;
        }
    }
}
