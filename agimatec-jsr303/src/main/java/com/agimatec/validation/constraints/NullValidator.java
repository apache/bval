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
import javax.validation.constraints.Null;

/**
 * Description: valid when object is null<br/>
 * User: roman <br/>
 * Date: 03.02.2009 <br/>
 * Time: 12:49:08 <br/>
 * Copyright: Agimatec GmbH
 */
public class NullValidator implements ConstraintValidator<Null, Object> {

    public void initialize(Null annotation) {
        // do nothing
    }

    public boolean isValid(Object object, ConstraintValidatorContext context) {
        return object == null;
    }
}
