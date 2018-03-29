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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.validation.ConstraintViolation;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.CascadableDescriptor;
import javax.validation.metadata.ContainerDescriptor;
import javax.validation.metadata.ContainerElementTypeDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.PropertyDescriptor;
import javax.validation.valueextraction.ValueExtractor;
import javax.validation.valueextraction.ValueExtractor.ValueReceiver;

import org.apache.bval.jsr.ApacheFactoryContext;
import org.apache.bval.jsr.ConstraintViolationImpl;
import org.apache.bval.jsr.GraphContext;
import org.apache.bval.jsr.descriptor.BeanD;
import org.apache.bval.jsr.descriptor.CascadableContainerD;
import org.apache.bval.jsr.descriptor.ComposedD;
import org.apache.bval.jsr.descriptor.ConstraintD;
import org.apache.bval.jsr.descriptor.ContainerElementTypeD;
import org.apache.bval.jsr.descriptor.ElementD;
import org.apache.bval.jsr.descriptor.PropertyD;
import org.apache.bval.jsr.metadata.ContainerElementKey;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.jsr.util.PathNavigation;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.ObjectWrapper;
import org.apache.bval.util.StringUtils;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.TypeUtils;

public final class ValidateProperty<T> extends ValidationJob<T> {

    interface Strategy<T> {
        default PathNavigation.Callback<?> callback(PathImpl.Builder pathBuilder, FindDescriptor findDescriptor) {
            return new PathNavigation.CompositeCallbackProcedure(Arrays.asList(pathBuilder, findDescriptor));
        }

        default T getRootBean() {
            return null;
        }

        ValidateProperty<T>.Frame<?> frame(ValidateProperty<T> job, PathImpl path);
    }

    static class ForBeanProperty<T> implements Strategy<T> {
        final ApacheFactoryContext validatorContext;
        final T rootBean;
        final GraphContext rootContext;
        final ObjectWrapper<GraphContext> leafContext;
        final ObjectWrapper<Object> value;

        ForBeanProperty(ApacheFactoryContext validatorContext, T bean) {
            super();
            this.validatorContext = validatorContext;
            this.rootBean = bean;
            this.rootContext = new GraphContext(validatorContext, PathImpl.create(), bean);
            this.leafContext = new ObjectWrapper<>(rootContext);
            this.value = new ObjectWrapper<>(bean);
        }

        @Override
        public PathNavigation.Callback<?> callback(PathImpl.Builder pathBuilder, FindDescriptor findDescriptor) {
            return new WalkGraph(validatorContext, pathBuilder, findDescriptor, value,
                (p, v) -> leafContext.accept(p.isRootPath() ? rootContext : rootContext.child(p, v)));
        }

        @Override
        public T getRootBean() {
            return rootBean;
        }

        public GraphContext baseContext(PathImpl path, ApacheFactoryContext validatorContext) {
            return new GraphContext(validatorContext, PathImpl.create(), rootBean).child(path, value.get());
        }

        @Override
        public ValidateProperty<T>.Frame<?> frame(ValidateProperty<T> job, PathImpl path) {
            if (job.descriptor instanceof BeanDescriptor) {
                return job.new LeafFrame<>(leafContext.get());
            }
            return job.new PropertyFrame<PropertyD<?>>(job.new BeanFrame<>(leafContext.get()),
                (PropertyD<?>) job.descriptor, leafContext.get().child(path, value.get()));
        }
    }

    static class ForPropertyValue<T> implements Strategy<T> {
        final Object value;

        ForPropertyValue(Object value) {
            super();
            this.value = value;
        }

        @Override
        public ValidateProperty<T>.Frame<?> frame(ValidateProperty<T> job, PathImpl path) {
            final GraphContext context = new GraphContext(job.validatorContext, path, value);
            if (job.descriptor instanceof BeanDescriptor) {
                return job.new LeafFrame<>(context);
            }
            return job.new PropertyFrame<PropertyD<?>>(null, (PropertyD<?>) job.descriptor, context);
        }
    }

    private interface Step {
        Type type();

        ElementD<?, ?> element();
    }

    private static class DescriptorWrapper implements Step {
        final ElementD<?, ?> wrapped;

        DescriptorWrapper(ElementDescriptor wrapped) {
            super();
            this.wrapped = (ElementD<?, ?>) wrapped;
        }

