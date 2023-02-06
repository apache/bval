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
package org.apache.bval.jsr.descriptor;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.ConstructorDescriptor;
import jakarta.validation.metadata.MethodDescriptor;
import jakarta.validation.metadata.MethodType;
import jakarta.validation.metadata.PropertyDescriptor;

import org.apache.bval.jsr.groups.GroupStrategy;
import org.apache.bval.jsr.metadata.Signature;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.CollectionSet;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.StringUtils;

public class BeanD<T> extends ElementD<Class<T>, MetadataReader.ForBean<T>> implements BeanDescriptor {

    private final Class<T> beanClass;
    private final Map<String, PropertyDescriptor> propertiesMap;
    private final Set<PropertyDescriptor> properties;
    private final Map<Signature, ConstructorD<T>> constructors;
    private final Map<Signature, MethodD> methods;
    private final GroupStrategy groupStrategy;
    
    private final Set<ConstructorDescriptor> constrainedConstructors;
    private final Map<Set<MethodType>, Set<MethodDescriptor>> methodCache = new HashMap<>();

    BeanD(MetadataReader.ForBean<T> reader) {
        super(reader);
        this.beanClass = reader.meta.getHost();

        groupStrategy = reader.getGroupStrategy();
        propertiesMap = reader.getProperties(this);
        properties =
            propertiesMap.values().stream().filter(DescriptorManager::isConstrained).collect(ToUnmodifiable.set());
        constructors = reader.getConstructors(this);
        methods = reader.getMethods(this);
        
        constrainedConstructors =
            constructors.isEmpty() ? Collections.emptySet() : new CollectionSet<>(constructors.values());
    }

    @Override
    public Class<?> getElementClass() {
        return beanClass;
    }

    @Override
    public boolean isBeanConstrained() {
        return hasConstraints() || !properties.isEmpty();
    }

    @Override
    public PropertyDescriptor getConstraintsForProperty(String propertyName) {
        return Optional.ofNullable(getProperty(propertyName)).filter(properties::contains).orElse(null);
    }

    @Override
    public Set<PropertyDescriptor> getConstrainedProperties() {
        return properties;
    }

    @Override
    public MethodDescriptor getConstraintsForMethod(String methodName, Class<?>... parameterTypes) {
        Exceptions.raiseIf(StringUtils.isBlank(methodName), IllegalArgumentException::new,
            "method name cannot be null/empty/blank");
        return methods.get(new Signature(methodName, parameterTypes));
    }

    @Override
    public Set<MethodDescriptor> getConstrainedMethods(MethodType methodType, MethodType... methodTypes) {
        return methodCache.computeIfAbsent(EnumSet.of(methodType, methodTypes), k -> {
            if (methods.isEmpty() || k.isEmpty()) {
                return Collections.emptySet();
            }
            if (k.size() == MethodType.values().length) {
                return new CollectionSet<>(methods.values());
            }
            return methods.values().stream().filter(m -> k.contains(m.getMethodType())).collect(ToUnmodifiable.set());
        });
    }

    @Override
    public ConstructorDescriptor getConstraintsForConstructor(Class<?>... parameterTypes) {
        return constructors.get(new Signature(beanClass.getName(), parameterTypes));
    }

    @Override
    public Set<ConstructorDescriptor> getConstrainedConstructors() {
        return constrainedConstructors;
    }

    public PropertyDescriptor getProperty(String propertyName) {
        Exceptions.raiseIf(StringUtils.isBlank(propertyName), IllegalArgumentException::new,
            "propertyName was null/empty/blank");

        return propertiesMap.get(propertyName);
    }

    @Override
    protected BeanD<T> getBean() {
        return this;
    }

    @Override
    public GroupStrategy getGroupStrategy() {
        return groupStrategy;
    }

    public final Type getGenericType() {
        return getElementClass();
    }
}
