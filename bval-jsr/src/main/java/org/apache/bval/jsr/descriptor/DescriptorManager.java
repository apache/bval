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
package org.apache.bval.jsr.descriptor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.CascadableDescriptor;
import jakarta.validation.metadata.ContainerDescriptor;
import jakarta.validation.metadata.ElementDescriptor;
import jakarta.validation.metadata.ExecutableDescriptor;
import jakarta.validation.metadata.MethodType;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.metadata.AnnotationBehaviorMergeStrategy;
import org.apache.bval.jsr.metadata.CompositeBuilder;
import org.apache.bval.jsr.metadata.DualBuilder;
import org.apache.bval.jsr.metadata.EmptyBuilder;
import org.apache.bval.jsr.metadata.HierarchyBuilder;
import org.apache.bval.jsr.metadata.MetadataBuilder;
import org.apache.bval.jsr.metadata.ReflectionBuilder;
import org.apache.bval.util.Validate;

public class DescriptorManager {
    public static <D extends ElementDescriptor & CascadableDescriptor & ContainerDescriptor> boolean isConstrained(
        D descriptor) {
        return descriptor != null && (descriptor.hasConstraints() || descriptor.isCascaded()
            || !descriptor.getConstrainedContainerElementTypes().isEmpty());
    }

    public static <D extends ElementDescriptor & CascadableDescriptor & ContainerDescriptor> boolean isCascaded(
        D descriptor) {
        return descriptor != null && (descriptor.isCascaded()
            || descriptor.getConstrainedContainerElementTypes().stream().anyMatch(DescriptorManager::isCascaded));
    }

    public static <E extends ExecutableDescriptor> boolean isConstrained(E descriptor) {
        return descriptor != null && (descriptor.hasConstrainedParameters() || descriptor.hasConstrainedReturnValue());
    }

    private final ApacheValidatorFactory validatorFactory;
    private final ConcurrentMap<Class<?>, BeanD<?>> beanDescriptors = new ConcurrentHashMap<>();
    // synchronization unnecessary
    private final ReflectionBuilder reflectionBuilder;

    public DescriptorManager(ApacheValidatorFactory validatorFactory) {
        super();
        this.validatorFactory = Validate.notNull(validatorFactory, "validatorFactory");
        this.reflectionBuilder = new ReflectionBuilder(validatorFactory);
    }

    public <T> BeanDescriptor getBeanDescriptor(Class<T> beanClass) {
        Validate.notNull(beanClass, IllegalArgumentException::new, "beanClass");

        // cannot use computeIfAbsent due to recursion being the usual case:
        final BeanD<?> existing = beanDescriptors.get(beanClass);
        if (existing != null) {
            return existing;
        }
        final BeanD<?> value = new BeanD<>(new MetadataReader(validatorFactory, beanClass).forBean(builder(beanClass)));
        final BeanD<?> previous = beanDescriptors.putIfAbsent(beanClass, value);
        return previous == null ? value : previous;
    }

    public void clear() {
        beanDescriptors.clear();
    }

    private <T> MetadataBuilder.ForBean<T> builder(Class<T> beanClass) {
        final MetadataBuilder.ForBean<T> primaryBuilder =
            new HierarchyBuilder(validatorFactory, reflectionBuilder::forBean).forBean(beanClass);

        final MetadataBuilder.ForBean<T> customBuilder =
            new HierarchyBuilder(validatorFactory, this::customBuilder).forBean(beanClass);

        return customBuilder.isEmpty() ? primaryBuilder
            : DualBuilder.forBean(beanClass, primaryBuilder, customBuilder, validatorFactory);
    }

    private <T> MetadataBuilder.ForBean<T> customBuilder(Class<T> beanClass) {
        final List<MetadataBuilder.ForBean<T>> customBuilders =
            validatorFactory.getMetadataBuilders().getCustomBuilders(beanClass);

        if (customBuilders.isEmpty()) {
            return null;
        }
        if (customBuilders.size() == 1) {
            return customBuilders.get(0);
        }
        return customBuilders.stream()
            .collect(CompositeBuilder.with(validatorFactory, AnnotationBehaviorMergeStrategy.consensus()).compose());
    }
}
