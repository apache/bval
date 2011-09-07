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

import java.util.Locale;
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

import org.apache.bval.jsr303.example.CompanyAddress;
import org.apache.bval.jsr303.example.FrenchAddress;
import org.apache.bval.jsr303.util.TestUtils;

/**
 * Description: <br/>
 */
public class ComposedConstraintsTest extends TestCase {
    static ValidatorFactory factory;

    static {
        factory = Validation.buildDefaultValidatorFactory();
        ((DefaultMessageInterpolator) factory.getMessageInterpolator()).setLocale(Locale.ENGLISH);
    }

    /**
     * Validator instance to test
     */
    protected Validator validator;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        validator = createValidator();
    }

    /**
     * Create the validator instance.
     * 
     * @return Validator
     */
    protected Validator createValidator() {
        return factory.getValidator();
    }

    public void testMetaDataAPI_ComposedConstraints() {
        ElementDescriptor ed =
              validator.getConstraintsForClass(FrenchAddress.class)
                    .getConstraintsForProperty("zipCode");
        Assert.assertEquals(1, ed.getConstraintDescriptors().size());
        for (ConstraintDescriptor<?> cd : ed.getConstraintDescriptors()) {
            Assert.assertTrue(cd.isReportAsSingleViolation());
            Assert.assertEquals(3, cd.getComposingConstraints().size());
            Assert.assertTrue("no composing constraints found!!",
                  !cd.getComposingConstraints().isEmpty());
            processConstraintDescriptor(cd); //check all constraints on zip code
        }
    }

    public void processConstraintDescriptor(ConstraintDescriptor<?> cd) {
        //Size.class is understood by the tool
        if (cd.getAnnotation().annotationType().equals(Size.class)) {
            Size m = (Size) cd.getAnnotation();//what for?
        }
        for (ConstraintDescriptor<?> composingCd : cd.getComposingConstraints()) {
            //check composing constraints recursively
            processConstraintDescriptor(composingCd);
        }
    }

    public void testValidateComposed() {
        FrenchAddress adr = new FrenchAddress();
        Set<ConstraintViolation<FrenchAddress>> findings = validator.validate(adr);
        Assert.assertEquals(1, findings.size()); // with @ReportAsSingleConstraintViolation

        ConstraintViolation<FrenchAddress> finding = findings.iterator().next();
        Assert.assertEquals("Wrong zipcode", finding.getMessage());

        adr.setZipCode("1234567");
        findings = validator.validate(adr);
        Assert.assertEquals(0, findings.size());

        adr.setZipCode("1234567234567");
        findings = validator.validate(adr);
        Assert.assertTrue(findings.size() > 0); // too long
    }

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
        Assert.assertTrue(findings.isEmpty());
    }

}
