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

import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.junit.Test;

/**
 * Checks that circular references in the bean graph are correctly detected when
 * validating.
 * 
 * @author Carlos Vara
 */
public class CircularReferencesTest extends ValidationTestBase {

    /**
     * Checks that validation correctly stops when finding a circular
     * dependency.
     */
    @Test
    public void testAutoreferringBean() {
        Person p1 = new Person();
        p1.name = "too-long-name";
        p1.sibling = p1;

        Set<ConstraintViolation<Person>> violations = validator.validate(p1);

        assertEquals("Only 1 violation should be reported", 1, violations.size());
        ConstraintViolation<Person> violation = violations.iterator().next();
        assertEquals("Incorrect violation path", "name", violation.getPropertyPath().toString());
    }

    /**
     * Checks that a bean is always validated when appearing in non-circular
     * paths inside the bean graph.
     */
    @Test
    public void testNonCircularArrayOfSameBean() {
        Boss boss = new Boss();
        Person p1 = new Person();
        p1.name = "too-long-name";

        boss.employees = new Person[] { p1, p1, p1, p1 };

        Set<ConstraintViolation<Boss>> violations = validator.validate(boss);

        assertEquals("A total of 4 violations should be reported", 4, violations.size());
    }

    public static class Person {

        @Valid
        public Person sibling;

        @Size(max = 10)
        public String name;

    }

    public static class Boss {

        @Valid
        public Person[] employees;

    }

}
