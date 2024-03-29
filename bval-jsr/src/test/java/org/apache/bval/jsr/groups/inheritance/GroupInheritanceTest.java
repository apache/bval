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
package org.apache.bval.jsr.groups.inheritance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Description: <br/>
 */
public class GroupInheritanceTest {
    private Validator validator;

    @Before
    public void setUp() {
        validator = ApacheValidatorFactory.getDefault().getValidator();
    }

    /**
     * validating the group BuyInOneClick will lead to the following constraints checking:
     *<pre>
     *  * @NotNull on firstname and lastname
     *  * @NotNull on defaultCreditCard</pre>
     * because Default and Billable are superinterfaces of BuyInOneClick.
     */
    @Test
    public void testValidGroupBuyInOneClick() {
        BillableUser user = new BillableUser();

        Set<ConstraintViolation<BillableUser>> violations = validator.validate(user, BuyInOneClick.class);
        assertEquals(3, violations.size());
        assertNotNull(TestUtils.getViolation(violations, "firstname"));
        assertNotNull(TestUtils.getViolation(violations, "lastname"));
        assertNotNull(TestUtils.getViolation(violations, "defaultCreditCard"));
    }
}
