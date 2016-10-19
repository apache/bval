/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval.constraints;

import org.apache.bval.jsr.example.Address;
import org.apache.bval.jsr.example.Author;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 
 * 
 * @version $Rev: 999729 $ $Date: 2010-09-21 21:37:54 -0500 (Tue, 21 Sep 2010) $
 */
@Target( { ANNOTATION_TYPE, METHOD, FIELD })
@Constraint(validatedBy = AuthorAddressKnown.Validator.class)
@Retention(RUNTIME)
public @interface AuthorAddressKnown {

    String message() default "{org.apache.bval.constraints.AuthorAddressKnown.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default { };

    public static class Validator implements ConstraintValidator<AuthorAddressKnown, Author> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void initialize(AuthorAddressKnown constraintAnnotation) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isValid(Author value, ConstraintValidatorContext context) {
            if (value.getAddresses() == null) {
                return false;
            }
            for (Address address : value.getAddresses()) {
                if (address != null) {
                    return true;
                }
            }
            return false;
        }

    }
}
