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
package com.agimatec.validation.constraints;

import org.apache.commons.lang.ArrayUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Description: <br/>
 * User: roman <br/>
 * Date: 29.10.2009 <br/>
 * Time: 14:41:07 <br/>
 * Copyright: Agimatec GmbH
 */
public class HasStringValidator implements ConstraintValidator<HasValue, String> {
    private String[] values;

    public void initialize(HasValue stringValues) {
        values = stringValues.value();
    }

    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return s == null || ArrayUtils.contains(values, s);
    }
}
