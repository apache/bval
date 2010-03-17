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

import javax.validation.Constraint;
import javax.validation.Payload;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Description: allow distinct string values for element (like enums) <br/>
 * User: roman <br/>
 * Date: 29.10.2009 <br/>
 * Time: 14:28:43 <br/>
 * Copyright: Agimatec GmbH
 */
@Target({ANNOTATION_TYPE, METHOD, FIELD})
@Constraint(validatedBy = {HasStringValidator.class})
@Retention(RUNTIME)
public @interface HasValue {
    String[] value();

    String message() default "Wrong value, must be one of {value}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default { };
}
