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
package org.apache.bval.jsr.groups.redefining;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.GroupDefinitionException;

import org.apache.bval.jsr.ValidationTestBase;
import org.apache.bval.jsr.util.TestUtils;
import org.junit.Test;

/**
 * Description: test Redefining the Default group for a class (spec. chapter 3.4.3)<br/>
 */
public class RedefiningDefaultGroupTest extends ValidationTestBase {

    /**
     * when an address object is validated for the group Default,
     * all constraints belonging to the group Default and hosted on Address are evaluated
     */
    @Test
    public void testValidateDefaultGroup() {
        Address address = new Address();
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        assertEquals(3, violations.size());
        assertNotNull(TestUtils.getViolation(violations, "street1"));
        assertNotNull(TestUtils.getViolation(violations, "zipCode"));
        assertNotNull(TestUtils.getViolation(violations, "city"));

        address.setStreet1("Elmstreet");
        address.setZipCode("1234");
        address.setCity("Gotham City");
        violations = validator.validate(address);
        assertTrue(violations.isEmpty());

        violations = validator.validate(address, Address.HighLevelCoherence.class);
        assertTrue(violations.isEmpty());

        address.setCity("error");
        violations = validator.validate(address, Address.HighLevelCoherence.class);
        assertEquals(1, violations.size());

        /**
         * If none fails, all HighLevelCoherence constraints present on Address are evaluated.
         *
         * In other words, when validating the Default group for Address,
         * the group sequence defined on the Address class is used.
         */
        violations = validator.validate(address);
        assertEquals(
              "redefined default group for Address must also validate HighLevelCoherence",
              1, violations.size());
    }

    @Test
    public void testValidateProperty() {
        Address address = new Address();
        address.setStreet1("");
        Set<ConstraintViolation<Address>> violations = validator.validateProperty(address, "street1");
        //prove that ExtraCareful group was validated:
        assertEquals(1, violations.size());
        assertNotNull(TestUtils.getViolation(violations, "street1"));
    }

    @Test
    public void testValidateValue() {
        Set<ConstraintViolation<Address>> violations = validator.validateValue(Address.class, "street1", "");
        //prove that ExtraCareful group was validated:
        assertEquals(1, violations.size());
        assertNotNull(TestUtils.getViolation(violations, "street1"));
    }

    @Test(expected = GroupDefinitionException.class)
    public void testRaiseGroupDefinitionException() {
        validator.validate(new InvalidRedefinedDefaultGroupAddress());
    }
}
