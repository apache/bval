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
package org.apache.bval.jsr.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Constraint;
import javax.validation.ConstraintDefinitionException;
import javax.validation.OverridesAttribute;
import javax.validation.Payload;
import javax.validation.ValidationException;
import javax.validation.constraintvalidation.ValidationTarget;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.ConfigurationImpl;
import org.apache.bval.jsr.ConstraintAnnotationAttributes;
import org.apache.bval.jsr.ConstraintCached.ConstraintValidatorInfo;
import org.apache.bval.jsr.groups.Group;
import org.apache.bval.jsr.groups.Groups;
import org.apache.bval.jsr.groups.GroupsComputer;
import org.apache.bval.jsr.metadata.Meta;
import org.apache.bval.jsr.xml.AnnotationProxyBuilder;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.StringUtils;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Manages (constraint) annotations according to the BV spec.
 * 
 * @since 2.0
 */
@Privilizing(@CallTo(Reflection.class))
public class AnnotationsManager {
    private static final class OverriddenAnnotationSpecifier {
        final Class<? extends Annotation> annotationType;
        final boolean impliesSingleComposingConstraint;
        final int constraintIndex;

        OverriddenAnnotationSpecifier(OverridesAttribute annotation) {
            this(annotation.constraint(), annotation.constraintIndex());
        }

