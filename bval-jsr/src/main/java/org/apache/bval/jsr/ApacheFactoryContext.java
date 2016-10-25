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

import org.apache.bval.MetaBeanFinder;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;

/**
 * Description: Represents the context that is used to create
 * {@link ClassValidator} instances.
 */
@Privilizing(@CallTo(Reflection.class))
public class ApacheFactoryContext implements ValidatorContext {
    private final ApacheValidatorFactory factory;
    private volatile MetaBeanFinder metaBeanFinder;

    private MessageInterpolator messageInterpolator;
    private TraversableResolver traversableResolver;
    private ParameterNameProvider parameterNameProvider;
    private ConstraintValidatorFactory constraintValidatorFactory;

    /**
     * Create a new ApacheFactoryContext instance.
     * 
     * @param factory validator factory
     * @param metaBeanFinder meta finder
     */
    public ApacheFactoryContext(ApacheValidatorFactory factory, MetaBeanFinder metaBeanFinder) {
        this.factory = factory;
        this.metaBeanFinder = metaBeanFinder;
    }

    /**
     * Get the {@link ApacheValidatorFactory} used by this
     * {@link ApacheFactoryContext}.
     * 
     * @return {@link ApacheValidatorFactory}
     */
    public ApacheValidatorFactory getFactory() {
        return factory;
    }

    /**
     * Get the metaBeanFinder.
     * 
     * @return {@link MetaBeanFinder}
     */
    public final MetaBeanFinder getMetaBeanFinder() {
        return metaBeanFinder;
    }

    /**
     * Discard cached metadata. Calling this method unnecessarily has the effect of severly
     * limiting performance, therefore only do so when changes have been made that affect
     * validation metadata, i.e. particularly NOT in response to:
     * <ul>
     *   <li>{@link #messageInterpolator(MessageInterpolator)}</li>
     *   <li>{@link #traversableResolver(TraversableResolver)}</li>
     *   <li>{@link #constraintValidatorFactory(ConstraintValidatorFactory)</li>
     * </ul>
     */
    private synchronized void resetMeta() {
        metaBeanFinder = factory.buildMetaBeanFinder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidatorContext messageInterpolator(MessageInterpolator messageInterpolator) {
        this.messageInterpolator = messageInterpolator;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidatorContext traversableResolver(TraversableResolver traversableResolver) {
        this.traversableResolver = traversableResolver;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidatorContext constraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
        this.constraintValidatorFactory = constraintValidatorFactory;
        return this;
    }

    @Override
    public ValidatorContext parameterNameProvider(ParameterNameProvider parameterNameProvider) {
        this.parameterNameProvider = parameterNameProvider;
        resetMeta(); // needed since parameter names are a component of validation metadata
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
        return new ClassValidator(this);
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

    boolean isTreatMapsLikeBeans() {
        return Boolean
            .parseBoolean(factory.getProperties().get(ApacheValidatorConfiguration.Properties.TREAT_MAPS_LIKE_BEANS));
    }
}
