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
package org.apache.bval.jsr.descriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.validation.ConstraintDeclarationException;
import javax.validation.GroupDefinitionException;
import javax.validation.GroupSequence;
import javax.validation.ParameterNameProvider;
import javax.validation.Valid;
import javax.validation.groups.ConvertGroup;
import javax.validation.groups.Default;
import javax.validation.metadata.PropertyDescriptor;
import javax.validation.metadata.Scope;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.ConstraintAnnotationAttributes;
import org.apache.bval.jsr.groups.Group;
import org.apache.bval.jsr.groups.GroupConversion;
import org.apache.bval.jsr.groups.GroupStrategy;
import org.apache.bval.jsr.groups.GroupsComputer;
import org.apache.bval.jsr.metadata.ContainerElementKey;
import org.apache.bval.jsr.metadata.EmptyBuilder;
import org.apache.bval.jsr.metadata.Meta;
import org.apache.bval.jsr.metadata.MetadataBuilder;
import org.apache.bval.jsr.metadata.Signature;
import org.apache.bval.jsr.util.AnnotationsManager;
import org.apache.bval.jsr.util.Methods;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.jsr.xml.AnnotationProxyBuilder;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.ObjectUtils;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.bval.util.reflection.Reflection.Interfaces;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

@Privilizing(@CallTo(Reflection.class))
class MetadataReader {

    class ForElement<E extends AnnotatedElement, B extends MetadataBuilder.ForElement<E>> {
        final Meta<E> meta;
        protected final B builder;

        ForElement(Meta<E> meta, B builder) {
            super();
            this.meta = Validate.notNull(meta, "meta");
            this.builder = Validate.notNull(builder, "builder");
        }

        Set<ConstraintD<?>> getConstraints() {
            return builder.getConstraintDeclarationMap(meta).entrySet().stream().filter(e -> e.getValue().length > 0)
                .flatMap(e -> {
                    final Meta<E> m = e.getKey();
                    final Class<?> declaredBy = m.getDeclaringClass();
                    final Scope scope = declaredBy.equals(beanClass) ? Scope.LOCAL_ELEMENT : Scope.HIERARCHY;
                    return Stream.of(e.getValue()).peek(
                        c -> validatorFactory.getAnnotationsManager().validateConstraintDefinition(c.annotationType()))
                        .map(c -> rewriteConstraint(c, declaredBy))
                        .map(c -> new ConstraintD<>(c, scope, m, validatorFactory));
                }).collect(ToUnmodifiable.set());
        }

        ApacheValidatorFactory getValidatorFactory() {
            return validatorFactory;
        }

        private <A extends Annotation> A rewriteConstraint(A constraint, Class<?> declaredBy) {
            boolean mustRewrite = false;
            Class<?>[] groups =
                ConstraintAnnotationAttributes.GROUPS.analyze(constraint.annotationType()).read(constraint);
            if (groups.length == 0) {
                mustRewrite = true;
                groups = GroupsComputer.DEFAULT_GROUP;
            }
            if (!(declaredBy.equals(beanClass) || beanClass.isInterface())) {
                if (ObjectUtils.arrayContains(groups, Default.class)
                    && !ObjectUtils.arrayContains(groups, declaredBy)) {
                    mustRewrite = true;
                    groups = ObjectUtils.arrayAdd(groups, declaredBy);
                }
            }
            if (mustRewrite) {
                final AnnotationProxyBuilder<A> builder = new AnnotationProxyBuilder<A>(constraint);
                builder.setGroups(groups);
                return builder.createAnnotation();
            }
            return constraint;
        }
    }

    class ForBean<T> extends MetadataReader.ForElement<Class<T>, MetadataBuilder.ForClass<T>> {
        private final MetadataBuilder.ForBean<T> beanBuilder;

        ForBean(Meta<Class<T>> meta, MetadataBuilder.ForBean<T> builder) {
            super(meta, Validate.notNull(builder, "builder").getClass(meta));
            this.beanBuilder = builder;
        }

