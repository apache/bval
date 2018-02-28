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

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Path;
import javax.validation.TraversableResolver;
import javax.validation.UnexpectedTypeException;
import javax.validation.ValidationException;
import javax.validation.groups.Default;
import javax.validation.metadata.CascadableDescriptor;
import javax.validation.metadata.ContainerDescriptor;
import javax.validation.metadata.ElementDescriptor.ConstraintFinder;
import javax.validation.metadata.PropertyDescriptor;

import org.apache.bval.jsr.ApacheFactoryContext;
import org.apache.bval.jsr.ConstraintViolationImpl;
import org.apache.bval.jsr.GraphContext;
import org.apache.bval.jsr.descriptor.BeanD;
import org.apache.bval.jsr.descriptor.CascadableContainerD;
import org.apache.bval.jsr.descriptor.ComposedD;
import org.apache.bval.jsr.descriptor.ConstraintD;
import org.apache.bval.jsr.descriptor.ElementD;
import org.apache.bval.jsr.descriptor.PropertyD;
import org.apache.bval.jsr.groups.Group;
import org.apache.bval.jsr.groups.Groups;
import org.apache.bval.jsr.util.NodeImpl;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.jsr.util.Proxies;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.ObjectUtils;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.TypeUtils;

public abstract class ValidationJob<T> {

    public abstract class Frame<D extends ElementD<?, ?>> {
        protected final Frame<?> parent;
        protected final D descriptor;
        protected final GraphContext context;

        protected Frame(Frame<?> parent, D descriptor, GraphContext context) {
            super();
            this.parent = parent;
            this.descriptor = Validate.notNull(descriptor, "descriptor");
            this.context = Validate.notNull(context, "context");
        }

        final ValidationJob<T> getJob() {
            return ValidationJob.this;
        }

        final void process(Class<?> group, Consumer<ConstraintViolation<T>> sink) {
            Validate.notNull(sink, "sink");

            each(expand(group), this::validateDescriptorConstraints, sink);
            recurse(group, sink);
        }

        abstract void recurse(Class<?> group, Consumer<ConstraintViolation<T>> sink);

        abstract Object getBean();

        protected void validateDescriptorConstraints(Class<?> group, Consumer<ConstraintViolation<T>> sink) {
            constraintsFrom(descriptor.findConstraints().unorderedAndMatchingGroups(group))
                .forEach(c -> validate(c, sink));
        }

