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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.bval.constraints.NotNullValidator;
import org.apache.bval.jsr303.example.AccessTestBusinessObject;
import org.apache.bval.jsr303.example.AccessTestBusinessObjectSub;
import org.apache.bval.jsr303.example.Address;
import org.apache.bval.jsr303.example.Author;
import org.apache.bval.jsr303.example.Book;
import org.apache.bval.jsr303.example.BusinessAddress;
import org.apache.bval.jsr303.example.Continent;
import org.apache.bval.jsr303.example.Country;
import org.apache.bval.jsr303.example.First;
import org.apache.bval.jsr303.example.Last;
import org.apache.bval.jsr303.example.RecursiveFoo;
import org.apache.bval.jsr303.util.TestUtils;

/**
 * Description: <br/>
 */
public class ValidationTest extends TestCase {
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

    public void testAccessStrategies_field_method() {
        AccessTestBusinessObject o1 = new AccessTestBusinessObject("1");
        AccessTestBusinessObjectSub o2 = new AccessTestBusinessObjectSub("3");
        Set<ConstraintViolation<AccessTestBusinessObject>> errors = validator.validate(o1);
        assertTrue(errors.isEmpty());
        Set<ConstraintViolation<AccessTestBusinessObjectSub>> errors2 = validator.validate(o2);
        assertTrue(errors2.isEmpty());

        o2 = new AccessTestBusinessObjectSub("1");
        errors2 = validator.validate(o2);
        assertEquals(1, errors2.size());

        // assert, that getvar2() and getVar2() are both validated with their
        // getter method
        o2 = new AccessTestBusinessObjectSub("3");
        o2.setVar2("1");
        o2.setvar2("2");
        errors2 = validator.validate(o2);
        assertEquals(2, errors2.size());

        o2.setvar2("5");
        o2.setVar2("6");
        errors2 = validator.validate(o2);
        assertEquals(0, errors2.size());

        o2.setvar2("5");
        o2.setVar2("-1");
        errors2 = validator.validate(o2);
        assertEquals(1, errors2.size());
    }

    public void testAccessStrategies_on_children() {
        AccessTestBusinessObject o1 = new AccessTestBusinessObject("1");
        AccessTestBusinessObject o2 = new AccessTestBusinessObject("2");
        o1.next(o2);
        Set<ConstraintViolation<AccessTestBusinessObject>> errors = validator.validate(o1);
        // assert, that field access 'next' is used and not getNext() is
        // called!!!
        assertEquals(1, errors.size());
        o2 = new AccessTestBusinessObject("1");
        o1.next(o2);
        errors = validator.validate(o1);
        assertEquals(0, errors.size());

        // assert that toBeIgnored not validated, because not annotated with
        // @Valid
        o1.setToBeIgnored(new AccessTestBusinessObject("99"));
        errors = validator.validate(o1);
        assertEquals(0, errors.size());

        o1.setNext(new AccessTestBusinessObject("99"));
        errors = validator.validate(o1);
        assertEquals(1, errors.size());
    }

    public void testBook() {
        Author author = new Author();
        author.setLastName("Baudelaire");
        author.setFirstName("");
        Book book = new Book();
        book.setAuthor(author);
        book.setSubtitle("12345678900125678901234578901234567890");

        // NotEmpty failure on the title field
        Set<ConstraintViolation<Book>> errors = validator.validate(book, Book.All.class);
        Assert.assertTrue(!errors.isEmpty());

        book.setTitle("Les fleurs du mal");
        author.setCompany("Some random publisher with a very very very long name");

        // author.firstName fails to pass the NotEmpty constraint
        // author.company fails to pass the Size constraint
    }

    /**
     * test: - dynamic resolution of associated object types. - inheritance of validation constraints - complex
     * valiation, different groups, nested object net
     */
    public void testValidAnnotation() {
        Author a = new Author();
        a.setAddresses(new ArrayList<Address>());
        BusinessAddress adr = new BusinessAddress();
        adr.setCountry(new Country());
        adr.setAddressline1("line1");
        adr.setAddressline2("line2");

        adr.setZipCode("1234567890123456789");
        a.getAddresses().add(adr);

        a.setFirstName("Karl");
        a.setLastName("May");

        Set<ConstraintViolation<Author>> found = validator.validate(a, Default.class, First.class, Last.class);
        Assert.assertTrue(!found.isEmpty());
        Assert.assertEquals(4, found.size());

        adr.setCity("Berlin");
        adr.setZipCode("12345");
        adr.setCompany("apache");
        found = validator.validate(a, Default.class, First.class, Last.class);
        Assert.assertEquals(1, found.size());
        ConstraintViolation<Author> ic = found.iterator().next();
        Assert.assertEquals("addresses[0].country.name", ic.getPropertyPath().toString());
    }

