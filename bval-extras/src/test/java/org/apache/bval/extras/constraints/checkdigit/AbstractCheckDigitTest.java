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
package org.apache.bval.extras.constraints.checkdigit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintValidator;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public abstract class AbstractCheckDigitTest {

    /** Check digit routine being tested */
    private int checkDigitLth;

    /** Check digit routine being tested */
    private ConstraintValidator<? extends Annotation, String> routine;

    /** Array of valid code values */
    private String[] valid;

    /** Array of invalid code values */
    private String[] invalid;

    /** code value which sums to zero */
    private String zeroSum;

    /** Prefix for error messages */
    private String missingMessage;

    public int getCheckDigitLth() {
        return 1;
    }

    protected abstract ConstraintValidator<? extends Annotation, String> getConstraint();

    protected abstract String[] getValid();

    protected String[] getInvalid() {
        return new String[] { "12345678A" };
    }

    protected String getZeroSum() {
        return "0000000000";
    }

    public String getMissingMessage() {
        return "Code is missing";
    }

    @Before
    public void setUp() {
        checkDigitLth = getCheckDigitLth();
        routine = getConstraint();
        valid = getValid();
        invalid = getInvalid();
        zeroSum = getZeroSum();
        missingMessage = getMissingMessage();
    }

    /**
     * Tear Down - clears routine and valid codes.
     */
    @After
    public void tearDown() {
        valid = null;
        routine = null;
    }

    /**
     * Test isValid() for valid values.
     */
    @Test
    public void testIsValidTrue() {
        // test valid values
        for (int i = 0; i < valid.length; i++) {
            assertTrue("valid[" + i + "]: " + valid[i], routine.isValid(valid[i], null));
        }
    }

    /**
     * Test isValid() for invalid values.
     */
    @Test
    public void testIsValidFalse() {
        // test invalid code values
        for (int i = 0; i < invalid.length; i++) {
            assertFalse("invalid[" + i + "]: " + invalid[i], routine.isValid(invalid[i], null));
        }

        // test invalid check digit values
        String[] invalidCheckDigits = createInvalidCodes(valid);
        for (int i = 0; i < invalidCheckDigits.length; i++) {
            assertFalse("invalid check digit[" + i + "]: " + invalidCheckDigits[i],
                routine.isValid(invalidCheckDigits[i], null));
        }
    }

    /**
     * Test missing code
     */
    @Test
    public void testMissingCode() {
        // isValid() zero length
        assertFalse("isValid() Zero Length", routine.isValid("", null));
    }

    /**
     * Test zero sum
     */
    @Test
    public void testZeroSum() {
        assertFalse("isValid() Zero Sum", routine.isValid(zeroSum, null));
    }

    /**
     * Returns an array of codes with invalid check digits.
     *
     * @param codes Codes with valid check digits
     * @return Codes with invalid check digits
     */
    protected String[] createInvalidCodes(String[] codes) {
        List<String> list = new ArrayList<String>();

        // create invalid check digit values
        for (int i = 0; i < codes.length; i++) {
            String code = removeCheckDigit(codes[i]);
            String check = checkDigit(codes[i]);
            for (int j = 0; j < 10; j++) {
                String curr = "" + Character.forDigit(j, 10);
                if (!curr.equals(check)) {
                    list.add(code + curr);
                }
            }
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Returns a code with the Check Digit (i.e. last character) removed.
     *
     * @param code The code
     * @return The code without the check digit
     */
    protected String removeCheckDigit(String code) {
        if (code == null || code.length() <= checkDigitLth) {
            return null;
        }
        return code.substring(0, code.length() - checkDigitLth);
    }

    /**
     * Returns the check digit (i.e. last character) for a code.
     *
     * @param code The code
     * @return The check digit
     */
    protected String checkDigit(String code) {
        if (code == null || code.length() <= checkDigitLth) {
            return "";
        }
        int start = code.length() - checkDigitLth;
        return code.substring(start);
    }

}
