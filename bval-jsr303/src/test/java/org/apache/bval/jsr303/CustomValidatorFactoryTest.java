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

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ConfigurationState;

import junit.framework.TestCase;

/**
 * Test the ability to force a particular {@link ValidatorFactory}
 * implementation class.
 * 
 * @version $Rev$ $Date$
 */
public class CustomValidatorFactoryTest extends TestCase {

    public static class CustomValidatorFactory extends ApacheValidatorFactory {

        /**
         * Create a new CustomValidatorFactory instance.
         * 
         * @param configurationState
         */
        public CustomValidatorFactory(ConfigurationState configurationState) {
            super(configurationState);
        }
    }

    public static class IncompatibleValidatorFactory implements ValidatorFactory {

        public ConstraintValidatorFactory getConstraintValidatorFactory() {
            return null;
        }

        public MessageInterpolator getMessageInterpolator() {
            return null;
        }

        public TraversableResolver getTraversableResolver() {
            return null;
        }

        public Validator getValidator() {
            return null;
        }

        public <T> T unwrap(Class<T> type) {
            return null;
        }

        public ValidatorContext usingContext() {
            return null;
        }

    }

    public static class NotAValidatorFactory {
        public NotAValidatorFactory(ConfigurationState configurationState) {
        }
    }

    public void testDefaultValidatorFactory() {
        Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory().unwrap(
            ApacheValidatorFactory.class);
    }

    public void testNoSuchType() {
        try {
            Validation.byProvider(ApacheValidationProvider.class).configure().addProperty(
                ApacheValidatorConfiguration.Properties.VALIDATOR_FACTORY_CLASSNAME, "no.such.type")
                .buildValidatorFactory();
            fail();
        } catch (ValidationException ex) {
            assertTrue(ex.getCause() instanceof ClassNotFoundException);
        }
    }

    public void testCustomValidatorFactory() {
        doTest(CustomValidatorFactory.class, null);
    }

    public void testInvalidType() {
        doTest(NotAValidatorFactory.class, ClassCastException.class);
    }

    public void testUnsupportedValidatorFactoryType() {
        doTest(IncompatibleValidatorFactory.class, NoSuchMethodException.class);
    }

    private void doTest(Class<?> validatorFactoryType, Class<? extends Exception> expectedFailureCause) {
        try {
            Validation.byProvider(ApacheValidationProvider.class).configure().addProperty(
                ApacheValidatorConfiguration.Properties.VALIDATOR_FACTORY_CLASSNAME, validatorFactoryType.getName())
                .buildValidatorFactory().unwrap(validatorFactoryType);
            assertNull(expectedFailureCause);
        } catch (ValidationException ex) {
            assertNotNull(expectedFailureCause);
            assertTrue(expectedFailureCause.isInstance(ex.getCause()));
        }
    }
}
