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
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.validation.metadata.Scope;

import org.apache.bval.jsr.descriptor.GroupConversion;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Validate;

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
            final List<Map<K, D>> maps = delegates.stream().map(toMap).collect(Collectors.toList());

            final Function<? super K, ? extends D> valueMapper = k -> {
                final List<D> mappedDelegates =
                    maps.stream().map(m -> m.get(k)).filter(Objects::nonNull).collect(Collectors.toList());
                return mappedDelegates.size() == 1 ? mappedDelegates.get(0) : merge.apply(mappedDelegates);
            };

            return maps.stream().map(Map::keySet).flatMap(Collection::stream).distinct()
                .collect(Collectors.toMap(Function.identity(), valueMapper));
        }
    }

    private class ForBean extends CompositeBuilder.Delegator<MetadataBuilder.ForBean>
        implements MetadataBuilder.ForBean {

        ForBean(List<MetadataBuilder.ForBean> delegates) {
            super(delegates);
        }

        @Override
        public MetadataBuilder.ForClass getClass(Metas<Class<?>> meta) {
            return new CompositeBuilder.ForClass(
                delegates.stream().map(d -> d.getClass(meta)).collect(Collectors.toList()));
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Field>> getFields(Metas<Class<?>> meta) {
            return merge(b -> b.getFields(meta), CompositeBuilder.ForContainer::new);
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Method>> getGetters(Metas<Class<?>> meta) {
            return merge(b -> b.getGetters(meta), CompositeBuilder.ForContainer::new);
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Constructor<?>>> getConstructors(Metas<Class<?>> meta) {
            return merge(b -> b.getConstructors(meta), CompositeBuilder.ForExecutable::new);
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Method>> getMethods(Metas<Class<?>> meta) {
            return merge(b -> b.getMethods(meta), CompositeBuilder.ForExecutable::new);
        }
    }

    class ForElement<DELEGATE extends MetadataBuilder.ForElement<E>, E extends AnnotatedElement>
        extends Delegator<DELEGATE> implements MetadataBuilder.ForElement<E> {

        ForElement(List<DELEGATE> delegates) {
            super(delegates);
        }

        @Override
        public Map<Scope, Annotation[]> getConstraintsByScope(Metas<E> meta) {
            return CompositeBuilder.this.getConstraintsByScope(this, meta);
        }

        @Override
        public final Annotation[] getDeclaredConstraints(Metas<E> meta) {
            return delegates.stream().map(d -> d.getDeclaredConstraints(meta)).flatMap(Stream::of)
                .toArray(Annotation[]::new);
        }
    }

    class ForClass extends ForElement<MetadataBuilder.ForClass, Class<?>> implements MetadataBuilder.ForClass {

        ForClass(List<MetadataBuilder.ForClass> delegates) {
            super(delegates);
        }

        @Override
        public List<Class<?>> getGroupSequence(Metas<Class<?>> meta) {
            return CompositeBuilder.this.getGroupSequence(this, meta);
        }
    }

    private class ForContainer<DELEGATE extends MetadataBuilder.ForContainer<E>, E extends AnnotatedElement>
        extends CompositeBuilder.ForElement<DELEGATE, E> implements MetadataBuilder.ForContainer<E> {

        ForContainer(List<DELEGATE> delegates) {
            super(delegates);
        }

        @Override
        public final boolean isCascade(Metas<E> meta) {
            return delegates.stream().anyMatch(d -> d.isCascade(meta));
        }

        @Override
        public final Set<GroupConversion> getGroupConversions(Metas<E> meta) {
            return delegates.stream().map(d -> d.getGroupConversions(meta)).flatMap(Collection::stream)
                .collect(ToUnmodifiable.set());
        }

        @Override
        public final Map<ContainerElementKey, MetadataBuilder.ForContainer<AnnotatedType>> getContainerElementTypes(
            Metas<E> meta) {
            return merge(b -> b.getContainerElementTypes(meta), CompositeBuilder.ForContainer::new);
        }
    }

    private class ForExecutable<DELEGATE extends MetadataBuilder.ForExecutable<E>, E extends Executable>
        extends Delegator<DELEGATE> implements MetadataBuilder.ForExecutable<E> {

        ForExecutable(List<DELEGATE> delegates) {
            super(delegates);
        }

        @Override
        public MetadataBuilder.ForContainer<E> getReturnValue(Metas<E> meta) {
            return new CompositeBuilder.ForContainer<>(
                delegates.stream().map(d -> d.getReturnValue(meta)).collect(Collectors.toList()));
        }

        @Override
        public List<MetadataBuilder.ForContainer<Parameter>> getParameters(Metas<E> meta) {
            final List<List<MetadataBuilder.ForContainer<Parameter>>> parameterLists =
                delegates.stream().map(d -> d.getParameters(meta)).collect(Collectors.toList());

            final Set<Integer> parameterCounts = parameterLists.stream().map(List::size).collect(Collectors.toSet());
            Validate.validState(parameterCounts.size() == 1, "Mismatched parameter counts: %s", parameterCounts);

            return IntStream.range(0, parameterCounts.iterator().next().intValue())
                .mapToObj(n -> new CompositeBuilder.ForContainer<>(parameterLists.get(n)))
                .collect(ToUnmodifiable.list());
        }

        @Override
        public MetadataBuilder.ForElement<E> getCrossParameter(Metas<E> meta) {
            return new CompositeBuilder.ForElement<MetadataBuilder.ForElement<E>, E>(
                delegates.stream().map(d -> d.getCrossParameter(meta)).collect(Collectors.toList()));
        }
    }

    public static CompositeBuilder with(AnnotationBehaviorMergeStrategy annotationBehaviorStrategy) {
        return new CompositeBuilder(annotationBehaviorStrategy);
    }

    private final AnnotationBehaviorMergeStrategy annotationBehaviorStrategy;

    CompositeBuilder(AnnotationBehaviorMergeStrategy annotationBehaviorMergeStrategy) {
        super();
        this.annotationBehaviorStrategy =
            Validate.notNull(annotationBehaviorMergeStrategy, "annotationBehaviorMergeStrategy");
    }

    public Collector<MetadataBuilder.ForBean, ?, MetadataBuilder.ForBean> compose() {
        return Collectors.collectingAndThen(Collectors.toList(), CompositeBuilder.ForBean::new);
    }

    protected <E extends AnnotatedElement> Map<Scope, Annotation[]> getConstraintsByScope(
        CompositeBuilder.ForElement<? extends MetadataBuilder.ForElement<E>, E> composite, Metas<E> meta) {
        return Collections.singletonMap(Scope.LOCAL_ELEMENT, composite.getDeclaredConstraints(meta));
    }

    protected List<Class<?>> getGroupSequence(CompositeBuilder.ForClass composite, Metas<Class<?>> meta) {
        final List<List<Class<?>>> groupSequence =
            composite.delegates.stream().map(d -> d.getGroupSequence(meta)).collect(Collectors.toList());
        Validate.validState(groupSequence.size() <= 1,
            "group sequence returned from multiple composite class metadata builders");
        return groupSequence.isEmpty() ? null : groupSequence.get(0);
    }
}
