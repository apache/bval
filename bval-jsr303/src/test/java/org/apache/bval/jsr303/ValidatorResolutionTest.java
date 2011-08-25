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

import junit.framework.TestCase;

import javax.validation.*;
import javax.validation.constraints.NotNull;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Locale;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Checks the correct behavior of the validator resolution algorithm.
 * 
 * @author Carlos Vara
 */
public class ValidatorResolutionTest extends TestCase {

    static ValidatorFactory factory;

    static {
        factory = Validation.buildDefaultValidatorFactory();
        ((DefaultMessageInterpolator) factory.getMessageInterpolator()).setLocale(Locale.ENGLISH);
    }

    private Validator getValidator() {
        return factory.getValidator();
    }

    /**
     * Check that a {@link ConstraintDefinitionException} is thrown when the
     * only available validator is associated with a different annotation type.
     */
    public void testInvalidValidator() {
        Validator validator = getValidator();
        try {
            validator.validate(new Person());
            fail("No exception thrown, but no valid validator available.");
        } catch (ConstraintDefinitionException e) {
            // correct
        }
    }
    
    
    public static class Person {
        @PersonName
        public String name;
    }
    
    @Constraint(validatedBy = {InvalidPersonNameValidator.class})
    @Documented
    @Target({ METHOD, FIELD, TYPE })
    @Retention(RUNTIME)
    public static @interface PersonName {
        String message() default "Wrong person name";
        Class<?>[] groups() default { };
        Class<? extends Payload>[] payload() default {};
    }
    
    public static class InvalidPersonNameValidator implements ConstraintValidator<NotNull, String> {
        public void initialize(NotNull constraintAnnotation) {
            // Nothing
        }
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return true;
        }
    }
    
}
