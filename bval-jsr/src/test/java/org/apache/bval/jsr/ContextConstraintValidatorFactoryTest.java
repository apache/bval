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

import static org.junit.Assert.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Payload;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Test;

/**
 * <a href="https://issues.apache.org/jira/browse/BVAL-111">https://issues.apache.org/jira/browse/BVAL-111</a>
 * was a serious regression that resulted in BVal's bypassing the context-specific {@link ConstraintValidatorFactory},
 * rather using the instance available from the {@link ValidatorFactory}.  Thus any solutions to e.g. inject
 * collaborators into {@link ConstraintValidator} implementations would fail. 
 */
public class ContextConstraintValidatorFactoryTest extends ValidationTestBase {

	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Constraint(validatedBy = { Contrived.Validator.class })
	public @interface Contrived {
		String message() default "{org.apache.bval.constraints.Contrived.message}";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};

		public static class Validator implements ConstraintValidator<Contrived, Object> {
			private Object requiredCollaborator;

			public Object getRequiredCollaborator() {
				return requiredCollaborator;
			}

			public void setRequiredCollaborator(Object requiredCollaborator) {
				this.requiredCollaborator = requiredCollaborator;
			}

			@Override
            public void initialize(Contrived constraintAnnotation) {
			}

			@Override
            public boolean isValid(Object value, ConstraintValidatorContext context) {
				getRequiredCollaborator().toString();
				return true;
			}

		}

	}

	@Contrived
	public static class Example {
	}

    @Override
    protected Validator createValidator() {
        final ConstraintValidatorFactory constraintValidatorFactory = new ConstraintValidatorFactory() {

            @Override
            public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                if (key.equals(Contrived.Validator.class)) {
                    final Contrived.Validator result = new Contrived.Validator();
                    result.setRequiredCollaborator(new Object());
                    @SuppressWarnings("unchecked")
                    final T t = (T) result;
                    return t;
                }
                return null;
            }

            @Override
            public void releaseInstance(ConstraintValidator<?, ?> instance) {
                // no-op
            }
        };
        return factory.usingContext().constraintValidatorFactory(constraintValidatorFactory).getValidator();
    }

	@Test
	public void testContextBoundConstraintValidatorFactory() {
		final Set<ConstraintViolation<Example>> violations = validator.validate(new Example());
		assertTrue(violations.isEmpty());
	}
}
