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

import static org.hamcrest.CoreMatchers.isA;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorContext;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.spi.ConfigurationState;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test the ability to force a particular {@link ValidatorFactory}
 * implementation class.
 */
public class CustomValidatorFactoryTest {

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

        @Override
        public ConstraintValidatorFactory getConstraintValidatorFactory() {
            return null;
        }

        @Override
        public ParameterNameProvider getParameterNameProvider() {
            return null;
        }

        @Override
        public MessageInterpolator getMessageInterpolator() {
            return null;
        }

        @Override
        public TraversableResolver getTraversableResolver() {
            return null;
        }

        @Override
        public Validator getValidator() {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> type) {
            return null;
        }

        @Override
        public void close() {

        }

        @Override
        public ValidatorContext usingContext() {
            return null;
        }

        @Override
        public ClockProvider getClockProvider() {
            return null;
        }

    }

    public static class NotAValidatorFactory {
        public NotAValidatorFactory(ConfigurationState configurationState) {
        }
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testDefaultValidatorFactory() {
        Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory()
            .unwrap(ApacheValidatorFactory.class);
    }

    @Test
    public void testNoSuchType() {
        thrown.expect(ValidationException.class);
        thrown.expectCause(isA(ClassNotFoundException.class));

        Validation.byProvider(ApacheValidationProvider.class).configure()
            .addProperty(ApacheValidatorConfiguration.Properties.VALIDATOR_FACTORY_CLASSNAME, "no.such.type")
            .buildValidatorFactory();
    }

    @Test
    public void testCustomValidatorFactory() {
        doTest(CustomValidatorFactory.class, null);
    }

    @Test
    public void testInvalidType() {
        doTest(NotAValidatorFactory.class, ClassCastException.class);
        doTest(NotAValidatorFactory.class, ClassCastException.class);
    }

    @Test
    public void testUnsupportedValidatorFactoryType() {
        doTest(IncompatibleValidatorFactory.class, NoSuchMethodException.class);
    }

    private void doTest(Class<?> validatorFactoryType, Class<? extends Exception> expectedFailureCause) {
        if (expectedFailureCause != null) {
            thrown.expect(ValidationException.class);
            thrown.expectCause(isA(expectedFailureCause));
        }
        Validation.byProvider(ApacheValidationProvider.class).configure()
            .addProperty(ApacheValidatorConfiguration.Properties.VALIDATOR_FACTORY_CLASSNAME,
                validatorFactoryType.getName())
            .buildValidatorFactory().unwrap(validatorFactoryType);
    }
}
