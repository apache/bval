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

import static java.util.Arrays.asList;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ExecutableValidator;
import javax.validation.executable.ValidateOnExecution;
import javax.validation.metadata.ConstructorDescriptor;
import javax.validation.metadata.MethodDescriptor;

import org.apache.bval.jsr.util.ClassHelper;
import org.apache.bval.jsr.util.Proxies;

/**
 * Interceptor class for the {@link BValBinding} {@link InterceptorBinding}.
 */
@SuppressWarnings("serial")
@Interceptor
@BValBinding
@Priority(4800)
// TODO: maybe add it through ASM to be compliant with CDI 1.0 containers using simply this class as a template to
// generate another one for CDI 1.1 impl
public class BValInterceptor implements Serializable {
    private transient volatile Map<Method, Boolean> methodConfiguration = new ConcurrentHashMap<>();
    private transient volatile Set<ExecutableType> classConfiguration;
    private transient volatile Boolean constructorValidated;

    @Inject
    private Validator validator;

    @Inject
    private BValExtension globalConfiguration;

    private transient volatile ExecutableValidator executableValidator;

    @AroundConstruct // TODO: see previous one
    public Object construct(InvocationContext context) throws Exception {
        @SuppressWarnings("rawtypes")
        final Constructor constructor = context.getConstructor();
        final Class<?> targetClass = constructor.getDeclaringClass();
        if (!isConstructorValidated(targetClass, constructor)) {
            return context.proceed();
        }

        final ConstructorDescriptor constraints =
            validator.getConstraintsForClass(targetClass).getConstraintsForConstructor(constructor.getParameterTypes());
        if (constraints == null) { // surely implicit constructor
            return context.proceed();
        }

        initExecutableValidator();

        {
            @SuppressWarnings("unchecked")
            final Set<ConstraintViolation<?>> violations =
                executableValidator.validateConstructorParameters(constructor, context.getParameters());
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        final Object result = context.proceed();

        {
            @SuppressWarnings("unchecked")
            final Set<ConstraintViolation<?>> violations =
                executableValidator.validateConstructorReturnValue(constructor, context.getTarget());
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        return result;
    }

    @AroundInvoke
    public Object invoke(final InvocationContext context) throws Exception {
        final Method method = context.getMethod();
        final Class<?> targetClass = Proxies.classFor(context.getTarget().getClass());
        if (!isMethodValidated(targetClass, method)) {
            return context.proceed();
        }

        final MethodDescriptor constraintsForMethod = validator.getConstraintsForClass(targetClass)
            .getConstraintsForMethod(method.getName(), method.getParameterTypes());
        if (constraintsForMethod == null) {
            return context.proceed();
        }

        initExecutableValidator();

        {
            final Set<ConstraintViolation<Object>> violations =
                executableValidator.validateParameters(context.getTarget(), method, context.getParameters());
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        final Object result = context.proceed();

        {
            final Set<ConstraintViolation<Object>> violations =
                executableValidator.validateReturnValue(context.getTarget(), method, result);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        return result;
    }

    private boolean isConstructorValidated(final Class<?> targetClass, final Constructor<?> constructor)
        throws NoSuchMethodException {
        initClassConfig(targetClass);

        if (constructorValidated == null) {
            synchronized (this) {
                if (constructorValidated == null) {
                    final AnnotatedType<?> annotatedType =
                        CDI.current().getBeanManager().createAnnotatedType(constructor.getDeclaringClass());
                    AnnotatedConstructor<?> annotatedConstructor = null;
                    for (final AnnotatedConstructor<?> ac : annotatedType.getConstructors()) {
                        if (!constructor.equals(ac.getJavaMember())) {
                            continue;
                        }
                        annotatedConstructor = ac;
                        break;
                    }
                    final ValidateOnExecution annotation = annotatedConstructor != null
                        ? annotatedConstructor.getAnnotation(ValidateOnExecution.class) : targetClass
                            .getConstructor(constructor.getParameterTypes()).getAnnotation(ValidateOnExecution.class);
                    if (annotation == null) {
                        constructorValidated = classConfiguration.contains(ExecutableType.CONSTRUCTORS);
                    } else {
                        final Collection<ExecutableType> types = Arrays.asList(annotation.type());
                        constructorValidated = types.contains(ExecutableType.CONSTRUCTORS)
                            || types.contains(ExecutableType.IMPLICIT) || types.contains(ExecutableType.ALL);
                    }
                }
            }
        }

        return constructorValidated;
    }

    private boolean isMethodValidated(final Class<?> targetClass, final Method method) throws NoSuchMethodException {
        initClassConfig(targetClass);

        if (methodConfiguration == null) {
            synchronized (this) {
                if (methodConfiguration == null) {
                    methodConfiguration = new ConcurrentHashMap<Method, Boolean>();
                }
            }
        }

        Boolean methodConfig = methodConfiguration.get(method);
        if (methodConfig == null) {
            synchronized (this) {
                methodConfig = methodConfiguration.get(method);
                if (methodConfig == null) {
                    final List<Class<?>> classHierarchy =
                        ClassHelper.fillFullClassHierarchyAsList(new LinkedList<>(), targetClass);
                    Collections.reverse(classHierarchy);

                    // search on method @ValidateOnExecution
                    ValidateOnExecution validateOnExecution = null;
                    ValidateOnExecution validateOnExecutionType = null;
                    for (final Class<?> c : classHierarchy) {
                        final AnnotatedType<?> annotatedType = CDI.current().getBeanManager().createAnnotatedType(c);
                        AnnotatedMethod<?> annotatedMethod = null;

                        for (final AnnotatedMethod<?> m : annotatedType.getMethods()) {
                            if (m.getJavaMember().getName().equals(method.getName())
                                && asList(method.getGenericParameterTypes())
                                    .equals(asList(m.getJavaMember().getGenericParameterTypes()))) {
                                annotatedMethod = m;
                                break;
                            }
                        }
                        if (annotatedMethod == null) {
                            continue;
                        }
                        try {
                            if (validateOnExecutionType == null) {
                                final ValidateOnExecution vat = annotatedType.getAnnotation(ValidateOnExecution.class);
                                if (vat != null) {
                                    validateOnExecutionType = vat;
                                }
                            }
                            final ValidateOnExecution mvat = annotatedMethod.getAnnotation(ValidateOnExecution.class);
                            if (mvat != null) {
                                validateOnExecution = mvat;
                            }
                        } catch (final Throwable h) {
                            // no-op
                        }
                    }

                    // if not found look in the class declaring the method
                    boolean classMeta = false;
                    if (validateOnExecution == null) {
                        validateOnExecution = validateOnExecutionType;
                        classMeta = validateOnExecution != null;
                    }

                    if (validateOnExecution == null) {
                        methodConfig = doValidMethod(method, classConfiguration);
                    } else {
                        final Set<ExecutableType> config = EnumSet.noneOf(ExecutableType.class);
                        for (final ExecutableType type : validateOnExecution.type()) {
                            if (ExecutableType.NONE == type) {
                                continue;
                            }
                            if (ExecutableType.ALL == type) {
                                config.add(ExecutableType.NON_GETTER_METHODS);
                                config.add(ExecutableType.GETTER_METHODS);
                                break;
                            }
                            if (ExecutableType.IMPLICIT == type) { // on method it just means validate, even on getters
                                config.add(ExecutableType.NON_GETTER_METHODS);
                                if (!classMeta) {
                                    config.add(ExecutableType.GETTER_METHODS);
                                } // else the annotation was not on the method so implicit doesn't mean getters
                            } else {
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
                    classConfiguration = EnumSet.noneOf(ExecutableType.class);

                    final AnnotatedType<?> annotatedType =
                        CDI.current().getBeanManager().createAnnotatedType(targetClass);
                    final ValidateOnExecution annotation = annotatedType.getAnnotation(ValidateOnExecution.class);
                    if (annotation == null) {
                        classConfiguration.addAll(globalConfiguration.getGlobalExecutableTypes());
                    } else {
                        for (final ExecutableType type : annotation.type()) {
                            if (ExecutableType.NONE == type) {
                                continue;
                            }
                            if (ExecutableType.ALL == type) {
                                classConfiguration.add(ExecutableType.CONSTRUCTORS);
                                classConfiguration.add(ExecutableType.NON_GETTER_METHODS);
                                classConfiguration.add(ExecutableType.GETTER_METHODS);
                                break;
                            }
                            if (ExecutableType.IMPLICIT == type) {
                                classConfiguration.add(ExecutableType.CONSTRUCTORS);
                                classConfiguration.add(ExecutableType.NON_GETTER_METHODS);
                            } else {
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

    private static boolean doValidMethod(final Method method, final Set<ExecutableType> config) {
        return isGetter(method) ? config.contains(ExecutableType.GETTER_METHODS)
            : config.contains(ExecutableType.NON_GETTER_METHODS);
    }

    private static boolean isGetter(final Method method) {
        final String name = method.getName();
        return method.getParameterTypes().length == 0 && !Void.TYPE.equals(method.getReturnType())
            && (name.startsWith("get") || name.startsWith("is") && boolean.class.equals(method.getReturnType()));
    }
}
