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
import javax.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

@Pattern.List({
    // email
    @Pattern(regexp = "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}"),
    // agimatec
    @Pattern(regexp = ".*?AGIMATEC.*?")
})
/**
 * test a constraint WITHOUT an own ConstraintValidator implementation.
 * the validations, that must be processed are in the combined constraints only!!
 * the @Constraint annotation is nevertheless required so that the framework searches
 * for combined constraints.  
 */
@Constraint(validatedBy = {})
@Documented
@Target({ANNOTATION_TYPE, METHOD, FIELD, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
public @interface AgimatecEmail {
    String message() default "Not an agimatec email";

    @OverridesAttribute(constraint = Pattern.class, name = "message",
          constraintIndex = 0) String emailMessage() default "Not an email";

    @OverridesAttribute(constraint = Pattern.class, name = "message",
          constraintIndex = 1) String agimatecMessage() default "Not Agimatec";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
          @interface List {
        AgimatecEmail[] value();
    }
}