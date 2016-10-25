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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.UnexpectedTypeException;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.apache.bval.constraints.SizeValidatorForCharSequence;
import org.apache.bval.jsr.example.Address;
import org.apache.bval.jsr.example.Book;
import org.apache.bval.jsr.example.Engine;
import org.apache.bval.jsr.example.IllustratedBook;
import org.apache.bval.jsr.example.MaxTestEntity;
import org.apache.bval.jsr.example.NoValidatorTestEntity;
import org.apache.bval.jsr.example.Second;
import org.apache.bval.jsr.example.SizeTestEntity;
import org.apache.bval.jsr.util.TestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Description: <br/>
 */
public class Jsr303Test extends ValidationTestBase {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testPropertyDescriptorHasConstraints() {
        BeanDescriptor cons = validator.getConstraintsForClass(Book.class);
        assertTrue(cons.getConstraintsForProperty("author").hasConstraints());
        assertTrue(cons.getConstraintsForProperty("title").hasConstraints());
        assertTrue(cons.getConstraintsForProperty("uselessField").hasConstraints());
        // cons.getConstraintsForProperty("unconstraintField") == null without Introspector
        // cons.getConstraintsForProperty("unconstraintField") != null with Introspector
        assertTrue(cons.getConstraintsForProperty("unconstraintField") == null
            || !cons.getConstraintsForProperty("unconstraintField").hasConstraints());
        assertNull(cons.getConstraintsForProperty("unknownField"));
    }

    @Test
    public void testValidateValue() {
        assertTrue(validator.validateValue(Book.class, "subtitle", "123456789098765432").isEmpty());
        assertFalse(validator.validateValue(Book.class, "subtitle",
            "123456789098765432123412345678909876543212341234564567890987654321234", Second.class).isEmpty());
        // tests for issue 22: validation of a field without any constraints
        assertTrue(validator.validateValue(Book.class, "unconstraintField", 4).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownProperty() {
        // tests for issue 22: validation of unknown field cause ValidationException
        validator.validateValue(Book.class, "unknownProperty", 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateNonCascadedRealNestedProperty() {
        validator.validateValue(IllustratedBook.class, "illustrator.firstName", "Edgar");
    }

    @Test
    public void testMetadataAPI_Book() {
        assertNotNull(validator.getConstraintsForClass(Book.class));
        // not necessary for implementation correctness, but we'll test nevertheless:
        assertSame(validator.getConstraintsForClass(Book.class), validator.getConstraintsForClass(Book.class));
        BeanDescriptor bc = validator.getConstraintsForClass(Book.class);
        assertEquals(Book.class, bc.getElementClass());
        assertNotNull(bc.getConstraintDescriptors());
        TestUtils.failOnModifiable(bc.getConstraintDescriptors(), "beanDescriptor constraintDescriptors");
    }

    @Test
    public void testMetadataAPI_Engine() {
        ElementDescriptor desc =
            validator.getConstraintsForClass(Engine.class).getConstraintsForProperty("serialNumber");
        assertNotNull(desc);
        assertEquals(String.class, desc.getElementClass());
    }

    @Test
    public void testMetadataAPI_Address() {
        assertFalse(validator.getConstraintsForClass(Address.class).getConstraintDescriptors().isEmpty());

        Set<PropertyDescriptor> props = validator.getConstraintsForClass(Address.class).getConstrainedProperties();
        TestUtils.failOnModifiable(props, "beanDescriptor constrainedProperties");
        Set<String> propNames = new HashSet<String>(props.size());
        for (PropertyDescriptor each : props) {
            TestUtils.failOnModifiable(each.getConstraintDescriptors(), "propertyDescriptor constraintDescriptors");
            propNames.add(each.getPropertyName());
        }
        assertTrue(propNames.contains("addressline1")); // annotated at
        // field level
        assertTrue(propNames.contains("addressline2"));
        assertTrue(propNames.contains("zipCode"));
        assertTrue(propNames.contains("country"));
        assertTrue(propNames.contains("city")); // annotated at method
        // level
        assertEquals(5, props.size());

        ElementDescriptor desc =
            validator.getConstraintsForClass(Address.class).getConstraintsForProperty("addressline1");
        assertNotNull(desc);
        boolean found = false;
        for (ConstraintDescriptor<?> each : desc.getConstraintDescriptors()) {
            if (SizeValidatorForCharSequence.class.equals(each.getConstraintValidatorClasses().get(0))) {
                assertTrue(each.getAttributes().containsKey("max"));
                assertEquals(30, each.getAttributes().get("max"));
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testValidateMultiValuedConstraints() {
        Engine engine = new Engine();
        engine.serialNumber = "abcd-defg-0123";
        Set<ConstraintViolation<Engine>> violations;
        violations = validator.validate(engine);
        assertEquals(0, violations.size());

        engine.serialNumber = "!)/(/()";
        violations = validator.validate(engine);
        assertEquals(2, violations.size());
        for (String msg : Arrays.asList("must contain alphabetical characters only", "must match ....-....-....")) {
            assertNotNull(TestUtils.getViolationWithMessage(violations, msg));
        }
    }

    @Test
    public void testConstraintValidatorResolutionAlgorithm() {
        MaxTestEntity entity = new MaxTestEntity();
        entity.setText("101");
        entity.setProperty("201");
        entity.setLongValue(301);
        entity.setDecimalValue(new BigDecimal(401));
        Set<ConstraintViolation<MaxTestEntity>> violations = validator.validate(entity);
        assertEquals(4, violations.size());
    }

    @Test
    public void testConstraintValidatorResolutionAlgorithm2() {
        thrown.expect(UnexpectedTypeException.class);
        thrown.expectMessage("No validator could be found for type java.lang.Object. "
            + "See: @Max at private java.lang.Object org.apache.bval.jsr.example." + "NoValidatorTestEntity.anything");

        NoValidatorTestEntity entity2 = new NoValidatorTestEntity();
        validator.validate(entity2);
    }

    @Test
    public void testSizeValidation() {
        SizeTestEntity en = new SizeTestEntity();
        en.ba = new byte[3];
        en.ca = new char[3];
        en.boa = new boolean[3];
        en.coll = Arrays.asList("1", "2", "3");
        en.da = new double[3];
        en.fa = new float[3];
        en.it = new int[3];
        en.la = new long[3];
        en.map = new HashMap<String, String>();
        en.map.put("1", "1");
        en.map.put("3", "3");
        en.map.put("2", "2");
        en.oa = new Integer[3];
        en.oa2 = new Integer[3];
        en.sa = new short[3];
        en.text = "123";
        Set<ConstraintViolation<SizeTestEntity>> vi = validator.validate(en);
        assertEquals(13, vi.size());
    }

    /**
     * JSR-303 Section 5.1.c, IllegalArgumentException should be thrown
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetConstraintsForNullClass() {
        validator.getConstraintsForClass(null);
    }

}
