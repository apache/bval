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
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.groups.GroupConversion;
import org.apache.bval.jsr.util.Methods;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Validate;

/**
 * Maintains two metadata builds in parallel. The "primary" build is assumed to be the reflection/annotation-based build
 * and is subject to the {@link AnnotationBehavior} prescribed by the "custom" build.
 */
public class DualBuilder {

    private static class Delegator<DELEGATE extends HasAnnotationBehavior> implements HasAnnotationBehavior {

        private final Delegator<?> parent;
        protected final DELEGATE primaryDelegate;
        protected final DELEGATE customDelegate;

        Delegator(Delegator<?> parent, DELEGATE primaryDelegate, DELEGATE customDelegate) {
            this.parent = parent;
            this.primaryDelegate = Validate.notNull(primaryDelegate, "primaryDelegate");
            this.customDelegate = Validate.notNull(customDelegate, "customDelegate");
        }

        AnnotationBehavior getCustomAnnotationBehavior() {
            final AnnotationBehavior annotationBehavior = customDelegate.getAnnotationBehavior();
            Validate.validState(annotationBehavior != null, "null %s returned from %s",
                AnnotationBehavior.class.getSimpleName(), customDelegate);
            if (annotationBehavior == AnnotationBehavior.ABSTAIN && parent != null) {
                return parent.getCustomAnnotationBehavior();
            }
            return annotationBehavior;
        }

        protected Stream<DELEGATE> activeDelegates() {
            return getCustomAnnotationBehavior() == AnnotationBehavior.EXCLUDE ? Stream.of(customDelegate)
                : Stream.of(primaryDelegate, customDelegate);
        }

        <K, D> Map<K, D> merge(Function<DELEGATE, Map<K, D>> toMap, BiFunction<D, D, D> parallel,
            Supplier<D> emptyBuilder) {

            final Map<K, D> primaries = toMap.apply(primaryDelegate);
            final Map<K, D> customs = toMap.apply(customDelegate);

            if (primaries.isEmpty() && customs.isEmpty()) {
                return Collections.emptyMap();
            }

            final Function<? super K, ? extends D> valueMapper = k -> {
                final D primary = primaries.get(k);
                final D custom = customs.get(k);

                if (custom == null) {
                    if (primary != null) {
                        switch (getCustomAnnotationBehavior()) {
                        case INCLUDE:
                        case ABSTAIN:
                            return primary;
                        default:
                            break;
                        }
                    }
                    return emptyBuilder.get();
                }
                return parallel.apply(primary, custom);
            };
            return Stream.of(primaries, customs).map(Map::keySet).flatMap(Collection::stream).distinct()
                .collect(Collectors.toMap(Function.identity(), valueMapper));
        }
    }

