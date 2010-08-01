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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.bval.jsr303.extensions.MethodValidator;

import com.google.inject.Inject;

/**
 * Method interceptor for {@link Validate} annotation.
 *
 * @version $Id$
 */
public final class ValidateMethodInterceptor implements MethodInterceptor {

    /**
     * The {@link ValidatorProvider} reference.
     */
    @Inject
    private ValidatorProvider validatorProvider;

    /**
     * Sets the {@link ValidatorProvider} reference.
     *
     * @param validatorProvider the {@link ValidatorProvider} reference.
     */
    public void setValidatorProvider(ValidatorProvider validatorProvider) {
        this.validatorProvider = validatorProvider;
    }

    /**
     * {@inheritDoc}
     */
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Validate validate = invocation.getMethod().getAnnotation(Validate.class);

        Validator validator = this.validatorProvider.get();
        MethodValidator methodValidator = validator.unwrap(MethodValidator.class);

        Set<ConstraintViolation<?>> constraintViolations = new HashSet<ConstraintViolation<?>>();
        Class<?> clazz = invocation.getMethod().getDeclaringClass();
        Method method = invocation.getMethod();
        Object[] arguments = invocation.getArguments();
        Class<?>[] groups = validate.groups();

        constraintViolations.addAll(methodValidator.validateParameters(clazz,
                method,
                arguments,
                groups));

        if (!constraintViolations.isEmpty()) {
            throw getException(new ConstraintViolationException("Validation error when calling method '"
                        + method
                        + "' with arguments "
                        + Arrays.deepToString(arguments), constraintViolations),
                    validate.rethrowExceptionsAs());
        }

        Object returnedValue = invocation.proceed();

        if (validate.validateReturnedValue()) {
            constraintViolations.addAll(methodValidator.validateReturnedValue(clazz, method, returnedValue, groups));
            if (!constraintViolations.isEmpty()) {
                throw getException(new ConstraintViolationException("Method '"
                        + method
                        + "' returned a not valid value "
                        + returnedValue, constraintViolations), validate.rethrowExceptionsAs());
            }
        }

        return returnedValue;
    }

    /**
     * Define the {@link Throwable} has to be thrown when a validation error
     * occurs and the user defined the custom error wrapper.
     *
     * @param exception the occurred validation error.
     * @param exceptionWrapperClass the user defined custom error wrapper.
     * @return the {@link Throwable} has o be thrown.
     */
    private static Throwable getException(ConstraintViolationException exception,
            Class<? extends Throwable> exceptionWrapperClass) {
        // check the thrown exception is of same re-throw type
        if (exceptionWrapperClass == ConstraintViolationException.class) {
            return exception;
        }

        // re-throw the exception as new exception
        Throwable rethrowEx = null;

        try {
            Constructor<? extends Throwable> constructor = exceptionWrapperClass.getConstructor(Throwable.class);
            rethrowEx = constructor.newInstance(exception);
        } catch (Exception e) {
            rethrowEx = new RuntimeException("Impossible to re-throw '"
                    + exceptionWrapperClass
                    + "', it needs the constructor with <Throwable> argument.", e);
        }

        return rethrowEx;
    }

}