    public void testPropertyPathWithIndex() {
        Author a = new Author();
        a.setAddresses(new ArrayList<Address>());
        Address adr = new Address();
        adr.setAddressline1("adr1");
        adr.setCity("Santiago");
        a.getAddresses().add(adr);
        adr = new Address();
        adr.setAddressline1("adr2");
        adr.setCity("Havanna");
        a.getAddresses().add(adr);
        adr = new Address();
        adr.setAddressline1("adr3");
        adr.setCity("Trinidad");
        a.getAddresses().add(adr);

        Set<ConstraintViolation<Author>> constraints = validator.validate(a);
        Assert.assertTrue(!constraints.isEmpty());

        assertPropertyPath("addresses[0].country", constraints);
        assertPropertyPath("addresses[1].country", constraints);
        assertPropertyPath("addresses[2].country", constraints);
    }

    /**
     * Check correct path reporting when validating a set of beans.
     */
    public void testPropertyPathOnSet() {
        Continent c = new Continent();
        c.name = "c1";
        Country country = new Country();
        country.setISO2Code("xx");
        country.setISO3Code("xxx");
        country.setName(null);
        c.countries.add(country);

        Set<ConstraintViolation<Continent>> constraints = validator.validate(c);
        Assert.assertEquals("Incorrect number of violations detected", 1, constraints.size());
        assertPropertyPath("countries[].name", constraints);

    }

    private <T> void assertPropertyPath(String propertyPath, Set<ConstraintViolation<T>> constraints) {
        for (ConstraintViolation<T> each : constraints) {
            if (each.getPropertyPath().toString().equals(propertyPath))
                return;
        }
        Assert.fail(propertyPath + " not found in " + constraints);
    }

    public void testPropertyPathRecursive() {
        RecursiveFoo foo1 = new RecursiveFoo(); // root
        RecursiveFoo foo11 = new RecursiveFoo();
        foo1.getFoos().add(foo11); // foos[0]
        RecursiveFoo foo12 = new RecursiveFoo();
        foo1.getFoos().add(foo12); // foos[1]
        RecursiveFoo foo2 = new RecursiveFoo();
        foo11.getFoos().add(foo2); // foos[0].foos[0]

        Set<ConstraintViolation<RecursiveFoo>> constraints = validator.validate(foo1);
        assertPropertyPath("foos[0].foos[0].foos", constraints);
        assertPropertyPath("foos[1].foos", constraints);
    }

    public void testNullElementInCollection() {
        try {
            validator.validate(null);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
        }
        RecursiveFoo foo = new RecursiveFoo();
        foo.getFoos().add(new RecursiveFoo());
        foo.getFoos().add(null);
        Assert.assertTrue(!validator.validate(foo).isEmpty());
        // check that no nullpointer exception gets thrown
    }

    public void testGroups() {
        Author author = new Author();
        author.setCompany("ACME");
        Book book = new Book();
        book.setTitle("");
        book.setAuthor(author);
        boolean foundTitleConstraint = false;
        Set<ConstraintViolation<Book>> constraintViolations = validator.validate(book, Book.All.class);
        assertEquals(1, constraintViolations.size());
        // assuming an english locale, the interpolated message is returned
        for (ConstraintViolation<Book> constraintViolation : constraintViolations) {
            if (constraintViolation.getRootBean().getClass() == Book.class) {
                Assert.assertEquals("may not be empty", constraintViolation.getMessage());
                Assert.assertTrue(book == constraintViolation.getRootBean());

                // the offending property
                if (constraintViolation.getPropertyPath().toString().equals("title")) {
                    foundTitleConstraint = true;
                    // the offending value
                    Assert.assertEquals(book.getTitle(), constraintViolation.getInvalidValue());
                }
            }
        }
        Assert.assertTrue(foundTitleConstraint);
    }

