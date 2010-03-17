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
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * <p>
 * --
 * This class is NOT part of the bean_validation spec and might disappear
 * as soon as a final version of the specification contains a similar functionality.
 * --
 * </p>
 * Description: annotation to validate an email address (by pattern)<br/>
 * User: roman.stumm <br/>
 * Date: 14.10.2008 <br/>
 * Time: 12:38:10 <br/>
 * Copyright: Agimatec GmbH
 */
@Documented
@Constraint(validatedBy = EmailValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, PARAMETER})
@Retention(RUNTIME)
public @interface Email {
    Class<?>[] groups() default {};

    String message() default "{com.agimatec.validation.constraints.Email.message}";

    Class<? extends Payload>[] payload() default {};
}
