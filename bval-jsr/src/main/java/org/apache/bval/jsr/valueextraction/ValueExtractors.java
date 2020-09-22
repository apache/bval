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
package org.apache.bval.jsr.valueextraction;

import org.apache.bval.jsr.metadata.ContainerElementKey;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.ObjectUtils;
import org.apache.bval.util.StringUtils;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.bval.util.reflection.Reflection.Interfaces;
import org.apache.bval.util.reflection.TypeUtils;

import javax.validation.ConstraintDeclarationException;
import javax.validation.metadata.ValidateUnwrappedValue;
import javax.validation.valueextraction.UnwrapByDefault;
import javax.validation.valueextraction.ValueExtractor;
import javax.validation.valueextraction.ValueExtractorDeclarationException;
import javax.validation.valueextraction.ValueExtractorDefinitionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link ValueExtractor} collection of some level of a bean validation hierarchy.
 */
public class ValueExtractors {
    public enum OnDuplicateContainerElementKey {
        EXCEPTION, OVERWRITE;
    }

    public static class UnwrappingInfo {
        public final ContainerElementKey containerElementKey;
        public final ValueExtractor<?> valueExtractor;

        private UnwrappingInfo(ContainerElementKey containerElementKey, ValueExtractor<?> valueExtractor) {
            super();
            this.containerElementKey = containerElementKey;
            this.valueExtractor = valueExtractor;
        }
        
        UnwrappingInfo inTermsOf(Class<?> containerClass) {
            final Class<?> keyContainer = containerElementKey.getContainerClass();
            if (keyContainer.equals(containerClass)) {
                return this;
            }
            Validate.validState(keyContainer.isAssignableFrom(containerClass), "Cannot render %s in terms of %s",
                containerElementKey, containerClass);

            final ContainerElementKey key;

            if (containerElementKey.getTypeArgumentIndex() == null) {
                key = new ContainerElementKey(containerClass, null);
            } else {
                Integer typeArgumentIndex = null;
                final Map<TypeVariable<?>, Type> typeArguments =
                    TypeUtils.getTypeArguments(containerClass, keyContainer);
                Type t = typeArguments
                    .get(keyContainer.getTypeParameters()[containerElementKey.getTypeArgumentIndex().intValue()]);
                while (t instanceof TypeVariable<?>) {
                    final TypeVariable<?> var = (TypeVariable<?>) t;
                    if (containerClass.equals(var.getGenericDeclaration())) {
                        typeArgumentIndex =
                            Integer.valueOf(ObjectUtils.indexOf(containerClass.getTypeParameters(), var));
                        break;
                    }
                    t = typeArguments.get(t);
                }
                key = new ContainerElementKey(containerClass, typeArgumentIndex);
            }
            return new UnwrappingInfo(key, valueExtractor);
        }

        @Override
        public String toString() {
            return String.format("%s:%s", containerElementKey, valueExtractor);
        }
    }

    public static final ValueExtractors EMPTY =
        new ValueExtractors(null, OnDuplicateContainerElementKey.EXCEPTION, Collections.emptyMap());