    private static class ForBean<T> extends DualBuilder.Delegator<MetadataBuilder.ForBean<T>>
        implements MetadataBuilder.ForBean<T> {

        ForBean(MetadataBuilder.ForBean<T> primaryDelegate, MetadataBuilder.ForBean<T> customDelegate) {
            super(null, primaryDelegate, customDelegate);
        }

        @Override
        public MetadataBuilder.ForClass<T> getClass(Meta<Class<T>> meta) {
            return new DualBuilder.ForClass<>(this, primaryDelegate.getClass(meta), customDelegate.getClass(meta));
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Field>> getFields(Meta<Class<T>> meta) {
            return merge(b -> b.getFields(meta), (t, u) -> new DualBuilder.ForContainer<>(this, t, u),
                EmptyBuilder.instance()::forContainer);
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Method>> getGetters(Meta<Class<T>> meta) {
            return merge(b -> b.getGetters(meta), (t, u) -> new DualBuilder.ForContainer<>(this, t, u),
                EmptyBuilder.instance()::forContainer);
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Constructor<? extends T>>> getConstructors(Meta<Class<T>> meta) {
            return merge(b -> b.getConstructors(meta), (t, u) -> new DualBuilder.ForExecutable<>(this, t, u),
                EmptyBuilder.instance()::forExecutable);
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Method>> getMethods(Meta<Class<T>> meta) {
            return merge(b -> b.getMethods(meta), (t, u) -> new DualBuilder.ForExecutable<>(this, t, u),
                EmptyBuilder.instance()::forExecutable);
        }
    }

    private static class ForElement<DELEGATE extends MetadataBuilder.ForElement<E>, E extends AnnotatedElement>
        extends Delegator<DELEGATE> implements MetadataBuilder.ForElement<E> {

        ForElement(Delegator<?> parent, DELEGATE primaryDelegate, DELEGATE customDelegate) {
            super(parent, primaryDelegate, customDelegate);
        }

        @Override
        public final Annotation[] getDeclaredConstraints(Meta<E> meta) {
            return activeDelegates().map(d -> d.getDeclaredConstraints(meta)).flatMap(Stream::of)
                .toArray(Annotation[]::new);
        }
    }

    private static class ForClass<T> extends ForElement<MetadataBuilder.ForClass<T>, Class<T>>
        implements MetadataBuilder.ForClass<T> {

        ForClass(Delegator<?> parent, MetadataBuilder.ForClass<T> primaryDelegate,
            MetadataBuilder.ForClass<T> customDelegate) {
            super(parent, primaryDelegate, customDelegate);
        }

        @Override
        public List<Class<?>> getGroupSequence(Meta<Class<T>> meta) {
            final List<Class<?>> customGroupSequence = customDelegate.getGroupSequence(meta);
            if (customGroupSequence != null) {
                return customGroupSequence;
            }
            return customDelegate.getAnnotationBehavior() == AnnotationBehavior.EXCLUDE ? null
                : primaryDelegate.getGroupSequence(meta);
        }
    }

    private static class ForContainer<DELEGATE extends MetadataBuilder.ForContainer<E>, E extends AnnotatedElement>
        extends DualBuilder.ForElement<DELEGATE, E> implements MetadataBuilder.ForContainer<E> {

        ForContainer(Delegator<?> parent, DELEGATE primaryDelegate, DELEGATE customDelegate) {
            super(parent, primaryDelegate, customDelegate);
        }

        @Override
        public final boolean isCascade(Meta<E> meta) {
            return activeDelegates().anyMatch(d -> d.isCascade(meta));
        }

        @Override
        public final Set<GroupConversion> getGroupConversions(Meta<E> meta) {
            return activeDelegates().map(d -> d.getGroupConversions(meta)).flatMap(Collection::stream)
                .collect(ToUnmodifiable.set());
        }

        @Override
        public final Map<ContainerElementKey, MetadataBuilder.ForContainer<AnnotatedType>> getContainerElementTypes(
            Meta<E> meta) {
            return merge(b -> b.getContainerElementTypes(meta), (t, u) -> new DualBuilder.ForContainer<>(this, t, u),
                EmptyBuilder.instance()::forContainer);
        }
    }

    private static class ForExecutable<DELEGATE extends MetadataBuilder.ForExecutable<E>, E extends Executable>
        extends Delegator<DELEGATE> implements MetadataBuilder.ForExecutable<E> {

        ForExecutable(Delegator<?> parent, DELEGATE primaryDelegate, DELEGATE customDelegate) {
            super(parent, primaryDelegate, customDelegate);
        }

        @Override
        public MetadataBuilder.ForContainer<E> getReturnValue(Meta<E> meta) {
            return new DualBuilder.ForContainer<>(this, primaryDelegate.getReturnValue(meta),
                customDelegate.getReturnValue(meta));
        }

        @Override
        public List<MetadataBuilder.ForContainer<Parameter>> getParameters(Meta<E> meta) {

            final List<MetadataBuilder.ForContainer<Parameter>> primaries = primaryDelegate.getParameters(meta);
            final List<MetadataBuilder.ForContainer<Parameter>> customs = customDelegate.getParameters(meta);

            Validate.validState(primaries.size() == customs.size(), "Mismatched parameter counts: %d vs. %d",
                primaries.size(), customs.size());

            return IntStream.range(0, primaries.size())
                .mapToObj(n -> new DualBuilder.ForContainer<>(this, primaries.get(n), customs.get(n)))
                .collect(ToUnmodifiable.list());
        }

        @Override
        public MetadataBuilder.ForElement<E> getCrossParameter(Meta<E> meta) {
            return new DualBuilder.ForElement<MetadataBuilder.ForElement<E>, E>(this,
                primaryDelegate.getCrossParameter(meta), customDelegate.getCrossParameter(meta));
        }
    }

    private static class CustomWrapper {
        private static class ForBean<T> implements MetadataBuilder.ForBean<T> {

            private final MetadataBuilder.ForBean<T> wrapped;
            private final Map<String, MetadataBuilder.ForContainer<Method>> getters;
            private final Map<Signature, MetadataBuilder.ForExecutable<Method>> methods;

            ForBean(MetadataBuilder.ForBean<T> wrapped, Map<String, MetadataBuilder.ForContainer<Method>> getters,
                Map<Signature, MetadataBuilder.ForExecutable<Method>> methods) {
                super();
                this.wrapped = Validate.notNull(wrapped, "wrapped");
                this.getters = Validate.notNull(getters, "getters");
                this.methods = Validate.notNull(methods, "methods");
            }

            @Override
            public AnnotationBehavior getAnnotationBehavior() {
                return wrapped.getAnnotationBehavior();
            }

            @Override
            public MetadataBuilder.ForClass<T> getClass(Meta<Class<T>> meta) {
                return wrapped.getClass(meta);
            }

            @Override
            public Map<String, MetadataBuilder.ForContainer<Field>> getFields(Meta<Class<T>> meta) {
                return wrapped.getFields(meta);
            }

            @Override
            public Map<String, MetadataBuilder.ForContainer<Method>> getGetters(Meta<Class<T>> meta) {
                return getters;
            }

            @Override
            public Map<Signature, MetadataBuilder.ForExecutable<Constructor<? extends T>>> getConstructors(
                Meta<Class<T>> meta) {
                return wrapped.getConstructors(meta);
            }

            @Override
            public Map<Signature, MetadataBuilder.ForExecutable<Method>> getMethods(Meta<Class<T>> meta) {
                return methods;
            }
        }

        private static class ForGetterMethod implements MetadataBuilder.ForExecutable<Method> {
            private final MetadataBuilder.ForContainer<Method> returnValue;

            private ForGetterMethod(MetadataBuilder.ForContainer<Method> returnValue) {
                super();
                this.returnValue = Validate.notNull(returnValue, "returnValue");
            }

            @Override
            public AnnotationBehavior getAnnotationBehavior() {
                return returnValue.getAnnotationBehavior();
            }

            @Override
            public MetadataBuilder.ForContainer<Method> getReturnValue(Meta<Method> meta) {
                return returnValue;
            }

            @Override
            public MetadataBuilder.ForElement<Method> getCrossParameter(Meta<Method> meta) {
                return EmptyBuilder.instance().forElement();
            }

            @Override
            public List<MetadataBuilder.ForContainer<Parameter>> getParameters(Meta<Method> meta) {
                return Collections.emptyList();
            }
        }
    }

    public static <T> MetadataBuilder.ForBean<T> forBean(Class<T> beanClass, MetadataBuilder.ForBean<T> primaryDelegate,
        MetadataBuilder.ForBean<T> customDelegate, ApacheValidatorFactory validatorFactory) {
        return new DualBuilder.ForBean<>(primaryDelegate, wrapCustom(customDelegate, beanClass, validatorFactory));
    }

    private static <T> MetadataBuilder.ForBean<T> wrapCustom(MetadataBuilder.ForBean<T> customDelegate,
        Class<T> beanClass, ApacheValidatorFactory validatorFactory) {
        final Meta.ForClass<T> meta = new Meta.ForClass<>(beanClass);

        final Map<String, MetadataBuilder.ForContainer<Method>> getters = customDelegate.getGetters(meta);
        final Map<Signature, MetadataBuilder.ForExecutable<Method>> methods = customDelegate.getMethods(meta);

        if (getters.isEmpty() && methods.keySet().stream().noneMatch(Signature::isGetter)) {
            // nothing to merge
            return customDelegate;
        }
        final CompositeBuilder composite =
            CompositeBuilder.with(validatorFactory, AnnotationBehaviorMergeStrategy.consensus());

        final Map<String, MetadataBuilder.ForContainer<Method>> mergedGetters = new TreeMap<>(getters);

        methods.forEach((k, v) -> {
            if (k.isGetter()) {
                mergedGetters.compute(Methods.propertyName(k.getName()), (p, d) -> {
                    final Method getter = Methods.getter(beanClass, p);
                    return Stream.of(d, v.getReturnValue(new Meta.ForMethod(getter))).filter(Objects::nonNull)
                        .collect(composite.composeContainer());
                });
            }
        });
        final Map<Signature, MetadataBuilder.ForExecutable<Method>> mergedMethods = new TreeMap<>(methods);

        getters.forEach((k, v) -> {
            final Method getter = Methods.getter(beanClass, k);
            final Signature signature = Signature.of(getter);
            
            final MetadataBuilder.ForContainer<Method> rv;
            if (methods.containsKey(signature)) {
                rv = Stream.of(methods.get(signature).getReturnValue(new Meta.ForMethod(getter)), v)
                    .collect(composite.composeContainer());
            } else {
                rv = v;
            }
            mergedMethods.put(signature, new CustomWrapper.ForGetterMethod(rv));
        });
        return new CustomWrapper.ForBean<>(customDelegate, mergedGetters, mergedMethods);
    }
}
