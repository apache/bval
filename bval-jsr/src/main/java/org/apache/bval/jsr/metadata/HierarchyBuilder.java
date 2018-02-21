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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.validation.metadata.Scope;

import org.apache.bval.jsr.descriptor.GroupConversion;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.bval.util.reflection.Reflection.Interfaces;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

@Privilizing(@CallTo(Reflection.class))
public class HierarchyBuilder extends CompositeBuilder {
    private static abstract class HierarchyDelegate<T> {
        final T delegate;

        HierarchyDelegate(T delegate) {
            super();
            this.delegate = Validate.notNull(delegate, "delegate");
        }

        static class ForBean extends HierarchyDelegate<MetadataBuilder.ForBean> implements MetadataBuilder.ForBean {
            final Metas<Class<?>> hierarchyType;

            ForBean(MetadataBuilder.ForBean delegate, Class<?> hierarchyType) {
                super(delegate);
                this.hierarchyType = new Metas.ForClass(hierarchyType);
            }

            @Override
            public MetadataBuilder.ForClass getClass(Metas<Class<?>> meta) {
                return new HierarchyDelegate.ForClass(delegate.getClass(hierarchyType), hierarchyType);
            }

            @Override
            public Map<String, MetadataBuilder.ForContainer<Field>> getFields(Metas<Class<?>> meta) {
                return delegate.getFields(hierarchyType);
            }

            @Override
            public Map<String, MetadataBuilder.ForContainer<Method>> getGetters(Metas<Class<?>> meta) {
                return delegate.getGetters(hierarchyType);
            }

            @Override
            public Map<Signature, MetadataBuilder.ForExecutable<Constructor<?>>> getConstructors(Metas<Class<?>> meta) {
                return delegate.getConstructors(hierarchyType);
            }

            @Override
            public Map<Signature, MetadataBuilder.ForExecutable<Method>> getMethods(Metas<Class<?>> meta) {
                final Map<Signature, MetadataBuilder.ForExecutable<Method>> m = delegate.getMethods(hierarchyType);

                return m;
            }
        }

        static class ForClass extends HierarchyDelegate<MetadataBuilder.ForClass> implements MetadataBuilder.ForClass {

            final Metas<Class<?>> hierarchyType;

            ForClass(MetadataBuilder.ForClass delegate, Metas<Class<?>> hierarchyType) {
                super(delegate);
                this.hierarchyType = hierarchyType;
            }

            @Override
            public Annotation[] getDeclaredConstraints(Metas<Class<?>> meta) {
                return delegate.getDeclaredConstraints(hierarchyType);
            }

            @Override
            public List<Class<?>> getGroupSequence(Metas<Class<?>> meta) {
                return delegate.getGroupSequence(hierarchyType);
            }
        }

        static class ForGetter extends HierarchyDelegate<MetadataBuilder.ForContainer<Method>>
            implements MetadataBuilder.ForContainer<Method> {
            final Metas.ForMethod meta;

            ForGetter(MetadataBuilder.ForContainer<Method> delegate,
                org.apache.bval.jsr.metadata.Metas.ForMethod meta) {
                super(delegate);
                this.meta = Validate.notNull(meta, "meta");
            }

            @Override
            public Annotation[] getDeclaredConstraints(Metas<Method> meta) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean isCascade(Metas<Method> meta) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public Set<GroupConversion> getGroupConversions(Metas<Method> meta) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Map<ContainerElementKey, org.apache.bval.jsr.metadata.MetadataBuilder.ForContainer<AnnotatedType>> getContainerElementTypes(
                Metas<Method> meta) {
                // TODO Auto-generated method stub
                return null;
            }
        }

        static class ForMethod extends HierarchyDelegate<MetadataBuilder.ForExecutable<Method>>
            implements MetadataBuilder.ForExecutable<Method> {
            final Metas.ForMethod meta;

            public ForMethod(MetadataBuilder.ForExecutable<Method> delegate, Metas.ForMethod meta) {
                super(delegate);
                this.meta = Validate.notNull(meta, "meta");
            }

            @Override
            public MetadataBuilder.ForContainer<Method> getReturnValue(Metas<Method> meta) {

                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public MetadataBuilder.ForElement<Method> getCrossParameter(Metas<Method> meta) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public List<MetadataBuilder.ForContainer<Parameter>> getParameters(Metas<Method> meta) {
                // TODO Auto-generated method stub
                return null;
            }
        }
    }

    private final Function<Class<?>, MetadataBuilder.ForBean> getBeanBuilder;

    public HierarchyBuilder(Function<Class<?>, MetadataBuilder.ForBean> getBeanBuilder) {
        super(AnnotationBehaviorMergeStrategy.first());
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

        // iterate the hierarchy, skipping the first (i.e. beanClass handled
        // above)
        final Iterator<Class<?>> hierarchy = Reflection.hierarchy(beanClass, Interfaces.INCLUDE).iterator();
        hierarchy.next();

        // skip Object.class; skip null/empty hierarchy builders, mapping others
        // to HierarchyDelegate
        hierarchy
            .forEachRemaining(t -> Optional.of(t).filter(Predicate.isEqual(Object.class).negate()).map(getBeanBuilder)
                .filter(b -> !b.isEmpty()).map(b -> new HierarchyDelegate.ForBean(b, t)).ifPresent(delegates::add));

        // if we have nothing but empty builders (which should only happen for
        // absent custom metadata), return empty:
        if (delegates.stream().allMatch(MetadataBuilder.ForBean::isEmpty)) {
            return EmptyBuilder.instance().forBean();
        }
        return delegates.stream().collect(compose());
    }

    @Override
    protected <E extends AnnotatedElement> Map<Scope, Annotation[]> getConstraintsByScope(
        CompositeBuilder.ForElement<? extends MetadataBuilder.ForElement<E>, E> composite, Metas<E> meta) {

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
    protected List<Class<?>> getGroupSequence(CompositeBuilder.ForClass composite, Metas<Class<?>> meta) {
        return composite.delegates.get(0).getGroupSequence(meta);
    }
}
