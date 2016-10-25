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
package org.apache.bval.extras.constraints.checkdigit;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static java.lang.Character.getNumericValue;

/**
 * <b>IBAN</b> (International Bank Account Number) Check Digit calculation/validation.
 * <p>
 * This rountine is based on the ISO 7064 Mod 97,10 check digit caluclation routine.
 * <p>
 * The two check digit characters in a IBAN number are the third and fourth characters
 * in the code. For <i>check digit</i> calculation/validation the first four characters are moved
 * to the end of the code.
 *  So <code>CCDDnnnnnnn</code> becomes <code>nnnnnnnCCDD</code> (where
 *  <code>CC</code> is the country code and <code>DD</code> is the check digit). For
 *  check digit calcualtion the check digit value should be set to zero (i.e.
 *  <code>CC00nnnnnnn</code> in this example.
 * <p>
 * For further information see
 *  <a href="http://en.wikipedia.org/wiki/International_Bank_Account_Number">Wikipedia -
 *  IBAN number</a>.
 */
public final class IBANValidator implements ConstraintValidator<IBAN, String> {

    private static final long MAX = 999999999;

    private static final long MODULUS = 97;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(String code, ConstraintValidatorContext context) {
        if (code.length() < 5) {
            return false;
        }

        String reformattedCode = code.substring(4) + code.substring(0, 4);
        long total = 0;
        for (int i = 0; i < reformattedCode.length(); i++) {
            int charValue = getNumericValue(reformattedCode.charAt(i));
            if (charValue < 0 || charValue > 35) {
                return false;
            }
            total = (charValue > 9 ? total * 100 : total * 10) + charValue;
            if (total > MAX) {
                total = (total % MODULUS);
            }
        }

        return (total % MODULUS) == 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(IBAN iban) {
        // not needed
    }

}
