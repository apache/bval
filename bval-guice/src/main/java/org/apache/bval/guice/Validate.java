/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bval.guice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.ConstraintViolationException;

import com.google.inject.BindingAnnotation;

/**
 * Marker for methods which arguments have to be validated.
 *
 * @version $Id$
 */
@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Validate {

    /**
     * The groups have to be validated, empty by default.
     *
     * @return the groups have to be validated, empty by default.
     */
    Class<?>[] groups() default {};

    /**
     * Marks if the returned object by the intercepted method execution has to
     * be validated, false by default.
     *
     * @return false by default.
     */
    boolean validateReturnedValue() default false;

    /**
     * The exception re-thrown when an error occurs during the validation.
     *
     * @return the exception re-thrown when an error occurs during the
     *         validation.
     */
    Class<? extends Throwable> rethrowExceptionsAs() default ConstraintViolationException.class;

    /**
     * A custom error message when throwing the custom exception.
     *
     * It supports java.util.Formatter place holders, intercepted method
     * arguments will be used as message format arguments.
     *
     * @return a custom error message when throwing the custom exception.
     * @see java.util.Formatter#format(String, Object...)
     */
    String exceptionMessage() default "";

}
