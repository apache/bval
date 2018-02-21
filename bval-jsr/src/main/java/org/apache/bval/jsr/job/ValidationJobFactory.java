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
package org.apache.bval.jsr.job;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;

import org.apache.bval.jsr.ApacheFactoryContext;
import org.apache.bval.util.Validate;

/**
 * Creates {@link ValidationJob} instances.
 */
public class ValidationJobFactory {

    private final ApacheFactoryContext validatorContext;

    /**
     * Create a new {@link ValidationJobFactory}.
     * 
     * @param validatorContext
     */
    public ValidationJobFactory(ApacheFactoryContext validatorContext) {
        super();
        this.validatorContext = Validate.notNull(validatorContext, "validatorContext");
    }

    /**
     * @see Validator#validate(Object, Class...)
     */
    public <T> ValidateBean<T> validateBean(T bean, Class<?>... groups) {
        return new ValidateBean<>(validatorContext, bean, groups);
    }

    /**
     * @see Validator#validateProperty(Object, String, Class...)
     */
    public <T> ValidateProperty<T> validateProperty(T bean, String property, Class<?>... groups) {
        try {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            final ValidateProperty<T> result = new ValidateProperty(validatorContext, bean, property, groups);
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException(e);
        }
    }

    /**
     * @see Validator#validateValue(Class, String, Object, Class...)
     */
    public <T> ValidateProperty<T> validateValue(Class<T> rootBeanClass, String property, Object value,
        Class<?>... groups) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final ValidateProperty<T> result =
            new ValidateProperty(validatorContext, rootBeanClass, property, value, groups);
        return result;
    }

    /**
     * @see ExecutableValidator#validateParameters(Object, Method, Object[], Class...)
     */
    public <T> ValidateParameters.ForMethod<T> validateParameters(T object, Method method, Object[] parameterValues,
        Class<?>... groups) {
        return new ValidateParameters.ForMethod<T>(validatorContext, object, method, parameterValues, groups);
    }

    /**
     * @see ExecutableValidator#validateReturnValue(Object, Method, Object, Class...)
     */
    public <T> ValidateReturnValue.ForMethod<T> validateReturnValue(T object, Method method, Object returnValue,
        Class<?>... groups) {
        return new ValidateReturnValue.ForMethod<>(validatorContext, object, method, returnValue, groups);
    }

    /**
     * @see ExecutableValidator#validateConstructorParameters(Constructor, Object[], Class...)
     */
    public <T> ValidateParameters.ForConstructor<T> validateConstructorParameters(Constructor<? extends T> constructor,
        Object[] parameterValues, Class<?>... groups) {
        return new ValidateParameters.ForConstructor<T>(validatorContext, constructor, parameterValues, groups);
    }

    /**
     * @see ExecutableValidator#validateConstructorReturnValue(Constructor, Object, Class...)
     */
    public <T> ValidateReturnValue.ForConstructor<T> validateConstructorReturnValue(
        Constructor<? extends T> constructor, T createdObject, Class<?>... groups) {
        return new ValidateReturnValue.ForConstructor<T>(validatorContext, constructor, createdObject, groups);
    }
}
