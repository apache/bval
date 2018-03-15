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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.CascadableDescriptor;
import javax.validation.metadata.ContainerDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.ExecutableDescriptor;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.metadata.AnnotationBehaviorMergeStrategy;
import org.apache.bval.jsr.metadata.CompositeBuilder;
import org.apache.bval.jsr.metadata.DualBuilder;
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

    public static <E extends ExecutableDescriptor> boolean isConstrained(E descriptor) {
        return descriptor != null && (descriptor.hasConstrainedParameters() || descriptor.hasConstrainedReturnValue());
    }

    private final ApacheValidatorFactory validatorFactory;
    private final ConcurrentMap<Class<?>, BeanD<?>> beanDescriptors = new ConcurrentHashMap<>();
    private final ReflectionBuilder reflectionBuilder;
    private final MetadataReader metadataReader;

    public DescriptorManager(ApacheValidatorFactory validatorFactory) {
        super();
        this.validatorFactory = Validate.notNull(validatorFactory, "validatorFactory");
        this.reflectionBuilder = new ReflectionBuilder(validatorFactory);
        this.metadataReader = new MetadataReader(validatorFactory);
    }

    public <T> BeanDescriptor getBeanDescriptor(Class<T> beanClass) {
        Validate.notNull(beanClass, IllegalArgumentException::new, "beanClass");

        // cannot use computeIfAbsent due to recursion being the usual case:
        if (beanDescriptors.containsKey(beanClass)) {
            return beanDescriptors.get(beanClass);
        }
        final BeanD<T> beanD = new BeanD<>(metadataReader.forBean(beanClass, builder(beanClass)));
        @SuppressWarnings("unchecked")
        final BeanD<T> result =
            Optional.ofNullable((BeanD<T>) beanDescriptors.putIfAbsent(beanClass, beanD)).orElse(beanD);
        return result;
    }

    public void clear() {
        beanDescriptors.clear();
    }

    private <T> MetadataBuilder.ForBean<T> builder(Class<T> beanClass) {
        final MetadataBuilder.ForBean<T> primaryBuilder =
            new HierarchyBuilder(validatorFactory, reflectionBuilder::forBean).forBean(beanClass);

        final MetadataBuilder.ForBean<T> customBuilder =
            new HierarchyBuilder(validatorFactory, this::customBuilder).forBean(beanClass);

        return customBuilder.isEmpty() ? primaryBuilder : DualBuilder.forBean(primaryBuilder, customBuilder);
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
