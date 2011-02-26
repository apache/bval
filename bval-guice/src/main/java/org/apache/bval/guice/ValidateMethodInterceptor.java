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

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.bval.jsr303.extensions.MethodValidator;

/**
 * Method interceptor for {@link Validate} annotation.
 *
 * @version $Id$
 */
public final class ValidateMethodInterceptor implements MethodInterceptor {

    private static final Class<?>[] CAUSE_TYPES = new Class[]{ Throwable.class };

    private static final Class<?>[] MESSAGE_CAUSE_TYPES = new Class[]{ String.class, Throwable.class };

    /**
     * The {@link ValidatorFactory} reference.
     */
    @Inject
    private ValidatorFactory validatorFactory;

    /**
     * Sets the {@link ValidatorFactory} reference.
     *
     * @param validatorFactory the {@link ValidatorFactory} reference
     */
    public void setValidatorFactory(ValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    /**
     * {@inheritDoc}
     */
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Validate validate = invocation.getMethod().getAnnotation(Validate.class);

        Validator validator = this.validatorFactory.getValidator();
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
            throw getException(new ConstraintViolationException(
                    String.format("Validation error when calling method '%s' with arguments ",
                            method,
                            Arrays.deepToString(arguments)),
                    constraintViolations),
                    validate.rethrowExceptionsAs(),
                    validate.exceptionMessage(),
                    arguments);
        }

        Object returnedValue = invocation.proceed();

        if (validate.validateReturnedValue()) {
            constraintViolations.addAll(methodValidator.validateReturnedValue(clazz, method, returnedValue, groups));

            if (!constraintViolations.isEmpty()) {
                throw getException(new ConstraintViolationException(
                        String.format("Method '%s' returned a not valid value ",
                                method,
                                returnedValue),
                        constraintViolations),
                        validate.rethrowExceptionsAs(),
                        validate.exceptionMessage(),
                        arguments);
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
            Class<? extends Throwable> exceptionWrapperClass,
            String exceptionMessage,
            Object[] arguments) {
        // check the thrown exception is of same re-throw type
        if (exceptionWrapperClass == ConstraintViolationException.class) {
            return exception;
        }

        // re-throw the exception as new exception
        Throwable rethrowEx = null;

        String errorMessage;
        Object[] initargs;
        Class<?>[] initargsType;

        if (exceptionMessage.length() != 0) {
            errorMessage = String.format(exceptionMessage, arguments);
            initargs = new Object[]{ errorMessage, exception };
            initargsType = MESSAGE_CAUSE_TYPES;
        } else {
            initargs = new Object[]{ exception };
            initargsType = CAUSE_TYPES;
        }

        Constructor<? extends Throwable> exceptionConstructor = getMatchingConstructor(exceptionWrapperClass, initargsType);
        if (exceptionConstructor != null) {
            try {
                rethrowEx = exceptionConstructor.newInstance(initargs);
            } catch (Exception e) {
                errorMessage = String.format("Impossible to re-throw '%s', it needs the constructor with %s argument(s).",
                        exceptionWrapperClass.getName(),
                        Arrays.toString(initargsType));
                rethrowEx = new RuntimeException(errorMessage, e);
            }
        } else {
            errorMessage = String.format("Impossible to re-throw '%s', it needs the constructor with %s or %s argument(s).",
                    exceptionWrapperClass.getName(),
                    Arrays.toString(CAUSE_TYPES),
                    Arrays.toString(MESSAGE_CAUSE_TYPES));
            rethrowEx = new RuntimeException(errorMessage);
        }

        return rethrowEx;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> Constructor<E> getMatchingConstructor(Class<E> type,
            Class<?>[] argumentsType) {
        Class<? super E> currentType = type;
        while (Object.class != currentType) {
            for (Constructor<?> constructor : currentType.getConstructors()) {
                if (Arrays.equals(argumentsType, constructor.getParameterTypes())) {
                    return (Constructor<E>) constructor;
                }
            }
            currentType = currentType.getSuperclass();
        }
        return null;
    }

}
