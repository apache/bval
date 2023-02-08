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
package org.apache.bval.jsr;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorContext;
import jakarta.validation.valueextraction.ValueExtractor;

import org.apache.bval.jsr.descriptor.ConstraintD;
import org.apache.bval.jsr.descriptor.DescriptorManager;
import org.apache.bval.jsr.groups.GroupsComputer;
import org.apache.bval.jsr.valueextraction.ValueExtractors;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Description: Represents the context that is used to create {@link ClassValidator} instances.
 */
@Privilizing(@CallTo(Reflection.class))
public class ApacheFactoryContext implements ValidatorContext {
    private final ApacheValidatorFactory factory;
    private final ValueExtractors valueExtractors;

    private MessageInterpolator messageInterpolator;
    private TraversableResolver traversableResolver;
    private ParameterNameProvider parameterNameProvider;
    private ConstraintValidatorFactory constraintValidatorFactory;
    private ClockProvider clockProvider;

    /**
     * Create a new ApacheFactoryContext instance.
     * 
     * @param factory
     *            validator factory
     * @param metaBeanFinder
     *            meta finder
     */
    public ApacheFactoryContext(ApacheValidatorFactory factory) {
        this.factory = factory;
        valueExtractors = factory.getValueExtractors().createChild();
    }

    /**
     * Discard cached metadata. Calling this method unnecessarily has the effect of severly limiting performance,
     * therefore only do so when changes have been made that affect validation metadata, i.e. particularly NOT in
     * response to:
     * <ul>
     * <li>{@link #messageInterpolator(MessageInterpolator)}</li>
     * <li>{@link #traversableResolver(TraversableResolver)}</li>
     * <li>{@link #constraintValidatorFactory(ConstraintValidatorFactory)</li>
     * </ul>
     */
    private synchronized void resetMeta() {
        getDescriptorManager().clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApacheFactoryContext messageInterpolator(MessageInterpolator messageInterpolator) {
        this.messageInterpolator = messageInterpolator;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApacheFactoryContext traversableResolver(TraversableResolver traversableResolver) {
        this.traversableResolver = traversableResolver;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApacheFactoryContext constraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
        this.constraintValidatorFactory = constraintValidatorFactory;
        return this;
    }

    @Override
    public ApacheFactoryContext parameterNameProvider(ParameterNameProvider parameterNameProvider) {
        this.parameterNameProvider = parameterNameProvider;
        resetMeta(); // needed since parameter names are a component of validation metadata
        return this;
    }

    @Override
    public ApacheFactoryContext clockProvider(ClockProvider clockProvider) {
        this.clockProvider = clockProvider;
        return this;
    }

    @Override
    public ApacheFactoryContext addValueExtractor(ValueExtractor<?> extractor) {
        valueExtractors.add(extractor);
        return this;
    }

    /**
     * Get the {@link ConstraintValidatorFactory}.
     * 
     * @return {@link ConstraintValidatorFactory}
     */
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return constraintValidatorFactory == null ? factory.getConstraintValidatorFactory()
            : constraintValidatorFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Validator getValidator() {
        return new ValidatorImpl(this);
    }

    /**
     * Get the {@link MessageInterpolator}.
     * 
     * @return {@link MessageInterpolator}
     */
    public MessageInterpolator getMessageInterpolator() {
        return messageInterpolator == null ? factory.getMessageInterpolator() : messageInterpolator;
    }

    /**
     * Get the {@link TraversableResolver}.
     * 
     * @return {@link TraversableResolver}
     */
    public TraversableResolver getTraversableResolver() {
        return traversableResolver == null ? factory.getTraversableResolver() : traversableResolver;
    }

    public ParameterNameProvider getParameterNameProvider() {
        return parameterNameProvider == null ? factory.getParameterNameProvider() : parameterNameProvider;
    }

    public ClockProvider getClockProvider() {
        return clockProvider == null ? factory.getClockProvider() : clockProvider;
    }

    public ValueExtractors getValueExtractors() {
        return valueExtractors;
    }

    public DescriptorManager getDescriptorManager() {
        // TODO implementation-specific feature to handle context-local descriptor customizations
        return factory.getDescriptorManager();
    }

    public GroupsComputer getGroupsComputer() {
        return factory.getGroupsComputer();
    }

    public ConstraintCached getConstraintsCache() {
        return factory.getConstraintsCache();
    }

    public ApacheValidatorFactory getFactory() {
        return factory;
    }

    public ConstraintValidator getOrComputeConstraintValidator(final ConstraintD<?> constraint, final Supplier<ConstraintValidator> computer) {
        final ConcurrentMap<ConstraintD<?>, ConstraintValidator<?, ?>> constraintsCache = factory.getConstraintsCache().getValidators();
        final ConstraintValidator<?, ?> validator = constraintsCache.get(constraint); // constraints are cached so we can query per instance
        if (validator != null) {
            return validator;
        }
        ConstraintValidator<?, ?> instance = computer.get();
        final ConstraintValidator<?, ?> existing = constraintsCache.putIfAbsent(constraint, instance);
        if (existing != null) {
            instance = existing;
        }
        return instance;
    }
}
