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
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;

import org.junit.Test;

/**
 * Description: <br>
 * User: roman.stumm<br>
 * Date: 06.04.2010<br>
 * Time: 13:45:09<br>
 */
public class DecimalMinMaxValidatorsTest {

    @DecimalMin("922392239223.06")
    public double dmin;
    @DecimalMax("922392239223.09")
    public double dmax;

    @Test
    public void testDecimalMinValue() {
        Validator v = Validation.buildDefaultValidatorFactory().getValidator();

        this.dmin = 922392239223.05;
        this.dmax = 922392239223.08;

        Set<ConstraintViolation<DecimalMinMaxValidatorsTest>> res = v.validate(this);
        assertFalse("Min validation failed", res.isEmpty());
    }

    @Test
    public void testDecimalMaxValue() {
        Validator v = Validation.buildDefaultValidatorFactory().getValidator();

        this.dmin = Double.MAX_VALUE;
        this.dmax = 922392239223.1;

        Set<ConstraintViolation<DecimalMinMaxValidatorsTest>> res = v.validate(this);
        assertFalse("Max validation failed", res.isEmpty());
    }

}