        @SuppressWarnings("unchecked")
        private Stream<ConstraintD<?>> constraintsFrom(ConstraintFinder finder) {
            // our ConstraintFinder implementation is a Stream supplier; reference without exposing it beyond its
            // package:
            if (finder instanceof Supplier<?>) {
                return (Stream<ConstraintD<?>>) ((Supplier<?>) finder).get();
            }
            return finder.getConstraintDescriptors().stream().map(ConstraintD.class::cast);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private boolean validate(ConstraintD<?> constraint, Consumer<ConstraintViolation<T>> sink) {
            if (!validatedPathsByConstraint
                .computeIfAbsent(constraint, k -> new ConcurrentSkipListSet<>(COMPARE_TO_STRING))
                .add(context.getPath())) {
                // seen, ignore:
                return true;
            }
            final ConstraintValidatorContextImpl<T> constraintValidatorContext =
                new ConstraintValidatorContextImpl<>(this, constraint);

            final ConstraintValidator constraintValidator = getConstraintValidator(constraint);

            final boolean valid;
            if (constraintValidator == null) {
                // null validator without exception implies composition:
                valid = true;
            } else {
                constraintValidator.initialize(constraint.getAnnotation());
                valid = constraintValidator.isValid(context.getValue(), constraintValidatorContext);
            }
            if (!valid) {
                constraintValidatorContext.getRequiredViolations().forEach(sink);
            }
            if (valid || !constraint.isReportAsSingleViolation()) {
                final boolean compositionValid = validateComposed(constraint, sink);

                if (!compositionValid) {
                    if (valid && constraint.isReportAsSingleViolation()) {
                        constraintValidatorContext.getRequiredViolations().forEach(sink);
                    }
                    return false;
                }
            }
            return valid;
        }

        private boolean validateComposed(ConstraintD<?> constraint, Consumer<ConstraintViolation<T>> sink) {
            if (constraint.getComposingConstraints().isEmpty()) {
                return true;
            }
            final Consumer<ConstraintViolation<T>> effectiveSink = constraint.isReportAsSingleViolation() ? cv -> {
            } : sink;

            // collect validation results to set of Boolean, ensuring all are evaluated:
            final Set<Boolean> results = constraint.getComposingConstraints().stream().map(ConstraintD.class::cast)
                .map(c -> validate(c, effectiveSink)).collect(Collectors.toSet());

            return Collections.singleton(Boolean.TRUE).equals(results);
        }

        @SuppressWarnings({ "rawtypes" })
        private ConstraintValidator getConstraintValidator(ConstraintD<?> constraint) {
            final Class<? extends ConstraintValidator> constraintValidatorClass =
                constraint.getConstraintValidatorClass();

            if (constraintValidatorClass == null) {
                Exceptions.raiseIf(constraint.getComposingConstraints().isEmpty(), UnexpectedTypeException::new,
                    "No %s type located for non-composed constraint %s", ConstraintValidator.class.getSimpleName(),
                    constraint);
                return null;
            }
            ConstraintValidator constraintValidator = null;
            Exception cause = null;
            try {
                constraintValidator =
                    validatorContext.getConstraintValidatorFactory().getInstance(constraintValidatorClass);
            } catch (Exception e) {
                cause = e;
            }
            Exceptions.raiseIf(constraintValidator == null, ValidationException::new, cause,
                "Unable to get %s instance from %s", constraintValidatorClass.getName(),
                validatorContext.getConstraintValidatorFactory());

            return constraintValidator;
        }

        protected Stream<Class<?>> expand(Class<?> group) {
            if (Default.class.equals(group)) {
                final List<Class<?>> groupSequence = descriptor.getGroupSequence();
                if (groupSequence != null) {
                    return groupSequence.stream();
                }
            }
            return Stream.of(group);
        }
    }

    public class BeanFrame extends Frame<BeanD> {

        BeanFrame(GraphContext context) {
            this(null, context);
        }

        BeanFrame(Frame<?> parent, GraphContext context) {
            super(parent, getBeanDescriptor(context.getValue()), context);
        }

        @Override
        void recurse(Class<?> group, Consumer<ConstraintViolation<T>> sink) {
            // bean frame has to do some convoluted things to properly handle groups and recursion; skipping
            // frame#process() on properties:
            final List<Frame<?>> propertyFrames = propertyFrames();

            each(expand(group), (g, s) -> propertyFrames.forEach(f -> f.validateDescriptorConstraints(g, s)), sink);
            propertyFrames.forEach(f -> f.recurse(group, sink));
        }

        protected Frame<?> propertyFrame(PropertyD<?> d, GraphContext context) {
            return new SproutFrame<>(this, d, context);
        }

        @Override
        Object getBean() {
            return context.getValue();
        }

        private List<Frame<?>> propertyFrames() {
            final Stream<PropertyD<?>> properties = descriptor.getConstrainedProperties().stream()
                .flatMap(d -> ComposedD.unwrap(d, PropertyD.class)).map(d -> (PropertyD<?>) d);

            final TraversableResolver traversableResolver = validatorContext.getTraversableResolver();

            final Stream<PropertyD<?>> reachableProperties =
                properties.filter(d -> traversableResolver.isReachable(context.getValue(),
                    new NodeImpl.PropertyNodeImpl(d.getPropertyName()), getRootBeanClass(), context.getPath(),
                    d.getElementType()));

            return reachableProperties.flatMap(
                d -> d.read(context).filter(context -> !context.isRecursive()).map(child -> propertyFrame(d, child)))
                .collect(Collectors.toList());
        }
    }

