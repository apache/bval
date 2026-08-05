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

import static org.junit.Assert.assertEquals;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.apache.bval.jsr.ValidationTestBase;
import org.junit.Test;

/**
 * {@code @Min}, {@code @Max}, {@code @DecimalMin} and {@code @DecimalMax} support any
 * {@link CharSequence}, not only {@link String}.
 */
public class MinMaxValidatorsForCharSequenceTest extends ValidationTestBase {

    public static class Bean {
        @Min(10)
        @Max(20)
        @DecimalMin("10.5")
        @DecimalMax("19.5")
        private final CharSequence value;

        Bean(CharSequence value) {
            this.value = value;
        }
    }

    @Test
    public void testValidCharSequence() {
        assertEquals(0, validator.validate(new Bean(new StringBuilder("15"))).size());
    }

    @Test
    public void testBelowMin() {
        assertEquals(2, validator.validate(new Bean(new StringBuilder("9"))).size());
    }

    @Test
    public void testAboveMax() {
        assertEquals(2, validator.validate(new Bean(new StringBuilder("21"))).size());
    }

    @Test
    public void testNullIsValid() {
        assertEquals(0, validator.validate(new Bean(null)).size());
    }

    @Test
    public void testNotANumber() {
        assertEquals(4, validator.validate(new Bean(new StringBuilder("foo"))).size());
    }
}