        @Override
        public Type type() {
            return wrapped.getGenericType();
        }

        @Override
        public ElementD<?, ?> element() {
            return wrapped;
        }
    }

    private static class TypeWrapper implements Step {
        final ApacheFactoryContext validatorContext;
        final Type type;

        TypeWrapper(ApacheFactoryContext validatorContext, Type type) {
            super();
            this.validatorContext = validatorContext;
            this.type = type;
        }

        @Override
        public Type type() {
            return type;
        }

        @Override
        public ElementD<?, ?> element() {
            final Class<?> beanClass = TypeUtils.getRawType(type, null);
            return beanClass == null ? null
                : (BeanD<?>) validatorContext.getDescriptorManager().getBeanDescriptor(beanClass);
        }
    }

    private static class FindDescriptor implements PathNavigation.Callback<ElementD<?, ?>> {
        private final ApacheFactoryContext validatorContext;
        Step current;

        FindDescriptor(ApacheFactoryContext validatorContext, Class<?> beanClass) {
            this.validatorContext = validatorContext;
            this.current = new DescriptorWrapper(validatorContext.getDescriptorManager().getBeanDescriptor(beanClass));
        }

        @Override
        public void handleProperty(String name) {
            final ElementDescriptor element = current.element();
            final BeanD<?> bean;
            if (element instanceof BeanD<?>) {
                bean = (BeanD<?>) element;
            } else {
                bean = (BeanD<?>) validatorContext.getDescriptorManager().getBeanDescriptor(element.getElementClass());
            }
            final PropertyDescriptor property = bean.getProperty(name);
            if (property == null) {
                Exceptions.raise(IllegalArgumentException::new, "Unknown property %s of %s", name,
                    bean.getElementClass());
            }
            current = new DescriptorWrapper(property);
        }

        @Override
        public void handleIndexOrKey(String value) {
            handleGenericInIterable();
        }

        @Override
        public void handleGenericInIterable() {
            final ElementDescriptor desc = current.element();
            if (desc instanceof CascadableContainerD<?, ?>) {
                final Step containerElement = handleContainerElement((CascadableContainerD<?, ?>) desc);
                if (containerElement != null) {
                    current = containerElement;
                    return;
                }
            }
            current = handleElementByType(current.type());
        }

        private Step handleContainerElement(CascadableContainerD<?, ?> desc) {
            final Set<ContainerElementTypeDescriptor> containerElements = desc.getConstrainedContainerElementTypes();
            if (containerElements.isEmpty()) {
                return null;
            }
            final ContainerElementTypeDescriptor element;
            if (containerElements.size() == 1) {
                element = containerElements.iterator().next();
            } else {
                final Collection<TypeVariable<?>> wellKnown = Arrays.asList(MAP_VALUE, ITERABLE_ELEMENT);

                final Optional<ContainerElementTypeD> found =
                    containerElements.stream().<ContainerElementTypeD> map(ContainerElementTypeD.class::cast)
                        .filter(d -> wellKnown.stream().anyMatch(d.getKey()::represents)).findFirst();

                if (!found.isPresent()) {
                    return null;
                }
                element = found.get();
            }
            return new DescriptorWrapper(element);
        }

        private Step handleElementByType(Type type) {
            Type elementType;

            if (TypeUtils.isArrayType(type)) {
                elementType = TypeUtils.getArrayComponentType(type);
            } else if (TypeUtils.isAssignable(type, Map.class)) {
                elementType =
                    Optional.ofNullable(TypeUtils.getTypeArguments(type, Map.class).get(MAP_VALUE)).orElse(MAP_VALUE);
            } else if (TypeUtils.isAssignable(type, Iterable.class)) {
                elementType =
                    Optional.ofNullable(TypeUtils.getTypeArguments(type, Iterable.class).get(ITERABLE_ELEMENT))
                        .orElse(ITERABLE_ELEMENT);
            } else {
                throw Exceptions.create(IllegalArgumentException::new, "Unable to resolve element type of %s", type);
            }
            return new TypeWrapper(validatorContext, elementType);
        }

        @Override
        public ElementD<?, ?> result() {
            return current.element();
        }
    }