    /**
     * Example 2.14. Using the fluent API to build custom constraint violations. test that: the
     * {@link org.apache.bval.constraints.ZipCodeCityCoherenceValidator} adds custom messages to the context and
     * suppresses the default message
     */
    public void testConstraintValidatorContextFluentAPI() {
        Address ad = new Address();
        ad.setCity("error");
        ad.setZipCode("error");
        ad.setAddressline1("something");
        ad.setCountry(new Country());
        ad.getCountry().setName("something");
        Set<ConstraintViolation<Address>> violations = validator.validate(ad);
        Assert.assertEquals(2, violations.size());
        for (ConstraintViolation<Address> each : violations) {
            Assert.assertTrue(each.getMessage().endsWith(" not OK"));
        }
        assertNotNull(TestUtils.getViolation(violations, "city"));
        assertNotNull(TestUtils.getViolation(violations, ""));
    }

    public void testValidateNestedPropertyPath() throws InvocationTargetException, NoSuchMethodException,
        IllegalAccessException {
        final String propPath = "addresses[0].country.ISO2Code";

        Author author = new Author();
        author.setAddresses(new ArrayList<Address>());
        Address adr = new Address();
        author.getAddresses().add(adr);
        Country country = new Country();
        adr.setCountry(country);
        country.setISO2Code("too_long");

        Set<ConstraintViolation<Author>> iv = validator.validateProperty(author, propPath);
        Assert.assertEquals(1, iv.size());
        ConstraintViolation<Author> vio = iv.iterator().next();
        assertEquals(propPath, vio.getPropertyPath().toString());
        assertSame(author, vio.getRootBean());
        assertSame(author.getAddresses().get(0).getCountry(), vio.getLeafBean());

        country.setISO2Code("23");
        iv = validator.validateProperty(author, propPath);
        Assert.assertEquals(0, iv.size());

        iv = validator.validateValue(Author.class, propPath, "345");
        Assert.assertEquals(1, iv.size());
        vio = iv.iterator().next();
        assertEquals(propPath, vio.getPropertyPath().toString());
        assertNull(vio.getRootBean());
        assertNull(vio.getLeafBean());

        iv = validator.validateValue(Author.class, propPath, "34");
        Assert.assertEquals(0, iv.size());
    }

    public void testValidateCascadingNestedBean() throws InvocationTargetException, NoSuchMethodException,
        IllegalAccessException {
        final String propPath = "addresses[0]";

        CascadingPropertyValidator v = validator.unwrap(CascadingPropertyValidator.class);
        Author author = new Author();
        author.setAddresses(new ArrayList<Address>());
        Address adr = new Address();
        author.getAddresses().add(adr);
        Country country = new Country();
        adr.setCity("dark");
        adr.setCountry(country);

        Set<ConstraintViolation<Author>> iv = v.validateProperty(author, propPath);
        Assert.assertEquals(1, iv.size()); // null address line 1 (no cascade)

        country.setISO2Code("too_long");
        iv = v.validateProperty(author, propPath, true);
        Assert.assertEquals(3, iv.size()); // null address line 1 + null
        // country.name + too long
        // country.iso2code

        country.setISO2Code("23");
        iv = v.validateProperty(author, propPath, true);
        Assert.assertEquals(2, iv.size()); // null address line 1 + null
        // country.name, country.iso2code
        // fixed

        Address value = new Address();
        value.setCity("whatever");
        value.setAddressline1("1 address line");
        iv = v.validateValue(Author.class, propPath, value, true);
        Assert.assertEquals(1, iv.size()); // null country

        value.setCountry(new Country());
        iv = v.validateValue(Author.class, propPath, value, true);
        Assert.assertEquals(1, iv.size()); // null country.name

        value.getCountry().setName("NWO");
        iv = v.validateValue(Author.class, propPath, value, true);
        Assert.assertEquals(0, iv.size());
    }

