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
import javax.validation.constraints.Size;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;

import org.apache.bval.jsr.example.CompanyAddress;
import org.apache.bval.jsr.example.FrenchAddress;
import org.apache.bval.jsr.util.TestUtils;
import org.junit.Test;

/**
 * Description: <br/>
 */
public class ComposedConstraintsTest extends ValidationTestBase {

    @Test
    public void testMetaDataAPI_ComposedConstraints() {
        ElementDescriptor ed =
              validator.getConstraintsForClass(FrenchAddress.class)
                    .getConstraintsForProperty("zipCode");
        assertEquals(1, ed.getConstraintDescriptors().size());
        for (ConstraintDescriptor<?> cd : ed.getConstraintDescriptors()) {
            assertTrue(cd.isReportAsSingleViolation());
            assertEquals(3, cd.getComposingConstraints().size());
            assertTrue("no composing constraints found!!", !cd.getComposingConstraints().isEmpty());
            processConstraintDescriptor(cd); // check all constraints on zip code
        }
    }

    private void processConstraintDescriptor(ConstraintDescriptor<?> cd) {
        //Size.class is understood by the tool
        if (Size.class.equals(cd.getAnnotation().annotationType())) {
            Size.class.cast(cd.getAnnotation());
        }
        for (ConstraintDescriptor<?> composingCd : cd.getComposingConstraints()) {
            //check composing constraints recursively
            processConstraintDescriptor(composingCd);
        }
    }

    @Test
    public void testValidateComposed() {
        FrenchAddress adr = new FrenchAddress();
        Set<ConstraintViolation<FrenchAddress>> findings = validator.validate(adr);
        assertEquals(1, findings.size()); // with @ReportAsSingleConstraintViolation

        ConstraintViolation<FrenchAddress> finding = findings.iterator().next();
        assertEquals("Wrong zipcode", finding.getMessage());

        adr.setZipCode("1234567");
        findings = validator.validate(adr);
        assertEquals(0, findings.size());

        adr.setZipCode("1234567234567");
        findings = validator.validate(adr);
        assertTrue(findings.size() > 0); // too long
    }

    @Test
    public void testOverridesAttributeConstraintIndex() {
        CompanyAddress adr = new CompanyAddress("invalid-string");
        Set<ConstraintViolation<CompanyAddress>> findings = validator.validate(adr);
        assertEquals(2, findings.size()); // without @ReportAsSingleConstraintViolation
        assertNotNull(TestUtils.getViolationWithMessage(findings, "Not COMPANY"));
        assertNotNull(TestUtils.getViolationWithMessage(findings, "Not an email"));

        adr =  new CompanyAddress("JOHN_DO@WEB.DE");
        findings = validator.validate(adr);
        assertEquals(1, findings.size());
        assertNotNull(TestUtils.getViolationWithMessage(findings, "Not COMPANY"));

        adr =  new CompanyAddress("JOHN_DO@COMPANY.DE");
        findings = validator.validate(adr);
        assertTrue(findings.isEmpty());
    }

}