        Map<String, PropertyDescriptor> getProperties(BeanD<T> parent) {
            final Map<String, List<PropertyD<?>>> properties = new LinkedHashMap<>();
            final Function<? super String, ? extends List<PropertyD<?>>> descriptorList = k -> new ArrayList<>();

            beanBuilder.getFields(meta).forEach((f, builder) -> {
                final Field fld = Reflection.find(meta.getHost(), t -> Reflection.getDeclaredField(t, f));
                properties.computeIfAbsent(f, descriptorList).add(
                    new PropertyD.ForField(new MetadataReader.ForContainer<>(new Meta.ForField(fld), builder), parent));
            });
            beanBuilder.getGetters(meta).forEach((g, builder) -> {
                final Method getter = Methods.getter(meta.getHost(), g);

                if (getter == null) {
                    Exceptions.raise(IllegalStateException::new, "Getter method for property %s not found", g);
                }
                properties.computeIfAbsent(g, descriptorList).add(new PropertyD.ForMethod(
                    new MetadataReader.ForContainer<>(new Meta.ForMethod(getter), builder), parent));
            });
            return properties.entrySet().stream().collect(ToUnmodifiable.map(Map.Entry::getKey, e -> {
                final List<PropertyD<?>> delegates = e.getValue();

                if (delegates.size() == 1) {
                    return delegates.get(0);
                }
                final Set<PropertyD<?>> constrained =
                    delegates.stream().filter(DescriptorManager::isConstrained).collect(Collectors.toSet());
                if (constrained.isEmpty()) {
                    return delegates.get(0);
                }
                if (constrained.size() == 1) {
                    return constrained.iterator().next();
                }
                return new ComposedD.ForProperty(delegates);
            }));
        }

        Map<Signature, MethodD> getMethods(BeanD<T> parent) {
            final Map<Signature, MetadataBuilder.ForExecutable<Method>> methodBuilders = beanBuilder.getMethods(meta);
            if (methodBuilders.isEmpty()) {
                return Collections.emptyMap();
            }
            final Map<Signature, MethodD> result = new LinkedHashMap<>();

            methodBuilders.forEach((sig, builder) -> {
                final Method m = Reflection.find(meta.getHost(),
                    t -> Reflection.getDeclaredMethod(t, sig.getName(), sig.getParameterTypes()));

                final MethodD descriptor =
                    new MethodD(new MetadataReader.ForMethod(new Meta.ForMethod(m), builder), parent);
                if (DescriptorManager.isConstrained(descriptor)) {
                    result.put(sig, descriptor);
                }
            });
            return Collections.unmodifiableMap(result);
        }

        Map<Signature, ConstructorD<T>> getConstructors(BeanD<T> parent) {
            final Map<Signature, MetadataBuilder.ForExecutable<Constructor<? extends T>>> ctorBuilders =
                beanBuilder.getConstructors(meta);

            if (ctorBuilders.isEmpty()) {
                return Collections.emptyMap();
            }
            final Map<Signature, ConstructorD<T>> result = new LinkedHashMap<>();

            ctorBuilders.forEach((sig, builder) -> {
                final Constructor<?> c = Reflection.getDeclaredConstructor(meta.getHost(), sig.getParameterTypes());
                @SuppressWarnings({ "unchecked", "rawtypes" })
                final Meta.ForConstructor<T> metaCtor = (Meta.ForConstructor) new Meta.ForConstructor<>(c);
                final ConstructorD<T> descriptor =
                    new ConstructorD<>(new MetadataReader.ForConstructor<T>(metaCtor, builder), parent);
                if (DescriptorManager.isConstrained(descriptor)) {
                    result.put(sig, descriptor);
                }
            });
            return Collections.unmodifiableMap(result);
        }

