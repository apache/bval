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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;

import org.apache.bval.jsr.util.TestUtils;
import org.junit.Test;

/**
 * Description: test that payload information can be retrieved
 * from error reports via the ConstraintDescriptor either accessed
 * through the ConstraintViolation objects<br/>
 */
public class PayloadTest extends ValidationTestBase {

    static class Severity {
        static class Info implements Payload {
        }

        static class Error implements Payload {
        }
    }

    static class Address {
        private String zipCode;
        private String city;

        Address(String zipCode, String city) {
            this.zipCode = zipCode;
            this.city = city;
        }

        @NotNull(message = "would be nice if we had one", payload = Severity.Info.class)
        public String getZipCode() {
            return zipCode;
        }

        @NotNull(message = "the city is mandatory", payload = Severity.Error.class)
        public String getCity() {
            return city;
        }
    }

    @Test
    public void testPayload() {
        Address address = new Address(null, null);
        final Set<ConstraintViolation<Address>> violations = validator.validate(address);
        assertEquals(2, violations.size());

        final ConstraintViolation<?> zipViolation = TestUtils.getViolation(violations, "zipCode");
        assertNotNull(zipViolation);
        assertEquals(1, zipViolation.getConstraintDescriptor().getPayload().size());
        assertTrue(zipViolation.getConstraintDescriptor().getPayload().contains(Severity.Info.class));

        final ConstraintViolation<?> cityViolation = TestUtils.getViolation(violations, "city");
        assertNotNull(cityViolation);
        assertEquals(1, cityViolation.getConstraintDescriptor().getPayload().size());
        assertTrue(cityViolation.getConstraintDescriptor().getPayload().contains(Severity.Error.class));
    }
}
