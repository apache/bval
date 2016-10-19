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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;

import org.junit.Test;


/**
 * Checks the correct behavior of the validator resolution algorithm.
 * 
 * @author Carlos Vara
 */
public class ValidatorResolutionTest extends ValidationTestBase {

    /**
     * Check that a {@link ConstraintDefinitionException} is thrown when the
     * only available validator is associated with a different annotation type.
     */
    @Test(expected = ConstraintDefinitionException.class)
    public void testInvalidValidator() {
        validator.validate(new Person());
    }

    public static class Person {
        @PersonName
        public String name;
    }

    @Constraint(validatedBy = { InvalidPersonNameValidator.class })
    @Documented
    @Target( { METHOD, FIELD, TYPE })
    @Retention(RUNTIME)
    public static @interface PersonName {
        String message() default "Wrong person name";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class InvalidPersonNameValidator implements ConstraintValidator<NotNull, String> {
        @Override
        public void initialize(NotNull constraintAnnotation) {
            // Nothing
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return true;
        }
    }

}
