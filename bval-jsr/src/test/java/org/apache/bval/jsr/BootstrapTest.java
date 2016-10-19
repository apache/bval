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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.validation.Configuration;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.bootstrap.ProviderSpecificBootstrap;
import javax.validation.spi.ValidationProvider;

import org.apache.bval.constraints.NotNullValidator;
import org.apache.bval.jsr.example.Customer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Description: <br/>
 */
public class BootstrapTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testDirectBootstrap() {
        Validator validator = ApacheValidatorFactory.getDefault().getValidator();
        assertNotNull(validator);
        assertTrue(ApacheValidatorFactory.getDefault() == ApacheValidatorFactory.getDefault());
    }

    @Test
    public void testEverydayBootstrap() {
        ApacheValidatorFactory factory = (ApacheValidatorFactory) Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        assertNotNull(validator);

        // each call to Validation.getValidationBuilder() returns a new builder
        // with new state
        ApacheValidatorFactory factory2 = (ApacheValidatorFactory) Validation.buildDefaultValidatorFactory();
        assertTrue(factory2 != factory);
        assertTrue(factory2.getMessageInterpolator() != factory.getMessageInterpolator());

    }

    @Test
    public void testLocalizedMessageInterpolatorFactory() {
        Configuration<?> builder = Validation.byDefaultProvider().configure();
        // changing the builder allows to create different factories
        DefaultMessageInterpolator interpolator = new DefaultMessageInterpolator();
        builder.messageInterpolator(interpolator);
        ApacheValidatorFactory factory = (ApacheValidatorFactory) builder.buildValidatorFactory();

        // ALTERNATIVE:
        // you could do it without modifying the builder or reusing it,
        // but then you need to use bval-core proprietary APIs:
        ((DefaultMessageInterpolator) factory.getMessageInterpolator()).setLocale(Locale.ENGLISH);
        // now factory's message resolver is using the english locale
    }

    /**
     * some tests based on RI tested behaviors to ensure our implementation
     * works as the reference implementation
     */
    @Test
    public void testCustomConstraintFactory() {

        Configuration<?> builder = Validation.byDefaultProvider().configure();
        assertDefaultBuilderAndFactory(builder);

        ValidatorFactory factory = builder.buildValidatorFactory();
        Validator validator = factory.getValidator();

        Customer customer = new Customer();
        customer.setFirstName("John");

        Set<ConstraintViolation<Customer>> ConstraintViolations = validator.validate(customer);
        assertFalse(ConstraintViolations.isEmpty());

        builder = Validation.byDefaultProvider().configure();
        builder.constraintValidatorFactory(new ConstraintValidatorFactory() {
            @Override
            public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                if (key == NotNullValidator.class) {
                    @SuppressWarnings("unchecked")
                    final T result = (T) new BadlyBehavedNotNullValidator();
                    return result;
                }
                return new DefaultConstraintValidatorFactory().getInstance(key);
            }

            @Override
            public void releaseInstance(ConstraintValidator<?, ?> instance) {
                // no-op
            }
        });
        factory = builder.buildValidatorFactory();
        validator = factory.getValidator();
        Set<ConstraintViolation<Customer>> ConstraintViolations2 = validator.validate(customer);
        assertTrue("Wrong number of constraints", ConstraintViolations.size() > ConstraintViolations2.size());
    }

    @Test
    public void testCustomResolverAndType() {
        ValidationProviderResolver resolver = new ValidationProviderResolver() {

            @Override
            public List<ValidationProvider<?>> getValidationProviders() {
                List<ValidationProvider<?>> list = new ArrayList<ValidationProvider<?>>(1);
                list.add(new ApacheValidationProvider());
                return list;
            }
        };

        ApacheValidatorConfiguration builder =
            Validation.byProvider(ApacheValidationProvider.class).providerResolver(resolver).configure();
        assertDefaultBuilderAndFactory(builder);
    }

    @Test
    public void testCustomResolver() {
        ValidationProviderResolver resolver = new ValidationProviderResolver() {

            @Override
            public List<ValidationProvider<?>> getValidationProviders() {
                return Collections.<ValidationProvider<?>> singletonList(new ApacheValidationProvider());
            }
        };

        Configuration<?> builder = Validation.byDefaultProvider().providerResolver(resolver).configure();
        assertDefaultBuilderAndFactory(builder);
    }

    private void assertDefaultBuilderAndFactory(Configuration<?> builder) {
        assertNotNull(builder);
        assertTrue(builder instanceof ConfigurationImpl);

        ValidatorFactory factory = builder.buildValidatorFactory();
        assertNotNull(factory);
        assertTrue(factory instanceof ApacheValidatorFactory);
    }

    @Test
    public void testFailingCustomResolver() {
        ValidationProviderResolver resolver = new ValidationProviderResolver() {

            @Override
            public List<ValidationProvider<?>> getValidationProviders() {
                return Collections.emptyList();
            }
        };

        ProviderSpecificBootstrap<ApacheValidatorConfiguration> type =
            Validation.byProvider(ApacheValidationProvider.class);

        final ProviderSpecificBootstrap<ApacheValidatorConfiguration> specializedBuilderFactory =
            type.providerResolver(resolver);
        
        thrown.expect(ValidationException.class);
        thrown.expectMessage("provider");
        thrown.expectMessage(ApacheValidationProvider.class.getName());

        specializedBuilderFactory.configure();
    }

    class BadlyBehavedNotNullValidator extends NotNullValidator {
        @Override
        public boolean isValid(Object object, ConstraintValidatorContext context) {
            return true;
        }
    }
}
