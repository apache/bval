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
package org.apache.bval.jsr303;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Size;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.bval.jsr303.example.AgimatecAddress;
import org.apache.bval.jsr303.example.FrenchAddress;
import org.apache.bval.jsr303.util.TestUtils;

/**
 * Description: <br/>
 * User: roman.stumm <br/>
 * Date: 31.10.2008 <br/>
 * Time: 16:45:11 <br/>
 * Copyright: Agimatec GmbH
 */
public class ComposedConstraintsTest extends TestCase {
    static ValidatorFactory factory;

    static {
        factory = Validation.buildDefaultValidatorFactory();
    }

    public void testMetaDataAPI_ComposedConstraints() {
        Validator addressValidator = factory.getValidator();
        ElementDescriptor ed =
              addressValidator.getConstraintsForClass(FrenchAddress.class)
                    .getConstraintsForProperty("zipCode");
        Assert.assertEquals(1, ed.getConstraintDescriptors().size());
        for (ConstraintDescriptor cd : ed.getConstraintDescriptors()) {
            Assert.assertTrue(cd.isReportAsSingleViolation());
            Assert.assertEquals(3, cd.getComposingConstraints().size());
            Assert.assertTrue("no composing constraints found!!",
                  !cd.getComposingConstraints().isEmpty());
            processConstraintDescriptor(cd); //check all constraints on zip code
        }
    }

    public void processConstraintDescriptor(ConstraintDescriptor cd) {
        //Size.class is understood by the tool
        if (cd.getAnnotation().annotationType().equals(Size.class)) {
            Size m = (Size) cd.getAnnotation();
//            System.out.println("size.max = " + m.max());  //read and use the metadata
        }
        for (Object composingCd : cd.getComposingConstraints()) {
            processConstraintDescriptor((ConstraintDescriptor) composingCd);
            //check composing constraints recursively
        }
    }

    public void testValidateComposed() {
        FrenchAddress adr = new FrenchAddress();
        Validator val = factory.getValidator();
        Set<ConstraintViolation<FrenchAddress>> findings = val.validate(adr);
        Assert.assertEquals(1, findings.size()); // with @ReportAsSingleConstraintViolation

        ConstraintViolation<FrenchAddress> finding = findings.iterator().next();
        Assert.assertEquals("Wrong zipcode", finding.getMessage());

        adr.setZipCode("12345");
        findings = val.validate(adr);
        Assert.assertEquals(0, findings.size());

        adr.setZipCode("1234567234567");
        findings = val.validate(adr);
        Assert.assertTrue(findings.size() > 0); // too long
    }

    public void testOverridesAttributeConstraintIndex() {
        AgimatecAddress adr = new AgimatecAddress("invalid-string");
        Validator val = factory.getValidator();
        Set<ConstraintViolation<AgimatecAddress>> findings = val.validate(adr);
        assertEquals(2, findings.size()); // without @ReportAsSingleConstraintViolation
        assertNotNull(TestUtils.getViolationWithMessage(findings, "Not Agimatec"));
        assertNotNull(TestUtils.getViolationWithMessage(findings, "Not an email"));

        adr =  new AgimatecAddress("ROMAN@GMX.DE");
        findings = val.validate(adr);
        assertEquals(1, findings.size());
        assertNotNull(TestUtils.getViolationWithMessage(findings, "Not Agimatec"));

        adr =  new AgimatecAddress("ROMAN@AGIMATEC.DE");
        findings = val.validate(adr);
        Assert.assertTrue(findings.isEmpty());
    }

}
