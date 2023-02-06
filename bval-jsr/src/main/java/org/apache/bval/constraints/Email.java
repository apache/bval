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

import jakarta.validation.Constraint;
import jakarta.validation.OverridesAttribute;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * --
 * NOTE - This constraint predates the equivalent version from the bean_validation spec.
 * --
 * </p>
 * Description: annotation to validate an email address (by pattern)<br/>
 */
@Deprecated
@Documented
@Constraint(validatedBy = {})
@jakarta.validation.constraints.Email
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
public @interface Email {
    @OverridesAttribute(constraint = jakarta.validation.constraints.Email.class, name = "groups")
    Class<?>[] groups() default {};

    @OverridesAttribute(constraint = jakarta.validation.constraints.Email.class, name = "message")
    String message() default "{org.apache.bval.constraints.Email.message}";

    @OverridesAttribute(constraint = jakarta.validation.constraints.Email.class, name = "payload")
    Class<? extends Payload>[] payload() default {};
}
