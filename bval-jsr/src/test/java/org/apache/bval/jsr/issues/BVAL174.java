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
package org.apache.bval.jsr.issues;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.util.Set;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

public class BVAL174 {

    @Audience("movies")
    public String getMovie() {
        return "";
    }

    @Audience("movies")
    public void addMovie(String newMovie) {

    }

    @Documented
    @jakarta.validation.Constraint(validatedBy = {Audience.Constraint.class})
    @Target({METHOD, ANNOTATION_TYPE})
    @Retention(RUNTIME)
    public @interface Audience {

        String value();

        Class<?>[] groups() default {};

        String message() default "The 'aud' claim must contain '{value}'";

        Class<? extends Payload>[] payload() default {};

        class Constraint implements ConstraintValidator<Audience, JsonWebToken> {
            private Audience audience;

            @Override
            public void initialize(final Audience constraint) {
                this.audience = constraint;
            }

            @Override
            public boolean isValid(final JsonWebToken value, final ConstraintValidatorContext context) {
                final Set<String> audience = value.getAudience();
                return audience != null && audience.contains(this.audience.value());
            }
        }
    }

    public class JsonWebToken {

        public Set<String> getAudience() {
            return null;
        }
    }
}