        GroupStrategy getGroupStrategy() {
            final Class<T> host = meta.getHost();
            if (host.isInterface()) {
                return validatorFactory.getGroupsComputer().computeGroups(host).asStrategy();
            }
            final GroupStrategy parentStrategy = Optional.ofNullable(host.getSuperclass()).filter(JDK.negate())
                .map(validatorFactory.getDescriptorManager()::getBeanDescriptor).map(BeanD.class::cast)
                .map(BeanD::getGroupStrategy).orElse(null);

            final List<Class<?>> groupSequence = builder.getGroupSequence(meta);

            final Set<Group> parentGroups = parentStrategy == null ? null : parentStrategy.getGroups();

            Group localGroup = Group.of(host);
            if (groupSequence == null) {
                final Set<Group> groups = new HashSet<>();
                groups.add(localGroup);

                for (Class<?> t : Reflection.hierarchy(host, Interfaces.INCLUDE)) {
                    if (JDK.test(t)) {
                        continue;
                    }
                    if (!t.isInterface()) {
                        continue;
                    }
                    if (AnnotationsManager.isAnnotationDirectlyPresent(t, GroupSequence.class)) {
                        continue;
                    }
                    final Group g = Group.of(t);
                    if (parentGroups != null && parentGroups.contains(g)) {
                        continue;
                    }
                    groups.add(g);
                }
                final GroupStrategy strategy = GroupStrategy.simple(groups);
                return parentStrategy == null ? strategy : GroupStrategy.composite(strategy, parentStrategy);
            }
            if (groupSequence.contains(Default.class)) {
                Exceptions.raise(GroupDefinitionException::new, "@%s for %s must not contain %s",
                    GroupSequence.class.getSimpleName(), host, Default.class.getName());
            }
            if (!groupSequence.contains(host)) {
                Exceptions.raise(GroupDefinitionException::new, "@%s for %s must contain %<s",
                    GroupSequence.class.getSimpleName(), host);
            }
            final Group.Sequence result =
                Group.sequence(groupSequence.stream().map(Group::of).collect(Collectors.toList()));

            final Deque<Group> expanded = new ArrayDeque<>();
            for (Class<?> t : Reflection.hierarchy(host, Interfaces.INCLUDE)) {
                if (JDK.test(t)) {
                    continue;
                }
                if (t.isInterface() && AnnotationsManager.isAnnotationDirectlyPresent(t, GroupSequence.class)) {
                    continue;
                }
                expanded.push(Group.of(t));
            }
            if (expanded.size() == 1) {
                return result;
            }
            return result.redefining(Collections.singletonMap(localGroup, GroupStrategy.simple(expanded)));
        }
    }

    class ForContainer<E extends AnnotatedElement> extends ForElement<E, MetadataBuilder.ForContainer<E>> {

        ForContainer(Meta<E> meta, MetadataBuilder.ForContainer<E> builder) {
            super(meta, builder);
        }

        boolean isCascaded() {
            return builder.isCascade(meta);
        }

        Set<GroupConversion> getGroupConversions() {
            final Set<GroupConversion> groupConversions = builder.getGroupConversions(meta);
            if (!groupConversions.isEmpty()) {
                if (!isCascaded()) {
                    Exceptions.raise(ConstraintDeclarationException::new, "@%s declared without @%s on %s",
                        ConvertGroup.class.getSimpleName(), Valid.class.getSimpleName(), meta.describeHost());
                }
                if (groupConversions.stream().map(GroupConversion::getFrom).distinct().count() < groupConversions
                    .size()) {
                    Exceptions.raise(ConstraintDeclarationException::new, "%s has duplicate 'from' group conversions",
                        meta.describeHost());
                }
                groupConversions.stream().map(GroupConversion::getFrom)
                    .forEach(from -> Exceptions.raiseIf(from.isAnnotationPresent(GroupSequence.class),
                        ConstraintDeclarationException::new,
                        "Invalid group conversion declared on %s from group sequence %s",
                        f -> f.args(meta.describeHost(), from)));
            }
            return groupConversions;
        }

        Set<ContainerElementTypeD> getContainerElementTypes(CascadableContainerD<?, ?> parent) {
            final Map<ContainerElementKey, MetadataBuilder.ForContainer<AnnotatedType>> containerElementTypes =
                builder.getContainerElementTypes(meta);

            if (containerElementTypes.isEmpty()) {
                return Collections.emptySet();
            }
            final Set<ContainerElementTypeD> result =
                new TreeSet<>(Comparator.comparing(ContainerElementTypeD::getKey));

            containerElementTypes.forEach((k, builder) -> {
                result.add(new ContainerElementTypeD(k,
                    new MetadataReader.ForContainer<>(new Meta.ForContainerElement(meta, k), builder), parent));
            });
            return Collections.unmodifiableSet(result);
        }
    }

