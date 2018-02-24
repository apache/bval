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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.validation.GroupDefinitionException;
import javax.validation.GroupSequence;
import javax.validation.ParameterNameProvider;
import javax.validation.groups.Default;
import javax.validation.metadata.PropertyDescriptor;
import javax.validation.metadata.Scope;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.metadata.ContainerElementKey;
import org.apache.bval.jsr.metadata.EmptyBuilder;
import org.apache.bval.jsr.metadata.MetadataBuilder;
import org.apache.bval.jsr.metadata.Metas;
import org.apache.bval.jsr.metadata.Signature;
import org.apache.bval.jsr.util.Methods;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;

class MetadataReader {

    class ForElement<E extends AnnotatedElement, B extends MetadataBuilder.ForElement<E>> {
        final Metas<E> meta;
        protected final B builder;

        ForElement(Metas<E> meta, B builder) {
            super();
            this.meta = Validate.notNull(meta, "meta");
            this.builder = Validate.notNull(builder, "builder");
        }

        Set<ConstraintD<?>> getConstraints() {
            return builder.getConstraintsByScope(meta).entrySet().stream()
                .flatMap(e -> describe(e.getValue(), e.getKey(), meta)).collect(ToUnmodifiable.set());
        }

        private Stream<ConstraintD<?>> describe(Annotation[] constraints, Scope scope, Metas<?> meta) {
            return Stream.of(constraints).map(c -> new ConstraintD<>(c, scope, meta, validatorFactory));
        }
    }

    class ForBean extends MetadataReader.ForElement<Class<?>, MetadataBuilder.ForClass> {
        private final MetadataBuilder.ForBean beanBuilder;

        ForBean(Metas<Class<?>> meta, MetadataBuilder.ForBean builder) {
            super(meta, Validate.notNull(builder, "builder").getClass(meta));
            this.beanBuilder = builder;
        }

