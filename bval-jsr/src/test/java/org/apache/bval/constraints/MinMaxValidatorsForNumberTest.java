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

import static org.junit.Assert.assertFalse;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.junit.Test;

/**
 * Check correct behaviour of {@link MinValidatorForNumber} and
 * {@link MaxValidatorForNumber} on boundary values.
 * <p/>
 * The chosen numbers: 9223372036854775806l and 9223372036854775807l cast to the
 * same double value.
 * 
 * @author Carlos Vara
 */
public class MinMaxValidatorsForNumberTest {

    @Min(value = 9223372036854775807l)
    public long min;

    @Max(value = 9223372036854775806l)
    public long max;

    @Test
    public void testMinBoundaryValue() {
        Validator v = Validation.buildDefaultValidatorFactory().getValidator();

        this.min = 9223372036854775806l;
        this.max = 0l;

        // Current min value is smaller, should fail, but it doesn't
        Set<ConstraintViolation<MinMaxValidatorsForNumberTest>> res = v.validate(this);
        assertFalse("Min validation failed", res.isEmpty());
    }

    @Test
    public void testMaxBoundaryValue() {
        Validator v = Validation.buildDefaultValidatorFactory().getValidator();

        this.min = Long.MAX_VALUE;
        this.max = 9223372036854775807l;

        // Current max value is bigger, should fail, but it doesn't
        Set<ConstraintViolation<MinMaxValidatorsForNumberTest>> res = v.validate(this);
        assertFalse("Max validation failed", res.isEmpty());
    }

}
