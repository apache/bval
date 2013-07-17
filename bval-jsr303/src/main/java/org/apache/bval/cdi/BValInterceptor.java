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

import org.apache.bval.jsr303.util.ClassHelper;
import org.apache.bval.jsr303.util.Proxies;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ExecutableValidator;
import javax.validation.executable.ValidateOnExecution;
import javax.validation.metadata.ConstructorDescriptor;
import javax.validation.metadata.MethodDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Interceptor
@BValBinding
@Priority(4800) // TODO: maybe add it through ASM to be compliant with CDI 1.0 containers using simply this class as a template to generate another one for CDI 1.1 impl
public class BValInterceptor {
    private Collection<ExecutableType> classConfiguration = null;
    private final Map<Method, Boolean> methodConfiguration = new ConcurrentHashMap<Method, Boolean>();
    private Boolean constructorValidated = null;

    @Inject
    private Validator validator;

    @Inject
    private BValExtension globalConfiguration;

    private ExecutableValidator executableValidator = null;

    @AroundConstruct // TODO: see previous one
    public Object construct(final InvocationContext context) throws Exception {
        final Constructor constructor = context.getConstructor();
        final Class<?> targetClass = constructor.getDeclaringClass();
        if (!isConstructorValidated(targetClass, constructor)) {
            return context.proceed();
        }

        final ConstructorDescriptor constraints = validator.getConstraintsForClass(targetClass).getConstraintsForConstructor(constructor.getParameterTypes());
        if (constraints == null) { // surely implicit constructor
            return context.proceed();
        }

        initExecutableValidator();

        {
            final Set<ConstraintViolation<?>> violations = executableValidator.validateConstructorParameters(constructor, context.getParameters());
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        final Object result = context.proceed();

        {
            final Set<ConstraintViolation<?>> violations = executableValidator.validateConstructorReturnValue(constructor, result);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        return result;
    }

    @AroundInvoke
    public Object invoke(final InvocationContext context) throws Throwable {
        final Method method = context.getMethod();
        final Class<?> targetClass = Proxies.classFor(context.getTarget().getClass());
        if (!isMethodValidated(targetClass, method)) {
            return context.proceed();
        }

        final MethodDescriptor constraintsForMethod = validator.getConstraintsForClass(targetClass).getConstraintsForMethod(method.getName(), method.getParameterTypes());
        if (constraintsForMethod == null) {
            return context.proceed();
        }

        initExecutableValidator();

        {
            final Set<ConstraintViolation<Object>> violations = executableValidator.validateParameters(context.getTarget(), method, context.getParameters());
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        final Object result = context.proceed();

        {
            final Set<ConstraintViolation<Object>> violations = executableValidator.validateReturnValue(context.getTarget(), method, result);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        return result;
    }

    private boolean isConstructorValidated(final Class<?> targetClass, final Constructor<?> constructor) throws NoSuchMethodException {
        initClassConfig(targetClass);

        if (constructorValidated == null) {
            synchronized (this) {
                if (constructorValidated == null) {
                    final ValidateOnExecution annotation = targetClass.getConstructor(constructor.getParameterTypes()).getAnnotation(ValidateOnExecution.class);
                    if (annotation == null) {
                        constructorValidated = classConfiguration.contains(ExecutableType.CONSTRUCTORS);
                    } else {
                        final Collection<ExecutableType> types = Arrays.asList(annotation.type());
                        constructorValidated = types.contains(ExecutableType.CONSTRUCTORS) || types.contains(ExecutableType.IMPLICIT) || types.contains(ExecutableType.ALL);
                    }
                }
            }
        }

        return constructorValidated;
    }

    private boolean isMethodValidated(final Class<?> targetClass, final Method method) throws NoSuchMethodException {
        initClassConfig(targetClass);

        Boolean methodConfig = methodConfiguration.get(method);
        if (methodConfig == null) {
            synchronized (this) {
                methodConfig = methodConfiguration.get(method);
                if (methodConfig == null) {
                    final List<Class<?>> classHierarchy = ClassHelper.fillFullClassHierarchyAsList(new ArrayList<Class<?>>(), targetClass);

                    Class<?> lastClassWithTheMethod = null;

                    // search on method @ValidateOnExecution
                    Collections.reverse(classHierarchy);
                    ValidateOnExecution validateOnExecution = null;
                    for (final Class<?> c : classHierarchy) {
                        try {
                            validateOnExecution = c.getDeclaredMethod(method.getName(), method.getParameterTypes()).getAnnotation(ValidateOnExecution.class);
                            if (lastClassWithTheMethod == null) {
                                lastClassWithTheMethod = c;
                            }
                            if (validateOnExecution != null) {
                                lastClassWithTheMethod = null;
                                break;
                            }
                        } catch (final Throwable h) {
                            // no-op
                        }
                    }

                    // if not found look in the class declaring the method
                    if (validateOnExecution == null && lastClassWithTheMethod != null) {
                        validateOnExecution = lastClassWithTheMethod.getAnnotation(ValidateOnExecution.class);
                    }

                    if (validateOnExecution == null) {
                        methodConfig = doValidMethod(method, classConfiguration);
                    } else {
                        final Collection<ExecutableType> config = new HashSet<ExecutableType>();
                        for (final ExecutableType type : validateOnExecution.type()) {
                            if (ExecutableType.IMPLICIT.equals(type)) { // on method it just means validate, even on getters
                                config.add(ExecutableType.NON_GETTER_METHODS);
                                if (lastClassWithTheMethod == null) {
                                    config.add(ExecutableType.GETTER_METHODS);
                                } // else the annotation was not on the method so implicit doesn't mean getters
                            } else if (ExecutableType.ALL.equals(type)) {
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

    private void initClassConfig(Class<?> targetClass) {
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
    }

    private void initExecutableValidator() {
        if (executableValidator == null) {
            synchronized (this) {
                if (executableValidator == null) {
                    executableValidator = validator.forExecutables();
                }
            }
        }
    }

    private static boolean doValidMethod(final Method method, final Collection<ExecutableType> config) {
        final boolean getter = isGetter(method);
        return (!getter && config.contains(ExecutableType.NON_GETTER_METHODS)) || (getter && config.contains(ExecutableType.GETTER_METHODS));
    }

    private static boolean isGetter(final Method method) {
        final String name = method.getName();
        return (name.startsWith("get") || name.startsWith("is")) && method.getReturnType() != Void.TYPE && method.getParameterTypes().length == 0;
    }
}
