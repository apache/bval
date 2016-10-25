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
 * Modulus 10 <b>SEDOL</b> (UK Securities) Check Digit calculation/validation.
 * <p>
 * SEDOL Numbers are 7 character alphanumeric codes used
 * to identify UK Securities (SEDOL stands for Stock Exchange Daily Official List).
 * <p>
 * Check digit calculation is based on <i>modulus 10</i> with digits being weighted
 * based on their position, from left to right, as follows:
 * <p>
 * <pre><code>
 *      position:  1  2  3  4  5  6  7
 *     weighting:  1  3  1  7  3  9  1
 * </code></pre>
 * <p>
 * See <a href="http://en.wikipedia.org/wiki/SEDOL">Wikipedia - SEDOL</a>
 * for more details.
 */
public final class SedolValidator extends ModulusValidator<Sedol> {

    /** weighting given to digits depending on their right position */
    private static final int[] POSITION_WEIGHT = new int[] { 1, 3, 1, 7, 3, 9, 1 };

    public SedolValidator() {
        super(10);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int weightedValue(int charValue, int leftPos, int rightPos) throws Exception {
        return (charValue * POSITION_WEIGHT[leftPos - 1]);
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
