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

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

import javax.annotation.Priority;
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

import org.apache.bval.jsr.descriptor.DescriptorManager;
import org.apache.bval.jsr.metadata.Signature;
import org.apache.bval.jsr.util.ExecutableTypes;
import org.apache.bval.jsr.util.Methods;
import org.apache.bval.jsr.util.Proxies;
import org.apache.bval.util.reflection.Reflection;
import org.apache.bval.util.reflection.Reflection.Interfaces;
import org.apache.bval.util.reflection.TypeUtils;

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
    private transient volatile Set<ExecutableType> classConfiguration;
    private transient volatile Map<Signature, Boolean> executableValidation;

    @Inject
    private Validator validator;

    @Inject
    private BValExtension globalConfiguration;

    private transient volatile ExecutableValidator executableValidator;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @AroundConstruct // TODO: see previous one
    public Object construct(InvocationContext context) throws Exception {
        final Constructor ctor = context.getConstructor();
        if (!isConstructorValidated(ctor)) {
            return context.proceed();
        }
        final ConstructorDescriptor constraints = validator.getConstraintsForClass(ctor.getDeclaringClass())
            .getConstraintsForConstructor(ctor.getParameterTypes());

        if (!DescriptorManager.isConstrained(constraints)) {
            return context.proceed();
        }
        initExecutableValidator();

        if (constraints.hasConstrainedParameters()) {
            final Set<ConstraintViolation<?>> violations =
                executableValidator.validateConstructorParameters(ctor, context.getParameters());
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
        final Object result = context.proceed();

        if (constraints.hasConstrainedReturnValue()) {
            final Set<ConstraintViolation<?>> violations =
                executableValidator.validateConstructorReturnValue(ctor, context.getTarget());
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

        if (!isExecutableValidated(targetClass, method, this::computeIsMethodValidated)) {
            return context.proceed();
        }

        final MethodDescriptor constraintsForMethod = validator.getConstraintsForClass(targetClass)
            .getConstraintsForMethod(method.getName(), method.getParameterTypes());

        if (!DescriptorManager.isConstrained(constraintsForMethod)) {
            return context.proceed();
        }
        initExecutableValidator();

        if (constraintsForMethod.hasConstrainedParameters()) {
            final Set<ConstraintViolation<Object>> violations =
                executableValidator.validateParameters(context.getTarget(), method, context.getParameters());
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
        final Object result = context.proceed();

        if (constraintsForMethod.hasConstrainedReturnValue()) {
            final Set<ConstraintViolation<Object>> violations =
                executableValidator.validateReturnValue(context.getTarget(), method, result);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
        return result;
    }

    private <T> boolean isConstructorValidated(final Constructor<T> constructor)
        {
        return isExecutableValidated(constructor.getDeclaringClass(), constructor, this::computeIsConstructorValidated);
    }

    private <T, E extends Executable> boolean isExecutableValidated(final Class<T> targetClass, final E executable,
        BiPredicate<? super Class<T>, ? super E> compute) {
        initClassConfig(targetClass);

        if (executableValidation == null) {
            synchronized (this) {
                if (executableValidation == null) {
                    executableValidation = new ConcurrentHashMap<>();
                }
            }
        }
        return executableValidation.computeIfAbsent(Signature.of(executable),
            s -> compute.test(targetClass, executable));
    }

    private void initClassConfig(Class<?> targetClass) {
        if (classConfiguration == null) {
            synchronized (this) {
                if (classConfiguration == null) {
                    final ValidateOnExecution annotation = CDI.current().getBeanManager()
                        .createAnnotatedType(targetClass).getAnnotation(ValidateOnExecution.class);

                    if (annotation == null) {
                        classConfiguration = globalConfiguration.getGlobalExecutableTypes();
                    } else {
                        classConfiguration = ExecutableTypes.interpret(annotation.type());
                    }
                }
            }
        }
    }

    private <T> boolean computeIsConstructorValidated(Class<T> targetClass, Constructor<T> ctor) {
        final AnnotatedType<T> annotatedType =
            CDI.current().getBeanManager().createAnnotatedType(ctor.getDeclaringClass());

        final ValidateOnExecution annotation =
            annotatedType.getConstructors().stream().filter(ac -> ctor.equals(ac.getJavaMember())).findFirst()
                .map(ac -> ac.getAnnotation(ValidateOnExecution.class))
                .orElseGet(() -> ctor.getAnnotation(ValidateOnExecution.class));

        final Set<ExecutableType> validatedExecutableTypes =
            annotation == null ? classConfiguration : ExecutableTypes.interpret(annotation.type());

        return validatedExecutableTypes.contains(ExecutableType.CONSTRUCTORS);
    }

    private <T> boolean computeIsMethodValidated(Class<T> targetClass, Method method) {
        Collection<ExecutableType> declaredExecutableTypes = null;

        for (final Class<?> c : Reflection.hierarchy(targetClass, Interfaces.INCLUDE)) {
            final AnnotatedType<?> annotatedType = CDI.current().getBeanManager().createAnnotatedType(c);

            final AnnotatedMethod<?> annotatedMethod = annotatedType.getMethods().stream()
                .filter(am -> Signature.of(am.getJavaMember()).equals(Signature.of(method))).findFirst().orElse(null);

            if (annotatedMethod == null) {
                continue;
            }
            if (annotatedMethod.isAnnotationPresent(ValidateOnExecution.class)) {
                final List<ExecutableType> validatedTypesOnMethod =
                    Arrays.asList(annotatedMethod.getAnnotation(ValidateOnExecution.class).type());

                // implicit directly on method -> early return:
                if (validatedTypesOnMethod.contains(ExecutableType.IMPLICIT)) {
                    return true;
                }
                declaredExecutableTypes = validatedTypesOnMethod;
                // ignore the hierarchy once the lowest method is found:
                break;
            }
            if (declaredExecutableTypes == null) {
                if (annotatedType.isAnnotationPresent(ValidateOnExecution.class)) {
                    declaredExecutableTypes =
                        Arrays.asList(annotatedType.getAnnotation(ValidateOnExecution.class).type());
                } else {
                    final Optional<Package> pkg = Optional.of(annotatedType).map(AnnotatedType::getBaseType)
                        .map(t -> TypeUtils.getRawType(t, null)).map(Class::getPackage)
                        .filter(p -> p.isAnnotationPresent(ValidateOnExecution.class));
                    if (pkg.isPresent()) {
                        declaredExecutableTypes =
                            Arrays.asList(pkg.get().getAnnotation(ValidateOnExecution.class).type());
                    }
                }
            }
        }
        final ExecutableType methodType =
            Methods.isGetter(method) ? ExecutableType.GETTER_METHODS : ExecutableType.NON_GETTER_METHODS;

        return Optional.ofNullable(declaredExecutableTypes).map(ExecutableTypes::interpret)
            .orElse(globalConfiguration.getGlobalExecutableTypes()).contains(methodType);
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
}
