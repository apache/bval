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

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.apache.bval.jsr.ValidationTestBase;
import org.apache.bval.jsr.util.TestUtils;
import org.junit.Test;

/**
 * Description: test features from spec chapter 3.4 group and group sequence
 * <br/>
 */
public class GroupValidationTest extends ValidationTestBase {

    /**
     * test spec: @NotNull on firstname and on lastname are validated when the
     * Default group is validated.
     */
    @Test
    public void testValidateFirstNameLastNameWithDefaultGroup() {
        BillableUser user = new BillableUser();

        Set<ConstraintViolation<BillableUser>> violations = validator.validate(user);
        assertEquals(2, violations.size());
        ConstraintViolation<?> violation = TestUtils.getViolation(violations, "firstname");
        assertNotNull(violation);
        assertEquals(user, violation.getRootBean());
        violation = TestUtils.getViolation(violations, "lastname");
        assertNotNull(violation);
        assertEquals(user, violation.getRootBean());
    }

    /**
     * test spec: @NotNull is checked on defaultCreditCard when either the
     * Billable or BuyInOneClick group is validated.
     */
    @Test
    public void testValidateDefaultCreditCardInBillableGroup() {
        BillableUser user = new BillableUser();

        Set<ConstraintViolation<BillableUser>> violations = validator.validate(user, Billable.class);
        assertEquals(1, violations.size());
        ConstraintViolation<?> violation = TestUtils.getViolation(violations, "defaultCreditCard");
        assertNotNull(violation);
        assertEquals(user, violation.getRootBean());
    }

    @Test
    public void testValidateDefaultCreditCardInBillableAndByInOneClickGroup() {
        BillableUser user = new BillableUser();

        Set<ConstraintViolation<BillableUser>> violations =
            validator.validate(user, BuyInOneClick.class, Billable.class);
        assertEquals(1, violations.size());
        ConstraintViolation<?> violation = TestUtils.getViolation(violations, "defaultCreditCard");
        assertNotNull(violation);
        assertEquals(user, violation.getRootBean());
    }

}
