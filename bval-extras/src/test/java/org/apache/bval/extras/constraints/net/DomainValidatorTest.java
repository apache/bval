/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bval.extras.constraints.net;

import org.junit.Before;
import org.junit.Test;

import javax.validation.Payload;
import java.lang.annotation.Annotation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the DomainValidator.
 */
public class DomainValidatorTest {

    private DomainValidator validator;

    @Before
    public void setUp() {
        validator = new DomainValidator();
    }

    @Test
    public void testValidDomains() {
        assertTrue("apache.org should validate", validator.isValid("apache.org", null));
        assertTrue("www.google.com should validate", validator.isValid("www.google.com", null));

        assertTrue("test-domain.com should validate", validator.isValid("test-domain.com", null));
        assertTrue("test---domain.com should validate", validator.isValid("test---domain.com", null));
        assertTrue("test-d-o-m-ain.com should validate", validator.isValid("test-d-o-m-ain.com", null));
        assertTrue("two-letter domain label should validate", validator.isValid("as.uk", null));

        assertTrue("case-insensitive ApAchE.Org should validate", validator.isValid("ApAchE.Org", null));

        assertTrue("single-character domain label should validate", validator.isValid("z.com", null));

        assertTrue("i.have.an-example.domain.name should validate", validator.isValid("i.have.an-example.domain.name", null));
    }

    @Test
    public void testInvalidDomains() {
        assertFalse("bare TLD .org shouldn't validate", validator.isValid(".org", null));
        assertFalse("domain name with spaces shouldn't validate", validator.isValid(" apache.org ", null));
        assertFalse("domain name containing spaces shouldn't validate", validator.isValid("apa che.org", null));
        assertFalse("domain name starting with dash shouldn't validate", validator.isValid("-testdomain.name", null));
        assertFalse("domain name ending with dash shouldn't validate", validator.isValid("testdomain-.name", null));
        assertFalse("domain name starting with multiple dashes shouldn't validate", validator.isValid("---c.com", null));
        assertFalse("domain name ending with multiple dashes shouldn't validate", validator.isValid("c--.com", null));
        assertFalse("domain name with invalid TLD shouldn't validate", validator.isValid("apache.rog", null));

        assertFalse("URL shouldn't validate", validator.isValid("http://www.apache.org", null));
        assertFalse("Empty string shouldn't validate as domain name", validator.isValid(" ", null));
    }

    @Test
    public void testTopLevelDomains() {
        // infrastructure TLDs
        assertTrue(".arpa should validate as iTLD", DomainValidator.isValidInfrastructureTld("arpa"));
        assertFalse(".com shouldn't validate as iTLD", DomainValidator.isValidInfrastructureTld("com"));

        // generic TLDs
        assertTrue(".name should validate as gTLD", DomainValidator.isValidGenericTld("name"));
        assertFalse(".us shouldn't validate as gTLD", DomainValidator.isValidGenericTld("us"));

        // country code TLDs
        assertTrue(".uk should validate as ccTLD", DomainValidator.isValidCountryCodeTld("uk"));
        assertFalse(".org shouldn't validate as ccTLD", DomainValidator.isValidCountryCodeTld("org"));

        // case-insensitive
        assertTrue(".COM should validate as TLD", validator.isValidTld("COM"));
        assertTrue(".BiZ should validate as TLD", validator.isValidTld("BiZ"));

        // corner cases
        assertFalse("invalid TLD shouldn't validate", validator.isValid("nope", null));
        assertFalse("empty string shouldn't validate as TLD", validator.isValid("", null));
    }

    @Test
    public void testAllowLocal() {
       DomainValidator noLocal = new DomainValidator();
       DomainValidator allowLocal = new DomainValidator();
       allowLocal.initialize( new Domain()
       {

            @Override
            public Class<? extends Annotation> annotationType() {
                // not needed
                return null;
            }

            @Override
            public Class<? extends Payload>[] payload() {
                // not needed
                return null;
            }

            @Override
            public String message() {
                // not needed
                return null;
            }

            @Override
            public Class<?>[] groups() {
                // not needed
                return null;
            }

            @Override
            public boolean allowLocal() {
                // enable the local
                return true;
            }
        });

       // Default won't allow local
       assertFalse("localhost.localdomain should validate", noLocal.isValid("localhost.localdomain", null));
       assertFalse("localhost should validate", noLocal.isValid("localhost", null));

       // But it may be requested
       assertTrue("localhost.localdomain should validate", allowLocal.isValid("localhost.localdomain", null));
       assertTrue("localhost should validate", allowLocal.isValid("localhost", null));
       assertTrue("hostname should validate", allowLocal.isValid("hostname", null));
       assertTrue("machinename should validate", allowLocal.isValid("machinename", null));

       // Check the localhost one with a few others
       assertTrue("apache.org should validate", allowLocal.isValid("apache.org", null));
       assertFalse("domain name with spaces shouldn't validate", allowLocal.isValid(" apache.org ", null));
    }

    @Test
    public void testIDN() {
       assertTrue("b\u00fccher.ch in IDN should validate", validator.isValid("www.xn--bcher-kva.ch", null));
    }

}