    public class SproutFrame<D extends ElementD<?, ?> & CascadableDescriptor & ContainerDescriptor> extends Frame<D> {

        public SproutFrame(D descriptor, GraphContext context) {
            this(null, descriptor, context);
        }

        public SproutFrame(Frame<?> parent, D descriptor, GraphContext context) {
            super(parent, descriptor, context);
        }

        @Override
        void recurse(Class<?> group, Consumer<ConstraintViolation<T>> sink) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            final Stream<CascadableContainerD<?, ?>> containerElements =
                descriptor.getConstrainedContainerElementTypes().stream()
                    .flatMap(d -> ComposedD.unwrap(d, (Class) CascadableContainerD.class));

            containerElements.flatMap(d -> d.read(context).map(child -> new SproutFrame<>(this, d, child)))
                .forEach(f -> f.process(group, sink));

            if (!descriptor.isCascaded()) {
                return;
            }
            if (descriptor instanceof PropertyDescriptor) {
                final TraversableResolver traversableResolver = validatorContext.getTraversableResolver();

                final PathImpl pathToTraversableObject = PathImpl.copy(context.getPath());
                final NodeImpl traversableProperty = pathToTraversableObject.removeLeafNode();

                if (!traversableResolver.isCascadable(context.getValue(), traversableProperty, getRootBeanClass(),
                    pathToTraversableObject, ((PropertyD<?>) descriptor).getElementType())) {
                    return;
                }
            }
            multiplex().filter(context -> context.getValue() != null).map(context -> new BeanFrame(this, context))
                .forEach(b -> b.process(group, sink));
        }

        private Stream<GraphContext> multiplex() {
            final Object value = context.getValue();
            if (value == null) {
                return Stream.empty();
            }
            if (value.getClass().isArray()) {
                // inconsistent: use Object[] here but specific type for Iterable? RI compatibility
                final Class<?> arrayType = value instanceof Object[] ? Object[].class : value.getClass();
                return IntStream.range(0, Array.getLength(value)).mapToObj(i -> context
                    .child(NodeImpl.atIndex(i).inContainer(arrayType, null), Array.get(value, i)));
            }
            if (Map.class.isInstance(value)) {
                return ((Map<?, ?>) value).entrySet().stream()
                    .map(e -> context.child(
                        setContainerInformation(NodeImpl.atKey(e.getKey()), MAP_VALUE, descriptor.getElementClass()),
                        e.getValue()));
            }
            if (List.class.isInstance(value)) {
                final List<?> l = (List<?>) value;
                return IntStream.range(0, l.size())
                    .mapToObj(i -> context.child(
                        setContainerInformation(NodeImpl.atIndex(i), ITERABLE_ELEMENT, descriptor.getElementClass()),
                        l.get(i)));
            }
            if (Iterable.class.isInstance(value)) {
                final Stream.Builder<Object> b = Stream.builder();
                ((Iterable<?>) value).forEach(b);
                return b.build()
                    .map(o -> context.child(
                        setContainerInformation(NodeImpl.atIndex(null), ITERABLE_ELEMENT, descriptor.getElementClass()),
                        o));
            }
            return Stream.of(context);
        }

        // RI apparently wants to use e.g. Set for Iterable containers, so use declared type + assigned type
        // variable if present. not sure I agree, FWIW
        private NodeImpl setContainerInformation(NodeImpl node, TypeVariable<?> originalTypeVariable,
            Class<?> containerType) {
            final TypeVariable<?> tv;
            if (containerType.equals(originalTypeVariable.getGenericDeclaration())) {
                tv = originalTypeVariable;
            } else {
                final Type assignedType =
                    TypeUtils.getTypeArguments(containerType, (Class<?>) originalTypeVariable.getGenericDeclaration())
                        .get(originalTypeVariable);
                tv = assignedType instanceof TypeVariable<?> ? (TypeVariable<?>) assignedType : null;
            }
            final int i = tv == null ? -1 : ObjectUtils.indexOf(containerType.getTypeParameters(), tv);
            return node.inContainer(containerType, i < 0 ? null : Integer.valueOf(i));
        }

