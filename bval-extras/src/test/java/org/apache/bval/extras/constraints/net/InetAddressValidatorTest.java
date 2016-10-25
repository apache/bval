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
package org.apache.bval.extras.constraints.net;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for InetAddressValidator.
 *
 * @version $Revision$
 */
public class InetAddressValidatorTest {

    private InetAddressValidator validator;

    @Before
    public void setUp() {
        validator = new InetAddressValidator();
    }

    /**
     * Test IPs that point to real, well-known hosts (without actually looking them up).
     */
    @Test
    public void testInetAddressesFromTheWild() {
        assertTrue("www.apache.org IP should be valid", validator.isValid("140.211.11.130", null));
        assertTrue("www.l.google.com IP should be valid", validator.isValid("72.14.253.103", null));
        assertTrue("fsf.org IP should be valid", validator.isValid("199.232.41.5", null));
        assertTrue("appscs.ign.com IP should be valid", validator.isValid("216.35.123.87", null));
    }

    /**
     * Test valid and invalid IPs from each address class.
     */
    @Test
    public void testInetAddressesByClass() {
        assertTrue("class A IP should be valid", validator.isValid("24.25.231.12", null));
        assertFalse("illegal class A IP should be invalid", validator.isValid("2.41.32.324", null));

        assertTrue("class B IP should be valid", validator.isValid("135.14.44.12", null));
        assertFalse("illegal class B IP should be invalid", validator.isValid("154.123.441.123", null));

        assertTrue("class C IP should be valid", validator.isValid("213.25.224.32", null));
        assertFalse("illegal class C IP should be invalid", validator.isValid("201.543.23.11", null));

        assertTrue("class D IP should be valid", validator.isValid("229.35.159.6", null));
        assertFalse("illegal class D IP should be invalid", validator.isValid("231.54.11.987", null));

        assertTrue("class E IP should be valid", validator.isValid("248.85.24.92", null));
        assertFalse("illegal class E IP should be invalid", validator.isValid("250.21.323.48", null));
    }

    /**
     * Test reserved IPs.
     */
    @Test
    public void testReservedInetAddresses() {
        assertTrue("localhost IP should be valid", validator.isValid("127.0.0.1", null));
        assertTrue("broadcast IP should be valid", validator.isValid("255.255.255.255", null));
    }

    /**
     * Test obviously broken IPs.
     */
    @Test
    public void testBrokenInetAddresses() {
        assertFalse("IP with characters should be invalid", validator.isValid("124.14.32.abc", null));
        assertFalse("IP with three groups should be invalid", validator.isValid("23.64.12", null));
        assertFalse("IP with five groups should be invalid", validator.isValid("26.34.23.77.234", null));
    }

}
