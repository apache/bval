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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Locale;
import java.util.Set;

import javax.validation.Constraint;
import javax.validation.ConstraintDefinitionException;
import javax.validation.Payload;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Min;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.bval.constraints.NotNullValidator;

/**
 * Checks the correct parsing of constraint definitions.
 * 
 * @author Carlos Vara
 */
public class ConstraintDefinitionsTest extends TestCase {
    static ValidatorFactory factory;

    static {
        factory = Validation.buildDefaultValidatorFactory();
        ((DefaultMessageInterpolator) factory.getMessageInterpolator()).setLocale(Locale.ENGLISH);
    }

    /**
     * Validator instance to test
     */
    protected Validator validator;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        validator = createValidator();
    }

    /**
     * Create the validator instance.
     * 
     * @return Validator
     */
    protected Validator createValidator() {
        return factory.getValidator();
    }

    /**
     * Checks the correct parsing of a constraint with an array of constraints
     * as attributes.
     */
    public void testCustomAttributes() {
        BeanDescriptor constraints = validator.getConstraintsForClass(Person.class);
        Set<ConstraintDescriptor<?>> ageConstraints =
            constraints.getConstraintsForProperty("age").getConstraintDescriptors();

        Assert.assertEquals("There should be 2 constraints in 'age'", ageConstraints.size(), 2);
        for (ConstraintDescriptor<?> cd : ageConstraints) {
            Assert.assertEquals("Annotation should be @Min", cd.getAnnotation().annotationType().getName(), Min.class
                .getName());
        }
    }

    /**
     * Checks that a {@link ConstraintDefinitionException} is thrown when
     * parsing a constraint definition with no <code>groups()</code> method.
     */
    public void testNoGroupsConstraint() {
        try {
            validator.validate(new NoGroups());
            fail("No exception thrown when parsing a constraint definition with no groups() method");
        } catch (ConstraintDefinitionException e) {
            // correct
        }
    }

    /**
     * Checks that a {@link ConstraintDefinitionException} is thrown when
     * parsing a constraint definition with an invalid <code>groups()</code>
     * method.
     */
    public void testInvalidDefaultGroupsConstraint() {
        try {
            validator.validate(new InvalidGroups());
            fail("No exception thrown when parsing a constraint definition with a groups() method does not return Class[]");
        } catch (ConstraintDefinitionException e) {
            // correct
        }
    }

    /**
     * Checks that a {@link ConstraintDefinitionException} is thrown when
     * parsing a constraint definition with no <code>payload()</code> method.
     */
    public void testNoPayloadConstraint() {
        try {
            validator.validate(new NoPayload());
            fail("No exception thrown when parsing a constraint definition with no payload() method");
        } catch (ConstraintDefinitionException e) {
            // correct
        }
    }

    /**
     * Checks that a {@link ConstraintDefinitionException} is thrown when
     * parsing a constraint definition with an invalid <code>payload()</code>
     * method.
     */
    public void testInvalidDefaultPayloadConstraint() {
        try {
            validator.validate(new InvalidPayload());
            fail("No exception thrown when parsing a constraint definition with a payload() method does not return an empty array");
        } catch (ConstraintDefinitionException e) {
            // correct
        }
    }

    /**
     * Checks that a {@link ConstraintDefinitionException} is thrown when
     * parsing a constraint definition with no <code>message()</code> method.
     */
    public void testNoMessageConstraint() {
        try {
            validator.validate(new NoMessage());
            fail("No exception thrown when parsing a constraint definition with no payload() method");
        } catch (ConstraintDefinitionException e) {
            // correct
        }
    }

    /**
     * Checks that a {@link ConstraintDefinitionException} is thrown when
     * parsing a constraint definition with an invalid <code>message()</code>
     * method.
     */
    public void testInvalidDefaultMessageConstraint() {
        try {
            validator.validate(new InvalidMessage());
            fail("No exception thrown when parsing a constraint definition with a message() method does not return a String");
        } catch (ConstraintDefinitionException e) {
            // correct
        }
    }

    /**
     * Checks that a {@link ConstraintDefinitionException} is thrown when
     * parsing a constraint definition with a method starting with 'valid'.
     */
    public void testInvalidAttributeConstraint() {
        try {
            validator.validate(new InvalidAttribute());
            fail("No exception thrown when parsing a constraint definition with a method starting with 'valid'");
        } catch (ConstraintDefinitionException e) {
            // correct
        }
    }

    public static class Person {
        @MinList( { @Min(value = 20), @Min(value = 30) })
        public Integer age;
    }

    @Target( { METHOD, FIELD, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Documented
    public static @interface MinList {
        Min[] value();
    }

    public static class NoGroups {
        @NoGroupsConstraint
        public String prop;
    }

    @Target( { METHOD, FIELD, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Documented
    @Constraint(validatedBy = { NotNullValidator.class })
    public static @interface NoGroupsConstraint {
        String message() default "def msg";

        Class<? extends Payload>[] payload() default {};
    }

    public static class InvalidGroups {
        @InvalidGroupsConstraint
        public String prop;
    }

    @Target( { METHOD, FIELD, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Documented
    @Constraint(validatedBy = { NotNullValidator.class })
    public static @interface InvalidGroupsConstraint {
        String message() default "def msg";

        String[] groups() default { "Group1" };

        Class<? extends Payload>[] payload() default {};
    }

    public static class NoPayload {
        @NoPayloadConstraint
        public String prop;
    }

    @Target( { METHOD, FIELD, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Documented
    @Constraint(validatedBy = { NotNullValidator.class })
    public static @interface NoPayloadConstraint {
        String message() default "def msg";

        String[] groups() default {};
    }

    public static class InvalidPayload {
        @InvalidPayloadConstraint
        public String prop;
    }

    @Target( { METHOD, FIELD, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Documented
    @Constraint(validatedBy = { NotNullValidator.class })
    public static @interface InvalidPayloadConstraint {
        String message() default "def msg";

        String[] groups() default {};

        Class<? extends Payload>[] payload() default { Payload1.class };

        public static class Payload1 implements Payload {
        }
    }

    public static class NoMessage {
        @NoMessageConstraint
        public String prop;
    }

    @Target( { METHOD, FIELD, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Documented
    @Constraint(validatedBy = { NotNullValidator.class })
    public static @interface NoMessageConstraint {
        String[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class InvalidMessage {
        @InvalidMessageConstraint(message = 2)
        public String prop;
    }

    @Target( { METHOD, FIELD, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Documented
    @Constraint(validatedBy = { NotNullValidator.class })
    public static @interface InvalidMessageConstraint {
        int message();

        String[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class InvalidAttribute {
        @InvalidAttributeConstraint
        public String prop;
    }

    @Target( { METHOD, FIELD, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Documented
    @Constraint(validatedBy = { NotNullValidator.class })
    public static @interface InvalidAttributeConstraint {
        String message() default "def msg";

        String[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        String validValue() default "1";
    }
}
