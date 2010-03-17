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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Description: <br/>
 * User: roman <br/>
 * Date: 18.11.2009 <br/>
 * Time: 14:03:22 <br/>
 * Copyright: Agimatec GmbH
 */
public class NotEmptyValidatorForString implements ConstraintValidator<NotEmpty, String> {
    public void initialize(NotEmpty constraintAnnotation) {
        // do nothing
    }

    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.length() > 0;
    }
}
