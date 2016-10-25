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

import static java.lang.Character.getNumericValue;

/**
 * Modulus 10 <b>CUSIP</b> (North American Securities)
 * Check Digit calculation/validation.
 * <p>
 * CUSIP Numbers are 9 character alphanumeric codes used
 * to identify North American Securities.
 * <p>
 * Check digit calculation uses the <i>Modulus 10 Double Add Double</i> technique
 * with every second digit being weighted by 2. Alphabetic characters are
 * converted to numbers by their position in the alphabet starting with A being 10.
 * Weighted numbers greater than ten are treated as two separate numbers.
 * <p>
 *
 * <p>
 * See <a href="http://en.wikipedia.org/wiki/CUSIP">Wikipedia - CUSIP</a>
 * for more details.
 */
public final class CUSIPValidator extends ModulusValidator<CUSIP> {

    /** weighting given to digits depending on their right position */
    private static final int[] POSITION_WEIGHT = new int[] { 2, 1 };

    public CUSIPValidator() {
        super(10);
    }

    /**
     * Calculates the <i>weighted</i> value of a character in the
     * code at a specified position.
     * <p>
     * ABA Routing numbers are weighted in the following manner:
     * <pre><code>
     *     left position: 1  2  3  4  5  6  7  8  9
     *            weight: 3  7  1  3  7  1  3  7  1
     * </code></pre>
     *
     * {@inheritDoc}
     */
    @Override
    protected int weightedValue(int charValue, int leftPos, int rightPos) throws Exception {
        int weight = POSITION_WEIGHT[rightPos % 2];
        int weightedValue = (charValue * weight);
        return sumDigits(weightedValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int toInt(char character, int leftPos, int rightPos) {
        int charValue = getNumericValue(character);
        if (charValue < 0 || charValue > 35) {
            throw new IllegalArgumentException("Invalid Character[" + leftPos + "] = '" + charValue + "'");
        }
        return charValue;
    }

}