    public void testValidateCascadingNestedProperty() throws InvocationTargetException, NoSuchMethodException,
        IllegalAccessException {
        final String propPath = "addresses[0].country";

        CascadingPropertyValidator v = validator.unwrap(CascadingPropertyValidator.class);
        Author author = new Author();
        author.setAddresses(new ArrayList<Address>());
        Address adr = new Address();
        author.getAddresses().add(adr);
        Country country = new Country();
        adr.setCity("dark");
        adr.setCountry(country);

        Set<ConstraintViolation<Author>> iv = v.validateProperty(author, propPath);
        Assert.assertEquals(0, iv.size());

        country.setISO2Code("too_long");
        iv = v.validateProperty(author, propPath, true);
        Assert.assertEquals(2, iv.size());
        // country.name + too long
        // country.iso2code

        country.setISO2Code("23");
        iv = v.validateProperty(author, propPath, true);
        Assert.assertEquals(1, iv.size());
        // country.name, country.iso2code

        Country value = null;
        iv = v.validateValue(Author.class, propPath, value, true);
        Assert.assertEquals(1, iv.size()); // null country

        value = new Country();
        iv = v.validateValue(Author.class, propPath, value, true);
        Assert.assertEquals(1, iv.size()); // null country.name

        value.setName("NWO");
        iv = v.validateValue(Author.class, propPath, value, true);
        Assert.assertEquals(0, iv.size());
    }

    public void testValidateCascadingNestedTipProperty() {
        final String propPath = "addresses[0].country.name";

        CascadingPropertyValidator v = validator.unwrap(CascadingPropertyValidator.class);
        Author author = new Author();
        author.setAddresses(new ArrayList<Address>());
        Address adr = new Address();
        author.getAddresses().add(adr);
        Country country = new Country();
        adr.setCity("dark");
        adr.setCountry(country);

        Set<ConstraintViolation<Author>> iv = v.validateProperty(author, propPath);
        Assert.assertEquals(1, iv.size());

        iv = v.validateProperty(author, propPath, true);
        Assert.assertEquals(1, iv.size());
    }

    public void testValidateCascadingKeyedElement() throws InvocationTargetException, NoSuchMethodException,
        IllegalAccessException {
        final String propPath = "[foo]";

        CascadingPropertyValidator v = validator.unwrap(CascadingPropertyValidator.class);
        final Address adr = new Address();
        @SuppressWarnings("serial")
        Object map = new HashMap<String, Address>() {
            {
                put("foo", adr);
            }
        };
        Country country = new Country();
        adr.setCity("dark");
        adr.setCountry(country);
        Set<ConstraintViolation<Object>> iv = v.validateProperty(map, propPath);
        Assert.assertEquals(1, iv.size()); // null address line 1 (no cascade)

        country.setISO2Code("too_long");
        iv = v.validateProperty(map, propPath, true);
        Assert.assertEquals(3, iv.size()); // null address line 1 + null
        // country.name + too long
        // country.iso2code

        country.setISO2Code("23");
        iv = v.validateProperty(map, propPath, true);
        Assert.assertEquals(2, iv.size()); // null address line 1 + null
        // country.name, country.iso2code
        // fixed

        Address value = new Address();
        value.setCity("whatever");
        value.setAddressline1("1 address line");

        Set<?> iv2 = v.validateValue(map.getClass(), propPath, value, true);
        Assert.assertEquals(1, iv2.size()); // null country

        value.setCountry(new Country());
        iv2 = v.validateValue(map.getClass(), propPath, value, true);
        Assert.assertEquals(1, iv2.size()); // null country.name

        value.getCountry().setName("NWO");
        iv2 = v.validateValue(map.getClass(), propPath, value, true);
        Assert.assertEquals(0, iv2.size());
    }

    @SuppressWarnings("unchecked")
    public void testValidateCascadingKeyedGenericElement() throws InvocationTargetException, NoSuchMethodException,
        IllegalAccessException {
        final String propPath = "[foo]";

        CascadingPropertyValidator v = validator.unwrap(CascadingPropertyValidator.class);
        final Address adr = new Address();
        Object map = new HashMap<String, Address>();
        ((Map<String, Address>) map).put("foo", adr);
        Country country = new Country();
        adr.setCity("dark");
        adr.setCountry(country);
        Set<?> iv = v.validateProperty(map, propPath);
        Assert.assertEquals(1, iv.size()); // null address line 1 (no cascade)

        country.setISO2Code("too_long");
        iv = v.validateProperty(map, propPath, true);
        Assert.assertEquals(3, iv.size()); // null address line 1 + null
        // country.name + too long
        // country.iso2code

        country.setISO2Code("23");
        iv = v.validateProperty(map, propPath, true);
        Assert.assertEquals(2, iv.size()); // null address line 1 + null
        // country.name, country.iso2code
        // fixed

        Address value = new Address();
        value.setCity("whatever");
        value.setAddressline1("1 address line");

        Set<?> iv2 = v.validateValue(Map.class, propPath, value, true);
        Assert.assertEquals(1, iv2.size()); // null country

        value.setCountry(new Country());
        iv2 = v.validateValue(Map.class, propPath, value, true);
        Assert.assertEquals(1, iv2.size()); // null country.name

        value.getCountry().setName("NWO");
        iv2 = v.validateValue(Map.class, propPath, value, true);
        Assert.assertEquals(0, iv2.size());
    }

