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
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.validation.ConstraintViolation;
import javax.validation.ParameterNameProvider;
import javax.validation.constraintvalidation.ValidationTarget;
import javax.validation.metadata.ExecutableDescriptor;

import org.apache.bval.jsr.ApacheFactoryContext;
import org.apache.bval.jsr.ConstraintViolationImpl;
import org.apache.bval.jsr.GraphContext;
import org.apache.bval.jsr.descriptor.ConstraintD;
import org.apache.bval.jsr.descriptor.CrossParameterD;
import org.apache.bval.jsr.descriptor.ParameterD;
import org.apache.bval.jsr.groups.Group;
import org.apache.bval.jsr.groups.GroupStrategy;
import org.apache.bval.jsr.metadata.Meta;
import org.apache.bval.jsr.util.NodeImpl;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.TypeUtils;

public abstract class ValidateParameters<E extends Executable, T> extends ValidateExecutable<E, T> {

    public static class ForMethod<T> extends ValidateParameters<Method, T> {

        ForMethod(ApacheFactoryContext validatorContext, T object, Method executable, Object[] parameterValues,
            Class<?>[] groups) {
            super(validatorContext, object, Validate.notNull(executable, IllegalArgumentException::new, "null method"),
                parameterValues, groups, new Meta.ForMethod(executable));
            Validate.notNull(object, IllegalArgumentException::new, "object");
        }

        @Override
        protected ExecutableDescriptor describe() {
            return validatorContext.getDescriptorManager().getBeanDescriptor(object.getClass())
                .getConstraintsForMethod(executable.getName(), executable.getParameterTypes());
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Class<T> getRootBeanClass() {
            return (Class<T>) object.getClass();
        }

        @Override
        protected List<String> getParameterNames(ParameterNameProvider parameterNameProvider) {
            return parameterNameProvider.getParameterNames(executable);
        }

        @Override
        protected T getRootBean() {
            return object;
        }
    }

    public static class ForConstructor<T> extends ValidateParameters<Constructor<? extends T>, T> {

        ForConstructor(ApacheFactoryContext validatorContext, Constructor<? extends T> executable,
            Object[] parameterValues, Class<?>[] groups) {
            super(validatorContext, null, Validate.notNull(executable, IllegalArgumentException::new, "null ctor"),
                parameterValues, groups, new Meta.ForConstructor<>(executable));
        }

        @Override
        protected ExecutableDescriptor describe() {
            return validatorContext.getDescriptorManager().getBeanDescriptor(executable.getDeclaringClass())
                .getConstraintsForConstructor(executable.getParameterTypes());
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Class<T> getRootBeanClass() {
            return (Class<T>) executable.getDeclaringClass();
        }

        @Override
        protected List<String> getParameterNames(ParameterNameProvider parameterNameProvider) {
            return parameterNameProvider.getParameterNames(executable);
        }

        @Override
        protected T getRootBean() {
            return null;
        }
    }

    class ParametersFrame extends Frame<CrossParameterD<?, ?>> {
        private final ExecutableDescriptor executableDescriptor;

        protected ParametersFrame(ExecutableDescriptor executableDescriptor, GraphContext context) {
            super(null, (CrossParameterD<?, ?>) Validate.notNull(executableDescriptor, "executableDescriptor")
                .getCrossParameterDescriptor(), context);
            this.executableDescriptor = executableDescriptor;
        }

        @Override
        protected ValidationTarget getValidationTarget() {
            return ValidationTarget.PARAMETERS;
        }

        @Override
        void process(GroupStrategy groups, Consumer<ConstraintViolation<T>> sink) {
            Validate.notNull(sink, "sink");
            final Lazy<Set<Frame<?>>> parameterFrames = new Lazy<>(this::parameterFrames);

            GroupStrategy.redefining(groups, Collections.singletonMap(Group.DEFAULT, descriptor.getGroupStrategy()))
                .applyTo(noViolations(gs -> {
                    validateDescriptorConstraints(gs, sink);
                    parameterFrames.get().forEach(p -> p.validateDescriptorConstraints(gs, sink));
                }));
            parameterFrames.get().forEach(p -> p.recurse(groups, sink));
        }

        @Override
        Object getBean() {
            return object;
        }

        private Set<Frame<?>> parameterFrames() {
            return executableDescriptor.getParameterDescriptors().stream()
                .map(pd -> new SproutFrame<ParameterD<?>>(this, (ParameterD<?>) pd, parameter(pd.getIndex())))
                .collect(Collectors.toSet());
        }
    }

    private static final String PARAMETERS_DO_NOT_MATCH = "Parameters do not match";

    protected final T object;
    protected final Lazy<List<String>> parameterNames =
        new Lazy<>(() -> getParameterNames(validatorContext.getParameterNameProvider()));

    private final Object[] parameterValues;

    ValidateParameters(ApacheFactoryContext validatorContext, T object, E executable, Object[] parameterValues,
        Class<?>[] groups, Meta<E> meta) {
        super(validatorContext, groups, meta);
        this.object = object;
        this.parameterValues =
            Validate.notNull(parameterValues, IllegalArgumentException::new, "null parameter values").clone();

        final Type[] genericParameterTypes = executable.getGenericParameterTypes();
        Exceptions.raiseUnless(parameterValues.length == genericParameterTypes.length, IllegalArgumentException::new,
            PARAMETERS_DO_NOT_MATCH);
        IntStream.range(0, genericParameterTypes.length)
            .forEach(n -> Exceptions.raiseUnless(TypeUtils.isInstance(parameterValues[n], genericParameterTypes[n]),
                IllegalArgumentException::new, PARAMETERS_DO_NOT_MATCH));
    }

    @Override
    protected Frame<?> computeBaseFrame() {
        return new ParametersFrame(describe(), new GraphContext(validatorContext,
            createBasePath().addNode(new NodeImpl.CrossParameterNodeImpl()), parameterValues));
    }

    @Override
    protected boolean hasWork() {
        final ExecutableDescriptor descriptor = describe();
        return descriptor != null && descriptor.hasConstrainedParameters();
    }

    protected abstract ExecutableDescriptor describe();

    protected abstract List<String> getParameterNames(ParameterNameProvider parameterNameProvider);

    protected abstract T getRootBean();

    @Override
    ConstraintViolationImpl<T> createViolation(String messageTemplate, String message,
        ConstraintValidatorContextImpl<T> context, PathImpl propertyPath) {
        return new ConstraintViolationImpl<T>(messageTemplate, message, getRootBean(), context.getFrame().getBean(),
            propertyPath, context.getFrame().context.getValue(), context.getConstraintDescriptor(), getRootBeanClass(),
            context.getConstraintDescriptor().unwrap(ConstraintD.class).getDeclaredOn(), null, parameterValues);
    }

    private GraphContext parameter(int i) {
        final PathImpl path = createBasePath();
        path.addNode(new NodeImpl.ParameterNodeImpl(parameterNames.get().get(i), i));
        return new GraphContext(validatorContext, path, parameterValues[i]);
    }
}
