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
import java.lang.annotation.Annotation;

import static java.lang.Character.getNumericValue;
import static java.lang.Character.isDigit;

/**
 * Abstract <b>Modulus</b> Check digit calculation/validation.
 * <p>
 * Provides a <i>base</i> class for building <i>modulus</i> Check
 * Digit routines.
 * <p>
 * This implementation only handles <i>numeric</i> codes, such as
 * <b>EAN-13</b>. For <i>alphanumeric</i> codes such as <b>EAN-128</b> you
 * will need to implement/override the <code>toInt()</code> and
 * <code>toChar()</code> methods.
 *
 * @param <A>
 */
abstract class ModulusValidator<A extends Annotation> implements ConstraintValidator<A, CharSequence> {

    private final int modulus;

    public ModulusValidator(int modulus) {
        this.modulus = modulus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void initialize(A annotation) {
        // not needed ATM
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(CharSequence code, ConstraintValidatorContext context) {
        if (code.length() == 0) {
            return false;
        }
        int total = 0;
        for (int i = 0; i < code.length(); i++) {
            int lth = code.length();
            int leftPos = i + 1;
            int rightPos = lth - i;
            try {
                int charValue = toInt(code.charAt(i), leftPos, rightPos);
                total += weightedValue(charValue, leftPos, rightPos);
            } catch (Throwable e) {
                return false;
            }
        }
        if (total == 0) {
            return false;
        }
        return (total % modulus) == 0;
    }

    /**
     * Calculates the <i>weighted</i> value of a character in the
     * code at a specified position.
     * <p>
     * Some modulus routines weight the value of a character
     * depending on its position in the code (e.g. ISBN-10), while
     * others use different weighting factors for odd/even positions
     * (e.g. EAN or Luhn). Implement the appropriate mechanism
     * required by overriding this method.
     *
     * @param charValue The numeric value of the character
     * @param leftPos The position of the character in the code, counting from left to right
     * @param rightPos The position of the character in the code, counting from right to left
     * @return The weighted value of the character
     */
    protected abstract int weightedValue(int charValue, int leftPos, int rightPos) throws Exception;

    /**
     * Convert a character at a specified position to an integer value.
     * <p>
     * <b>Note:</b> this implementation only handlers numeric values
     * For non-numeric characters, override this method to provide
     * character-->integer conversion.
     *
     * @param character The character to convert
     * @param leftPos The position of the character in the code, counting from left to right
     * @param rightPos The positionof the character in the code, counting from right to left
     * @return The integer value of the character
     */
    protected int toInt(char character, int leftPos, int rightPos) {
        if (isDigit(character)) {
            return getNumericValue(character);
        }
        throw new IllegalArgumentException("Invalid Character[" + leftPos + "] = '" + character + "'");
    }

    /**
     * Add together the individual digits in a number.
     *
     * @param number The number whose digits are to be added
     * @return The sum of the digits
     */
    protected static int sumDigits(int number) {
        int total = 0;
        int todo = number;
        while (todo > 0) {
            total += todo % 10;
            todo = todo / 10;
        }
        return total;
    }

}
