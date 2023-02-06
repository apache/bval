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
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.validation.ConstraintDeclarationException;
import jakarta.validation.ConstraintTarget;
import jakarta.validation.GroupSequence;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraintvalidation.ValidationTarget;
import jakarta.validation.groups.ConvertGroup;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.ConstraintAnnotationAttributes;
import org.apache.bval.jsr.groups.GroupConversion;
import org.apache.bval.jsr.util.AnnotationsManager;
import org.apache.bval.jsr.util.Methods;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.ObjectUtils;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

@Privilizing(@CallTo(Reflection.class))
public class ReflectionBuilder {

    private class ForBean<T> implements MetadataBuilder.ForBean<T> {
        private final Meta<Class<T>> meta;

        ForBean(Meta<Class<T>> meta) {
            super();
            this.meta = Validate.notNull(meta, "meta");
        }

        @Override
        public MetadataBuilder.ForClass<T> getClass(Meta<Class<T>> ignored) {
            return new ReflectionBuilder.ForClass<>(meta);
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Field>> getFields(Meta<Class<T>> ignored) {
            final Field[] declaredFields = Reflection.getDeclaredFields(meta.getHost());
            if (declaredFields.length == 0) {
                return Collections.emptyMap();
            }
            return Stream.of(declaredFields).filter(f -> !(Modifier.isStatic(f.getModifiers()) || f.isSynthetic()))
                .collect(
                    Collectors.toMap(Field::getName, f -> new ReflectionBuilder.ForContainer<>(new Meta.ForField(f))));
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Method>> getGetters(Meta<Class<T>> ignored) {
            final Method[] declaredMethods = Reflection.getDeclaredMethods(meta.getHost());
            if (declaredMethods.length == 0) {
                return Collections.emptyMap();
            }
            final Map<String, Set<Method>> getters = new HashMap<>();
            for (Method m : declaredMethods) {
                if (Methods.isGetter(m)) {
                    getters.computeIfAbsent(Methods.propertyName(m), k -> new LinkedHashSet<>()).add(m);
                }
            }
            final Map<String, MetadataBuilder.ForContainer<Method>> result = new TreeMap<>();
            getters.forEach((k, methods) -> {
                if ("class".equals(k)) {
                    return;
                }
                final List<MetadataBuilder.ForContainer<Method>> delegates = methods.stream()
                    .map(g -> new ReflectionBuilder.ForContainer<>(new Meta.ForMethod(g))).collect(Collectors.toList());
                if (delegates.isEmpty()) {
                    return;
                }
                final MetadataBuilder.ForContainer<Method> builder;
                if (delegates.size() == 1) {
                    builder = delegates.get(0);
                } else {
                    builder = compositeBuilder.get().new ForContainer<>(delegates);
                }
                result.put(k, builder);
            });
            return result;
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Constructor<? extends T>>> getConstructors(Meta<Class<T>> ignored) {
            final Constructor<? extends T>[] declaredConstructors = Reflection.getDeclaredConstructors(meta.getHost());
            if (declaredConstructors.length == 0) {
                return Collections.emptyMap();
            }
            return Stream.of(declaredConstructors).collect(
                Collectors.toMap(Signature::of, c -> new ReflectionBuilder.ForExecutable<>(new Meta.ForConstructor<>(c),
                    ParameterNameProvider::getParameterNames)));
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Method>> getMethods(Meta<Class<T>> ignored) {
            final Method[] declaredMethods = Reflection.getDeclaredMethods(meta.getHost());
            if (declaredMethods.length == 0) {
                return Collections.emptyMap();
            }
            final Map<Signature, Set<Method>> methodsBySignature = new HashMap<>();
            for (Method m : declaredMethods) {
                if (!Modifier.isStatic(m.getModifiers())) {
                    methodsBySignature.computeIfAbsent(Signature.of(m), k -> new LinkedHashSet<>()).add(m);
                }
            }
            final Map<Signature, MetadataBuilder.ForExecutable<Method>> result = new TreeMap<>();

            // we can't filter the getters since they can be validated, todo: read the config to know if we need or not

            methodsBySignature.forEach((sig, methods) -> {
                final List<MetadataBuilder.ForExecutable<Method>> delegates =
                    methods.stream().map(g -> new ReflectionBuilder.ForExecutable<>(new Meta.ForMethod(g),
                        ParameterNameProvider::getParameterNames)).collect(Collectors.toList());
                if (delegates.isEmpty()) {
                    return;
                }
                final MetadataBuilder.ForExecutable<Method> builder;
                if (delegates.size() == 1) {
                    builder = delegates.get(0);
                } else {
                    builder = compositeBuilder.get().new ForExecutable<MetadataBuilder.ForExecutable<Method>, Method>(
                        delegates, ParameterNameProvider::getParameterNames);
                }
                result.put(sig, builder);
            });
            return result;
        }
    }

    private abstract class ForElement<E extends AnnotatedElement> implements MetadataBuilder.ForElement<E> {
        final Meta<E> meta;

        ForElement(Meta<E> meta) {
            super();
            this.meta = Validate.notNull(meta, "meta");
        }

        @Override
        public Annotation[] getDeclaredConstraints(Meta<E> ignored) {
            return AnnotationsManager.getDeclaredConstraints(meta);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || this.getClass().isInstance(obj) && ((ForElement<?>) obj).meta.equals(meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getClass(), meta);
        }
    }

    private class ForClass<T> extends ForElement<Class<T>> implements MetadataBuilder.ForClass<T> {

        ForClass(Meta<Class<T>> meta) {
            super(meta);
        }

        @Override
        public List<Class<?>> getGroupSequence(Meta<Class<T>> ignored) {
            final GroupSequence groupSequence = AnnotationsManager.getAnnotation(meta.getHost(), GroupSequence.class);
            return groupSequence == null ? null : Collections.unmodifiableList(Arrays.asList(groupSequence.value()));
        }
    }

    private class ForContainer<E extends AnnotatedElement> extends ReflectionBuilder.ForElement<E>
        implements MetadataBuilder.ForContainer<E> {

        ForContainer(Meta<E> meta) {
            super(meta);
        }

        @Override
        public Map<ContainerElementKey, MetadataBuilder.ForContainer<AnnotatedType>> getContainerElementTypes(
            Meta<E> ignored) {
            final AnnotatedType annotatedType = meta.getAnnotatedType();
            if (annotatedType instanceof AnnotatedParameterizedType) {

                final AnnotatedParameterizedType container = (AnnotatedParameterizedType) annotatedType;

                final Map<ContainerElementKey, MetadataBuilder.ForContainer<AnnotatedType>> result = new TreeMap<>();

                final AnnotatedType[] typeArgs = container.getAnnotatedActualTypeArguments();
                for (int i = 0; i < typeArgs.length; i++) {
                    final ContainerElementKey key = new ContainerElementKey(container, i);
                    result.put(key, new ReflectionBuilder.ForContainer<>(new Meta.ForContainerElement(meta, key)));
                }
                return result;
            }
            return Collections.emptyMap();
        }

        @Override
        public boolean isCascade(Meta<E> ignored) {
            return AnnotationsManager.isAnnotationDirectlyPresent(meta.getHost(), Valid.class);
        }

        @Override
        public Set<GroupConversion> getGroupConversions(Meta<E> ignored) {
            return Stream.of(AnnotationsManager.getDeclaredAnnotationsByType(meta.getHost(), ConvertGroup.class))
                .map(cg -> GroupConversion.from(cg.from()).to(cg.to())).collect(ToUnmodifiable.set());
        }
    }

    private class ForExecutable<E extends Executable> implements MetadataBuilder.ForExecutable<E> {

        final Meta<E> meta;
        final BiFunction<ParameterNameProvider, E, List<String>> getParameterNames;

        ForExecutable(Meta<E> meta, BiFunction<ParameterNameProvider,E, List<String>> getParameterNames) {
            super();
            this.meta = Validate.notNull(meta, "meta");
            this.getParameterNames = Validate.notNull(getParameterNames, "getParameterNames");
        }

        @Override
        public List<MetadataBuilder.ForContainer<Parameter>> getParameters(Meta<E> ignored) {
            final Parameter[] parameters = meta.getHost().getParameters();
            if (parameters.length == 0) {
                return Collections.emptyList();
            }
            final List<String> parameterNames = getParameterNames.apply(validatorFactory.getParameterNameProvider(),meta.getHost());

            return IntStream.range(0, parameters.length).mapToObj(
                n -> new ReflectionBuilder.ForContainer<>(new Meta.ForParameter(parameters[n], parameterNames.get(n))))
                .collect(ToUnmodifiable.list());
        }

        @Override
        public ForContainer<E> getReturnValue(Meta<E> ignored) {
            return new ReflectionBuilder.ForContainer<E>(meta) {

                @Override
                public Annotation[] getDeclaredConstraints(Meta<E> meta) {
                    return getConstraints(ConstraintTarget.RETURN_VALUE);
                }
            };
        }

        @Override
        public MetadataBuilder.ForElement<E> getCrossParameter(Meta<E> ignored) {
            return new ReflectionBuilder.ForElement<E>(meta) {
                @Override
                public Annotation[] getDeclaredConstraints(Meta<E> meta) {
                    return getConstraints(ConstraintTarget.PARAMETERS);
                }
            };
        }

        private Annotation[] getConstraints(ConstraintTarget constraintTarget) {
            return Optional.of(getConstraintsByTarget()).map(m -> m.get(constraintTarget))
                .map(l -> l.toArray(new Annotation[l.size()])).orElse(ObjectUtils.EMPTY_ANNOTATION_ARRAY);
        }

        private Map<ConstraintTarget, List<Annotation>> getConstraintsByTarget() {
            final Annotation[] declaredConstraints = AnnotationsManager.getDeclaredConstraints(meta);
            if (ObjectUtils.isEmptyArray(declaredConstraints)) {
                return Collections.emptyMap();
            }
            final Map<ConstraintTarget, List<Annotation>> result = new EnumMap<>(ConstraintTarget.class);

            for (Annotation constraint : declaredConstraints) {
                final Class<? extends Annotation> constraintType = constraint.annotationType();
                final Optional<ConstraintTarget> explicitTarget =
                    Optional.of(ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO.analyze(constraintType))
                        .filter(ConstraintAnnotationAttributes.Worker::isValid)
                        .<ConstraintTarget> map(w -> w.read(constraint)).filter(et -> et != ConstraintTarget.IMPLICIT);

                final ConstraintTarget target;

                if (explicitTarget.isPresent()) {
                    target = explicitTarget.get();
                } else {
                    final Set<ValidationTarget> supportedTargets =
                        validatorFactory.getAnnotationsManager().supportedTargets(constraintType);

                    if (supportedTargets.size() == 1) {
                        final ValidationTarget validationTarget = supportedTargets.iterator().next();
                        switch (validationTarget) {
                        case PARAMETERS:
                            target = ConstraintTarget.PARAMETERS;
                            break;
                        case ANNOTATED_ELEMENT:
                            target = ConstraintTarget.RETURN_VALUE;
                            break;
                        default:
                            throw Exceptions.create(IllegalStateException::new, "Unknown %s %s for %s",
                                ValidationTarget.class.getSimpleName(), validationTarget, constraintType);
                        }
                    } else {
                        target = impliedConstraintTarget();
                        if (target == null) {
                            Exceptions.raise(ConstraintDeclarationException::new,
                                "Found %d possible %s types for constraint type %s and no explicit assignment via #%s()",
                                supportedTargets.size(), ValidationTarget.class.getSimpleName(),
                                constraintType.getName(),
                                ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO.getAttributeName());
                        }
                    }
                }
                result.computeIfAbsent(target, k -> new ArrayList<>()).add(constraint);
            }
            return result;
        }

        private ConstraintTarget impliedConstraintTarget() {
            if (meta.getHost().getParameterCount() == 0) {
                return ConstraintTarget.RETURN_VALUE;
            }
            if (Void.TYPE.equals(meta.getType())) {
                return ConstraintTarget.PARAMETERS;
            }
            return null;
        }
    }

    private final ApacheValidatorFactory validatorFactory;
    private final Lazy<CompositeBuilder> compositeBuilder;

    public ReflectionBuilder(ApacheValidatorFactory validatorFactory) {
        super();
        this.validatorFactory = Validate.notNull(validatorFactory, "validatorFactory");
        this.compositeBuilder =
            new Lazy<>(() -> new CompositeBuilder(this.validatorFactory, x -> AnnotationBehavior.ABSTAIN));
    }

    public <T> MetadataBuilder.ForBean<T> forBean(Class<T> beanClass) {
        return new ReflectionBuilder.ForBean<>(new Meta.ForClass<T>(beanClass));
    }
}
