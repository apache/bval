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
package com.agimatec.validation.jsr303;

import com.agimatec.validation.constraints.SizeValidatorForString;
import com.agimatec.validation.jsr303.example.*;
import com.agimatec.validation.jsr303.util.TestUtils;
import junit.framework.Assert;
import junit.framework.TestCase;

import javax.validation.ConstraintViolation;
import javax.validation.UnexpectedTypeException;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.PropertyDescriptor;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Description: <br/>
 * User: roman.stumm <br/>
 * Date: 01.04.2008 <br/>
 * Time: 15:12:29 <br/>
 * Copyright: Agimatec GmbH 2008
 */
public class Jsr303Test extends TestCase {
/*    static {
        AgimatecValidatorFactory.getDefault().getMetaBeanManager()
                .addResourceLoader("com/agimatec/validation/example/test-beanInfos.xml");
    }*/

    /*  public void testUseAgimatecXmlMetaData() {
        Validator validator = getValidator();

        BusinessObject object = new BusinessObject();
        object.setTitle("1234567834567 too long title ");
        Set<ConstraintViolation<BusinessObject>> violations = validator.validate(object);
        Assert.assertNotNull(violations);
        Assert.assertTrue(!violations.isEmpty());

        Assert.assertTrue(!validator.validateProperty(object, "title").isEmpty());
    }*/

    public void testPropertyDescriptorHasConstraints() {
        BeanDescriptor cons = getValidator().getConstraintsForClass(Book.class);
        assertTrue(cons.getConstraintsForProperty("author").hasConstraints());
        assertTrue(cons.getConstraintsForProperty("title").hasConstraints());
        assertTrue(cons.getConstraintsForProperty("uselessField").hasConstraints());
        // cons.getConstraintsForProperty("unconstraintField") == null without Introspector
        // cons.getConstraintsForProperty("unconstraintField") != null with Introspector
        assertTrue(cons.getConstraintsForProperty("unconstraintField") == null ||
              !cons.getConstraintsForProperty("unconstraintField").hasConstraints());
        assertNull(cons.getConstraintsForProperty("unknownField"));
    }

    public void testValidateValue() {
        Validator validator = getValidator();
        assertTrue(
              validator.validateValue(Book.class, "subtitle", "123456789098765432").isEmpty());
        assertFalse(validator.validateValue(Book.class, "subtitle",
              "123456789098765432123412345678909876543212341234564567890987654321234",
              Second.class).isEmpty());
        // tests for issue 22: validation of a field without any constraints 
        assertEquals(0, validator.validateValue(Book.class, "unconstraintField", 4).size());
        // tests for issue 22: validation of unknown field cause ValidationException
        try {
            validator.validateValue(Book.class, "unknownProperty", 4);
            fail("unknownProperty not detected");
        } catch (ValidationException ex) {
            // OK
            assertEquals(
                  "unknown property 'unknownProperty' in com.agimatec.validation.jsr303.example.Book",
                  ex.getMessage());
        }
    }

    public void testMetadataAPI_Book() {
        Validator validator = getValidator();
        Assert.assertNotNull(validator.getConstraintsForClass(Book.class));
        Assert.assertTrue(validator.getConstraintsForClass(Book.class) ==
              validator.getConstraintsForClass(Book.class));
        BeanDescriptor bc = validator.getConstraintsForClass(Book.class);
//        assertEquals(ElementType.TYPE, bc.getElementType());
        Assert.assertEquals(Book.class, bc.getElementClass());
//        assertEquals(false, bc.isCascaded());
//        assertEquals("", bc.getPropertyPath());
        Assert.assertTrue(bc.getConstraintDescriptors() != null);
    }

    public void testMetadataAPI_Engine() {
        Validator validator = getValidator();
        ElementDescriptor desc = validator.getConstraintsForClass(Engine.class)
              .getConstraintsForProperty("serialNumber");
        assertNotNull(desc);
//        assertEquals(ElementType.FIELD, desc.getElementType());
        Assert.assertEquals(String.class, desc.getElementClass());
    }

    public void testMetadataAPI_Address() {
        Validator validator = getValidator();
        Assert.assertFalse(validator.getConstraintsForClass(Address.class)
              .getConstraintDescriptors().isEmpty());

        Set<PropertyDescriptor> props =
              validator.getConstraintsForClass(Address.class).getConstrainedProperties();
        Set<String> propNames = new HashSet(props.size());
        for (PropertyDescriptor each : props) {
            propNames.add(each.getPropertyName());
        }
        Assert.assertTrue(propNames.contains("addressline1")); // annotated at field level
        Assert.assertTrue(propNames.contains("addressline2"));
        Assert.assertTrue(propNames.contains("zipCode"));
        Assert.assertTrue(propNames.contains("country"));
        Assert.assertTrue(propNames.contains("city"));       // annotated at method level
        Assert.assertEquals(5, props.size());

        ElementDescriptor desc = validator.getConstraintsForClass(Address.class)
              .getConstraintsForProperty("addressline1");
        Assert.assertNotNull(desc);
        boolean found = false;
        for (ConstraintDescriptor each : desc.getConstraintDescriptors()) {
            if (each.getConstraintValidatorClasses().get(0)
                  .equals(SizeValidatorForString.class)) {
                Assert.assertTrue(each.getAttributes().containsKey("max"));
                assertEquals(30, each.getAttributes().get("max"));
                found = true;
            }
        }
        Assert.assertTrue(found);

    }

    public void testValidateMultiValuedConstraints() {
        Validator validator = getValidator();
        Engine engine = new Engine();
        engine.serialNumber = "abcd-defg-0123";
        Set<ConstraintViolation<Engine>> violations;
        violations = validator.validate(engine);
        assertEquals(0, violations.size());

        engine.serialNumber = "!)/(/()";
        violations = validator.validate(engine);
        assertEquals(2, violations.size());
        for (String msg : new String[]{"must contain alphabetical characters only",
              "must match ....-....-...."}) {
            assertNotNull(TestUtils.getViolationWithMessage(violations, msg));
        }
    }

    public void testConstraintValidatorResolutionAlgorithm() {
        MaxTestEntity entity = new MaxTestEntity();
        entity.setText("101");
        entity.setProperty("201");
        entity.setLongValue(301);
        entity.setDecimalValue(new BigDecimal(401));
        Validator validator = getValidator();
        Set<ConstraintViolation<MaxTestEntity>> violations = validator.validate(entity);
        assertEquals(4, violations.size());

        NoValidatorTestEntity entity2 = new NoValidatorTestEntity();
        try {
            validator.validate(entity2);
            fail("UnexpectedTypeException expected but not thrown");
        } catch (UnexpectedTypeException ex) {
            // we expected this
            assertEquals("No validator could be found for type java.lang.Object. " +
                  "See: @Max at private java.lang.Object " +
                  "com.agimatec.validation.jsr303.example." + "NoValidatorTestEntity.anything",
                  ex.getMessage());
        }
    }

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
        en.map = new HashMap();
        en.map.put("1", "1");
        en.map.put("3", "3");
        en.map.put("2", "2");
        en.oa = new Integer[3];
        en.oa2 = new Integer[3];
        en.sa = new short[3];
        en.text = "123";
        Set<ConstraintViolation<SizeTestEntity>> vi = getValidator().validate(en);
        assertEquals(13, vi.size());
    }

    private Validator getValidator() {
//        return Validation.buildDefaultValidatorFactory().getValidator();
        return AgimatecValidatorFactory.getDefault().getValidator();
    }
}
