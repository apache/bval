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

import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.metadata.BeanDescriptor;

import org.junit.Test;

/**
 * Several checks to validate that the implementations of {@link Validator} and
 * {@link BeanDescriptor} throw the correct exceptions as per the spec.
 * 
 * @author Carlos Vara
 */
public class ExceptionsContractTest extends ValidationTestBase {

    /**
     * Checks that the correct exception is thrown when validating a bean whose
     * getter throws an exception.
     */
    @Test(expected = ValidationException.class)
    public void testExceptionThrowingBean() {
        validator.validate(new ExceptionThrowingBean());
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when passing
     * <code>null</code> as group array.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateNullGroup() {
        validator.validate(new String(), (Class<?>[]) null);
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when passing a
     * {@code null} property name.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateNullPropertyName() {
        validator.validateProperty(new Person(), null);
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when passing an
     * empty property name.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateEmptyPropertyName() {
        validator.validateProperty(new Person(), "");
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when passing an
     * invalid property name.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateInvalidPropertyName() {
        validator.validateProperty(new Person(), "surname");
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when trying to
     * validate a property on a null object.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidatePropertyOnNullBean() {
        validator.validateProperty(null, "class");
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when passing
     * <code>null</code> as group array in a
     * {@link Validator#validateProperty(Object, String, Class...)} call.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidatePropertyNullGroup() {
        validator.validateProperty(new Person(), "name", (Class<?>[]) null);
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when calling
     * {@link Validator#validateValue(Class, String, Object, Class...)} with a
     * <code>null</code> class.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateValueOnNullClass() {
        validator.validateValue(null, "class", Object.class);
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when passing a
     * {@code null} property name to
     * {@link Validator#validateValue(Class, String, Object, Class...)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateValueNullPropertyName() {
        validator.validateValue(Person.class, null, "John");
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when passing an
     * empty property name to
     * {@link Validator#validateValue(Class, String, Object, Class...)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateValueEmptyPropertyName() {
        validator.validateValue(Person.class, "", "John");
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when passing an
     * invalid property name to
     * {@link Validator#validateValue(Class, String, Object, Class...)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateValueInvalidPropertyName() {
        validator.validateValue(Person.class, "unexistant", "John");
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when calling
     * {@link Validator#validateValue(Class, String, Object, Class...)} with a
     * <code>null</code> group array.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateValueNullGroup() {
        validator.validateValue(Person.class, "name", "John", (Class<?>[]) null);
    }

    /**
     * Enforces the "not a valid object property" part of the {@link IllegalArgumentException}
     * declaration on {@link Validator#validateValue(Class, String, Object, Class...)}
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateIncompatibleValue() {
        validator.validateValue(Person.class, "name", 666);
    }

    /**
     * Enforces the "not a valid object property" part of the {@link IllegalArgumentException}
     * declaration on {@link Validator#validateValue(Class, String, Object, Class...)}
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateIncompatiblePrimitiveValue() {
        validator.validateValue(Person.class, "age", null);
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when calling
     * {@link BeanDescriptor#getConstraintsForProperty(String)} with an invalid
     * property name.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetConstraintsForNullProperty() {
        BeanDescriptor personDescriptor = validator.getConstraintsForClass(Person.class);
        personDescriptor.getConstraintsForProperty(null);
    }

    /**
     * Checks that an {@link IllegalArgumentException} is thrown when calling
     * {@link BeanDescriptor#getConstraintsForProperty(String)} with an invalid
     * property name.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetConstraintsForEmptyProperty() {
        BeanDescriptor personDescriptor = validator.getConstraintsForClass(Person.class);
        personDescriptor.getConstraintsForProperty("");
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

        @Min(0)
        public int age;
    }

}
