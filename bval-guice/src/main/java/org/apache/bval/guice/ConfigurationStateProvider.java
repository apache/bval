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

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.spi.ConfigurationState;
import javax.validation.spi.ValidationProvider;

import org.apache.bval.jsr303.ConfigurationImpl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@code javax.validation.spi.ConfigurationState} provider implementation.
 *
 * @version $Id$
 */
@Singleton
public final class ConfigurationStateProvider implements Provider<ConfigurationState> {

    private final ConfigurationImpl configurationState;

    @Inject
    public ConfigurationStateProvider(ValidationProvider<?> aProvider) {
        this.configurationState = new ConfigurationImpl(null, aProvider);
    }

    @Inject
    public void traversableResolver(TraversableResolver traversableResolver) {
        this.configurationState.traversableResolver(traversableResolver);
    }

    @Inject
    public void messageInterpolator(MessageInterpolator messageInterpolator) {
        this.configurationState.messageInterpolator(messageInterpolator);
    }

    @Inject
    public void constraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
        this.configurationState.constraintValidatorFactory(constraintValidatorFactory);
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurationState get() {
        return this.configurationState;
    }

}
