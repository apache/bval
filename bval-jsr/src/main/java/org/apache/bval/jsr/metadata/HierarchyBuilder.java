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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.validation.ElementKind;
import javax.validation.ParameterNameProvider;
import javax.validation.metadata.Scope;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.groups.GroupConversion;
import org.apache.bval.jsr.util.Methods;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.bval.util.reflection.Reflection.Interfaces;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

@Privilizing(@CallTo(Reflection.class))
public class HierarchyBuilder extends CompositeBuilder {
    static abstract class HierarchyDelegate<E extends AnnotatedElement, T> {
        final T delegate;
        final Meta<E> hierarchyElement;

        HierarchyDelegate(T delegate, Meta<E> hierarchyElement) {
            super();
            this.delegate = Validate.notNull(delegate, "delegate");
            this.hierarchyElement = Validate.notNull(hierarchyElement, "hierarchyElement");
        }

        Meta<E> getHierarchyElement() {
            return hierarchyElement;
        }
    }

    static abstract class ElementDelegate<E extends AnnotatedElement, T extends MetadataBuilder.ForElement<E>>
        extends HierarchyDelegate<E, T> implements MetadataBuilder.ForElement<E> {

        ElementDelegate(T delegate, Meta<E> hierarchyElement) {
            super(delegate, hierarchyElement);
        }
        
        Annotation[] getDeclaredConstraints() {
            return delegate.getDeclaredConstraints(hierarchyElement);
        }

        @Override
        public final Annotation[] getDeclaredConstraints(Meta<E> meta) {
            return getDeclaredConstraints();
        }
    }

