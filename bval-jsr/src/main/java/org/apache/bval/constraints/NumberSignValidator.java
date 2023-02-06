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

import java.lang.annotation.Annotation;
import java.util.function.IntPredicate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.apache.bval.util.Validate;

/**
 * Description: validate positive/negative number values.
 */
public abstract class NumberSignValidator<A extends Annotation> implements ConstraintValidator<A, Number> {
    public static class ForPositive extends NumberSignValidator<Positive> {
        public static class OrZero extends NumberSignValidator<PositiveOrZero> {
            public OrZero() {
                super(n -> n >= 0);
            }
        }

        public ForPositive() {
            super(n -> n > 0);
        }
    }

    public static class ForNegative extends NumberSignValidator<Negative> {
        public static class OrZero extends NumberSignValidator<NegativeOrZero> {
            public OrZero() {
                super(n -> n <= 0);
            }
        }

        public ForNegative() {
            super(n -> n < 0);
        }
    }

    private final IntPredicate comparisonTest;

    protected NumberSignValidator(IntPredicate comparisonTest) {
        super();
        this.comparisonTest = Validate.notNull(comparisonTest);
    }

    @Override
    public boolean isValid(Number value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        final double d = value.doubleValue();
        if (Double.isNaN(d)) {
            return false;
        }
        return comparisonTest.test(Double.compare(d, 0.0));
    }
}
