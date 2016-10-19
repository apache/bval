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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.Payload;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;

import org.apache.bval.jsr.CustomConstraintValidatorFactoryTest.GoodPerson.GoodPersonValidator;

import junit.framework.TestCase;

/**
 * Checks that overriding the default {@link ConstraintValidatorFactory} works
 * as expected.
 *
 * @author Carlos Vara
 */
public class CustomConstraintValidatorFactoryTest extends TestCase {

    /**
     * If the custom ConstraintValidatorFactory returns <code>null</code> for a
     * valid {@link ConstraintValidatorFactory#getInstance(Class)} call, a
     * validation exception should be thrown.
     */
    public void testValidationExceptionWhenFactoryReturnsNullValidator() {

        ConstraintValidatorFactory customFactory = new ConstraintValidatorFactory() {
            @Override
            public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                return null; // always return null
            }

            @Override
            public void releaseInstance(ConstraintValidator<?, ?> instance) {
                // no-op
            }
        };

        // Create a validator with this factory
        ApacheValidatorConfiguration customConfig = Validation.byProvider(ApacheValidationProvider.class).configure().constraintValidatorFactory(customFactory);
        Validator validator = customConfig.buildValidatorFactory().getValidator();

        try {
            validator.validate(new Person());
            fail("ValidationException must be thrown when factory returns a null constraint validator.");
        } catch (ValidationException e) {
            // correct
        }
    }

    @GoodPerson
    public static class Person {
    }

    @Constraint(validatedBy = { GoodPersonValidator.class })
    @Target({ METHOD, FIELD, ANNOTATION_TYPE, TYPE })
    @Retention(RUNTIME)
    @Documented
    public static @interface GoodPerson {

        String message() default "Not a good person";
        Class<?>[] groups() default { };
        Class<? extends Payload>[] payload() default {};

        public static class GoodPersonValidator implements ConstraintValidator<GoodPerson, Person> {
            @Override
            public void initialize(GoodPerson constraintAnnotation) {
            }

            @Override
            public boolean isValid(Person value, ConstraintValidatorContext context) {
                return true;
            }
        }
    }

}
