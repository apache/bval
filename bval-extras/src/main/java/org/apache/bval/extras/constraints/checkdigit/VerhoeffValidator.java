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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * <b>Verhoeff</b> (Dihedral) Check Digit calculation/validation.
 * <p>
 * Check digit calculation for numeric codes using a
 * <a href="http://en.wikipedia.org/wiki/Dihedral_group">Dihedral Group</a>
 * of order 10.
 * <p>
 * See <a href="http://en.wikipedia.org/wiki/Verhoeff_algorithm">Wikipedia
 *  - Verhoeff algorithm</a> for more details.
 */
public final class VerhoeffValidator
    implements ConstraintValidator<Verhoeff, String> {

    /** D - multiplication table */
    private static final int[][] D_TABLE = new int[][] {
        {0,  1,  2,  3,  4,  5,  6,  7,  8,  9},
        {1,  2,  3,  4,  0,  6,  7,  8,  9,  5},
        {2,  3,  4,  0,  1,  7,  8,  9,  5,  6},
        {3,  4,  0,  1,  2,  8,  9,  5,  6,  7},
        {4,  0,  1,  2,  3,  9,  5,  6,  7,  8},
        {5,  9,  8,  7,  6,  0,  4,  3,  2,  1},
        {6,  5,  9,  8,  7,  1,  0,  4,  3,  2},
        {7,  6,  5,  9,  8,  2,  1,  0,  4,  3},
        {8,  7,  6,  5,  9,  3,  2,  1,  0,  4},
        {9,  8,  7,  6,  5,  4,  3,  2,  1,  0}};

    /** P - permutation table */
    private static final int[][] P_TABLE = new int[][] {
        {0,  1,  2,  3,  4,  5,  6,  7,  8,  9},
        {1,  5,  7,  6,  2,  8,  3,  0,  9,  4},
        {5,  8,  0,  3,  7,  9,  6,  1,  4,  2},
        {8,  9,  1,  6,  0,  4,  3,  5,  2,  7},
        {9,  4,  5,  3,  1,  2,  6,  8,  7,  0},
        {4,  2,  8,  6,  5,  7,  3,  9,  0,  1},
        {2,  7,  9,  3,  8,  0,  6,  4,  1,  5},
        {7,  0,  4,  6,  9,  1,  3,  2,  5,  8}};

    /**
     * {@inheritDoc}
     */
    public boolean isValid(String code, ConstraintValidatorContext context) {
        if (code.length() == 0) {
            return false;
        }

        int checksum = 0;
        for (int i = 0; i < code.length(); i++) {
            int idx = code.length() - (i + 1);
            int num = getNumericValue(code.charAt(idx));
            if (num < 0 || num > 9) {
                return false;
            }
            checksum = D_TABLE[checksum][P_TABLE[i % 8][num]];
        }
        return checksum == 0;
    }

    /**
     * {@inheritDoc}
     */
    public void initialize(Verhoeff iban) {
        // not needed
    }

}