    abstract class ForExecutable<E extends Executable, SELF extends ForExecutable<E, SELF>>
        extends ForElement<E, MetadataBuilder.ForElement<E>> {
        private final MetadataBuilder.ForExecutable<E> executableBuilder;

        ForExecutable(Meta<E> meta, MetadataBuilder.ForExecutable<E> executableBuilder) {
            super(meta, EmptyBuilder.instance().forElement());
            this.executableBuilder = Validate.notNull(executableBuilder, "executableBuilder");
        }

        <X extends ExecutableD<E, SELF, X>> List<ParameterD<X>> getParameterDescriptors(X parent) {
            final Parameter[] parameters = meta.getHost().getParameters();

            final List<String> parameterNames =
                getParameterNames(validatorFactory.getParameterNameProvider(), meta.getHost());

            final List<MetadataBuilder.ForContainer<Parameter>> builders = executableBuilder.getParameters(meta);

            return IntStream.range(0, parameters.length).mapToObj(i -> {
                final Meta.ForParameter param = new Meta.ForParameter(parameters[i], parameterNames.get(i));
                final MetadataBuilder.ForContainer<Parameter> parameterBuilder =
                    builders.size() > i ? builders.get(i) : EmptyBuilder.instance().forContainer();
                return new ParameterD<>(param, i, new MetadataReader.ForContainer<>(param, parameterBuilder), parent);
            }).collect(ToUnmodifiable.list());
        }

        <X extends ExecutableD<E, SELF, X>> CrossParameterD<X, E> getCrossParameterDescriptor(X parent) {
            final Meta.ForCrossParameter<E> cp = new Meta.ForCrossParameter<>(meta);
            return new CrossParameterD<>(new MetadataReader.ForElement<>(cp, executableBuilder.getCrossParameter(cp)),
                parent);
        }

        <X extends ExecutableD<E, SELF, X>> ReturnValueD<X, E> getReturnValueDescriptor(X parent) {
            return new ReturnValueD<>(new MetadataReader.ForContainer<>(meta, executableBuilder.getReturnValue(meta)),
                parent);
        }

        abstract List<String> getParameterNames(ParameterNameProvider parameterNameProvider, E host);
    }

    class ForMethod extends ForExecutable<Method, ForMethod> {
        ForMethod(Meta<Method> meta, MetadataBuilder.ForExecutable<Method> builder) {
            super(meta, builder);
        }

        @Override
        List<String> getParameterNames(ParameterNameProvider parameterNameProvider, Method host) {
            return parameterNameProvider.getParameterNames(host);
        }
    }

    class ForConstructor<T> extends ForExecutable<Constructor<? extends T>, ForConstructor<T>> {

        ForConstructor(Meta<Constructor<? extends T>> meta,
            MetadataBuilder.ForExecutable<Constructor<? extends T>> builder) {
            super(meta, builder);
        }

        @Override
        List<String> getParameterNames(ParameterNameProvider parameterNameProvider, Constructor<? extends T> host) {
            return parameterNameProvider.getParameterNames(host);
        }
    }

    private static final Predicate<Class<?>> JDK = t -> t.getName().startsWith("java.");

    private final ApacheValidatorFactory validatorFactory;
    private final Class<?> beanClass;

    MetadataReader(ApacheValidatorFactory validatorFactory, Class<?> beanClass) {
        super();
        this.validatorFactory = Validate.notNull(validatorFactory, "validatorFactory");
        this.beanClass = Validate.notNull(beanClass, "beanClass");
    }

    @SuppressWarnings("unchecked")
    <T> MetadataReader.ForBean<T> forBean(MetadataBuilder.ForBean<T> builder) {
        return new MetadataReader.ForBean<>(new Meta.ForClass<>((Class<T>) beanClass), builder);
    }
}