        OverriddenAnnotationSpecifier(Class<? extends Annotation> annotationType, int constraintIndex) {
            super();
            this.annotationType = annotationType;
            this.impliesSingleComposingConstraint = constraintIndex < 0;
            this.constraintIndex = Math.max(constraintIndex, 0);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || !obj.getClass().equals(getClass())) {
                return false;
            }
            final OverriddenAnnotationSpecifier other = (OverriddenAnnotationSpecifier) obj;
            return Objects.equals(annotationType, other.annotationType) && constraintIndex == other.constraintIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(annotationType, constraintIndex);
        }
    }

    private static class Composition {
        final Lazy<Map<OverriddenAnnotationSpecifier, Map<String, String>>> overrides = new Lazy<>(HashMap::new);
        final Annotation[] components;

        Composition(Class<? extends Annotation> annotationType) {
            // TODO detect recursion
            components = getDeclaredConstraints(annotationType);

            if (!isComposed()) {
                return;
            }
            final Map<Class<? extends Annotation>, AtomicInteger> constraintCounts = new HashMap<>();
            for (Annotation a : components) {
                constraintCounts.computeIfAbsent(a.annotationType(), k -> new AtomicInteger()).incrementAndGet();
            }
            // create a map of overridden constraints to overridden attributes:
            for (Method m : Reflection.getDeclaredMethods(annotationType)) {
                final String from = m.getName();
                for (OverridesAttribute overridesAttribute : m.getDeclaredAnnotationsByType(OverridesAttribute.class)) {
                    final String to =
                        Optional.of(overridesAttribute.name()).filter(StringUtils::isNotBlank).orElse(from);

                    final OverriddenAnnotationSpecifier spec = new OverriddenAnnotationSpecifier(overridesAttribute);
                    final int count = constraintCounts.get(spec.annotationType).get();

                    if (spec.impliesSingleComposingConstraint) {
                        Exceptions.raiseUnless(count == 1, ConstraintDefinitionException::new,
                            "Expected a single composing %s constraint", spec.annotationType);
                    } else if (count <= spec.constraintIndex) {
                        Exceptions.raise(ConstraintDefinitionException::new,
                            "Expected at least %s composing %s constraints", spec.constraintIndex + 1,
                            spec.annotationType);
                    }
                    final Map<String, String> attributeMapping =
                        overrides.get().computeIfAbsent(spec, k -> new HashMap<>());

                    if (attributeMapping.containsKey(to)) {
                        Exceptions.raise(ConstraintDefinitionException::new,
                            "Attempt to override %s#%s() index %d from multiple sources",
                            overridesAttribute.constraint(), to, overridesAttribute.constraintIndex());
                    }
                    attributeMapping.put(to, from);
                }
            }
        }

        boolean isComposed() {
            return components.length > 0;
        }

        Annotation[] getComponents(Annotation source) {
            final Class<?>[] groups =
                ConstraintAnnotationAttributes.GROUPS.analyze(source.annotationType()).read(source);

            final Class<? extends Payload>[] payload =
                ConstraintAnnotationAttributes.PAYLOAD.analyze(source.annotationType()).read(source);

            final Map<Class<? extends Annotation>, AtomicInteger> constraintCounts = new HashMap<>();

            return Stream.of(components).map(c -> {
                final int index =
                    constraintCounts.computeIfAbsent(c.annotationType(), k -> new AtomicInteger()).getAndIncrement();

                final AnnotationProxyBuilder<Annotation> proxyBuilder = new AnnotationProxyBuilder<>(c);

                proxyBuilder.setGroups(groups);
                proxyBuilder.setPayload(payload);

                overrides.optional().map(o -> o.get(new OverriddenAnnotationSpecifier(c.annotationType(), index)))
                    .ifPresent(m -> {
                        final Map<String, Object> sourceAttributes = readAttributes(source);
                        m.forEach((k, v) -> proxyBuilder.setValue(k, sourceAttributes.get(v)));
                    });
                return proxyBuilder.isChanged() ? proxyBuilder.createAnnotation() : c;
            }).toArray(Annotation[]::new);
        }
    }

    public static Map<String, Object> readAttributes(Annotation a) {
        final Lazy<Map<String, Object>> result = new Lazy<>(LinkedHashMap::new);

        Stream.of(Reflection.getDeclaredMethods(a.annotationType())).filter(m -> m.getParameterCount() == 0)
            .forEach(m -> {
                final boolean mustUnset = Reflection.setAccessible(m, true);
                try {
                    result.get().put(m.getName(), m.invoke(a));
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    Exceptions.raise(ValidationException::new, e, "Caught exception reading attributes of %s", a);
                } finally {
                    if (mustUnset) {
                        Reflection.setAccessible(m, false);
                    }
                }
            });
        return result.optional().map(Collections::unmodifiableMap).orElseGet(Collections::emptyMap);
    }

    /**
     * Meta-annotation aware.
     * 
     * @param e
     * @param t
     * @return {@code boolean}
     * @see AnnotatedElement#isAnnotationPresent(Class)
     */
    public static boolean isAnnotationPresent(AnnotatedElement e, Class<? extends Annotation> t) {
        if (e.isAnnotationPresent(t)) {
            return true;
        }
        return Stream.of(e.getAnnotations()).map(Annotation::annotationType).anyMatch(a -> isAnnotationPresent(a, t));
    }

    /**
     * Get declared annotations with a particular meta-annotation.
     * 
     * @param e
     * @param meta
     * @return {@link Annotation}[]
     */
    public static Annotation[] getDeclared(AnnotatedElement e, Class<? extends Annotation> meta) {
        return Stream.of(e.getDeclaredAnnotations()).filter(ann -> isAnnotationPresent(ann.annotationType(), meta))
            .toArray(Annotation[]::new);
    }

    /**
     * Accounts for {@link Constraint} meta-annotation AND {@link Repeatable}
     * constraint annotations.
     * 
     * @param meta
     * @return Annotation[]
     */
    public static Annotation[] getDeclaredConstraints(Meta<?> meta) {
        final Annotation[] result = getDeclaredConstraints(meta.getHost());
        final Class<?> dc = meta.getDeclaringClass();
        if (dc.isInterface()) {
            final GroupsComputer groupsComputer = new GroupsComputer();
            // ensure interface group is implied by Default group:
            Stream.of(result).map(c -> {
                final Groups groups = groupsComputer
                    .computeGroups(ConstraintAnnotationAttributes.GROUPS.analyze(c.annotationType()).read(c));
                if (groups.getGroups().stream().anyMatch(Group::isDefault)) {
                    final Set<Class<?>> groupClasses = groups.getGroups().stream().map(Group::getGroup)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                    if (groupClasses.add(dc)) {
                        final AnnotationProxyBuilder<?> proxyBuilder = new AnnotationProxyBuilder<>(c);
                        proxyBuilder.setGroups(groupClasses.toArray(new Class[groupClasses.size()]));
                        return proxyBuilder.createAnnotation();
                    }
                }
                return c;
            }).toArray(n -> result);
        }
        return result;
    }

    private static Annotation[] getDeclaredConstraints(AnnotatedElement e) {
        return Stream.of(e.getDeclaredAnnotations()).flatMap((Function<Annotation, Stream<Annotation>>) a -> {
            final ConstraintAnnotationAttributes.Worker<? extends Annotation> analyzer =
                ConstraintAnnotationAttributes.VALUE.analyze(a.annotationType());
            if (analyzer.isValid()) {
                return Stream.of(analyzer.<Annotation[]> read(a));
            }
            return Stream.of(a);
        }).filter(a -> a.annotationType().isAnnotationPresent(Constraint.class)).toArray(Annotation[]::new);
    }

    public static boolean declaresAttribute(Class<? extends Annotation> annotationType, String name) {
        try {
            annotationType.getDeclaredMethod(name);
            return true;
        } catch (NoSuchMethodException | SecurityException e) {
            return false;
        }
    }

    private final ApacheValidatorFactory validatorFactory;
    private final LRUCache<Class<? extends Annotation>, Composition> compositions;

    public AnnotationsManager(ApacheValidatorFactory validatorFactory) {
        super();
        this.validatorFactory = Validate.notNull(validatorFactory);
        final String cacheSize =
            validatorFactory.getProperties().get(ConfigurationImpl.Properties.CONSTRAINTS_CACHE_SIZE);
        try {
            compositions = new LRUCache<>(Integer.parseInt(cacheSize));
        } catch (NumberFormatException e) {
            throw Exceptions.create(IllegalStateException::new, e,
                "Cannot parse value %s for configuration property %s", cacheSize,
                ConfigurationImpl.Properties.CONSTRAINTS_CACHE_SIZE);
        }
    }

    /**
     * Retrieve the composing constraints for the specified constraint
     * {@link Annotation}.
     * 
     * @param a
     * @return {@link Annotation}[]
     */
    public Annotation[] getComposingConstraints(Annotation a) {
        return getComposition(a.annotationType()).getComponents(a);
    }

    /**
     * Learn whether {@code a} is composed.
     * 
     * @param a
     * @return {@code boolean}
     */
    public boolean isComposed(Annotation a) {
        return getComposition(a.annotationType()).isComposed();
    }

    /**
     * Get the supported targets for {@code constraintType}.
     * 
     * @param constraintType
     * @return {@link Set} of {@link ValidationTarget}
     */
    public <A extends Annotation> Set<ValidationTarget> supportedTargets(Class<A> constraintType) {
        final Set<ConstraintValidatorInfo<A>> constraintValidatorInfo =
            validatorFactory.getConstraintsCache().getConstraintValidatorInfo(constraintType);
        final Stream<Set<ValidationTarget>> s;
        if (constraintValidatorInfo.isEmpty()) {
            // must be for composition:
            s = Stream.of(new Composition(constraintType).components).map(Annotation::annotationType)
                .map(this::supportedTargets);
        } else {
            s = constraintValidatorInfo.stream().map(ConstraintValidatorInfo::getSupportedTargets);
        }
        return s.flatMap(Collection::stream)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(ValidationTarget.class)));
    }

    private Composition getComposition(Class<? extends Annotation> annotationType) {
        return compositions.computeIfAbsent(annotationType, ct -> {
            final Set<ValidationTarget> composedTargets = supportedTargets(annotationType);
            final Composition result = new Composition(annotationType);
            Stream.of(result.components).map(Annotation::annotationType).forEach(at -> {
                final Set<ValidationTarget> composingTargets = supportedTargets(at);
                if (Collections.disjoint(composingTargets, composedTargets)) {
                    Exceptions.raise(ConstraintDefinitionException::new,
                        "Attempt to compose %s of %s but validator types are incompatible", annotationType.getName(),
                        at.getName());
                }
            });
            return result;
        });
    }
}
