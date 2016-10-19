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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;

/**
 * Validates that the <code>String</code> being validated consists of digits,
 * and matches the pattern defined in the constraint.
 */
public class DigitsValidatorForString implements ConstraintValidator<Digits, String> {

    private int integral;
    private int fractional;

    public int getIntegral() {
        return integral;
    }

    public void setIntegral(int integral) {
        this.integral = integral;
    }

    public int getFractional() {
        return fractional;
    }

    public void setFractional(int fractional) {
        this.fractional = fractional;
    }

    @Override
    public void initialize(Digits annotation) {
        this.integral = annotation.integer();
        this.fractional = annotation.fraction();
        if (integral < 0) {
            throw new IllegalArgumentException("The length of the integer part cannot be negative.");
        }
        if (fractional < 0) {
            throw new IllegalArgumentException("The length of the fraction part cannot be negative.");
        }
    }

    @Override
    public boolean isValid(String str, ConstraintValidatorContext context) {
        //null values are valid
        if (str == null) {
            return true;
        }

        final BigDecimal bigDecimal = getBigDecimalValue(str);
        if (bigDecimal == null) {
            return false;
        }

        int intLength = bigDecimal.precision() - bigDecimal.scale();
        if (integral >= intLength) {
            int factionLength = bigDecimal.scale() < 0 ? 0 : bigDecimal.scale();
            return fractional >= factionLength;
        }
        return false;
    }

    private BigDecimal getBigDecimalValue(String str) {
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
