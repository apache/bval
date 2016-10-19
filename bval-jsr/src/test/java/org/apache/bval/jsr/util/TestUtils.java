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
package org.apache.bval.jsr.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor.ConstraintFinder;

/**
 * Description: <br/>
 */
public class TestUtils {
    /**
     * @param violations
     * @param propertyPath
     *            - string format of a propertyPath
     * @return the constraintViolation with the propertyPath's string
     *         representation given
     */
    public static <T> ConstraintViolation<T> getViolation(Set<ConstraintViolation<T>> violations, String propertyPath) {
        for (ConstraintViolation<T> each : violations) {
            if (each.getPropertyPath().toString().equals(propertyPath)) {
                return each;
            }
        }
        return null;
    }

    /**
     * @param violations
     * @param propertyPath
     * @return count of violations
     */
    public static <T> int countViolations(Set<ConstraintViolation<T>> violations, String propertyPath) {
        int result = 0;
        for (ConstraintViolation<T> each : violations) {
            if (each.getPropertyPath().toString().equals(propertyPath)) {
                result++;
            }
        }
        return result;
    }

    /**
     * @param <T>
     * @param violations
     * @param message
     * @return the constraint violation with the specified message found, if any
     */
    public static <T> ConstraintViolation<T> getViolationWithMessage(Set<ConstraintViolation<T>> violations,
        String message) {
        for (ConstraintViolation<T> each : violations) {
            if (each.getMessage().equals(message)) {
                return each;
            }
        }
        return null;
    }

    /**
     * assume set addition either does nothing, returning false per collection
     * contract, or throws an Exception; in either case size should remain
     * unchanged
     * 
     * @param collection
     */
    public static void failOnModifiable(Collection<?> collection, String description) {
        int size = collection.size();
        try {
            assertFalse(String.format("should not permit modification to %s", description), collection.add(null));
        } catch (Exception e) {
            // okay
        }
        assertEquals("constraint descriptor set size changed", size, collection.size());
    }

    /**
     * Assert that the specified ConstraintFinder provides constraints of each of the specified types.
     * @param constraintFinder
     * @param types
     */
    public static void assertConstraintTypesFound(ConstraintFinder constraintFinder, Class<? extends Annotation>... types) {
        outer: for (Class<? extends Annotation> type : types) {
            for (ConstraintDescriptor<?> descriptor : constraintFinder.getConstraintDescriptors()) {
                if (descriptor.getAnnotation().annotationType().equals(type)) {
                    continue outer;
                }
            }
            fail(String.format("Missing expected constraint descriptor of type %s", type));
        }
    }
}
