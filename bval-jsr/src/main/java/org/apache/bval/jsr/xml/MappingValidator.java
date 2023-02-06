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
package org.apache.bval.jsr.xml;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.validation.ValidationException;

import org.apache.bval.jsr.ConstraintAnnotationAttributes;
import org.apache.bval.jsr.metadata.ContainerElementKey;
import org.apache.bval.jsr.metadata.Meta;
import org.apache.bval.jsr.metadata.Meta.ForConstructor;
import org.apache.bval.jsr.metadata.Signature;
import org.apache.bval.jsr.util.Methods;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.bval.util.reflection.TypeUtils;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

@Privilizing(@CallTo(Reflection.class))
public class MappingValidator {
    private static final Set<ConstraintAnnotationAttributes> RESERVED_CONSTRAINT_ELEMENT_NAMES = Collections
        .unmodifiableSet(EnumSet.of(ConstraintAnnotationAttributes.GROUPS, ConstraintAnnotationAttributes.MESSAGE,
            ConstraintAnnotationAttributes.PAYLOAD, ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO));

    private static <T> BinaryOperator<T> enforceUniqueness(String message, Function<? super T, ?> describe) {
        return (t, u) -> {
            throw Exceptions.create(ValidationException::new, message, describe.apply(t));
        };
    }

    private final ConstraintMappingsType constraintMappings;
    private final Function<String, Class<?>> resolveClass;

    public MappingValidator(ConstraintMappingsType constraintMappings, Function<String, Class<?>> resolveClass) {
        super();
        this.constraintMappings = Validate.notNull(constraintMappings, "constraintMappings");
        this.resolveClass = Validate.notNull(resolveClass, "resolveClass");
    }

    public void validateMappings() {
        constraintMappings.getBean().stream().map(this::applyChecks).collect(Collectors.toMap(Function.identity(),
            Function.identity(), enforceUniqueness("Duplicate XML constrained bean %s", Class::getName)));
    }

    private Class<?> applyChecks(BeanType bean) {
        final Class<?> t = resolveClass.apply(bean.getClazz());

        final ClassType classType = bean.getClassType();
        if (classType != null) {
            constraints(new Meta.ForClass<>(t), classType.getConstraint());
        }
        final Set<String> fieldProperties = fieldProperties(t, bean.getField());
        final Set<String> getterProperties = getterProperties(t, bean.getGetter());
        final Set<Signature> methods = methods(t, bean.getMethod());
        @SuppressWarnings("unused")
        final Set<Signature> constructors = constructors(t, bean.getConstructor());

        final Set<String> propertyOverlap = new HashSet<>(fieldProperties);
        propertyOverlap.retainAll(getterProperties);

        if (!propertyOverlap.isEmpty()) {
            Exceptions.raise(ValidationException::new,
                "The following %s properties were specified via XML field and getter: %s", bean.getClazz(),
                propertyOverlap);
        }
        final Set<String> getterMethodOverlap = methods.stream().filter(s -> s.getParameterTypes().length == 0)
            .map(Signature::getName).filter(Methods::isGetter).map(Methods::propertyName)
            .filter(getterProperties::contains).collect(Collectors.toSet());

        if (!getterMethodOverlap.isEmpty()) {
            Exceptions.raise(ValidationException::new,
                "The following %s getters were specified via XML getter and method: %s", bean.getClazz(),
                getterMethodOverlap);
        }
        return t;
    }

    private Set<String> fieldProperties(Class<?> t, List<FieldType> fields) {
        return fields.stream().peek(f -> {
            final Field fld = Reflection.find(t, c -> Reflection.getDeclaredField(c, f.getName()));
            if (fld == null) {
                Exceptions.raise(ValidationException::new, "Unknown XML constrained field %s of %s", f.getName(), t);
            }
            final Meta.ForField metaField = new Meta.ForField(fld);
            constraints(metaField, f.getConstraint());
            containerElements(metaField, f.getContainerElementType());
        }).collect(Collectors.toMap(FieldType::getName, Function.identity(),
            enforceUniqueness("Duplicate XML constrained field %s of " + t, FieldType::getName))).keySet();
    }

    private Set<String> getterProperties(Class<?> t, List<GetterType> getters) {
        return getters.stream().peek(g -> {
            final Method getter = Methods.getter(t, g.getName());
            if (getter == null) {
                Exceptions.raise(ValidationException::new, "Unknown XML constrained getter for property %s of %s",
                    g.getName(), t);
            }
            final Meta.ForMethod metaGetter = new Meta.ForMethod(getter);
            constraints(metaGetter, g.getConstraint());
            containerElements(metaGetter, g.getContainerElementType());
        }).collect(Collectors.toMap(GetterType::getName, Function.identity(),
            enforceUniqueness("Duplicate XML constrained getter %s of " + t, GetterType::getName))).keySet();
    }

