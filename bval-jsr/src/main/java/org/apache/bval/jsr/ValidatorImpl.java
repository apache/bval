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
package org.apache.bval.jsr;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;

import org.apache.bval.jsr.job.ValidationJobFactory;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;

public class ValidatorImpl implements CascadingPropertyValidator, ExecutableValidator {

    private final ApacheFactoryContext validatorContext;
    private final ValidationJobFactory validationJobFactory;

    ValidatorImpl(ApacheFactoryContext validatorContext) {
        super();
        this.validatorContext = Validate.notNull(validatorContext, "validatorContext");
        this.validationJobFactory = new ValidationJobFactory(validatorContext);
    }

    @Override
    public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
        return validatorContext.getDescriptorManager().getBeanDescriptor(clazz);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        return validationJobFactory.validateBean(object, groups).getResults();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, boolean cascade,
        Class<?>... groups) {
        return validationJobFactory.validateProperty(object, propertyName, groups).cascade(cascade).getResults();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value,
        boolean cascade, Class<?>... groups) {
        return validationJobFactory.validateValue(beanType, propertyName, value, groups).cascade(cascade).getResults();
    }

    @Override
    public ExecutableValidator forExecutables() {
        return this;
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateParameters(T object, Method method, Object[] parameterValues,
        Class<?>... groups) {
        return validationJobFactory.validateParameters(object, method, parameterValues, groups).getResults();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateReturnValue(T object, Method method, Object returnValue,
        Class<?>... groups) {
        return validationJobFactory.validateReturnValue(object, method, returnValue, groups).getResults();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateConstructorParameters(Constructor<? extends T> constructor,
        Object[] parameterValues, Class<?>... groups) {
        return validationJobFactory.<T> validateConstructorParameters(constructor, parameterValues, groups)
            .getResults();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateConstructorReturnValue(Constructor<? extends T> constructor,
        T createdObject, Class<?>... groups) {
        return validationJobFactory.<T> validateConstructorReturnValue(constructor, createdObject, groups).getResults();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        // FIXME 2011-03-27 jw:
        // This code is unsecure.
        // It should allow only a fixed set of classes.
        // Can't fix this because don't know which classes this method should support.

        if (type.isAssignableFrom(getClass())) {
            @SuppressWarnings("unchecked")
            final T result = (T) this;
            return result;
        }
        if (!(type.isInterface() || Modifier.isAbstract(type.getModifiers()))) {
            return newInstance(type);
        }
        try {
            final Class<?> cls = Reflection.toClass(type.getName() + "Impl");
            if (type.isAssignableFrom(cls)) {
                @SuppressWarnings("unchecked")
                final Class<? extends T> implClass = (Class<? extends T>) cls;
                return newInstance(implClass);
            }
        } catch (ClassNotFoundException e) {
        }
        throw new ValidationException("Type " + type + " not supported");
    }

    private <T> T newInstance(final Class<T> cls) {
        final Constructor<T> cons = Reflection.getDeclaredConstructor(cls, ApacheFactoryContext.class);
        if (cons == null) {
            throw new ValidationException("Cannot instantiate " + cls);
        }
        Reflection.makeAccessible(cons);
        try {
            return cons.newInstance(validatorContext);
        } catch (final Exception ex) {
            throw new ValidationException("Cannot instantiate " + cls, ex);
        }
    }
}
