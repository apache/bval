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
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.bval.jsr.groups.GroupConversion;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.ObjectUtils;
import org.apache.bval.util.Validate;

public class EmptyBuilder {
    private static final Map<AnnotationBehavior, EmptyBuilder> INSTANCES = new EnumMap<>(AnnotationBehavior.class);

    public static EmptyBuilder instance() {
        return instance(AnnotationBehavior.ABSTAIN);
    }

    public static EmptyBuilder instance(AnnotationBehavior annotationBehavior) {
        return INSTANCES.computeIfAbsent(annotationBehavior, EmptyBuilder::new);
    }

    private class Level implements HasAnnotationBehavior {

        @Override
        public final AnnotationBehavior getAnnotationBehavior() {
            return annotationBehavior;
        }
    }

    private class ForBean extends Level implements MetadataBuilder.ForBean {
        private final Lazy<EmptyBuilder.ForClass> forClass = new Lazy<>(EmptyBuilder.ForClass::new);

        @Override
        public MetadataBuilder.ForClass getClass(Meta<Class<?>> meta) {
            return forClass.get();
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Field>> getFields(Meta<Class<?>> meta) {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Method>> getGetters(Meta<Class<?>> meta) {
            return Collections.emptyMap();
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Constructor<?>>> getConstructors(Meta<Class<?>> meta) {
            return Collections.emptyMap();
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Method>> getMethods(Meta<Class<?>> meta) {
            return Collections.emptyMap();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    }

    private class ForElement<E extends AnnotatedElement> extends Level implements MetadataBuilder.ForElement<E> {

        @Override
        public final Annotation[] getDeclaredConstraints(Meta<E> meta) {
            return ObjectUtils.EMPTY_ANNOTATION_ARRAY;
        }
    }

    private class ForClass extends ForElement<Class<?>> implements MetadataBuilder.ForClass {

        @Override
        public List<Class<?>> getGroupSequence(Meta<Class<?>> meta) {
            return null;
        }
    }

    private class ForContainer<E extends AnnotatedElement> extends ForElement<E>
        implements MetadataBuilder.ForContainer<E> {

        @Override
        public boolean isCascade(Meta<E> meta) {
            return false;
        }

        @Override
        public Set<GroupConversion> getGroupConversions(Meta<E> meta) {
            return Collections.emptySet();
        }

        @Override
        public Map<ContainerElementKey, MetadataBuilder.ForContainer<AnnotatedType>> getContainerElementTypes(
            Meta<E> meta) {
            return Collections.emptyMap();
        }
    }

    private class ForExecutable<E extends Executable> extends Level implements MetadataBuilder.ForExecutable<E> {

        @SuppressWarnings("unchecked")
        @Override
        public MetadataBuilder.ForElement<E> getCrossParameter(Meta<E> meta) {
            return forElement.get();
        }

        @Override
        public List<MetadataBuilder.ForContainer<Parameter>> getParameters(Meta<E> meta) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        @Override
        public MetadataBuilder.ForContainer<E> getReturnValue(Meta<E> meta) {
            return forContainer.get();
        }
    }

    private final AnnotationBehavior annotationBehavior;
    private final Lazy<EmptyBuilder.ForBean> forBean;
    @SuppressWarnings("rawtypes")
    private final Lazy<EmptyBuilder.ForContainer> forContainer;
    @SuppressWarnings("rawtypes")
    private final Lazy<EmptyBuilder.ForExecutable> forExecutable;
    @SuppressWarnings("rawtypes")
    private final Lazy<EmptyBuilder.ForElement> forElement;

    private EmptyBuilder(AnnotationBehavior annotationBehavior) {
        super();
        this.annotationBehavior = Validate.notNull(annotationBehavior, "annotationBehavior");
        forBean = new Lazy<>(EmptyBuilder.ForBean::new);
        forContainer = new Lazy<>(EmptyBuilder.ForContainer::new);
        forExecutable = new Lazy<>(EmptyBuilder.ForExecutable::new);
        forElement = new Lazy<>(EmptyBuilder.ForElement::new);
    }

    public MetadataBuilder.ForBean forBean() {
        return forBean.get();
    }

    @SuppressWarnings("unchecked")
    public <E extends AnnotatedElement> MetadataBuilder.ForContainer<E> forContainer() {
        return forContainer.get();
    }

    @SuppressWarnings("unchecked")
    public <E extends Executable> MetadataBuilder.ForExecutable<E> forExecutable() {
        return forExecutable.get();
    }

    @SuppressWarnings("unchecked")
    public <E extends AnnotatedElement> MetadataBuilder.ForElement<E> forElement() {
        return forElement.get();
    }
}
