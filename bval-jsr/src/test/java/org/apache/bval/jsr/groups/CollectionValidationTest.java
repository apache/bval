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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.apache.bval.jsr.DefaultMessageInterpolator;
import org.apache.bval.jsr.example.Address;
import org.apache.bval.jsr.example.Author;
import org.apache.bval.jsr.example.Book;
import org.apache.bval.jsr.example.Country;
import org.apache.bval.jsr.example.Customer;
import org.apache.bval.jsr.example.Employee;
import org.apache.bval.jsr.example.Library;
import org.apache.bval.jsr.example.Person;
import org.apache.bval.jsr.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Description: <br/>
 */
public class CollectionValidationTest {
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
    @Before
    public void setUp() throws Exception {
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

    @Test
    public void testValidateList() {
        Author author = new Author();
        author.setFirstName("Peter");
        author.setLastName("Ford");
        author.setCompany("IBM");
        author.setAddresses(new ArrayList<Address>());

        Address adr1, adr2, adr3;
        adr1 = new Address();
        adr1.setCountry(new Country());
        adr1.getCountry().setName("Germany");
        adr1.setCity("Bonn");
        adr1.setAddressline1("Strasse 1");

        adr2 = new Address();
        adr2.setCountry(new Country());
        adr2.getCountry().setName("Cuba");
        adr2.setCity("Habana");
        adr2.setAddressline1("Calle 2");

        adr3 = new Address();
        adr3.setCountry(new Country());
        adr3.getCountry().setName("USA");
        adr3.setCity("San Francisco");
        adr3.setAddressline1("Street 3");

        author.getAddresses().add(adr1);
        author.getAddresses().add(adr2);
        author.getAddresses().add(adr3);

        Set<ConstraintViolation<Author>> violations;

        violations = validator.validate(author);
        assertEquals(0, violations.size());

        adr2.setCity(null); // violate not null
        adr3.setAddressline1(null); // violate not null

        violations = validator.validate(author);
        assertEquals(2, violations.size());
        assertNotNull(TestUtils.getViolation(violations, "addresses[1].city"));
        assertNotNull(TestUtils.getViolation(violations, "addresses[2].addressline1"));
    }

    @Test
    public void testValidateMapAndRedefinedDefaultGroupOnNonRootBean() {
        Library lib = new Library();
        lib.setLibraryName("Leibnitz Bibliothek");

        Book book1, book2, book3;

        book1 = new Book();
        book1.setTitle("History of time");
        book1.setSubtitle("How it really works");
        Author hawking = new Author();
        hawking.setFirstName("Stephen");
        hawking.setLastName("Hawking");
        hawking.setAddresses(new ArrayList<Address>(1));
        Address adr = new Address();
        adr.setAddressline1("Street 1");
        adr.setCity("London");
        adr.setCountry(new Country());
        adr.getCountry().setName("England");
        hawking.getAddresses().add(adr);
        book1.setAuthor(hawking);

        book2 = new Book();
        Author castro = new Author();
        castro.setFirstName("Fidel");
        castro.setLastName("Castro Ruz");
        book2.setAuthor(castro);
        book2.setTitle("My life");

        book3 = new Book();
        book3.setTitle("World best jokes");
        Author someone = new Author();
        someone.setFirstName("John");
        someone.setLastName("Do");
        book3.setAuthor(someone);

        lib.getTaggedBooks().put("science", book1);
        lib.getTaggedBooks().put("politics", book2);
        lib.getTaggedBooks().put("humor", book3);

        Set<ConstraintViolation<Library>> violations;

        violations = validator.validate(lib);
        assertTrue(violations.isEmpty());

        book2.setTitle(null);
        book3.getAuthor().setFirstName(""); // violate NotEmpty validation
        book1.getAuthor().getAddresses().get(0).setCity(null);
        /*
         * This, by the way, tests redefined default group sequence behavior on
         * non-root-beans (Library.Book)!!
         */
        violations = validator.validate(lib);
        assertEquals("redefined default group of Book not correctly validated from Library", 3, violations.size());
        assertNotNull(TestUtils.getViolation(violations, "taggedBooks[politics].title"));
        assertNotNull(TestUtils.getViolation(violations, "taggedBooks[humor].author.firstName"));
        assertNotNull(TestUtils.getViolation(violations, "taggedBooks[science].author.addresses[0].city"));
    }

    @Test
    public void testValidateArray() {
        Library lib = new Library();
        lib.setLibraryName("Unibibliothek");
        lib.setPersons(new Person[3]);
        lib.getPersons()[0] = new Employee("Marcel", "Reich-Ranicki");
        lib.getPersons()[1] = new Employee("Elke", "Heidenreich");
        lib.getPersons()[2] = new Customer(); // not validated, because only
        // getEmployees() is @Valid

        Set<ConstraintViolation<Library>> violations;
        violations = validator.validate(lib);
        assertTrue(violations.isEmpty());

        ((Employee) lib.getPersons()[1]).setFirstName(""); // violate NotEmpty
        // constraint
        violations = validator.validate(lib);
        assertEquals(1, violations.size());
        assertNotNull(TestUtils.getViolation(violations, "employees[1].firstName"));
    }
}