    public static final ValueExtractors DEFAULT;
    static {
        final Properties defaultExtractors = new Properties();
        try {
            defaultExtractors.load(ValueExtractors.class.getResourceAsStream("DefaultExtractors.properties"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        final Map<ContainerElementKey, ValueExtractor<?>> m = new TreeMap<>();
        final Consumer<ValueExtractor<?>> put = ve -> m.put(ContainerElementKey.forValueExtractor(ve), ve);

        split(defaultExtractors.getProperty(ValueExtractor.class.getName())).map(cn -> {
            try {
                @SuppressWarnings("unchecked")
                final Class<? extends ValueExtractor<?>> result =
                    (Class<? extends ValueExtractor<?>>) Reflection.toClass(cn).asSubclass(ValueExtractor.class);
                return result;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).map(ValueExtractors::newInstance).forEach(put);

        split(defaultExtractors.getProperty(ValueExtractor.class.getName() + ".container"))
            .flatMap(ValueExtractors::loadValueExtractors).forEach(put);

        DEFAULT = new ValueExtractors(null, OnDuplicateContainerElementKey.EXCEPTION, Collections.unmodifiableMap(m));
    }

    public static Class<?> getExtractedType(ValueExtractor<?> extractor, Type target) {
        final ContainerElementKey key = ContainerElementKey.forValueExtractor(extractor);
        Type result = key.getAnnotatedType().getType();
        if (result instanceof WildcardType && key.getTypeArgumentIndex() != null) {
            result = TypeUtils.getTypeArguments(target, key.getContainerClass())
                .get(key.getContainerClass().getTypeParameters()[key.getTypeArgumentIndex().intValue()]);
        }
        Exceptions.raiseUnless(result instanceof Class<?>, ValueExtractorDefinitionException::new,
            "%s did not resolve to a %s relative to %s", f -> f.args(key, Class.class.getName(), target));
        return (Class<?>) result;
    }

    public static boolean isUnwrapByDefault(ValueExtractor<?> valueExtractor) {
        if (valueExtractor != null) {
            for (Class<?> t : Reflection.hierarchy(valueExtractor.getClass(), Interfaces.INCLUDE)) {
                if (t.isAnnotationPresent(UnwrapByDefault.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Stream<String> split(String s) {
        return Stream.of(StringUtils.split(s, ','));
    }

    private static <T> T newInstance(Class<T> t) {
        try {
            return t.getConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    private static Stream<ValueExtractor<?>> loadValueExtractors(String containerClassName) {
        try {
            final Class<? extends BooleanSupplier> activation =
                Reflection.toClass(containerClassName + "$Activation").asSubclass(BooleanSupplier.class);
            if (!newInstance(activation).getAsBoolean()) {
                return Stream.empty();
            }
        } catch (ClassNotFoundException e) {
            // always active
        }
        final Class<?> containerClass;
        try {
            containerClass = Reflection.toClass(containerClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        return Stream.of(containerClass.getClasses()).filter(ValueExtractor.class::isAssignableFrom).map(c -> {
            @SuppressWarnings("unchecked")
            final Class<? extends ValueExtractor<?>> result =
                (Class<? extends ValueExtractor<?>>) c.asSubclass(ValueExtractor.class);
            return result;
        }).map(ValueExtractors::newInstance);
    }

    private static <T> Optional<T> maximallySpecific(Collection<T> candidates, Function<? super T, Class<?>> toType) {
        final Collection<T> result;
        if (candidates.size() > 1) {
            result = new HashSet<>();
            for (T candidate : candidates) {
                final Class<?> candidateType = toType.apply(candidate);
                if (candidates.stream().filter(Predicate.isEqual(candidate).negate()).map(toType)
                    .allMatch(t -> t.isAssignableFrom(candidateType))) {
                    result.add(candidate);
                }
            }
        } else {
            result = candidates;
        }
        return result.size() == 1 ? Optional.of(result.iterator().next()) : Optional.empty();
    }

    private final ValueExtractors parent;
    private final Map<ContainerElementKey, ValueExtractor<?>> valueExtractors = new ConcurrentHashMap<>();
    private final Map<ContainerElementKey, ValueExtractor<?>> searchCache = new ConcurrentHashMap<>();
    private final OnDuplicateContainerElementKey onDuplicateContainerElementKey;

    public ValueExtractors() {
        this(OnDuplicateContainerElementKey.EXCEPTION);
    }

    public ValueExtractors(OnDuplicateContainerElementKey onDuplicateContainerElementKey) {
        this(DEFAULT, Validate.notNull(onDuplicateContainerElementKey));
    }

    private ValueExtractors(ValueExtractors parent, OnDuplicateContainerElementKey onDuplicateContainerElementKey) {
        this.parent = parent;
        this.onDuplicateContainerElementKey = onDuplicateContainerElementKey;
    }

    private ValueExtractors(ValueExtractors parent, OnDuplicateContainerElementKey onDuplicateContainerElementKey,
        Map<ContainerElementKey, ValueExtractor<?>> backingMap) {
        this(parent, onDuplicateContainerElementKey);
        this.valueExtractors.clear();
        this.valueExtractors.putAll(backingMap);
    }

    public ValueExtractors createChild() {
        return createChild(OnDuplicateContainerElementKey.EXCEPTION);
    }

    public ValueExtractors createChild(OnDuplicateContainerElementKey onDuplicateContainerElementKey) {
        return new ValueExtractors(this, onDuplicateContainerElementKey);
    }

    public void add(ValueExtractor<?> extractor) {
        final ContainerElementKey key = ContainerElementKey.forValueExtractor(extractor);
        if (key == null) {
            Exceptions.raise(IllegalStateException::new, "Computed null %s for %s",
                ContainerElementKey.class.getSimpleName(), extractor);
        }
        final Map<ContainerElementKey, ValueExtractor<?>> m = valueExtractors;
        if (onDuplicateContainerElementKey == OnDuplicateContainerElementKey.EXCEPTION) {
            synchronized (this) {
                if (m.containsKey(key)) {
                    Exceptions.raise(ValueExtractorDeclarationException::new,
                        "Multiple context-level %ss specified for %s", ValueExtractor.class.getSimpleName(), key);
                }
                m.put(key, extractor);
            }
        } else {
            m.put(key, extractor);
        }
        searchCache.clear();
    }

    public Map<ContainerElementKey, ValueExtractor<?>> getValueExtractors() {
        final Map<ContainerElementKey, ValueExtractor<?>> result = new HashMap<>();
        populate(result);
        return result;
    }

    public ValueExtractor<?> find(ContainerElementKey key) {
        final ValueExtractor<?> cacheHit = searchCache.get(key);
        if (cacheHit != null) {
            return cacheHit;
        }
        final Map<ContainerElementKey, ValueExtractor<?>> allValueExtractors = getValueExtractors();
        if (allValueExtractors.containsKey(key)) {
            return allValueExtractors.get(key);
        }
        final Map<ValueExtractor<?>, ContainerElementKey> candidates = Stream
            .concat(Stream.of(key), key.getAssignableKeys().stream()).filter(allValueExtractors::containsKey).collect(
                Collectors.toMap(allValueExtractors::get, Function.identity(), (quid, quo) -> quo, LinkedHashMap::new));

        final Optional<ValueExtractor<?>> result =
            maximallySpecific(candidates.keySet(), ve -> candidates.get(ve).getContainerClass());
        if (result.isPresent()) {
            searchCache.put(key, result.get());
            return result.get();
        }
        throw Exceptions.create(ConstraintDeclarationException::new, "Could not determine %s for %s",
            ValueExtractor.class.getSimpleName(), key);
    }

    public Optional<UnwrappingInfo> findUnwrappingInfo(Class<?> containerClass,
        ValidateUnwrappedValue valueUnwrapping) {
        if (valueUnwrapping == ValidateUnwrappedValue.SKIP) {
            return Optional.empty();
        }
        final Map<ContainerElementKey, ValueExtractor<?>> allValueExtractors = getValueExtractors();

        final Set<UnwrappingInfo> unwrapping = allValueExtractors.entrySet().stream()
            .filter(e -> e.getKey().getContainerClass().isAssignableFrom(containerClass))
            .filter(e -> valueUnwrapping == ValidateUnwrappedValue.UNWRAP || isUnwrapByDefault(e.getValue()))
            .map(e -> new UnwrappingInfo(e.getKey(), e.getValue())).collect(Collectors.toSet());

        final Optional<UnwrappingInfo> result =
            maximallySpecific(unwrapping, u -> u.containerElementKey.getContainerClass())
                .map(u -> u.inTermsOf(containerClass));

        if (!result.isPresent() && valueUnwrapping == ValidateUnwrappedValue.UNWRAP) {
            Exceptions.raise(ConstraintDeclarationException::new, "Could not determine %s for %s",
                ValueExtractor.class.getSimpleName(), containerClass);
        }
        return result;
    }

    private void populate(Map<ContainerElementKey, ValueExtractor<?>> target) {
        if (parent != null) {
            parent.populate(target);
        }
        target.putAll(valueExtractors);
    }

}
