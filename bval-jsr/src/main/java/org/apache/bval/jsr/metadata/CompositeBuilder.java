/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval.jsr.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.validation.ElementKind;
import javax.validation.ParameterNameProvider;
import javax.validation.metadata.Scope;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.groups.GroupConversion;
import org.apache.bval.jsr.util.Methods;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;

public class CompositeBuilder {

    class Delegator<DELEGATE extends HasAnnotationBehavior> implements HasAnnotationBehavior {

        protected final List<DELEGATE> delegates;

        Delegator(List<DELEGATE> delegates) {
            this.delegates = Validate.notNull(delegates, "delegates");
            Validate.isTrue(!delegates.isEmpty(), "no delegates specified");
            Validate.isTrue(delegates.stream().noneMatch(Objects::isNull), "One or more supplied delegates was null");
        }

        @Override
        public AnnotationBehavior getAnnotationBehavior() {
            return annotationBehaviorStrategy.apply(delegates);
        }

        <K, D> Map<K, D> merge(Function<DELEGATE, Map<K, D>> toMap, Function<List<D>, D> merge) {
            return merge(toMap, (k, l) -> merge.apply(l));
        }

        <K, D> Map<K, D> merge(Function<DELEGATE, Map<K, D>> toMap, BiFunction<K, List<D>, D> merge) {
            final List<Map<K, D>> maps = delegates.stream().map(toMap).collect(Collectors.toList());

            final Function<? super K, ? extends D> valueMapper = k -> {
                final List<D> mappedDelegates =
                    maps.stream().map(m -> m.get(k)).filter(Objects::nonNull).collect(Collectors.toList());
                return mappedDelegates.size() == 1 ? mappedDelegates.get(0) : merge.apply(k, mappedDelegates);
            };
            return maps.stream().map(Map::keySet).flatMap(Collection::stream).distinct()
                .collect(Collectors.toMap(Function.identity(), valueMapper));
        }
    }

