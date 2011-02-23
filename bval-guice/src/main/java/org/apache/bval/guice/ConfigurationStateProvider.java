/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bval.guice;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ConfigurationState;
import javax.validation.spi.ValidationProvider;

import org.apache.bval.jsr303.ConfigurationImpl;

/**
 * The {@code javax.validation.spi.ConfigurationState} provider implementation.
 *
 * @version $Id$
 */
public final class ConfigurationStateProvider implements Provider<ConfigurationState> {

    @com.google.inject.Inject(optional = true)
    private BootstrapState bootstrapState;

    @Inject
    private ValidationProvider<?> validationProvider;

    @Inject
    private TraversableResolver traversableResolver;

    @Inject
    private MessageInterpolator messageInterpolator;

    @Inject
    private ConstraintValidatorFactory constraintValidatorFactory;

    public void setBootstrapState(BootstrapState bootstrapState) {
        this.bootstrapState = bootstrapState;
    }

    public void setValidationProvider(ValidationProvider<?> validationProvider) {
        this.validationProvider = validationProvider;
    }

    public void setTraversableResolver(TraversableResolver traversableResolver) {
        this.traversableResolver = traversableResolver;
    }

    public void setMessageInterpolator(MessageInterpolator messageInterpolator) {
        this.messageInterpolator = messageInterpolator;
    }

    public void setConstraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
        this.constraintValidatorFactory = constraintValidatorFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurationState get() {
        ConfigurationImpl configuration = new ConfigurationImpl(this.bootstrapState, this.validationProvider);
        configuration.traversableResolver(this.traversableResolver);
        configuration.messageInterpolator(this.messageInterpolator);
        configuration.constraintValidatorFactory(this.constraintValidatorFactory);
        return configuration;
    }

}
