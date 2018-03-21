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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstructorDescriptor;
import javax.validation.metadata.MethodDescriptor;
import javax.validation.metadata.MethodType;
import javax.validation.metadata.PropertyDescriptor;

import org.apache.bval.jsr.metadata.Signature;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.StringUtils;

public class BeanD<T> extends ElementD<Class<T>, MetadataReader.ForBean<T>> implements BeanDescriptor {

    private final Class<T> beanClass;
    private final List<Class<?>> groupSequence;
    private final Map<String, PropertyDescriptor> propertiesMap;
    private final Set<PropertyDescriptor> properties;
    private final Map<Signature, ConstructorD<T>> constructors;
    private final Map<Signature, MethodD> methods;

    BeanD(MetadataReader.ForBean<T> reader) {
        super(reader);
        this.beanClass = reader.meta.getHost();

        groupSequence = reader.getGroupSequence();
        propertiesMap = reader.getProperties(this);
        properties = propertiesMap.values().stream().filter(DescriptorManager::isConstrained).collect(ToUnmodifiable.set());
        constructors = reader.getConstructors(this);
        methods = reader.getMethods(this);
    }

    @Override
    public Class<?> getElementClass() {
        return beanClass;
    }

    @Override
    public boolean isBeanConstrained() {
        return hasConstraints() || properties.stream().anyMatch(DescriptorManager::isConstrained);
    }

    @Override
    public PropertyDescriptor getConstraintsForProperty(String propertyName) {
        return Optional.ofNullable(getProperty(propertyName)).filter(DescriptorManager::isConstrained).orElse(null);
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
        final EnumSet<MethodType> filter = EnumSet.of(methodType, methodTypes);
        return methods.values().stream().filter(m -> filter.contains(m.getMethodType())).collect(ToUnmodifiable.set());
    }

    @Override
    public ConstructorDescriptor getConstraintsForConstructor(Class<?>... parameterTypes) {
        return constructors.get(new Signature(beanClass.getName(), parameterTypes));
    }

    @Override
    public Set<ConstructorDescriptor> getConstrainedConstructors() {
        return constructors.values().stream().collect(ToUnmodifiable.set());
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
    public List<Class<?>> getGroupSequence() {
        return groupSequence;
    }

    public final Type getGenericType() {
        return getElementClass();
    }
}
