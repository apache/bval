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
package org.apache.bval.jsr303.groups.implicit;

import junit.framework.TestCase;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.apache.bval.jsr303.AgimatecValidatorFactory;
import org.apache.bval.jsr303.util.TestUtils;

import java.util.Set;

/**
 * Description: test spec chapter 3.4.4. Implicit grouping<br/>
 * User: roman <br/>
 * Date: 05.10.2009 <br/>
 * Time: 12:45:24 <br/>
 * Copyright: Agimatec GmbH
 */
public class ImplicitGroupingTest extends TestCase {
    private Validator validator;

    protected void setUp() {
        validator = AgimatecValidatorFactory.getDefault().getValidator();
    }

    public void testValidateImplicitGrouping() {
        Order order = new Order();
        // When an Order object is validated on the Default group, ...
        Set<ConstraintViolation<Order>> violations = validator.validate(order);
        assertNotNull(TestUtils.getViolation(violations, "creationDate"));
        assertNotNull(TestUtils.getViolation(violations, "lastUpdate"));
        assertNotNull(TestUtils.getViolation(violations, "lastModifier"));
        assertNotNull(TestUtils.getViolation(violations, "lastReader"));
        assertNotNull(TestUtils.getViolation(violations, "orderNumber"));
        assertEquals(5, violations.size());

        // When an Order object is validated on the Auditable group, ...

        /* Only the constraints present on Auditable (and any of its super interfaces)
           and belonging to the Default group are validated
           when the group Auditable is requested. */
        violations = validator.validate(order, Auditable.class);
        assertEquals("Implicit grouping not correctly implemented", 4, violations.size());
        assertNotNull(TestUtils.getViolation(violations, "creationDate"));
        assertNotNull(TestUtils.getViolation(violations, "lastUpdate"));
        assertNotNull(TestUtils.getViolation(violations, "lastModifier"));
        assertNotNull(TestUtils.getViolation(violations, "lastReader"));
    }
}
