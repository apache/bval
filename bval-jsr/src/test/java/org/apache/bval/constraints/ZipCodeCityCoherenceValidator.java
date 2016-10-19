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


import org.apache.bval.jsr.example.ZipCodeCityCarrier;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Description: Class not implemented! simple dummy implemenation for tests only! <br/>
 * User: roman.stumm <br/>
 * Date: 01.04.2008 <br/>
 * Time: 11:45:22 <br/>
 */
public class ZipCodeCityCoherenceValidator
      implements ConstraintValidator<ZipCodeCityCoherence, ZipCodeCityCarrier> {
    @Override
    public void initialize(ZipCodeCityCoherence constraintAnnotation) {
    }

    @Override
    public boolean isValid(ZipCodeCityCarrier adr, ConstraintValidatorContext context) {
        boolean r = true;
        if ("error".equals(adr.getZipCode())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("zipcode not OK").addConstraintViolation();
            r = false;
        }
        if ("error".equals(adr.getCity())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("city not OK").addPropertyNode("city").addConstraintViolation();
            r = false;
        }
        return r;
    }
}
