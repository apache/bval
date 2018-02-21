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

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.validation.valueextraction.ExtractedValue;
import javax.validation.valueextraction.ValueExtractor;
import javax.validation.valueextraction.ValueExtractorDefinitionException;

import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.LazyInt;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.TypeUtils;

public class ContainerElementKey implements Comparable<ContainerElementKey> {
    private static Logger log = Logger.getLogger(ContainerElementKey.class.getName());

    public static ContainerElementKey forValueExtractor(ValueExtractor<?> extractor) {
        @SuppressWarnings("rawtypes")
        final Class<? extends ValueExtractor> extractorType = extractor.getClass();
        final Lazy<Set<ContainerElementKey>> result = new Lazy<>(HashSet::new);

        Stream.of(extractorType.getAnnotatedInterfaces()).filter(AnnotatedParameterizedType.class::isInstance)
            .map(AnnotatedParameterizedType.class::cast)
            .filter(apt -> ValueExtractor.class.equals(((ParameterizedType) apt.getType()).getRawType()))
            .forEach(decl -> {
                final AnnotatedType containerType = decl.getAnnotatedActualTypeArguments()[0];

                if (containerType.isAnnotationPresent(ExtractedValue.class)) {
                    Exceptions.raiseIf(void.class.equals(containerType.getAnnotation(ExtractedValue.class).type()),
                        ValueExtractorDefinitionException::new, "%s does not specify %s type for %s", extractorType,
                        ExtractedValue.class.getSimpleName(), containerType);
                    result.get().add(new ContainerElementKey(containerType, null));
                }
                Optional.of(containerType).filter(AnnotatedParameterizedType.class::isInstance)
                    .map(AnnotatedParameterizedType.class::cast)
                    .map(AnnotatedParameterizedType::getAnnotatedActualTypeArguments).ifPresent(args -> {
                        IntStream.range(0, args.length).forEach(n -> {
                            if (args[n].isAnnotationPresent(ExtractedValue.class)) {
                                if (void.class.equals(args[n].getAnnotation(ExtractedValue.class).type())) {
                                    log.warning(String.format("Ignoring non-default %s type specified for %s by %s",
                                        ExtractedValue.class.getSimpleName(), containerType, extractorType));
                                }
                                result.get().add(new ContainerElementKey(containerType, Integer.valueOf(n)));
                            }
                        });
                    });
            });
        return result.optional().filter(s -> s.size() == 1)
            .orElseThrow(() -> new ValueExtractorDefinitionException(extractorType.getName())).iterator().next();
    }

    private static Integer validTypeArgumentIndex(Integer typeArgumentIndex, Class<?> containerClass) {
        if (typeArgumentIndex != null) {
            final int i = typeArgumentIndex.intValue();
            Validate.isTrue(i >= 0 && i < containerClass.getTypeParameters().length,
                "type argument index %d is invalid for container type %s", typeArgumentIndex, containerClass);
        }
        return typeArgumentIndex;
    }

    private final Integer typeArgumentIndex;
    private final Class<?> containerClass;
    private final LazyInt hashCode = new LazyInt(() -> Objects.hash(getContainerClass(), getTypeArgumentIndex()));
    private final Lazy<String> toString = new Lazy<>(() -> String.format("%s: %s<[%d]>",
        ContainerElementKey.class.getSimpleName(), getContainerClass().getName(), getTypeArgumentIndex()));
    private final AnnotatedType annotatedType;

    public ContainerElementKey(AnnotatedType containerType, Integer typeArgumentIndex) {
        super();
        Validate.notNull(containerType, "containerType");
        this.containerClass = TypeUtils.getRawType(containerType.getType(), null);
        this.typeArgumentIndex = validTypeArgumentIndex(typeArgumentIndex, containerClass);
        this.annotatedType = typeArgumentIndex == null ? containerType : ((AnnotatedParameterizedType) containerType)
            .getAnnotatedActualTypeArguments()[typeArgumentIndex.intValue()];
    }

    public Class<?> getContainerClass() {
        return containerClass;
    }

    public Integer getTypeArgumentIndex() {
        return typeArgumentIndex;
    }

    public AnnotatedType getAnnotatedType() {
        return annotatedType;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || Optional.ofNullable(obj).filter(ContainerElementKey.class::isInstance)
            .map(ContainerElementKey.class::cast)
            .filter(
                cek -> Objects.equals(containerClass, cek.containerClass) && typeArgumentIndex == cek.typeArgumentIndex)
            .isPresent();
    }

    @Override
    public int hashCode() {
        return hashCode.getAsInt();
    }

    @Override
    public String toString() {
        return toString.get();
    }

    @Override
    public int compareTo(ContainerElementKey o) {
        return Comparator.comparing(ContainerElementKey::containerClassName)
            .thenComparing(Comparator.comparing(ContainerElementKey::getTypeArgumentIndex)).compare(this, o);
    }

    public Set<ContainerElementKey> getAssignableKeys() {
        final Lazy<Set<ContainerElementKey>> result = new Lazy<>(LinkedHashSet::new);

        if (typeArgumentIndex != null) {
            final TypeVariable<?> var = containerClass.getTypeParameters()[typeArgumentIndex.intValue()];

            Stream
                .concat(Stream.of(containerClass.getAnnotatedSuperclass()),
                    Stream.of(containerClass.getAnnotatedInterfaces()))
                .filter(AnnotatedParameterizedType.class::isInstance).map(AnnotatedParameterizedType.class::cast)
                .forEach(t -> {
                    final AnnotatedType[] args = t.getAnnotatedActualTypeArguments();

                    for (int i = 0; i < args.length; i++) {
                        if (args[i].getType().equals(var)) {
                            result.get().add(new ContainerElementKey(t, Integer.valueOf(i)));
                        }
                    }
                });
        }
        return result.optional().map(Collections::unmodifiableSet).orElseGet(Collections::emptySet);
    }

    public boolean represents(TypeVariable<?> var) {
        return Optional.ofNullable(typeArgumentIndex)
            .map(index -> getContainerClass().getTypeParameters()[index.intValue()]).filter(var::equals).isPresent();
    }

    private String containerClassName() {
        return getContainerClass().getName();
    }
}
