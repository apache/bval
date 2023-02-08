/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.bval.jsr;

import jakarta.el.*;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * EL5.0 ExpressionFactory lookups the ServiceLoader before the system property
 * In our tests we have at least 2 EL impls and DefaultMessageInterpolatorTest needs a replaceable ExpressionFactory
 * So this a wrapper, which checks the system property first and then asking the ServiceLoader
 */
public class DelegateExpressionFactory extends ExpressionFactory {

    public DelegateExpressionFactory()  {

    }

    @Override
    public ValueExpression createValueExpression(ELContext context, String expression, Class<?> expectedType) {
        return getWrapped().createValueExpression(context, expression, expectedType);
    }

    @Override
    public ValueExpression createValueExpression(Object instance, Class<?> expectedType) {
        return getWrapped().createValueExpression(instance, expectedType);
    }

    @Override
    public MethodExpression createMethodExpression(ELContext context, String expression, Class<?> expectedReturnType, Class<?>[] expectedParamTypes) {
        return getWrapped().createMethodExpression(context, expression, expectedReturnType, expectedParamTypes);
    }

    @Override
    public <T> T coerceToType(Object obj, Class<T> targetType) {
        return getWrapped().coerceToType(obj, targetType);
    }

    @Override
    public ELResolver getStreamELResolver() {
        return getWrapped().getStreamELResolver();
    }

    @Override
    public Map<String, Method> getInitFunctionMap() {
        return getWrapped().getInitFunctionMap();
    }

    public ExpressionFactory getWrapped()
    {
        String systemProperty = System.getProperty(ExpressionFactory.class.getName());
        if (systemProperty != null)  {
            try {
                return (ExpressionFactory) Class.forName(systemProperty).getConstructor().newInstance();
            } catch (Exception e) {
                return null;
            }
        }

        try {
            ServiceLoader<ExpressionFactory> serviceLoader = ServiceLoader.load(ExpressionFactory.class,
                    Thread.currentThread().getContextClassLoader());
            Iterator<ExpressionFactory> iter = serviceLoader.iterator();
            while (iter.hasNext()) {
                ExpressionFactory service = iter.next();
                if (service != null && service.getClass() != this.getClass()) {
                    return service;
                }
            }
        } catch (Exception ex) {
        }

        return null;
    }
}
