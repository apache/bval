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

import junit.framework.Assert;
import junit.framework.TestCase;

import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;
import javax.validation.metadata.BeanDescriptor;
import java.util.Locale;

/**
 * Several checks to validate that the implementations of {@link Validator} and
 * {@link BeanDescriptor} throw the correct exceptions as per the spec.
 * 
 * @author Carlos Vara
 */
public class ExceptionsContractTest extends TestCase {

    static ValidatorFactory factory;

    static {
        factory = Validation.buildDefaultValidatorFactory();
        ((DefaultMessageInterpolator) factory.getMessageInterpolator()).setLocale(Locale.ENGLISH);
    }

    private Validator getValidator() {
        return factory.getValidator();
    }

    /**
     * Checks that the correct exception is thrown when validating a bean whose
     * getter throws an exception.
     */
    public void testExceptionThrowingBean() {
        Validator validator = getValidator();
        try {
            validator.validate(new ExceptionThrowingBean());
            Assert.fail("No exception thrown when validating a bean whose getter throws a RTE");
        } catch (ValidationException e) {
            // Correct
        }
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when passing
     * <code>null</code> as group array.
     */
    public void testValidateNullGroup() {
        Validator validator = getValidator();
        try {
            Class<?>[] groups = null;
            validator.validate(new String(), groups);
            Assert.fail("No exception thrown when passing null as group array");
        } catch (IllegalArgumentException e) {
            // Correct
        }
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when passing an
     * invalid property name.
     */
    public void testValidateInvalidPropertyName() {
        Validator validator = getValidator();

        // Null propertyName
        try {
            validator.validateProperty(new Person(), null);
        } catch (IllegalArgumentException e) {
            // Correct
        }

        // Empty propertyName
        try {
            validator.validateProperty(new Person(), "");
        } catch (IllegalArgumentException e) {
            // Correct
        }

        // Invalid propertyName
        try {
            validator.validateProperty(new Person(), "surname");
        } catch (IllegalArgumentException e) {
            // Correct
        }

    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when trying to
     * validate a property on a null object.
     */
    public void testValidatePropertyOnNullBean() {
        Validator validator = getValidator();
        try {
            validator.validateProperty(null, "class");
        } catch (IllegalArgumentException e) {
            // Correct
        }
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when passing
     * <code>null</code> as group array in a
     * {@link Validator#validateProperty(Object, String, Class...)} call.
     */
    public void testValidatePropertyNullGroup() {
        Validator validator = getValidator();
        try {
            Class<?>[] groups = null;
            validator.validateProperty(new Person(), "name", groups);
            Assert.fail("No exception thrown when passing null as group array");
        } catch (IllegalArgumentException e) {
            // Correct
        }
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when calling
     * {@link Validator#validateValue(Class, String, Object, Class...)} with a
     * <code>null</code> class.
     */
    public void testValidateValueOnNullClass() {
        Validator validator = getValidator();
        try {
            validator.validateValue(null, "class", Object.class);
            Assert.fail("No exception thrown when passing null as group array");
        } catch (IllegalArgumentException e) {
            // Correct
        }
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when passing an
     * invalid property name to
     * {@link Validator#validateValue(Class, String, Object, Class...)}.
     */
    public void testValidateValueInvalidPropertyName() {
        Validator validator = getValidator();

        // Null propertyName
        try {
            validator.validateValue(Person.class, null, "John");
        } catch (IllegalArgumentException e) {
            // Correct
        }

        // Empty propertyName
        try {
            validator.validateValue(Person.class, "", "John");
        } catch (IllegalArgumentException e) {
            // Correct
        }

        // Invalid propertyName
        try {
            validator.validateValue(Person.class, "unexistant", "John");
        } catch (IllegalArgumentException e) {
            // Correct
        }
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when calling
     * {@link Validator#validateValue(Class, String, Object, Class...)} with a
     * <code>null</code> group array.
     */
    public void testValidateValueNullGroup() {
        Validator validator = getValidator();
        try {
            Class<?>[] groups = null;
            validator.validateValue(Person.class, "name", "John", groups);
            Assert.fail("No exception thrown when passing null as group array");
        } catch (IllegalArgumentException e) {
            // Correct
        }
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when calling
     * {@link BeanDescriptor#getConstraintsForProperty(String)} with an invalid
     * property name.
     */
    public void testGetConstraintsForInvalidProperty() {
        Validator validator = getValidator();
        BeanDescriptor personDescriptor = validator.getConstraintsForClass(Person.class);
        
        try {
            personDescriptor.getConstraintsForProperty(null);
            fail("No exception thrown when calling getConstraintsForProperty with null property");
        } catch (IllegalArgumentException e) {
            // Correct
        }
        
        try {
            personDescriptor.getConstraintsForProperty("");
            fail("No exception thrown when calling getConstraintsForProperty with empty property");
        } catch (IllegalArgumentException e) {
            // Correct
        }
    }
    

    public static class ExceptionThrowingBean {

        @NotNull
        public String getValue() {
            throw new IllegalStateException();
        }

    }

    public static class Person {
        
        @NotNull
        public String name;
        
    }

}
