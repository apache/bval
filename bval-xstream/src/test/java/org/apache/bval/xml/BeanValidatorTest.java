/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval.xml;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.bval.BeanValidator;
import org.apache.bval.MetaBeanFinder;
import org.apache.bval.ValidationResults;
import org.apache.bval.example.BusinessObject;
import org.apache.bval.example.BusinessObjectAddress;
import org.apache.bval.model.Features;
import org.apache.bval.model.Features.Property;
import org.apache.bval.model.MetaBean;
import org.apache.bval.routines.Reasons;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * BeanValidator Tester.
 */
public class BeanValidatorTest extends TestCase {
    public BeanValidatorTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testValidateMapAsBean() {
        XMLMetaBeanManagerFactory.getRegistry().addLoader(new XMLMetaBeanURLLoader(
              BusinessObject.class.getResource("test-beanInfos.xml")));

        MetaBean mb = XMLMetaBeanManagerFactory.getFinder()
              .findForId("org.apache.bval.example.Address");

        // 1. validate a bean
        BusinessObjectAddress adr = new BusinessObjectAddress();
        BeanValidator<ValidationResults> validator = new BeanValidator<ValidationResults>();
        ValidationResults results = validator.validate(adr, mb);
        assertEquals(2,
              results.getErrorsByReason().get(Features.Property.MANDATORY).size());

        // 2. validate a map with the same metabean
        validator.setTreatMapsLikeBeans(true);
        results = validator.validate(new HashMap<String, Object>(), mb);
        assertFalse(results.isEmpty());
        assertEquals(2,
              results.getErrorsByReason().get(Features.Property.MANDATORY).size());

        // 3. validate as empty map (jsr303 behavior)
        validator.setTreatMapsLikeBeans(false);
        results = validator.validate(new HashMap<Object, Object>(), mb);
        assertTrue(results.isEmpty());
    }

    public void testValidate() {
        MetaBeanFinder finder = XMLMetaBeanManagerFactory.getFinder();
        XMLMetaBeanManagerFactory.getRegistry().addLoader(new XMLMetaBeanURLLoader(
              BusinessObject.class.getResource("test-beanInfos.xml")));
        MetaBean info = finder.findForClass(BusinessObject.class);
        BusinessObject object = new BusinessObject();
        object.setAddress(new BusinessObjectAddress());
        object.getAddress().setOwner(object);
        BeanValidator<ValidationResults> validator = new BeanValidator<ValidationResults>();
        ValidationResults results = validator.validate(object, info);
        assertTrue(results.hasErrorForReason(Property.MANDATORY));
        assertTrue(results.hasError(object, null));
        assertTrue(results.hasError(object.getAddress(), null));

        assertTrue(
              validator.validateProperty(object, info.getProperty("firstName")).hasError(
                    object, "firstName"));

        object.setUserId(1L);
        object.setFirstName("Hans");
        object.setLastName("Tester");
        object.setAddress(new BusinessObjectAddress());
        object.getAddress().setOwner(object);
        assertFalse(validator.validate(object, info).isEmpty());

        object.getAddress().setCountry("0123456789012345678");
        assertFalse(validator.validate(object, info).isEmpty());

        object.getAddress().setCountry("Germany");
        object.setAddresses(new ArrayList<BusinessObjectAddress>());
        object.getAddresses().add(object.getAddress());
        object.getAddresses().add(object.getAddress());
        object.getAddresses().add(object.getAddress());
        assertTrue(validator.validate(object, info).isEmpty());

        // 4th address is too much!
        object.getAddresses().add(object.getAddress());
        assertFalse(
              validator.validate(object, info).isEmpty()); // cardinality error found
    }

    public static Test suite() {
        return new TestSuite(BeanValidatorTest.class);
    }
}