        @Override
        Object getBean() {
            return Optional.ofNullable(parent).map(Frame::getBean).orElse(null);
        }
    }

    private static final Comparator<Path> COMPARE_TO_STRING = Comparator.comparing(Object::toString);

    protected static final TypeVariable<?> MAP_VALUE = Map.class.getTypeParameters()[1];
    protected static final TypeVariable<?> ITERABLE_ELEMENT = Iterable.class.getTypeParameters()[0];

    protected final ApacheFactoryContext validatorContext;

    private final Groups groups;
    private final Lazy<Set<ConstraintViolation<T>>> results = new Lazy<>(LinkedHashSet::new);

    private ConcurrentMap<ConstraintD<?>, Set<Path>> validatedPathsByConstraint;

    ValidationJob(ApacheFactoryContext validatorContext, Class<?>[] groups) {
        super();
        this.validatorContext = Validate.notNull(validatorContext, "validatorContext");
        this.groups = validatorContext.getGroupsComputer().computeGroups(groups);
    }

    public final Set<ConstraintViolation<T>> getResults() {
        if (results.optional().isPresent()) {
            return results.get();
        }
        final Frame<?> baseFrame = computeBaseFrame();
        Validate.validState(baseFrame != null, "%s computed null baseFrame", getClass().getName());

        final Consumer<ConstraintViolation<T>> sink = results.consumer(Set::add);

        validatedPathsByConstraint = new ConcurrentHashMap<>();

        try {
            groups.getGroups().stream().map(Group::getGroup).forEach(g -> baseFrame.process(g, sink));

            sequences: for (List<Group> seq : groups.getSequences()) {
                final boolean proceed = each(seq.stream().map(Group::getGroup), baseFrame::process, sink);
                if (!proceed) {
                    break sequences;
                }
            }
        } finally {
            validatedPathsByConstraint = null;
        }
        return results.optional().map(Collections::unmodifiableSet).orElse(Collections.emptySet());
    }

    private boolean each(Stream<Class<?>> groupSequence, BiConsumer<Class<?>, Consumer<ConstraintViolation<T>>> closure,
        Consumer<ConstraintViolation<T>> sink) {
        final Lazy<Set<ConstraintViolation<T>>> sequenceViolations = new Lazy<>(LinkedHashSet::new);
        for (Class<?> g : (Iterable<Class<?>>) () -> groupSequence.iterator()) {
            closure.accept(g, sequenceViolations.consumer(Set::add));
            if (sequenceViolations.optional().isPresent()) {
                sequenceViolations.get().forEach(sink);
                return false;
            }
        }
        return true;
    }

    private BeanD getBeanDescriptor(Object bean) {
        final Class<? extends Object> t = Proxies.classFor(Validate.notNull(bean, "bean").getClass());
        return (BeanD) validatorContext.getDescriptorManager().getBeanDescriptor(t);
    }

    final ConstraintViolationImpl<T> createViolation(String messageTemplate, ConstraintValidatorContextImpl<T> context,
        Path propertyPath) {
        return createViolation(messageTemplate, interpolate(messageTemplate, context), context, propertyPath);
    }

    abstract ConstraintViolationImpl<T> createViolation(String messageTemplate, String message,
        ConstraintValidatorContextImpl<T> context, Path propertyPath);

    protected abstract Frame<?> computeBaseFrame();

    protected abstract Class<T> getRootBeanClass();

    private final String interpolate(String messageTemplate, MessageInterpolator.Context context) {
        try {
            return validatorContext.getMessageInterpolator().interpolate(messageTemplate, context);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException(e);
        }
    }
}