    public void testValidateCascadingIndexedElement() throws InvocationTargetException, NoSuchMethodException,
        IllegalAccessException {
        final String propPath = "[0]";
        CascadingPropertyValidator v = validator.unwrap(CascadingPropertyValidator.class);
        Address value = new Address();
        value.setCity("whatever");
        value.setAddressline1("1 address line");
        Set<ConstraintViolation<Address[]>> iv;
        Address[] array = { value };
        iv = v.validateProperty(array, propPath, true);
        Assert.assertEquals(1, iv.size()); // null country

        value.setCountry(new Country());
        iv = v.validateProperty(array, propPath, true);
        Assert.assertEquals(1, iv.size()); // null country.name

        value.getCountry().setName("NWO");
        iv = v.validateProperty(array, propPath, true);
        Assert.assertEquals(0, iv.size());

        value = new Address();
        value.setCity("whatever");
        value.setAddressline1("1 address line");
        Set<?> iv2;
        iv2 = v.validateValue(array.getClass(), propPath, value, true);
        Assert.assertEquals(1, iv2.size()); // null country

        value.setCountry(new Country());
        iv2 = v.validateValue(array.getClass(), propPath, value, true);
        Assert.assertEquals(1, iv2.size()); // null country.name

        value.getCountry().setName("NWO");
        iv2 = v.validateValue(array.getClass(), propPath, value, true);
        Assert.assertEquals(0, iv2.size());
    }

    public void testValidateCascadingIndexedGenericElement() throws InvocationTargetException, NoSuchMethodException,
    IllegalAccessException {
        final String propPath = "[0]";
        CascadingPropertyValidator v = validator.unwrap(CascadingPropertyValidator.class);
        Address value = new Address();
        value.setCity("whatever");
        value.setAddressline1("1 address line");
        Set<?> iv;
        Object list = Collections.singletonList(value);
        iv = v.validateProperty(list, propPath, true);
        Assert.assertEquals(1, iv.size()); // null country
        
        value.setCountry(new Country());
        iv = v.validateProperty(list, propPath, true);
        Assert.assertEquals(1, iv.size()); // null country.name
        
        value.getCountry().setName("NWO");
        iv = v.validateProperty(list, propPath, true);
        Assert.assertEquals(0, iv.size());
        
        value = new Address();
        value.setCity("whatever");
        value.setAddressline1("1 address line");
        Set<?> iv2;
        iv2 = v.validateValue(List.class, propPath, value, true);
        Assert.assertEquals(1, iv2.size()); // null country
        
        value.setCountry(new Country());
        iv2 = v.validateValue(List.class, propPath, value, true);
        Assert.assertEquals(1, iv2.size()); // null country.name
        
        value.getCountry().setName("NWO");
        iv2 = v.validateValue(List.class, propPath, value, true);
        Assert.assertEquals(0, iv2.size());
    }
    
    public interface Foo {
    }

    public static class FooAddress extends Address {
        /**
         * {@inheritDoc}
         */
        @Override
        @NotNull(groups = Foo.class)
        public String getCity() {
            return super.getCity();
        }
    }

    public void testValidateCascadingPropertyWithMultipleGroupsIgnoresSiblingProperties() {
        final String propPath = "addresses[0].country";

        CascadingPropertyValidator v = validator.unwrap(CascadingPropertyValidator.class);
        Author author = new Author();
        author.setAddresses(new ArrayList<Address>());
        Address adr = new FooAddress();
        author.getAddresses().add(adr);
        Country country = new Country();
        adr.setCountry(country);

        Set<ConstraintViolation<Author>> iv = v.validateProperty(author, propPath, true, Default.class, Foo.class);
        Assert.assertEquals(1, iv.size());
    }

