/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.bval.jsr;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import javax.validation.Constraint;
import javax.validation.ConstraintViolation;
import javax.validation.OverridesAttribute;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.validation.metadata.ConstraintDescriptor;

import org.junit.Test;

/**
 * Checks that groups are correctly inherited from the root constraint to its
 * compositing constraints.
 * 
 * @author Carlos Vara
 */
public class ConstraintCompositionTest extends ValidationTestBase {

    /**
     * Check correct group inheritance on constraint composition on a 1 level
     * hierarchy.
     */
    @Test
    public void test1LevelInheritance() {
        Set<ConstraintViolation<Person>> violations = validator.validate(new Person());

        assertEquals("Wrong number of violations detected", 1, violations.size());
        String msg = violations.iterator().next().getMessage();
        assertEquals("Incorrect violation message", "A person needs a non null name", msg);

        violations = validator.validate(new Person(), Group1.class);
        assertEquals("Wrong number of violations detected", 0, violations.size());
    }

    /**
     * Check correct group inheritance on constraint composition on a 2 level
     * hierarchy.
     */
    @Test
    public void test2LevelInheritance() {
        Set<ConstraintViolation<Man>> violations = validator.validate(new Man());

        assertEquals("Wrong number of violations detected", 0, violations.size());

        violations = validator.validate(new Man(), Group1.class);
        assertEquals("Wrong number of violations detected", 1, violations.size());
        String msg = violations.iterator().next().getMessage();
        assertEquals("Incorrect violation message", "A person needs a non null name", msg);
    }

    /**
     * Checks that the groups() value of the constraint annotations are
     * correctly set to the inherited ones.
     */
    @Test
    public void testAnnotationGroupsAreInherited() {
        // Check that the groups() value is right when querying the metadata
        ConstraintDescriptor<?> manNameDesc =
            validator.getConstraintsForClass(Man.class).getConstraintsForProperty("name").getConstraintDescriptors()
                .iterator().next();
        ConstraintDescriptor<?> personNameDesc = manNameDesc.getComposingConstraints().iterator().next();
        ConstraintDescriptor<?> notNullDesc = personNameDesc.getComposingConstraints().iterator().next();
        assertEquals("There should only be 1 group", 1, manNameDesc.getGroups().size());
        assertTrue("Group1 should be present", manNameDesc.getGroups().contains(Group1.class));
        assertEquals("There should only be 1 group", 1, personNameDesc.getGroups().size());
        assertTrue("Group1 should be present", personNameDesc.getGroups().contains(Group1.class));
        assertEquals("There should only be 1 group", 1, personNameDesc.getGroups().size());
        assertTrue("Group1 should be present", notNullDesc.getGroups().contains(Group1.class));

        // Check that the groups() value is right when accessing it from an
        // error
        Set<ConstraintViolation<Man>> violations = validator.validate(new Man(), Group1.class);
        Set<Class<?>> notNullGroups = violations.iterator().next().getConstraintDescriptor().getGroups();
        assertEquals("There should only be 1 group", 1, notNullGroups.size());
        assertTrue("Group1 should be the only group", notNullGroups.contains(Group1.class));
    }

    /**
     * Checks that the payload() value of the constraint annotations are
     * correctly set to the inherited ones.
     */
    @Test
    public void testAnnotationPayloadsAreInherited() {
        // Check that the payload() value is right when querying the metadata
        ConstraintDescriptor<?> manNameDesc =
            validator.getConstraintsForClass(Man.class).getConstraintsForProperty("name").getConstraintDescriptors()
                .iterator().next();
        ConstraintDescriptor<?> personNameDesc = manNameDesc.getComposingConstraints().iterator().next();
        ConstraintDescriptor<?> notNullDesc = personNameDesc.getComposingConstraints().iterator().next();
        assertEquals("There should only be 1 payload class", 1, manNameDesc.getPayload().size());
        assertTrue("Payload1 should be present", manNameDesc.getPayload().contains(Payload1.class));
        assertEquals("There should only be 1 payload class", 1, personNameDesc.getPayload().size());
        assertTrue("Payload1 should be present", personNameDesc.getPayload().contains(Payload1.class));
        assertEquals("There should only be 1 payload class", 1, personNameDesc.getPayload().size());
        assertTrue("Payload1 should be present", notNullDesc.getPayload().contains(Payload1.class));

        // Check that the payload() value is right when accessing it from an
        // error
        Set<ConstraintViolation<Man>> violations = validator.validate(new Man(), Group1.class);
        Set<Class<? extends Payload>> notNullPayload =
            violations.iterator().next().getConstraintDescriptor().getPayload();
        assertEquals("There should only be 1 payload class", 1, notNullPayload.size());
        assertTrue("Payload1 should be the only payload", notNullPayload.contains(Payload1.class));
    }

