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
package org.apache.bval.jsr.groups;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.GroupDefinitionException;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotNull;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.ValidationTestBase;
import org.apache.bval.jsr.descriptor.BeanD;
import org.apache.bval.jsr.example.Author;
import org.apache.bval.jsr.example.Book;
import org.apache.bval.jsr.example.First;
import org.apache.bval.jsr.example.Last;
import org.apache.bval.jsr.example.Second;
import org.apache.bval.jsr.util.TestUtils;
import org.junit.Test;

/**
 * Description: test of group sequence behavior<br/>
 */
public class GroupSequenceTest extends ValidationTestBase {
    @Test(expected = GroupDefinitionException.class)
    public void testGroupSequence1() {
        ApacheValidatorFactory.getDefault().usingContext().getValidator().getConstraintsForClass(GInterface1.class);
    }

    @Test
    public void testGroupSequence2() {
        BeanD<?> bean = (BeanD<?>) ApacheValidatorFactory.getDefault().usingContext().getValidator()
            .getConstraintsForClass(GClass1.class);

        assertEquals(Group.of(GClass1.class), bean.getGroupStrategy());
    }

    @Test
    public void testGroupSequence3() {
        BeanD<?> bean = (BeanD<?>) ApacheValidatorFactory.getDefault().usingContext().getValidator()
            .getConstraintsForClass(GClass2.class);

        class TestPredicate implements Predicate<GroupStrategy> {

            final List<GroupStrategy> strategies = new ArrayList<>();

            @Override
            public boolean test(GroupStrategy t) {
                return strategies.add(t);
            }
        }

        final TestPredicate p = new TestPredicate();

        bean.getGroupStrategy().applyTo(p);

        Group g1 = Group.of(GClass1.class);
        Group g2 = Group.of(GClass2.class);

        assertEquals(Arrays.asList(g1, GroupStrategy.simple(g1, g2)), p.strategies);
    }

    @Test
    public void testGroupSequence4() {
        BeanD<?> bean = (BeanD<?>) ApacheValidatorFactory.getDefault().usingContext().getValidator()
            .getConstraintsForClass(GClass3.class);

        assertEquals(Group.sequence(Group.of(GClass3.class), Group.of(GClass1.class)), bean.getGroupStrategy());
    }

    @Test
    public void testGroups() {
        Author author = new Author();
        author.setLastName("");
        author.setFirstName("");
        Book book = new Book();
        book.setTitle("");
        book.setAuthor(author);

        Set<ConstraintViolation<Book>> constraintViolations =
            validator.validate(book, First.class, Second.class, Last.class);
        assertEquals("Wrong number of constraints", 3, constraintViolations.size());
        assertNotNull(TestUtils.getViolation(constraintViolations, "title"));
        assertNotNull(TestUtils.getViolation(constraintViolations, "author.firstName"));
        assertNotNull(TestUtils.getViolation(constraintViolations, "author.lastName"));

        author.setFirstName("Gavin");
        author.setLastName("King");

        constraintViolations = validator.validate(book, First.class, Second.class, Last.class);
        ConstraintViolation<?> constraintViolation = constraintViolations.iterator().next();
        assertEquals(1, constraintViolations.size());
        assertEquals("must not be empty", constraintViolation.getMessage());
        assertEquals(book, constraintViolation.getRootBean());
        assertEquals(book.getTitle(), constraintViolation.getInvalidValue());
        assertEquals("title", constraintViolation.getPropertyPath().toString());

        book.setTitle("My fault");
        book.setSubtitle("confessions of a president - a book for a nice price");

        constraintViolations = validator.validate(book, First.class, Second.class, Last.class);
        assertEquals(1, constraintViolations.size());
        constraintViolation = constraintViolations.iterator().next();
        assertEquals("size must be between 0 and 30", constraintViolation.getMessage());
        assertEquals(book, constraintViolation.getRootBean());
        assertEquals(book.getSubtitle(), constraintViolation.getInvalidValue());
        assertEquals("subtitle", constraintViolation.getPropertyPath().toString());

        book.setSubtitle("Capitalism in crisis");
        author.setCompany("1234567890ß9876543212578909876542245678987432");

        constraintViolations = validator.validate(book);
        constraintViolation = constraintViolations.iterator().next();
        assertEquals(1, constraintViolations.size());
        assertEquals("size must be between 0 and 40", constraintViolation.getMessage());
        assertEquals(book, constraintViolation.getRootBean());
        assertEquals(author.getCompany(), constraintViolation.getInvalidValue());
        assertEquals("author.company", constraintViolation.getPropertyPath().toString());

        author.setCompany("apache");

        constraintViolations = validator.validate(book, First.class, Second.class, Last.class);
        assertEquals(0, constraintViolations.size());
    }

    @Test
    public void testGroupSequence() {
        Author author = new Author();
        author.setLastName("");
        author.setFirstName("");
        Book book = new Book();
        book.setAuthor(author);

        Set<ConstraintViolation<Book>> constraintViolations = validator.validate(book, Book.All.class);
        assertEquals(2, constraintViolations.size());

        author.setFirstName("Kelvin");
        author.setLastName("Cline");

        constraintViolations = validator.validate(book, Book.All.class);
        ConstraintViolation<?> constraintViolation = constraintViolations.iterator().next();
        assertEquals(1, constraintViolations.size());
        assertEquals("must not be empty", constraintViolation.getMessage());
        assertEquals(book, constraintViolation.getRootBean());
        assertEquals(book.getTitle(), constraintViolation.getInvalidValue());
        assertEquals("title", constraintViolation.getPropertyPath().toString());

        book.setTitle("247307892430798789024389798789");
        book.setSubtitle("f43u rlök fjöq3liu opiur ölw3kj rölkj d");

        constraintViolations = validator.validate(book, Book.All.class);
        assertEquals(1, constraintViolations.size());
    }

    /**
     * Check that when there is one constraint failure in one of the groups in
     * a sequence, validation stops.
     * JSR-303: 3.4.3
     */
    @Test
    public void testValidationStopsWhenFailuresOnGroup() {
        // Validate Dummy with its redefined Default group
        Set<ConstraintViolation<Dummy>> violations = validator.validate(new Dummy());
        assertEquals("Only 1 violation expected", 1, violations.size());
        ConstraintViolation<Dummy> violation = violations.iterator().next();
        assertEquals("Group1 should be evaluated first", "field1", violation.getPropertyPath().toString());
    }

    @GroupSequence({ Dummy.Group1.class, Dummy.class })
    public static class Dummy {

        @NotNull(groups = Group1.class)
        public String field1;

        @NotNull
        public String field2;

        interface Group1 {
        }
    }
}
