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
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.validation.ConstraintValidator;

import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.TypeUtils;

/**
 * Provides the {@link ConstraintValidator} implementations registered via the service loader mechanism, i.e. named in
 * {@code META-INF/services/jakarta.validation.ConstraintValidator} resources. Discovered types are indexed by the
 * constraint annotation they validate, which is the first type argument of their {@link ConstraintValidator}
 * implementation.
 *
 * @since 4.0
 */
public class ServiceLoaderValidatorMappingProvider extends ValidatorMappingProvider {
    public static final String SOURCE = "META-INF/services/" + ConstraintValidator.class.getName();

    private static final Logger log = Logger.getLogger(ServiceLoaderValidatorMappingProvider.class.getName());

    private final Map<Class<? extends Annotation>, List<Class<? extends ConstraintValidator<?, ?>>>> validatorTypes;

    public ServiceLoaderValidatorMappingProvider(Collection<? extends Class<?>> serviceLoadedTypes) {
        super();
        Validate.notNull(serviceLoadedTypes, "serviceLoadedTypes");

        final Map<Class<? extends Annotation>, List<Class<? extends ConstraintValidator<?, ?>>>> index = new HashMap<>();
        for (Class<?> type : serviceLoadedTypes) {
            if (Modifier.isAbstract(type.getModifiers())) {
                ignore(type, "it is not instantiable");
                continue;
            }
            final Class<? extends Annotation> constraintType = constraintTypeOf(type);
            if (constraintType == null) {
                continue;
            }
            @SuppressWarnings("unchecked")
            final Class<? extends ConstraintValidator<?, ?>> validatorType =
                (Class<? extends ConstraintValidator<?, ?>>) type.asSubclass(ConstraintValidator.class);

            index.computeIfAbsent(constraintType, k -> new ArrayList<>()).add(validatorType);
        }
        index.replaceAll((k, v) -> Collections.unmodifiableList(v));
        this.validatorTypes = Collections.unmodifiableMap(index);
    }

    @Override
    protected <A extends Annotation> ValidatorMapping<A> doGetValidatorMapping(Class<A> constraintType) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final List<Class<? extends ConstraintValidator<A, ?>>> types = (List) validatorTypes.get(constraintType);

        return types == null ? null : new ValidatorMapping<>(SOURCE, types);
    }

    private static Class<? extends Annotation> constraintTypeOf(Class<?> validatorType) {
        final Map<TypeVariable<?>, Type> typeArguments =
            TypeUtils.getTypeArguments(validatorType, ConstraintValidator.class);

        final Type constraintParameter =
            typeArguments == null ? null : typeArguments.get(ConstraintValidator.class.getTypeParameters()[0]);

        if (constraintParameter instanceof Class<?>
            && Annotation.class.isAssignableFrom((Class<?>) constraintParameter)) {
            return ((Class<?>) constraintParameter).asSubclass(Annotation.class);
        }
        ignore(validatorType, "the constraint annotation type it validates cannot be determined");
        return null;
    }

    private static void ignore(Class<?> validatorType, String reason) {
        if (log.isLoggable(Level.WARNING)) {
            log.log(Level.WARNING, String.format("Ignoring %s declared in %s: %s", validatorType.getName(), SOURCE,
                reason));
        }
    }
}