    /**
     * Checks that {@link OverridesAttribute#constraintIndex()} parsing and
     * applying works.
     */
    @Test
    public void testIndexedOverridesAttributes() {
        Person p = new Person();
        p.name = "valid";

        // With a valid id, no errors expected
        p.id = "1234";
        Set<ConstraintViolation<Person>> constraintViolations = validator.validate(p);
        assertTrue("No violations should be reported on valid id", constraintViolations.isEmpty());

        // With a short id, only 1 error expected
        p.id = "1";
        constraintViolations = validator.validate(p);
        assertEquals("Only 1 violation expected", 1, constraintViolations.size());
        ConstraintViolation<Person> violation = constraintViolations.iterator().next();
        assertEquals("Wrong violation", "Id is too short", violation.getMessage());

        // With a long id, only 1 error expected
        p.id = "loooooong id";
        constraintViolations = validator.validate(p);
        assertEquals("Only 1 violation expected", 1, constraintViolations.size());
        violation = constraintViolations.iterator().next();
        assertEquals("Wrong violation", "Id is too long", violation.getMessage());
    }

    /**
     * Checks that errors are reported correctly when using
     * {@link ReportAsSingleViolation}.
     */
    @Test
    public void testReportAsAsingleViolation() {
        Code c = new Code();
        c.code = "very invalid code";
        Set<ConstraintViolation<Code>> constraintViolations = validator.validate(c);

        // Only 1 error expected
        assertEquals("Only 1 violation expected", 1, constraintViolations.size());
        ConstraintViolation<Code> violation = constraintViolations.iterator().next();
        assertEquals("Wrong violation message", "Invalid code", violation.getMessage());
        assertEquals("Wrong violation type", ElevenDigitsCode.class,
            ((Annotation) violation.getConstraintDescriptor().getAnnotation()).annotationType());
    }

    public static class Person {
        @PersonName
        String name;

        @PersonId
        String id;
    }

    public static class Man {
        @ManName(groups = { Group1.class }, payload = { Payload1.class })
        String name;
    }

    public static class Code {
        @ElevenDigitsCode
        String code;
    }

    @NotNull(message = "A person needs a non null name", groups = { Group1.class }, payload = {})
    @Constraint(validatedBy = {})
    @Documented
    @Target( { METHOD, FIELD, TYPE })
    @Retention(RUNTIME)
    public static @interface PersonName {
        String message() default "Wrong person name";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    @PersonName(groups = { Group2.class }, payload = { Payload1.class, Payload2.class })
    @Constraint(validatedBy = {})
    @Documented
    @Target( { METHOD, FIELD, TYPE })
    @Retention(RUNTIME)
    public static @interface ManName {
        String message() default "Wrong man name";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    @Size.List( { @Size(min = 3, max = 3, message = "Id is too short"),
        @Size(min = 5, max = 5, message = "Id is too long") })
    @Constraint(validatedBy = {})
    @Documented
    @Target( { METHOD, FIELD, TYPE })
    @Retention(RUNTIME)
    public static @interface PersonId {
        String message() default "Wrong person id";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        @OverridesAttribute(constraint = Size.class, constraintIndex = 0, name = "max")
        int maxSize() default 1000;

        @OverridesAttribute(constraint = Size.class, constraintIndex = 1, name = "min")
        int minSize() default 0;
    }

    @Size(min = 11, max = 11)
    @Pattern(regexp = "\\d*")
    @Constraint(validatedBy = {})
    @ReportAsSingleViolation
    @Documented
    @Target( { METHOD, FIELD, TYPE })
    @Retention(RUNTIME)
    public static @interface ElevenDigitsCode {
        String message() default "Invalid code";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static interface Group1 {
    }

    public static interface Group2 {
    }

    public static class Payload1 implements Payload {
    }

    public static class Payload2 implements Payload {
    }

}
