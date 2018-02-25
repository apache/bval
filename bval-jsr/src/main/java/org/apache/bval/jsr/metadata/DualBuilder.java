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
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.bval.jsr.groups.GroupConversion;
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

    private static class ForBean extends DualBuilder.Delegator<MetadataBuilder.ForBean>
        implements MetadataBuilder.ForBean {

        ForBean(MetadataBuilder.ForBean primaryDelegate, MetadataBuilder.ForBean customDelegate) {
            super(null, primaryDelegate, customDelegate);
        }

        @Override
        public MetadataBuilder.ForClass getClass(Meta<Class<?>> meta) {
            return new DualBuilder.ForClass(this, primaryDelegate.getClass(meta), customDelegate.getClass(meta));
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Field>> getFields(Meta<Class<?>> meta) {
            return merge(b -> b.getFields(meta), (t, u) -> new DualBuilder.ForContainer<>(this, t, u),
                EmptyBuilder.instance()::forContainer);
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Method>> getGetters(Meta<Class<?>> meta) {
            return merge(b -> b.getGetters(meta), (t, u) -> new DualBuilder.ForContainer<>(this, t, u),
                EmptyBuilder.instance()::forContainer);
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Constructor<?>>> getConstructors(Meta<Class<?>> meta) {
            return merge(b -> b.getConstructors(meta), (t, u) -> new DualBuilder.ForExecutable<>(this, t, u),
                EmptyBuilder.instance()::forExecutable);
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Method>> getMethods(Meta<Class<?>> meta) {
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

    private static class ForClass extends ForElement<MetadataBuilder.ForClass, Class<?>>
        implements MetadataBuilder.ForClass {

        ForClass(Delegator<?> parent, MetadataBuilder.ForClass primaryDelegate,
            MetadataBuilder.ForClass customDelegate) {
            super(parent, primaryDelegate, customDelegate);
        }

        @Override
        public List<Class<?>> getGroupSequence(Meta<Class<?>> meta) {
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

    public static MetadataBuilder.ForBean forBean(MetadataBuilder.ForBean primaryDelegate,
        MetadataBuilder.ForBean customDelegate) {
        return new DualBuilder.ForBean(primaryDelegate, customDelegate);
    }
}
