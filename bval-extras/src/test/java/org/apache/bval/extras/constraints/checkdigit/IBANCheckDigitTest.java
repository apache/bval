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

import org.junit.Ignore;
import org.junit.Test;

import javax.validation.ConstraintValidator;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * IVAN Check Digit Test.
 */
public class IBANCheckDigitTest extends AbstractCheckDigitTest {

    @Override
    public int getCheckDigitLth() {
        return 2;
    }

    @Override
    protected ConstraintValidator<? extends Annotation, String> getConstraint() {
        return new IBANValidator();
    }

    @Override
    protected String[] getValid() {
        return new String[] {
            "AD1200012030200359100100",
            "AT611904300234573201",
            "AT611904300234573201",
            "BE68539007547034",
            "BE62510007547061",
            "CY17002001280000001200527600",
            "CZ6508000000192000145399",
            "DK5000400440116243",
            "EE382200221020145685",
            "FI2112345600000785",
            "FR1420041010050500013M02606",
            "DE89370400440532013000",
            "GI75NWBK000000007099453",
            "GR1601101250000000012300695",
            "HU42117730161111101800000000",
            "IS140159260076545510730339",
            "IE29AIBK93115212345678",
            "IT60X0542811101000000123456",
            "LV80BANK0000435195001",
            "LT121000011101001000",
            "LU280019400644750000",
            "NL91ABNA0417164300",
            "NO9386011117947",
            "PL27114020040000300201355387",
            "PT50000201231234567890154",
            "SK3112000000198742637541",
            "SI56191000000123438",
            "ES8023100001180000012345",
            "SE3550000000054910000003",
            "CH3900700115201849173",
            "GB29NWBK60161331926819"
        };
    }

    @Override
    protected String[] getInvalid() {
        return new String[] {"510007+47061BE63"};
    }

    @Override
    protected String getZeroSum() {
        return null;
    }

    @Override
    public String getMissingMessage() {
        return "Invalid Code length=0";
    }

    /**
     * Test zero sum
     */
    @Override
    @Test
    @Ignore
    public void testZeroSum() {
        // ignore, don't run this test
    }

    @Override
    protected String[] createInvalidCodes( String[] codes ) {
        List<String> list = new ArrayList<String>();

        // create invalid check digit values
        for (int i = 0; i < codes.length; i++) {
            String code = removeCheckDigit(codes[i]);
            String check  = checkDigit(codes[i]);
            for (int j = 0; j < 96; j++) {
                String curr =  j > 9 ? "" + j : "0" + j;
                if (!curr.equals(check)) {
                    list.add(code.substring(0, 2) + curr + code.substring(4));
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
    @Override
    protected String removeCheckDigit(String code) {
        return code.substring(0, 2) + "00" + code.substring(4);
    }

    /**
     * Returns the check digit (i.e. last character) for a code.
     *
     * @param code The code
     * @return The check digit
     */
    @Override
    protected String checkDigit(String code) {
        if (code == null || code.length() <= getCheckDigitLth()) {
            return "";
        }
       return code.substring(2, 4);
    }

}