    public void testMetadataAPI() {
        BeanDescriptor bookBeanDescriptor = validator.getConstraintsForClass(Book.class);

        // expect no constraints on Book's Class-Level
        Assert.assertFalse(bookBeanDescriptor.hasConstraints());
        // but there are constraints on Book's Property-Level
        Assert.assertTrue(bookBeanDescriptor.isBeanConstrained());
        Assert.assertTrue(bookBeanDescriptor.getConstraintDescriptors().size() == 0); // no
        // constraint
        // more specifically "author" and "title"
        Assert.assertEquals(4, bookBeanDescriptor.getConstrainedProperties().size());
        // not a property
        Assert.assertTrue(bookBeanDescriptor.getConstraintsForProperty("doesNotExist") == null);
        // property with no constraint
        Assert.assertTrue(bookBeanDescriptor.getConstraintsForProperty("description") == null);
        PropertyDescriptor propertyDescriptor = bookBeanDescriptor.getConstraintsForProperty("title");
        Assert.assertEquals(2, propertyDescriptor.getConstraintDescriptors().size());
        Assert.assertTrue("title".equals(propertyDescriptor.getPropertyName()));
        // assuming the implementation returns the NotEmpty constraint first
        Iterator<ConstraintDescriptor<?>> iter = propertyDescriptor.getConstraintDescriptors().iterator();
        ConstraintDescriptor<?> constraintDescriptor = null;
        while (iter.hasNext()) {
            constraintDescriptor = iter.next();
            if (constraintDescriptor.getAnnotation().annotationType().equals(NotNull.class)) {
                break;
            }

        }
        Assert.assertTrue(constraintDescriptor != null);
        Assert.assertTrue(constraintDescriptor.getGroups().size() == 1); // "first"
        Assert.assertEquals(NotNullValidator.class, constraintDescriptor.getConstraintValidatorClasses().get(0));
        // assuming the implementation returns the Size constraint first
        propertyDescriptor = bookBeanDescriptor.getConstraintsForProperty("subtitle");
        Iterator<ConstraintDescriptor<?>> iterator = propertyDescriptor.getConstraintDescriptors().iterator();
        constraintDescriptor = iterator.next();
        Assert.assertTrue(constraintDescriptor.getAnnotation().annotationType().equals(Size.class));
        Assert.assertTrue(((Integer) constraintDescriptor.getAttributes().get("max")) == 30);
        Assert.assertTrue(constraintDescriptor.getGroups().size() == 1);
        propertyDescriptor = bookBeanDescriptor.getConstraintsForProperty("author");
        Assert.assertTrue(propertyDescriptor.getConstraintDescriptors().size() == 1);
        Assert.assertTrue(propertyDescriptor.isCascaded());
        Assert.assertNull(bookBeanDescriptor.getConstraintsForProperty("unconstraintField"));
    }

    public void testKeyedMetadata() {
        @SuppressWarnings("serial")
        BeanDescriptor beanDescriptor = validator.getConstraintsForClass(new HashMap<String, Object>() {
        }.getClass());
        Assert.assertNotNull(beanDescriptor);
        Assert.assertFalse(beanDescriptor.isBeanConstrained());
        Assert.assertNull(beanDescriptor.getConstraintsForProperty("[foo]"));
    }

    public void testGenericKeyedMetadata() {
        BeanDescriptor beanDescriptor = validator.getConstraintsForClass(Map.class);
        Assert.assertNotNull(beanDescriptor);
        Assert.assertFalse(beanDescriptor.isBeanConstrained());
        Assert.assertNull(beanDescriptor.getConstraintsForProperty("[foo]"));
    }
    
    public void testIndexedMetadata() {
        BeanDescriptor beanDescriptor = validator.getConstraintsForClass(Array.newInstance(Author.class, 0).getClass());
        Assert.assertNotNull(beanDescriptor);
        Assert.assertFalse(beanDescriptor.isBeanConstrained());
        Assert.assertNull(beanDescriptor.getConstraintsForProperty("[0]"));
    }
    
    public void testGenericIndexedMetadata() {
        BeanDescriptor beanDescriptor = validator.getConstraintsForClass(List.class);
        Assert.assertNotNull(beanDescriptor);
        Assert.assertFalse(beanDescriptor.isBeanConstrained());
        Assert.assertNull(beanDescriptor.getConstraintsForProperty("[0]"));
    }
}
