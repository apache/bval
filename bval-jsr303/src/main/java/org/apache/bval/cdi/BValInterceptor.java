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
package org.apache.bval.cdi;

import org.apache.bval.jsr303.util.Proxies;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ExecutableValidator;
import javax.validation.executable.ValidateOnExecution;
import javax.validation.metadata.MethodDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Interceptor
@BValBinding
public class BValInterceptor {
    private Collection<ExecutableType> classConfiguration = null;
    private final Map<Method, Boolean> methodConfiguration = new ConcurrentHashMap<Method, Boolean>();

    @Inject
    private Validator validator;

    @Inject
    private BValExtension globalConfiguration;

    @AroundInvoke
    public Object around(final InvocationContext context) throws Throwable {
        final Method method = context.getMethod();
        if (!isMethodValidated(Proxies.classFor(context.getTarget().getClass()), method)) {
            return context.proceed();
        }

        final ExecutableValidator ev = validator.forExecutables();

        final MethodDescriptor constraintsForMethod = validator.getConstraintsForClass(Proxies.classFor(method.getDeclaringClass())).getConstraintsForMethod(method.getName(), method.getParameterTypes());
        if (constraintsForMethod == null) {
            return context.proceed();
        }

        {
            final Set<ConstraintViolation<Object>> violations = ev.validateParameters(context.getTarget(), method, context.getParameters());
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        final Object result = context.proceed();

        {
            final Set<ConstraintViolation<Object>> violations = ev.validateReturnValue(context.getTarget(), method, result);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        return result;
    }

    private boolean isMethodValidated(final Class<?> targetClass, final Method method) throws NoSuchMethodException {
        Boolean methodConfig;// config
        if (classConfiguration == null) {
            synchronized (this) {
                if (classConfiguration == null) {
                    classConfiguration = new CopyOnWriteArraySet<ExecutableType>();

                    final ValidateOnExecution annotation = targetClass.getAnnotation(ValidateOnExecution.class);
                    if (annotation == null) {
                        classConfiguration.addAll(globalConfiguration.getGlobalExecutableTypes());
                    } else {
                        for (final ExecutableType type : annotation.type()) {
                            if (ExecutableType.IMPLICIT.equals(type)) {
                                classConfiguration.add(ExecutableType.CONSTRUCTORS);
                                classConfiguration.add(ExecutableType.NON_GETTER_METHODS);
                            } else if (ExecutableType.ALL.equals(type)) {
                                classConfiguration.add(ExecutableType.CONSTRUCTORS);
                                classConfiguration.add(ExecutableType.NON_GETTER_METHODS);
                                classConfiguration.add(ExecutableType.GETTER_METHODS);
                                break;
                            } else if (!ExecutableType.NONE.equals(type)) {
                                classConfiguration.add(type);
                            }
                        }
                    }
                }
            }
        }

        methodConfig = methodConfiguration.get(method);
        if (methodConfig == null) {
            synchronized (this) {
                methodConfig = methodConfiguration.get(method);
                if (methodConfig == null) {
                    // reuse Proxies to avoid issue with some subclassing libs removing annotations
                    final ValidateOnExecution annotation = targetClass.getMethod(method.getName(), method.getParameterTypes()).getAnnotation(ValidateOnExecution.class);
                    if (annotation == null) {
                        methodConfig = doValidMethod(method, classConfiguration);
                    } else {
                        final Collection<ExecutableType> config = new HashSet<ExecutableType>();
                        for (final ExecutableType type : annotation.type()) {
                            if (ExecutableType.IMPLICIT.equals(type)) { // on method it just means validate, even on getters
                                config.add(ExecutableType.CONSTRUCTORS);
                                config.add(ExecutableType.NON_GETTER_METHODS);
                                config.add(ExecutableType.GETTER_METHODS);
                            } else if (ExecutableType.ALL.equals(type)) {
                                config.add(ExecutableType.CONSTRUCTORS);
                                config.add(ExecutableType.NON_GETTER_METHODS);
                                config.add(ExecutableType.GETTER_METHODS);
                                break;
                            } else if (!ExecutableType.NONE.equals(type)) {
                                config.add(type);
                            }
                        }
                        methodConfig = doValidMethod(method, config);
                    }
                }
                methodConfiguration.put(method, methodConfig);
            }
        }

        return methodConfig;
    }

    private boolean doValidMethod(final Method method, final Collection<ExecutableType> config) {
        final boolean getter = isGetter(method);
        return (!getter && config.contains(ExecutableType.NON_GETTER_METHODS)) || (getter && config.contains(ExecutableType.GETTER_METHODS));
    }

    private static boolean isGetter(final Method method) {
        final String name = method.getName();
        return (name.startsWith("get") || name.startsWith("is")) && method.getReturnType() != Void.TYPE && method.getParameterTypes().length == 0;
    }
}