    private static class WalkGraph extends PathNavigation.CallbackProcedure {
        final ApacheFactoryContext validatorContext;
        final PathImpl.Builder pathBuilder;
        final FindDescriptor findDescriptor;
        final ObjectWrapper<Object> value;
        final BiConsumer<PathImpl, Object> recordLeaf;

        WalkGraph(ApacheFactoryContext validatorContext, PathImpl.Builder pathBuilder, FindDescriptor findDescriptor,
            ObjectWrapper<Object> value, BiConsumer<PathImpl, Object> recordLeaf) {
            this.validatorContext = validatorContext;
            this.pathBuilder = pathBuilder;
            this.findDescriptor = findDescriptor;
            this.value = value;
            this.recordLeaf = recordLeaf;
        }

        @Override
        public void handleProperty(String name) {
            final PathImpl p = PathImpl.copy(pathBuilder.result());
            pathBuilder.handleProperty(name);
            if (value.optional().isPresent()) {
                recordLeaf.accept(p, value.get());

                findDescriptor.handleProperty(name);

                final PropertyD<?> propertyD =
                    ComposedD.unwrap(findDescriptor.current.element(), PropertyD.class).findFirst().get();
                try {
                    value.accept(propertyD.getValue(value.get()));
                } catch (Exception e) {
                    Exceptions.raise(IllegalStateException::new, e, "Unable to get value of property %s",
                        propertyD.getPropertyName());
                }
            }
        }