        Map<String, PropertyDescriptor> getProperties(BeanD parent) {
            final Map<String, List<PropertyD<?>>> properties = new LinkedHashMap<>();
            final Function<? super String, ? extends List<PropertyD<?>>> descriptorList = k -> new ArrayList<>();

            beanBuilder.getFields(meta).forEach((f, builder) -> {
                final Field fld = Reflection.find(meta.getHost(), t -> Reflection.getDeclaredField(t, f));
                properties.computeIfAbsent(f, descriptorList).add(new PropertyD.ForField(
                    new MetadataReader.ForContainer<>(new Metas.ForField(fld), builder), parent));
            });

            beanBuilder.getGetters(meta).forEach((g, builder) -> {
                final Method getter = Reflection.find(meta.getHost(), t -> {
                    return Stream.of(Reflection.getDeclaredMethods(t)).filter(Methods::isGetter)
                        .filter(m -> g.equals(Methods.propertyName(m))).findFirst().orElse(null);
                });
                Exceptions.raiseIf(getter == null, IllegalStateException::new,
                    "Getter method for property %s not found", g);

                properties.computeIfAbsent(g, descriptorList).add(new PropertyD.ForMethod(
                    new MetadataReader.ForContainer<>(new Metas.ForMethod(getter), builder), parent));
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

        Map<Signature, MethodD> getMethods(BeanD parent) {
            final Map<Signature, MetadataBuilder.ForExecutable<Method>> methodBuilders = beanBuilder.getMethods(meta);
            if (methodBuilders.isEmpty()) {
                return Collections.emptyMap();
            }
            final Map<Signature, MethodD> result = new LinkedHashMap<>();

            methodBuilders.forEach((sig, builder) -> {
                final Method m = Reflection.find(meta.getHost(),
                    t -> Reflection.getDeclaredMethod(t, sig.getName(), sig.getParameterTypes()));

                result.put(sig, new MethodD(new MetadataReader.ForMethod(new Metas.ForMethod(m), builder), parent));
            });
            return Collections.unmodifiableMap(result);
        }

        Map<Signature, ConstructorD> getConstructors(BeanD parent) {
            final Map<Signature, MetadataBuilder.ForExecutable<Constructor<?>>> ctorBuilders =
                beanBuilder.getConstructors(meta);

            if (ctorBuilders.isEmpty()) {
                return Collections.emptyMap();
            }
            final Map<Signature, ConstructorD> result = new LinkedHashMap<>();

            ctorBuilders.forEach((sig, builder) -> {
                final Constructor<?> c = Reflection.getDeclaredConstructor(meta.getHost(), sig.getParameterTypes());
                result.put(sig,
                    new ConstructorD(new MetadataReader.ForConstructor(new Metas.ForConstructor(c), builder), parent));
            });
            return Collections.unmodifiableMap(result);
        }

        List<Class<?>> getGroupSequence() {
            List<Class<?>> result = builder.getGroupSequence(meta);
            if (result == null) {
                // resolve group sequence/Default redefinition up class hierarchy:
                final Class<?> superclass = meta.getHost().getSuperclass();
                if (superclass != null) {
                    // attempt to mock parent sequence intent by appending this type immediately after supertype:
                    result = ((ElementD<?, ?>) validatorFactory.getDescriptorManager().getBeanDescriptor(superclass))
                        .getGroupSequence();
                    if (result != null) {
                        result = new ArrayList<>(result);
                        result.add(result.indexOf(superclass) + 1, meta.getHost());
                    }
                }
            }
            if (result == null) {
                return null;
            }
            Exceptions.raiseUnless(result.contains(meta.getHost()), GroupDefinitionException::new,
                "@%s for %s must contain %<s", GroupSequence.class.getSimpleName(), meta.getHost());
            Exceptions.raiseIf(result.contains(Default.class), GroupDefinitionException::new,
                "@%s for %s must not contain %s", GroupSequence.class.getSimpleName(), meta.getHost(),
                Default.class.getName());
            return Collections.unmodifiableList(result);
        }
    }

    class ForContainer<E extends AnnotatedElement> extends ForElement<E, MetadataBuilder.ForContainer<E>> {

        ForContainer(Metas<E> meta, MetadataBuilder.ForContainer<E> builder) {
            super(meta, builder);
        }

        boolean isCascaded() {
            return builder.isCascade(meta);
        }

        Set<GroupConversion> getGroupConversions() {
            return builder.getGroupConversions(meta);
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
                    new MetadataReader.ForContainer<>(new Metas.ForContainerElement(meta, k), builder), parent));
            });
            return Collections.unmodifiableSet(result);
        }
    }

    abstract class ForExecutable<E extends Executable, SELF extends ForExecutable<E, SELF>>
        extends ForElement<E, MetadataBuilder.ForElement<E>> {
        private final MetadataBuilder.ForExecutable<E> executableBuilder;

        ForExecutable(Metas<E> meta, MetadataBuilder.ForExecutable<E> executableBuilder) {
            super(meta, EmptyBuilder.instance().forElement());
            this.executableBuilder = Validate.notNull(executableBuilder, "executableBuilder");
        }

        <X extends ExecutableD<E, SELF, X>> List<ParameterD<X>> getParameterDescriptors(X parent) {
            final Parameter[] parameters = meta.getHost().getParameters();

            final List<String> parameterNames =
                getParameterNames(validatorFactory.getParameterNameProvider(), meta.getHost());

            final List<MetadataBuilder.ForContainer<Parameter>> builders = executableBuilder.getParameters(meta);

            return IntStream.range(0, parameters.length).mapToObj(i -> {
                final Metas.ForParameter param = new Metas.ForParameter(parameters[i], parameterNames.get(i));
                return new ParameterD<>(param, i, new MetadataReader.ForContainer<>(param, builders.get(i)), parent);
            }).collect(ToUnmodifiable.list());
        }

        <X extends ExecutableD<E, SELF, X>> CrossParameterD<X, E> getCrossParameterDescriptor(X parent) {
            final Metas.ForCrossParameter<E> cp = new Metas.ForCrossParameter<>(meta);
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
        ForMethod(Metas<Method> meta, MetadataBuilder.ForExecutable<Method> builder) {
            super(meta, builder);
        }

        @Override
        List<String> getParameterNames(ParameterNameProvider parameterNameProvider, Method host) {
            return parameterNameProvider.getParameterNames(host);
        }
    }

    class ForConstructor extends ForExecutable<Constructor<?>, ForConstructor> {

        ForConstructor(Metas<Constructor<?>> meta, MetadataBuilder.ForExecutable<Constructor<?>> builder) {
            super(meta, builder);
        }

        @Override
        List<String> getParameterNames(ParameterNameProvider parameterNameProvider, Constructor<?> host) {
            return parameterNameProvider.getParameterNames(host);
        }
    }

    private final ApacheValidatorFactory validatorFactory;

    MetadataReader(ApacheValidatorFactory validatorFactory) {
        super();
        this.validatorFactory = Validate.notNull(validatorFactory, "validatorFactory");
    }

    MetadataReader.ForBean forBean(Class<?> beanClass, MetadataBuilder.ForBean builder) {
        return new MetadataReader.ForBean(new Metas.ForClass(beanClass), builder);
    }
}
