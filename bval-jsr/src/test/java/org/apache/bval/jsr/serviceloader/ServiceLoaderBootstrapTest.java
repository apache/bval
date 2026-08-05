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
package org.apache.bval.jsr.serviceloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Exercises constraint validator discovery through a real
 * {@code META-INF/services/jakarta.validation.ConstraintValidator} on the test classpath, i.e. the
 * {@code ParticipantFactory} lookup and the wiring done by {@code ApacheValidatorFactory}, which the provider's own
 * unit tests bypass.
 */
public class ServiceLoaderBootstrapTest {

    public static class ServiceLoadedBean {
        @ServiceLoadedConstraint
        public String value;

        public ServiceLoadedBean(String value) {
            this.value = value;
        }
    }

    public static class CollidingBean {
        @CollidingConstraint
        public String value;
    }

    private static Validator validator;

    @BeforeClass
    public static void setup() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void testServiceLoadedValidatorApplied() {
        final Set<ConstraintViolation<ServiceLoadedBean>> violations =
            validator.validate(new ServiceLoadedBean("nope"));

        assertEquals(1, violations.size());
        assertEquals(ServiceLoadedConstraint.class,
            violations.iterator().next().getConstraintDescriptor().getAnnotation().annotationType());
    }

    @Test
    public void testServiceLoadedValidatorSatisfied() {
        assertTrue(validator.validate(new ServiceLoadedBean("valid")).isEmpty());
    }

    /**
     * Two validators for the same constraint and target type, one from {@code validatedBy()} and one from the service
     * loader, neither source overriding the other.
     */
    @Test
    public void testCollisionWithAnnotationDeclaredValidator() {
        assertThrows(ValidationException.class, () -> validator.validate(new CollidingBean()));
    }
}
