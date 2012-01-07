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

/**
 * Modulus 10 <b>ABA Number</b> (or <b>Routing Transit Number</b> (RTN)) Check Digit
 * calculation/validation.
 * <p>
 * ABA Numbers (or Routing Transit Numbers) are a nine digit numeric code used
 * to identify American financial institutions for things such as checks or deposits
 * (ABA stands for the American Bankers Association).
 * <p>
 * Check digit calculation is based on <i>modulus 10</i> with digits being weighted
 * based on their position (from right to left) as follows:
 * <ul>
 *     <li>Digits 1, 4 and & 7 are weighted 1
 *     <li>Digits 2, 5 and & 8 are weighted 7
 *     <li>Digits 3, 6 and & 9 are weighted 3
 * </ul>
 * <p>
 * For further information see
 *  <a href="http://en.wikipedia.org/wiki/Routing_transit_number">Wikipedia -
 *  Routing transit number</a>.
 */
public final class ABANumberValidator
    extends ModulusValidator<ABANumber> {

    /** weighting given to digits depending on their right position */
    private static final int[] POSITION_WEIGHT = new int[] {3, 1, 7};

    public ABANumberValidator() {
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
    protected int weightedValue( int charValue, int leftPos, int rightPos )
            throws Exception {
        int weight = POSITION_WEIGHT[rightPos % 3];
        return (charValue * weight);
    }

}