    private Set<Signature> methods(Class<?> t, List<MethodType> methods) {
        return methods.stream().map(mt -> {
            final Class<?>[] parameterTypes = getParameterTypes(mt.getParameter());
            final Signature result = new Signature(mt.getName(), parameterTypes);
            final Method m = Reflection.find(t, c -> Reflection.getDeclaredMethod(c, mt.getName(), parameterTypes));
            Exceptions.raiseIf(m == null, ValidationException::new, "Unknown method %s of %s", result, t);

            Optional.of(mt).map(MethodType::getReturnValue).ifPresent(rv -> {
                final Meta.ForMethod metaMethod = new Meta.ForMethod(m);
                constraints(metaMethod, rv.getConstraint());
                containerElements(metaMethod, rv.getContainerElementType());
            });
            final Parameter[] params = m.getParameters();

            IntStream.range(0, parameterTypes.length).forEach(n -> {
                final Meta.ForParameter metaParam = new Meta.ForParameter(params[n], params[n].getName());
                final ParameterType parameterType = mt.getParameter().get(n);
                constraints(metaParam, parameterType.getConstraint());
                containerElements(metaParam, parameterType.getContainerElementType());
            });

            return result;
        }).collect(Collectors.toSet());
    }

    private Set<Signature> constructors(Class<?> t, List<ConstructorType> ctors) {
        return ctors.stream().map(ctor -> {
            final Class<?>[] parameterTypes = getParameterTypes(ctor.getParameter());
            final Signature result = new Signature(t.getSimpleName(), parameterTypes);
            final Constructor<?> dc = Reflection.getDeclaredConstructor(t, parameterTypes);
            Exceptions.raiseIf(dc == null, ValidationException::new, "Unknown %s constructor %s", t, result);

            Optional.of(ctor).map(ConstructorType::getReturnValue).ifPresent(rv -> {
                final ForConstructor<?> metaCtor = new Meta.ForConstructor<>(dc);
                constraints(metaCtor, rv.getConstraint());
                containerElements(metaCtor, rv.getContainerElementType());
            });
            final Parameter[] params = dc.getParameters();

            IntStream.range(0, parameterTypes.length).forEach(n -> {
                final Meta.ForParameter metaParam = new Meta.ForParameter(params[n], params[n].getName());
                final ParameterType parameterType = ctor.getParameter().get(n);
                constraints(metaParam, parameterType.getConstraint());
                containerElements(metaParam, parameterType.getContainerElementType());
            });
            return result;
        }).collect(Collectors.toSet());
    }

    private Class<?>[] getParameterTypes(List<ParameterType> paramElements) {
        return paramElements.stream().map(ParameterType::getType).map(resolveClass).toArray(Class[]::new);
    }

    private Set<ContainerElementKey> containerElements(Meta<?> meta,
        List<ContainerElementTypeType> containerElementTypes) {
        if (containerElementTypes.isEmpty()) {
            return Collections.emptySet();
        }
        final Class<?> containerType = TypeUtils.getRawType(meta.getType(), null);
        final int typeParameterCount = containerType.getTypeParameters().length;
        if (typeParameterCount == 0) {
            Exceptions.raise(ValidationException::new, "Cannot specify container element types for %s",
                meta.describeHost());
        }
        return containerElementTypes.stream().map(e -> {
            Integer typeArgumentIndex = e.getTypeArgumentIndex();
            if (typeArgumentIndex == null) {
                if (typeParameterCount > 1) {
                    Exceptions.raise(ValidationException::new,
                        "Unable to resolve unspecified type argument index for %s", meta.describeHost());
                }
                typeArgumentIndex = Integer.valueOf(0);
            }
            final ContainerElementKey result = new ContainerElementKey(containerType, typeArgumentIndex);

            final Meta.ForContainerElement elementMeta = new Meta.ForContainerElement(meta, result);

            constraints(elementMeta, e.getConstraint());
            containerElements(elementMeta, e.getContainerElementType());

            return result;
        }).collect(Collectors.toMap(Function.identity(), ContainerElementKey::getTypeArgumentIndex, enforceUniqueness(
            "Duplicate XML constrained container element %d of " + meta.describeHost(), Function.identity()))).keySet();
    }

    private void constraints(Meta<?> meta, List<ConstraintType> constraints) {
        constraints.forEach(constraint -> {
            final Class<?> annotation = resolveClass.apply(constraint.getAnnotation());
            Exceptions.raiseUnless(annotation.isAnnotation(), ValidationException::new, "%s is not an annotation",
                annotation);

            final Set<String> missingElements = Stream.of(Reflection.getDeclaredMethods(annotation))
                .filter(m -> m.getParameterCount() == 0 && m.getDefaultValue() == null).map(Method::getName)
                .collect(Collectors.toSet());

            for (final ElementType elementType : constraint.getElement()) {
                final String name = elementType.getName();
                if (RESERVED_CONSTRAINT_ELEMENT_NAMES.stream().map(ConstraintAnnotationAttributes::getAttributeName)
                    .anyMatch(Predicate.isEqual(name))) {
                    Exceptions.raise(ValidationException::new, "Constraint of %s declares reserved parameter name %s.",
                        meta.describeHost(), name);
                }
                missingElements.remove(name);
            }
            Exceptions.raiseUnless(missingElements.isEmpty(), ValidationException::new,
                "Missing required elements of %s: %s", annotation, missingElements);
        });
    }
}
