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

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.DecimalMax;

public abstract class DecimalMaxValidator<T> implements ConstraintValidator<DecimalMax, T> {
    public static class ForString extends DecimalMaxValidator<String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || isValid(new BigDecimal(value));
        }
    }

    public static class ForNumber extends DecimalMaxValidator<Number> {
        @Override
        public boolean isValid(Number value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            final BigDecimal bigValue;
            if (value instanceof BigDecimal) {
                bigValue = (BigDecimal) value;
            } else if (value instanceof BigInteger) {
                bigValue = new BigDecimal((BigInteger) value);
            } else {
                bigValue = new BigDecimal(value.doubleValue());
            }
            return isValid(bigValue);
        }
    }

    private BigDecimal maxValue;
    private boolean inclusive;

    @Override
    public void initialize(DecimalMax annotation) {
        try {
            this.maxValue = new BigDecimal(annotation.value());
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(annotation.value() + " does not represent a valid BigDecimal format");
        }
        this.inclusive = annotation.inclusive();
    }

    protected boolean isValid(BigDecimal value) {
        // null values are valid
        if (value == null) {
            return true;
        }
        final int comparison = value.compareTo(maxValue);
        return comparison < 0 || inclusive && comparison == 0;
    }
}
