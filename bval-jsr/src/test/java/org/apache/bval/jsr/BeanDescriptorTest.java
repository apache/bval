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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.ElementDescriptor.ConstraintFinder;
import javax.validation.metadata.PropertyDescriptor;
import javax.validation.metadata.Scope;

import org.apache.bval.jsr.util.TestUtils;
import org.junit.Test;

/**
 * Tests the implementation of {@link BeanDescriptor} and its dependent
 * interfaces.
 * 
 * @author Carlos Vara
 */
public class BeanDescriptorTest extends ValidationTestBase {

    /**
     * Check that groups(), message() and payload() are always in the
     * attributes.
     */
    @Test
    public void testMandatoryAttributesPresentInConstraintDescriptor() {
        Set<ConstraintDescriptor<?>> nameDescriptors =
            validator.getConstraintsForClass(Form.class).getConstraintsForProperty("name").getConstraintDescriptors();
        assertEquals("Incorrect number of descriptors", 1, nameDescriptors.size());
        ConstraintDescriptor<?> nameDescriptor = nameDescriptors.iterator().next();
        assertTrue("groups attribute not present", nameDescriptor.getAttributes().containsKey("groups"));
        assertTrue("payload attribute not present", nameDescriptor.getAttributes().containsKey("payload"));
        assertTrue("message attribute not present", nameDescriptor.getAttributes().containsKey("message"));
    }

    /**
     * Check that the groups() attribute value has the correct value when
     * inheriting groups.
     */
    @Test
    public void testCorrectValueForInheritedGroupsAttribute() {
        Set<ConstraintDescriptor<?>> passwordDescriptors = validator.getConstraintsForClass(Account.class)
            .getConstraintsForProperty("password").getConstraintDescriptors();
        assertEquals("Incorrect number of descriptors", 1, passwordDescriptors.size());
        ConstraintDescriptor<?> passwordDescriptor = passwordDescriptors.iterator().next();
        assertEquals("Incorrect number of composing constraints", 1,
            passwordDescriptor.getComposingConstraints().size());
        ConstraintDescriptor<?> notNullDescriptor = passwordDescriptor.getComposingConstraints().iterator().next();

        // Check that the groups value containts Group1.class
        Class<?>[] notNullGroups = (Class<?>[]) notNullDescriptor.getAttributes().get("groups");
        boolean found = false;
        for (Class<?> group : notNullGroups) {
            if (group == Group1.class) {
                found = true;
                break;
            }
        }
        assertTrue("Group1 not present in groups attribute", found);
    }

    /**
     * Check that the groups() attribute value contains the correct interface as
     * implicit group when the constraint is defined in that interface instead
     * of the queried class.
     */
    @Test
    public void testImplicitGroupIsPresent() {
        Set<ConstraintDescriptor<?>> nameDescriptors =
            validator.getConstraintsForClass(Woman.class).getConstraintsForProperty("name").getConstraintDescriptors();
        assertEquals("Incorrect number of descriptors", 1, nameDescriptors.size());
        ConstraintDescriptor<?> notNullDescriptor = nameDescriptors.iterator().next();

        // Check that the groups attribute value contains the implicit group
        // Person and the Default group
        Class<?>[] notNullGroups = (Class<?>[]) notNullDescriptor.getAttributes().get("groups");
        assertEquals("Incorrect number of groups", 2, notNullGroups.length);
        assertTrue("Default group not present",
            notNullGroups[0].equals(Default.class) || notNullGroups[1].equals(Default.class));
        assertTrue("Implicit group not present",
            notNullGroups[0].equals(Person.class) || notNullGroups[1].equals(Person.class));
    }

    /**
     * Check that the groups() attribute value does not contain the implicit
     * interface group when querying the interface directly.
     */
    @Test
    public void testNoImplicitGroupWhenQueryingInterfaceDirectly() {
        Set<ConstraintDescriptor<?>> nameDescriptors =
            validator.getConstraintsForClass(Person.class).getConstraintsForProperty("name").getConstraintDescriptors();
        assertEquals("Incorrect number of descriptors", 1, nameDescriptors.size());
        ConstraintDescriptor<?> notNullDescriptor = nameDescriptors.iterator().next();

        // Check that only the default group is present
        Class<?>[] notNullGroups = (Class<?>[]) notNullDescriptor.getAttributes().get("groups");
        assertEquals("Incorrect number of groups", 1, notNullGroups.length);
        assertTrue("Default group not present", notNullGroups[0].equals(Default.class));
    }

    /**
     * Check that the implementations of
     * {@link ElementDescriptor#getElementClass()} work as defined in the spec.
     */
    @Test
    public void testElementDescriptorGetElementClass() {
        BeanDescriptor beanDescriptor = validator.getConstraintsForClass(Person.class);
        assertEquals("Incorrect class returned", Person.class, beanDescriptor.getElementClass());

        PropertyDescriptor nameDescriptor = beanDescriptor.getConstraintsForProperty("name");
        assertEquals("Incorrect class returned", String.class, nameDescriptor.getElementClass());
    }

    /**
     * Check the correct behavior of
     * {@link ConstraintFinder#lookingAt(javax.validation.metadata.Scope)}.
     */
    @Test
    public void testConstraintFinderLookingAt() {
        PropertyDescriptor nameDescriptor =
            validator.getConstraintsForClass(Woman.class).getConstraintsForProperty("name");
        Set<ConstraintDescriptor<?>> constraints =
            nameDescriptor.findConstraints().lookingAt(Scope.HIERARCHY).getConstraintDescriptors();
        assertEquals("Incorrect number of descriptors", 1, constraints.size());

        constraints = nameDescriptor.findConstraints().lookingAt(Scope.LOCAL_ELEMENT).getConstraintDescriptors();
        assertEquals("Incorrect number of descriptors", 0, constraints.size());
        TestUtils.failOnModifiable(constraints, "constraintFinder constraintDescriptors");
        //verify that changes to one ConstraintFinder don't affect the base:
        constraints = nameDescriptor.getConstraintDescriptors();
        assertEquals("Incorrect number of descriptors", 1, constraints.size());
    }

    public static class Form {
        @NotNull
        public String name;
    }

    public static class Account {
        @Password(groups = { Group1.class })
        public String password;
    }

    @NotNull(groups = {})
    @Constraint(validatedBy = {})
    @Documented
    @Target({ METHOD, FIELD, TYPE })
    @Retention(RUNTIME)
    public static @interface Password {
        String message() default "Invalid password";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static interface Group1 {
    }

    public static class Woman implements Person {

        private String name;

        @Override
        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static interface Person {
        @NotNull
        String getName();
    }

}
