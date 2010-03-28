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

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.validation.Validator;

import org.apache.bval.jsr303.AgimatecValidatorFactory;
import org.apache.bval.jsr303.example.Customer;

/**
 * EmailValidator Tester.
 *
 * @author Roman Stumm
 * @version 1.0
 * @since <pre>10/14/2008</pre>
 */
public class EmailValidatorTest extends TestCase {
    public EmailValidatorTest(String name) {
        super(name);
    }

    public void testEmail() {
        Validator validator = AgimatecValidatorFactory.getDefault().getValidator();
        Customer customer = new Customer();
        customer.setCustomerId("id-1");
        customer.setFirstName("Mary");
        customer.setLastName("Do");
        customer.setPassword("12345");

        Assert.assertEquals(0, validator.validate(customer).size());

        customer.setEmailAddress("some@invalid@address");
        Assert.assertEquals(1, validator.validate(customer).size());

        customer.setEmailAddress("some.valid-012345@address_at-test.org");
        Assert.assertEquals(0, validator.validate(customer).size());
    }


    public static Test suite() {
        return new TestSuite(EmailValidatorTest.class);
    }
}
