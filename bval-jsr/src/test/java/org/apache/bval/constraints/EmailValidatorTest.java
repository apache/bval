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
package org.apache.bval.constraints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.validation.Validator;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.example.Customer;
import org.junit.Before;
import org.junit.Test;

/**
 * EmailValidator Tester.
 *
 * @author Roman Stumm
 * @version 1.0
 * @since <pre>10/14/2008</pre>
 */
public class EmailValidatorTest {
    public static class EmailAddressBuilder {
        @Email
        private StringBuilder buffer = new StringBuilder();

        /**
         * Get the buffer.
         * @return StringBuilder
         */
        public StringBuilder getBuffer() {
            return buffer;
        }

    }

    private Validator validator;

    @Before
    public void setUp() throws Exception {
        validator = ApacheValidatorFactory.getDefault().getValidator();
    }

    @Test
    public void testEmail() {
        Customer customer = new Customer();
        customer.setCustomerId("id-1");
        customer.setFirstName("Mary");
        customer.setLastName("Do");
        customer.setPassword("12345");

        assertTrue(validator.validate(customer).isEmpty());

        customer.setEmailAddress("some@invalid@address");
        assertEquals(1, validator.validate(customer).size());

        customer.setEmailAddress("some.valid-012345@address_at-test.org");
        assertTrue(validator.validate(customer).isEmpty());
    }

    @Test
    public void testEmailCharSequence() {
        EmailAddressBuilder emailAddressBuilder = new EmailAddressBuilder();
        assertTrue(validator.validate(emailAddressBuilder).isEmpty());
        emailAddressBuilder.getBuffer().append("foo");
        assertEquals(1, validator.validate(emailAddressBuilder).size());
        emailAddressBuilder.getBuffer().append('@');
        assertEquals(1, validator.validate(emailAddressBuilder).size());
        emailAddressBuilder.getBuffer().append("bar");
        assertTrue(validator.validate(emailAddressBuilder).isEmpty());
        emailAddressBuilder.getBuffer().append('.');
        assertEquals(1, validator.validate(emailAddressBuilder).size());
        emailAddressBuilder.getBuffer().append("baz");
        assertTrue(validator.validate(emailAddressBuilder).isEmpty());
    }

}
