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

import java.math.BigDecimal;

/**
 * DigitsConstraintValidator Tester.
 *
 * @author <Authors name>
 * @since <pre>02/03/2009</pre>
 * @version 1.0
 */
public class DigitsValidatorTest extends TestCase {
    public DigitsValidatorTest(String name) {
        super(name);
    }

    public void testValidateNumber() {
        DigitsValidatorForNumber validator = new DigitsValidatorForNumber();
        validator.setFractional(4);
        validator.setIntegral(2);
        Assert.assertFalse(validator.isValid(new BigDecimal("100.1234"), null));
        Assert.assertFalse(validator.isValid(new BigDecimal("99.12345"), null));
        Assert.assertTrue(validator.isValid(new BigDecimal("99.1234"), null));
        Assert.assertFalse(validator.isValid(Double.valueOf(100.1234), null));
        Assert.assertFalse(validator.isValid(Double.valueOf(99.12345), null));
        Assert.assertTrue(validator.isValid(Double.valueOf(99.1234), null));
        Assert.assertTrue(validator.isValid(Double.valueOf(99.123400), null));
        Assert.assertTrue(validator.isValid(new BigDecimal("99.123400"), null));
    }

    public void testValidateString() {
        DigitsValidatorForString validator = new DigitsValidatorForString();
        validator.setFractional(4);
        validator.setIntegral(2);
        String val = "100.12345";
        Assert.assertFalse(validator.isValid(val, null));
        val = "99.1234";
        Assert.assertTrue(validator.isValid(val, null));
    }

    public void testValidateNumber2() {
        DigitsValidatorForNumber validator = new DigitsValidatorForNumber();
        validator.setFractional(4);
        validator.setIntegral(2);
        Long val = new Long("100");
        Assert.assertFalse(validator.isValid(val, null));
        val = new Long("99");
        Assert.assertTrue(validator.isValid(val, null));
    }

    public void testValidateString2() {
        DigitsValidatorForString validator = new DigitsValidatorForString();
        validator.setFractional(0);
        validator.setIntegral(2);
        String val = "99.5";
        Assert.assertFalse(validator.isValid(val, null));
        val = "99";
        Assert.assertTrue(validator.isValid(val, null));
    }

    public static Test suite() {
        return new TestSuite(DigitsValidatorTest.class);
    }
}
