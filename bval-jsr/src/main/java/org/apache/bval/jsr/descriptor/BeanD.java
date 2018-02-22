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
import org.apache.bval.util.Lazy;
import org.apache.bval.util.StringUtils;

public class BeanD extends ElementD<Class<?>, MetadataReader.ForBean> implements BeanDescriptor {

    private static boolean constrainedProperty(PropertyDescriptor pd) {
        return pd.hasConstraints() || pd.isCascaded();
    }

    private final Class<?> beanClass;

    private final Lazy<List<Class<?>>> groupSequence;
    private final Lazy<Map<String, PropertyDescriptor>> propertiesMap;
    private final Lazy<Set<PropertyDescriptor>> properties;
    private final Lazy<Map<Signature, ConstructorD>> constructors;
    private final Lazy<Map<Signature, MethodD>> methods;

    BeanD(MetadataReader.ForBean reader) {
        super(reader);
        this.beanClass = reader.meta.getHost();

        groupSequence = new Lazy<>(reader::getGroupSequence);
        propertiesMap = new Lazy<>(() -> reader.getProperties(this));
        properties = new Lazy<>(() -> propertiesMap.get().values().stream().filter(BeanD::constrainedProperty)
            .collect(ToUnmodifiable.set()));
        constructors = new Lazy<>(() -> reader.getConstructors(this));
        methods = new Lazy<>(() -> reader.getMethods(this));
    }

    @Override
    public Class<?> getElementClass() {
        return beanClass;
    }

    @Override
    public boolean isBeanConstrained() {
        return hasConstraints() || properties.get().stream().anyMatch(DescriptorManager::isConstrained);
    }

    @Override
    public PropertyDescriptor getConstraintsForProperty(String propertyName) {
        return Optional.ofNullable(getProperty(propertyName)).filter(BeanD::constrainedProperty).orElse(null);
    }

    @Override
    public Set<PropertyDescriptor> getConstrainedProperties() {
        return properties.get();
    }

    @Override
    public MethodDescriptor getConstraintsForMethod(String methodName, Class<?>... parameterTypes) {
        final Map<Signature, MethodD> methods = this.methods.get();
        final Signature key = new Signature(methodName, parameterTypes);
        return methods.get(key);
    }

    @SuppressWarnings("unlikely-arg-type")
    @Override
    public Set<MethodDescriptor> getConstrainedMethods(MethodType methodType, MethodType... methodTypes) {
        EnumSet<MethodType> filter = EnumSet.of(methodType, methodTypes);
        return methods.get().values().stream().filter(m -> filter.contains(m.getMethodType()))
                      .collect(ToUnmodifiable.set());
    }

    @Override
    public ConstructorDescriptor getConstraintsForConstructor(Class<?>... parameterTypes) {
        return constructors.get().get(new Signature(beanClass.getName(), parameterTypes));
    }

    @Override
    public Set<ConstructorDescriptor> getConstrainedConstructors() {
        return constructors.get().values().stream().collect(ToUnmodifiable.set());
    }

    public PropertyDescriptor getProperty(String propertyName) {
        Exceptions.raiseIf(StringUtils.isBlank(propertyName), IllegalArgumentException::new,
            "propertyName was null/empty/blank");

        return propertiesMap.get().get(propertyName);
    }

    @Override
    protected BeanD getBean() {
        return this;
    }

    @Override
    public List<Class<?>> getGroupSequence() {
        return groupSequence.get();
    }

    public final Type getGenericType() {
        return getElementClass();
    }
}