    class ForBean<T> extends CompositeBuilder.Delegator<MetadataBuilder.ForBean<T>>
        implements MetadataBuilder.ForBean<T> {

        ForBean(List<MetadataBuilder.ForBean<T>> delegates) {
            super(delegates);
        }

        @Override
        public MetadataBuilder.ForClass<T> getClass(Meta<Class<T>> meta) {
            return new CompositeBuilder.ForClass<>(
                delegates.stream().map(d -> d.getClass(meta)).collect(Collectors.toList()));
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Field>> getFields(Meta<Class<T>> meta) {
            return merge(b -> b.getFields(meta), (f, l) -> {
                final Field fld = Reflection.find(meta.getHost(), t -> Reflection.getDeclaredField(t, f));
                if (fld == null) {
                    Exceptions.raise(IllegalStateException::new, "Could not find field %s of %s", f, meta.getHost());
                }
                return forContainer(l, new Meta.ForField(fld), ElementKind.PROPERTY);
            });
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Method>> getGetters(Meta<Class<T>> meta) {
            return merge(b -> b.getGetters(meta), (g, l) -> {
                final Method getter = Methods.getter(meta.getHost(), g);
                if (getter == null) {
                    Exceptions.raise(IllegalStateException::new, "Could not find getter for property %s of %s", g,
                        meta.getHost());
                }
                return forContainer(l, new Meta.ForMethod(getter), ElementKind.PROPERTY);
            });
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Constructor<? extends T>>> getConstructors(Meta<Class<T>> meta) {
            return merge(b -> b.getConstructors(meta),
                d -> new CompositeBuilder.ForExecutable<>(d, ParameterNameProvider::getParameterNames));
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Method>> getMethods(Meta<Class<T>> meta) {
            return merge(b -> b.getMethods(meta),
                d -> new CompositeBuilder.ForExecutable<>(d, ParameterNameProvider::getParameterNames));
        }
    }

    class ForElement<DELEGATE extends MetadataBuilder.ForElement<E>, E extends AnnotatedElement>
        extends Delegator<DELEGATE> implements MetadataBuilder.ForElement<E> {

        ForElement(List<DELEGATE> delegates) {
            super(delegates);
        }

        @Override
        public Map<Scope, Annotation[]> getConstraintsByScope(Meta<E> meta) {
            return CompositeBuilder.this.getConstraintsByScope(this, meta);
        }

        @Override
        public final Annotation[] getDeclaredConstraints(Meta<E> meta) {
            return delegates.stream().map(d -> d.getDeclaredConstraints(meta)).flatMap(Stream::of)
                .toArray(Annotation[]::new);
        }
    }

    class ForClass<T> extends ForElement<MetadataBuilder.ForClass<T>, Class<T>> implements MetadataBuilder.ForClass<T> {

        ForClass(List<MetadataBuilder.ForClass<T>> delegates) {
            super(delegates);
        }

        @Override
        public List<Class<?>> getGroupSequence(Meta<Class<T>> meta) {
            return CompositeBuilder.this.getGroupSequence(this, meta);
        }
    }

    class ForContainer<DELEGATE extends MetadataBuilder.ForContainer<E>, E extends AnnotatedElement>
        extends CompositeBuilder.ForElement<DELEGATE, E> implements MetadataBuilder.ForContainer<E> {

        ForContainer(List<DELEGATE> delegates) {
            super(delegates);
        }

        @Override
        public boolean isCascade(Meta<E> meta) {
            return delegates.stream().anyMatch(d -> d.isCascade(meta));
        }

        @Override
        public Set<GroupConversion> getGroupConversions(Meta<E> meta) {
            return delegates.stream().map(d -> d.getGroupConversions(meta)).flatMap(Collection::stream)
                .collect(ToUnmodifiable.set());
        }

        @Override
        public Map<ContainerElementKey, MetadataBuilder.ForContainer<AnnotatedType>> getContainerElementTypes(
            Meta<E> meta) {
            return merge(b -> b.getContainerElementTypes(meta),
                (k, l) -> forContainer(l, new Meta.ForContainerElement(meta, k), ElementKind.CONTAINER_ELEMENT));
        }
    }

    class ForExecutable<DELEGATE extends MetadataBuilder.ForExecutable<E>, E extends Executable>
        extends Delegator<DELEGATE> implements MetadataBuilder.ForExecutable<E> {

        private final BiFunction<ParameterNameProvider, E, List<String>> getParameterNames;

        ForExecutable(List<DELEGATE> delegates, BiFunction<ParameterNameProvider, E, List<String>> getParameterNames) {
            super(delegates);
            this.getParameterNames = Validate.notNull(getParameterNames, "getParameterNames");
        }

        @Override
        public MetadataBuilder.ForContainer<E> getReturnValue(Meta<E> meta) {
            return CompositeBuilder.this.forContainer(
                delegates.stream().map(d -> d.getReturnValue(meta)).collect(Collectors.toList()), meta,
                ElementKind.RETURN_VALUE);
        }

        @Override
        public List<MetadataBuilder.ForContainer<Parameter>> getParameters(Meta<E> meta) {
            final List<List<MetadataBuilder.ForContainer<Parameter>>> parameterLists =
                delegates.stream().map(d -> d.getParameters(meta)).collect(Collectors.toList());

            final Set<Integer> parameterCounts = parameterLists.stream().map(List::size).collect(Collectors.toSet());
            Validate.validState(parameterCounts.size() == 1, "Mismatched parameter counts: %s", parameterCounts);

            final int parameterCount = parameterCounts.iterator().next().intValue();
            final List<Meta<Parameter>> metaParams = getMetaParameters(meta, getParameterNames);
            return IntStream.range(0, parameterCount).mapToObj(n -> {
                return forContainer(parameterLists.stream().map(l -> l.get(n)).collect(Collectors.toList()),
                    metaParams.get(n), ElementKind.PARAMETER);
            }).collect(ToUnmodifiable.list());
        }

        @Override
        public MetadataBuilder.ForElement<E> getCrossParameter(Meta<E> meta) {
            return forCrossParameter(
                delegates.stream().map(d -> d.getCrossParameter(meta)).collect(Collectors.toList()), meta);
        }
    }

    public static CompositeBuilder with(ApacheValidatorFactory validatorFactory,
        AnnotationBehaviorMergeStrategy annotationBehaviorStrategy) {
        return new CompositeBuilder(validatorFactory, annotationBehaviorStrategy);
    }

    private final AnnotationBehaviorMergeStrategy annotationBehaviorStrategy;
    protected final ApacheValidatorFactory validatorFactory;

    CompositeBuilder(ApacheValidatorFactory validatorFactory,
        AnnotationBehaviorMergeStrategy annotationBehaviorMergeStrategy) {
        super();
        this.annotationBehaviorStrategy =
            Validate.notNull(annotationBehaviorMergeStrategy, "annotationBehaviorMergeStrategy");
        this.validatorFactory = Validate.notNull(validatorFactory, "validatorFactory");
    }

    public <T> Collector<MetadataBuilder.ForBean<T>, ?, MetadataBuilder.ForBean<T>> compose() {
        return Collectors.collectingAndThen(Collectors.toList(), CompositeBuilder.ForBean::new);
    }

    protected final <E extends Executable> List<Meta<Parameter>> getMetaParameters(Meta<E> meta,
        BiFunction<ParameterNameProvider, E, List<String>> getParameterNames) {
        final Parameter[] parameters = meta.getHost().getParameters();
        final List<String> parameterNames =
            getParameterNames.apply(validatorFactory.getParameterNameProvider(), meta.getHost());

        if (parameterNames.size() != parameters.length) {
            Exceptions.raise(IllegalStateException::new, "%s returned wrong number of parameter names",
                validatorFactory.getParameterNameProvider());
        }
        return IntStream.range(0, parameters.length)
            .mapToObj(n -> new Meta.ForParameter(parameters[n], parameterNames.get(n))).collect(Collectors.toList());
    }

    protected <E extends AnnotatedElement> Map<Scope, Annotation[]> getConstraintsByScope(
        CompositeBuilder.ForElement<? extends MetadataBuilder.ForElement<E>, E> composite, Meta<E> meta) {
        return Collections.singletonMap(Scope.LOCAL_ELEMENT, composite.getDeclaredConstraints(meta));
    }

    protected <T> List<Class<?>> getGroupSequence(CompositeBuilder.ForClass<T> composite, Meta<Class<T>> meta) {
        final List<List<Class<?>>> groupSequence =
            composite.delegates.stream().map(d -> d.getGroupSequence(meta)).collect(Collectors.toList());
        Validate.validState(groupSequence.size() <= 1,
            "group sequence returned from multiple composite class metadata builders");
        return groupSequence.isEmpty() ? null : groupSequence.get(0);
    }

    protected <DELEGATE extends MetadataBuilder.ForContainer<E>, E extends AnnotatedElement> MetadataBuilder.ForContainer<E> forContainer(
        List<DELEGATE> delegates, Meta<E> meta, ElementKind elementKind) {
        return new CompositeBuilder.ForContainer<>(delegates);
    }

    protected <DELEGATE extends MetadataBuilder.ForElement<E>, E extends Executable> MetadataBuilder.ForElement<E> forCrossParameter(
        List<DELEGATE> delegates, Meta<E> meta) {
        return new CompositeBuilder.ForElement<>(delegates);
    }
}
