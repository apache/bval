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
import javax.validation.OverridesAttribute;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Description: example for composed constraint.
 * not implemented! simple dummy implemenation for tests only! <br/>
 * User: roman.stumm <br/>
 * Date: 31.10.2008 <br/>
 * Time: 16:34:56 <br/>
 * Copyright: Agimatec GmbH
 */
@NotEmpty
@NotNull
@Size(min = 4, max = 5, message = "Zipcode should be of size {value}")
@Constraint(validatedBy = FrenchZipcodeValidator.class)
@ReportAsSingleViolation
@Documented
@Target({ANNOTATION_TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface FrenchZipCode {
    @OverridesAttribute.List({
            @OverridesAttribute(constraint = Size.class, name= "min"),
        @OverridesAttribute(constraint = Size.class, name = "max")})
    int size() default 6;

    @OverridesAttribute(constraint=Size.class, name="message")
    String sizeMessage() default "{error.zipcode.size}";

    String message() default "Wrong zipcode";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default { };
}
