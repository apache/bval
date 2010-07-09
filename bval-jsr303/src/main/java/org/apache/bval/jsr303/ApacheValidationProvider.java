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
package org.apache.bval.jsr303;

import javax.validation.Configuration;
import javax.validation.ValidationException;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ConfigurationState;
import javax.validation.spi.ValidationProvider;

/**
 * Description: Implementation of {@link ValidationProvider} for jsr303 implementation of
 * the apache-validation framework.
 * <p/>
 * <br/>
 * User: roman.stumm <br/>
 * Date: 29.10.2008 <br/>
 * Time: 14:45:41 <br/>
 */
public class ApacheValidationProvider
      implements ValidationProvider<ApacheValidatorConfiguration> {
    /**
     * Learn whether a particular builder class is suitable for this {@link ValidationProvider}.
     * @param builderClass
     * @return boolean suitability
     */
    public boolean isSuitable(Class<? extends Configuration<?>> builderClass) {
        return ApacheValidatorConfiguration.class == builderClass;
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurationImpl createSpecializedConfiguration(BootstrapState state) {
        return new ConfigurationImpl(state, this);
    }

    /**
     * {@inheritDoc}
     */
    public Configuration<?> createGenericConfiguration(BootstrapState state) {
        return new ConfigurationImpl(state, null);
    }

    /**
     * {@inheritDoc}
     * @throws javax.validation.ValidationException
     *          if the ValidatorFactory cannot be built
     */
    public ApacheValidatorFactory buildValidatorFactory(ConfigurationState configuration) {
        try {
            ApacheValidatorFactory factory = new ApacheValidatorFactory();
            factory.configure(configuration);
            return factory;
        } catch (RuntimeException ex) {
            throw new ValidationException("error building ValidatorFactory", ex);
        }
    }

}