        @Override
        public void handleIndexOrKey(final String indexOrKey) {
            pathBuilder.handleIndexOrKey(indexOrKey);
            findDescriptor.handleIndexOrKey(indexOrKey);
            if (value.optional().isPresent()) {
                ElementDescriptor element = findDescriptor.current.element();
                if (element instanceof ContainerElementTypeD) {
                    value.accept(handleContainer(value.get(), ((ContainerElementTypeD) element).getKey(), indexOrKey));
                } else {
                    value.accept(handleBasic(value.get(), indexOrKey));

                    if (element == null && value.optional().isPresent()) {
                        // no generic info available at some previous index level; fall back to runtime type of value
                        // and repair structure of findDescriptor:
                        findDescriptor.current = new TypeWrapper(validatorContext, value.get().getClass());
                        element = findDescriptor.current.element();
                    }
                    if (element instanceof BeanDescriptor) {
                        recordLeaf.accept(PathImpl.copy(pathBuilder.result()), value.get());
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        private Object handleContainer(Object o, ContainerElementKey key, String indexOrKey) {
            @SuppressWarnings("rawtypes")
            final ValueExtractor valueExtractor = validatorContext.getValueExtractors().find(key);

            final ObjectWrapper<Object> result = new ObjectWrapper<>();
            valueExtractor.extractValues(o, new ValueReceiver() {

                @Override
                public void indexedValue(String nodeName, int index, Object object) {
                    if (Integer.toString(index).equals(indexOrKey)) {
                        result.accept(object);
                    }
                }

                @Override
                public void iterableValue(String nodeName, Object object) {
                    // ?
                    result.accept(object);
                }

                @Override
                public void keyedValue(String nodeName, Object key, Object object) {
                    if (String.valueOf(key).equals(indexOrKey)) {
                        result.accept(object);
                    }
                }

                @Override
                public void value(String nodeName, Object object) {
                    // ?
                    result.accept(object);
                }
            });
            return result.get();
        }

        private Object handleBasic(Object o, String indexOrKey) {
            if (Map.class.isInstance(o)) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                    if (String.valueOf(e.getKey()).equals(indexOrKey)) {
                        return e.getValue();
                    }
                }
            } else {
                try {
                    final int index = Integer.parseInt(indexOrKey);
                    if (index < 0) {
                        Exceptions.raise(IllegalArgumentException::new, "Invalid index %d", index);
                    }
                    if (o != null && TypeUtils.isArrayType(o.getClass())) {
                        if (Array.getLength(o) > index) {
                            return Array.get(o, index);
                        }
                    } else if (List.class.isInstance(o)) {
                        final List<?> l = (List<?>) o;
                        if (l.size() > index) {
                            return l.get(index);
                        }
                    } else if (Iterable.class.isInstance(o)) {
                        int i = -1;
                        for (Object e : (Iterable<?>) o) {
                            if (++i == index) {
                                return e;
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                }
            }
            return null;
        }

        @Override
        public void handleGenericInIterable() {
            throw new UnsupportedOperationException("Cannot resolve generic inIterable against actual object graph");
        }
    }

    class LeafFrame<L> extends BeanFrame<L> {

        LeafFrame(GraphContext context) {
            super(context);
        }

        @Override
        protected ValidationJob<T>.Frame<?> propertyFrame(PropertyD<?> d, GraphContext context) {
            return new PropertyFrame<>(this, d, context);
        }
    }

    class PropertyFrame<D extends ElementD<?, ?> & CascadableDescriptor & ContainerDescriptor> extends SproutFrame<D> {

        PropertyFrame(ValidationJob<T>.Frame<?> parent, D descriptor, GraphContext context) {
            super(parent, descriptor, context);
        }

        @Override
        void recurse(Class<?> group, Consumer<ConstraintViolation<T>> sink) {
            if (cascade) {
                super.recurse(group, sink);
            }
        }
    }

    private final Strategy<T> strategy;
    private final Class<T> rootBeanClass;
    private final PathImpl propertyPath;
    private final T rootBean;
    private ElementD<?, ?> descriptor;
    private boolean cascade;

    private ValidateProperty(Strategy<T> strategy, ApacheFactoryContext validatorContext, Class<T> rootBeanClass,
        String property, Class<?>[] groups) {
        super(validatorContext, groups);

        Exceptions.raiseIf(StringUtils.isBlank(property), IllegalArgumentException::new,
            "property cannot be null/empty/blank");

        this.strategy = strategy;
        this.rootBeanClass = Validate.notNull(rootBeanClass, IllegalArgumentException::new, "rootBeanClass");

        final PathImpl.Builder pathBuilder = new PathImpl.Builder();
        final FindDescriptor findDescriptor = new FindDescriptor(validatorContext, rootBeanClass);

        PathNavigation.navigate(property, strategy.callback(pathBuilder, findDescriptor));

        this.propertyPath = pathBuilder.result();
        this.descriptor = findDescriptor.result();
        this.rootBean = strategy.getRootBean();
    }

    ValidateProperty(ApacheFactoryContext validatorContext, Class<T> rootBeanClass, String property, Object value,
        Class<?>[] groups) {
        this(new ForPropertyValue<>(value), validatorContext, rootBeanClass, property, groups);
        if (descriptor == null) {
            // should only occur when the root class is raw

            final Class<?> t;
            if (value == null) {
                t = Object.class;
            } else {
                t = value.getClass();
            }
            descriptor = (ElementD<?, ?>) validatorContext.getDescriptorManager().getBeanDescriptor(t);
        } else {
            final Class<?> propertyType = descriptor.getElementClass();
            if (!TypeUtils.isInstance(value, propertyType)) {
                Exceptions.raise(IllegalArgumentException::new, "%s is not an instance of %s", value, propertyType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    ValidateProperty(ApacheFactoryContext validatorContext, T bean, String property, Class<?>[] groups)
        throws Exception {
        this(new ForBeanProperty<>(validatorContext, bean), validatorContext,
            (Class<T>) Validate.notNull(bean, IllegalArgumentException::new, "bean").getClass(), property, groups);

        if (descriptor == null) {
            Exceptions.raise(IllegalArgumentException::new, "Could not resolve property name/path: %s", property);
        }
    }

    public ValidateProperty<T> cascade(boolean cascade) {
        this.cascade = cascade;
        return this;
    }

    @Override
    protected Frame<?> computeBaseFrame() {
        // TODO assign bean as its own property and figure out what to do

        return strategy.frame(this, propertyPath);
    }

    @Override
    protected Class<T> getRootBeanClass() {
        return rootBeanClass;
    }

    @Override
    ConstraintViolationImpl<T> createViolation(String messageTemplate, String message,
        ConstraintValidatorContextImpl<T> context, PathImpl propertyPath) {
        return new ConstraintViolationImpl<>(messageTemplate, message, rootBean, context.getFrame().getBean(),
            propertyPath, context.getFrame().context.getValue(), context.getConstraintDescriptor(), rootBeanClass,
            context.getConstraintDescriptor().unwrap(ConstraintD.class).getDeclaredOn(), null, null);
    }
}
