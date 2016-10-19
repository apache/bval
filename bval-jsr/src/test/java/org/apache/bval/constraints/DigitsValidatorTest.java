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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.junit.Test;

/**
 * DigitsConstraintValidator Tester.
 *
 * @author <Authors name>
 * @since <pre>02/03/2009</pre>
 * @version 1.0
 */
public class DigitsValidatorTest  {
    @Test
    public void testValidateNumber() {
        DigitsValidatorForNumber validator = new DigitsValidatorForNumber();
        validator.setFractional(4);
        validator.setIntegral(2);
        assertFalse(validator.isValid(new BigDecimal("100.1234"), null));
        assertFalse(validator.isValid(new BigDecimal("99.12345"), null));
        assertTrue(validator.isValid(new BigDecimal("99.1234"), null));
        assertFalse(validator.isValid(Double.valueOf(100.1234), null));
        assertFalse(validator.isValid(Double.valueOf(99.12345), null));
        assertTrue(validator.isValid(Double.valueOf(99.1234), null));
        assertTrue(validator.isValid(Double.valueOf(99.123400), null));
        assertTrue(validator.isValid(new BigDecimal("99.123400"), null));
    }

    @Test
    public void testValidateString() {
        DigitsValidatorForString validator = new DigitsValidatorForString();
        validator.setFractional(4);
        validator.setIntegral(2);
        assertFalse(validator.isValid("100.12345", null));
        assertTrue(validator.isValid("99.1234", null));
    }

    @Test
    public void testValidateNumber2() {
        DigitsValidatorForNumber validator = new DigitsValidatorForNumber();
        validator.setFractional(4);
        validator.setIntegral(2);
        assertFalse(validator.isValid(Long.valueOf("100"), null));
        assertTrue(validator.isValid(Long.valueOf("99"), null));
    }

    @Test
    public void testValidateString2() {
        DigitsValidatorForString validator = new DigitsValidatorForString();
        validator.setFractional(0);
        validator.setIntegral(2);
        assertFalse(validator.isValid("99.5", null));
        assertTrue(validator.isValid("99", null));
    }

}
