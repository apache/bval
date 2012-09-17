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

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.validation.Configuration;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ConfigurationState;
import javax.validation.spi.ValidationProvider;

import org.apache.commons.lang3.ClassUtils;

/**
 * Description: Implementation of {@link ValidationProvider} for jsr303
 * implementation of the apache-validation framework.
 * <p/>
 * <br/>
 * User: roman.stumm <br/>
 * Date: 29.10.2008 <br/>
 * Time: 14:45:41 <br/>
 */
public class ApacheValidationProvider implements ValidationProvider<ApacheValidatorConfiguration> {

    /**
     * Learn whether a particular builder class is suitable for this
     * {@link ValidationProvider}.
     * 
     * @param builderClass
     * @return boolean suitability
     */
    public boolean isSuitable(Class<? extends Configuration<?>> builderClass) {
        return ApacheValidatorConfiguration.class.equals(builderClass);
    }

    /**
     * {@inheritDoc}
     */
    public ApacheValidatorConfiguration createSpecializedConfiguration(BootstrapState state) {
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
     * 
     * @throws javax.validation.ValidationException
     *             if the ValidatorFactory cannot be built
     */
    public ValidatorFactory buildValidatorFactory(final ConfigurationState configuration) {
        final Class<? extends ValidatorFactory> validatorFactoryClass;
        try {
            String validatorFactoryClassname =
                configuration.getProperties().get(ApacheValidatorConfiguration.Properties.VALIDATOR_FACTORY_CLASSNAME);

            if (validatorFactoryClassname == null) {
                validatorFactoryClass = ApacheValidatorFactory.class;
            } else {
                validatorFactoryClass =
                    ClassUtils.getClass(validatorFactoryClassname).asSubclass(ValidatorFactory.class);
            }
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("error building ValidatorFactory", ex);
        }

        // FIXME 2011-03-27 jw:
        // Should not use privileged action, but to avoid breaking things
        // doing it here like the former version of this class did.
        //
        // The privileged action should be applied by the ValidatorFactory
        // itself, if required.
        // No privileges should be required to access the constructor,
        // because the classloader of ApacheValidationProvider will always
        // be an ancestor of the loader of validatorFactoryClass.
        return (System.getSecurityManager() == null)
            ? instantiateValidatorFactory(validatorFactoryClass, configuration) : AccessController
                .doPrivileged(new PrivilegedAction<ValidatorFactory>() {
                    public ValidatorFactory run() {
                        return instantiateValidatorFactory(validatorFactoryClass, configuration);
                    }
                });
    }

    private static ValidatorFactory instantiateValidatorFactory(
        final Class<? extends ValidatorFactory> validatorFactoryClass, final ConfigurationState configuration) {
        try {
            return validatorFactoryClass.getConstructor(ConfigurationState.class).newInstance(configuration);
        } catch (final Exception ex) {
            throw new ValidationException("Cannot instantiate : " + validatorFactoryClass, ex);
        }
    }

}