    private class BeanDelegate extends HierarchyDelegate<Class<?>, MetadataBuilder.ForBean>
        implements MetadataBuilder.ForBean {

        BeanDelegate(MetadataBuilder.ForBean delegate, Class<?> hierarchyType) {
            super(delegate, new Meta.ForClass(hierarchyType));
        }

        @Override
        public MetadataBuilder.ForClass getClass(Meta<Class<?>> meta) {
            return new ClassDelegate(delegate.getClass(hierarchyElement), hierarchyElement);
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Field>> getFields(Meta<Class<?>> meta) {
            return delegate.getFields(hierarchyElement);
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Constructor<?>>> getConstructors(Meta<Class<?>> meta) {
            // ignore hierarchical ctors:
            return hierarchyElement.equals(meta) ? delegate.getConstructors(hierarchyElement) : Collections.emptyMap();
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Method>> getGetters(Meta<Class<?>> meta) {
            final Map<String, MetadataBuilder.ForContainer<Method>> getters = delegate.getGetters(hierarchyElement);
            final Map<String, MetadataBuilder.ForContainer<Method>> result = new LinkedHashMap<>();

            getters.forEach((k, v) -> {
                final Method getter = Methods.getter(hierarchyElement.getHost(), k);

                Exceptions.raiseIf(getter == null, IllegalStateException::new,
                    "delegate builder specified unknown getter");

                result.put(k, new ContainerDelegate<Method>(v, new Meta.ForMethod(getter)));
            });
            return result;
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Method>> getMethods(Meta<Class<?>> meta) {
            final Map<Signature, MetadataBuilder.ForExecutable<Method>> methods = delegate.getMethods(hierarchyElement);
            if (methods.isEmpty()) {
                return methods;
            }
            final Map<Signature, MetadataBuilder.ForExecutable<Method>> result = new LinkedHashMap<>();
            methods
                .forEach((k, v) -> result.put(k,
                    new ExecutableDelegate<>(v, new Meta.ForMethod(
                        Reflection.getDeclaredMethod(hierarchyElement.getHost(), k.getName(), k.getParameterTypes())),
                        ParameterNameProvider::getParameterNames)));
            return result;
        }
    }

    private class ClassDelegate extends ElementDelegate<Class<?>, MetadataBuilder.ForClass>
        implements MetadataBuilder.ForClass {

        ClassDelegate(MetadataBuilder.ForClass delegate, Meta<Class<?>> hierarchyType) {
            super(delegate, hierarchyType);
        }

        @Override
        public List<Class<?>> getGroupSequence(Meta<Class<?>> meta) {
            return delegate.getGroupSequence(hierarchyElement);
        }
    }

    class ContainerDelegate<E extends AnnotatedElement> extends ElementDelegate<E, MetadataBuilder.ForContainer<E>>
        implements MetadataBuilder.ForContainer<E> {

        ContainerDelegate(MetadataBuilder.ForContainer<E> delegate, Meta<E> hierarchyElement) {
            super(delegate, hierarchyElement);
        }
        
        boolean isCascade() {
            return delegate.isCascade(hierarchyElement);
        }

        @Override
        public final boolean isCascade(Meta<E> meta) {
            return isCascade();
        }

        Set<GroupConversion> getGroupConversions() {
            return delegate.getGroupConversions(hierarchyElement);
        }

        @Override
        public final Set<GroupConversion> getGroupConversions(Meta<E> meta) {
            return getGroupConversions();
        }

        @Override
        public Map<ContainerElementKey, MetadataBuilder.ForContainer<AnnotatedType>> getContainerElementTypes(
            Meta<E> meta) {
            final Map<ContainerElementKey, MetadataBuilder.ForContainer<AnnotatedType>> containerElementTypes =
                delegate.getContainerElementTypes(hierarchyElement);

            final Map<ContainerElementKey, MetadataBuilder.ForContainer<AnnotatedType>> result = new LinkedHashMap<>();

            containerElementTypes.forEach((k, v) -> {
                result.put(k, new ContainerDelegate<>(v, new Meta.ForContainerElement(hierarchyElement, k)));
            });
            return result;
        }
    }

    private class ExecutableDelegate<E extends Executable>
        extends HierarchyDelegate<E, MetadataBuilder.ForExecutable<E>> implements MetadataBuilder.ForExecutable<E> {

        final BiFunction<ParameterNameProvider, E, List<String>> getParameterNames;

        ExecutableDelegate(MetadataBuilder.ForExecutable<E> delegate, Meta<E> hierarchyElement,
            BiFunction<ParameterNameProvider, E, List<String>> getParameterNames) {
            super(delegate, hierarchyElement);
            this.getParameterNames = Validate.notNull(getParameterNames, "getParameterNames");
        }

        @Override
        public MetadataBuilder.ForContainer<E> getReturnValue(Meta<E> meta) {
            return new ContainerDelegate<>(delegate.getReturnValue(hierarchyElement), hierarchyElement);
        }

        @Override
        public MetadataBuilder.ForElement<E> getCrossParameter(Meta<E> meta) {
            return new CrossParameterDelegate<>(delegate.getCrossParameter(hierarchyElement), hierarchyElement);
        }

        @Override
        public List<MetadataBuilder.ForContainer<Parameter>> getParameters(Meta<E> meta) {
            final List<MetadataBuilder.ForContainer<Parameter>> parameterDelegates =
                delegate.getParameters(hierarchyElement);

            if (parameterDelegates.isEmpty()) {
                return parameterDelegates;
            }
            final List<Meta<Parameter>> metaParameters = getMetaParameters(hierarchyElement, getParameterNames);

            Exceptions.raiseUnless(metaParameters.size() == parameterDelegates.size(), IllegalStateException::new,
                "Got wrong number of parameter delegates for %s", meta.getHost());

            return IntStream.range(0, parameterDelegates.size())
                .mapToObj(n -> new ContainerDelegate<>(parameterDelegates.get(n), metaParameters.get(n)))
                .collect(Collectors.toList());
        }
    }

    private class CrossParameterDelegate<E extends Executable>
        extends ElementDelegate<E, MetadataBuilder.ForElement<E>> implements MetadataBuilder.ForElement<E> {

        CrossParameterDelegate(MetadataBuilder.ForElement<E> delegate, Meta<E> hierarchyElement) {
            super(delegate, hierarchyElement);
        }
    }

    private class ForCrossParameter<E extends Executable>
        extends CompositeBuilder.ForElement<CrossParameterDelegate<E>, E> {

        ForCrossParameter(List<CrossParameterDelegate<E>> delegates) {
            super(delegates);
            Liskov.validateCrossParameterHierarchy(delegates);
        }
    }

    private class ForContainer<E extends AnnotatedElement>
        extends CompositeBuilder.ForContainer<ContainerDelegate<E>, E> {

        ForContainer(List<ContainerDelegate<E>> delegates, ElementKind elementKind) {
            super(delegates);
            Liskov.validateContainerHierarchy(delegates, Validate.notNull(elementKind, "elementKind"));
        }
    }

    private final Function<Class<?>, MetadataBuilder.ForBean> getBeanBuilder;

    public HierarchyBuilder(ApacheValidatorFactory validatorFactory,
        Function<Class<?>, MetadataBuilder.ForBean> getBeanBuilder) {
        super(validatorFactory, AnnotationBehaviorMergeStrategy.first());
        this.getBeanBuilder = Validate.notNull(getBeanBuilder, "getBeanBuilder function was null");
    }

    public MetadataBuilder.ForBean forBean(Class<?> beanClass) {
        final List<MetadataBuilder.ForBean> delegates = new ArrayList<>();

        /*
         * First add the delegate for the requested bean class, forcing to empty if absent. This is important for the
         * same reason that we use the #first() AnnotationBehaviorMergeStrategy: namely, that custom metadata overrides
         * only from the immediately available mapping per the BV spec.
         */
        delegates.add(Optional.of(beanClass).map(getBeanBuilder).orElseGet(() -> EmptyBuilder.instance().forBean()));

        // iterate the hierarchy, skipping the first (i.e. beanClass handled above)
        final Iterator<Class<?>> hierarchy = Reflection.hierarchy(beanClass, Interfaces.INCLUDE).iterator();
        hierarchy.next();

        // skip Object.class; skip null/empty hierarchy builders, mapping others to BeanDelegate
        hierarchy.forEachRemaining(t -> Optional.of(t).filter(Predicate.isEqual(Object.class).negate())
            .map(getBeanBuilder).filter(b -> !b.isEmpty()).map(b -> new BeanDelegate(b, t)).ifPresent(delegates::add));

        // if we have nothing but empty builders (which should only happen for
        // absent custom metadata), return empty:
        if (delegates.stream().allMatch(MetadataBuilder.ForBean::isEmpty)) {
            return EmptyBuilder.instance().forBean();
        }
        return delegates.stream().collect(compose());
    }

    @Override
    protected <E extends AnnotatedElement> Map<Scope, Annotation[]> getConstraintsByScope(
        CompositeBuilder.ForElement<? extends MetadataBuilder.ForElement<E>, E> composite, Meta<E> meta) {

        final Iterator<? extends MetadataBuilder.ForElement<E>> iter = composite.delegates.iterator();

        final Map<Scope, Annotation[]> result = new EnumMap<>(Scope.class);
        result.put(Scope.LOCAL_ELEMENT, iter.next().getDeclaredConstraints(meta));

        if (iter.hasNext()) {
            final List<Annotation> hierarchyConstraints = new ArrayList<>();
            iter.forEachRemaining(d -> Collections.addAll(hierarchyConstraints, d.getDeclaredConstraints(meta)));
            result.put(Scope.HIERARCHY, hierarchyConstraints.toArray(new Annotation[hierarchyConstraints.size()]));
        }
        return result;
    }

    @Override
    protected List<Class<?>> getGroupSequence(CompositeBuilder.ForClass composite, Meta<Class<?>> meta) {
        return composite.delegates.get(0).getGroupSequence(meta);
    }

    @Override
    protected <DELEGATE extends MetadataBuilder.ForContainer<E>, E extends AnnotatedElement> MetadataBuilder.ForContainer<E> forContainer(
        List<DELEGATE> delegates, Meta<E> meta, ElementKind elementKind) {

        if (delegates.isEmpty()) {
            return super.forContainer(delegates, meta, elementKind);
        }
        final List<ContainerDelegate<E>> hierarchyDelegates = delegates.stream()
            .<ContainerDelegate<E>> map(
                d -> d instanceof ContainerDelegate<?> ? (ContainerDelegate<E>) d : new ContainerDelegate<>(d, meta))
            .collect(Collectors.toList());

        @SuppressWarnings("unchecked")
        final CompositeBuilder.ForContainer<DELEGATE, E> result =
            (CompositeBuilder.ForContainer<DELEGATE, E>) new HierarchyBuilder.ForContainer<E>(hierarchyDelegates,
                elementKind);

        return result;
    }

    @Override
    protected <DELEGATE extends MetadataBuilder.ForElement<E>, E extends Executable> MetadataBuilder.ForElement<E> forCrossParameter(
        List<DELEGATE> delegates, Meta<E> meta) {

        if (delegates.isEmpty()) {
            return super.forCrossParameter(delegates, meta);
        }
        final List<CrossParameterDelegate<E>> hierarchyDelegates =
            delegates.stream()
                .<CrossParameterDelegate<E>> map(d -> d instanceof CrossParameterDelegate<?>
                    ? (CrossParameterDelegate<E>) d : new CrossParameterDelegate<>(d, meta))
                .collect(Collectors.toList());
        return new HierarchyBuilder.ForCrossParameter<>(hierarchyDelegates);
    }
}
