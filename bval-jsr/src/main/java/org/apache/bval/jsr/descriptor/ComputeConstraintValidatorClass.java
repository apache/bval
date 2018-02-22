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
package org.apache.bval.jsr.descriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintValidator;
import javax.validation.UnexpectedTypeException;
import javax.validation.constraintvalidation.ValidationTarget;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.ConstraintCached.ConstraintValidatorInfo;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.bval.util.reflection.Reflection.Interfaces;
import org.apache.bval.util.reflection.TypeUtils;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

@Privilizing(@CallTo(Reflection.class))
class ComputeConstraintValidatorClass<A extends Annotation>
    implements Supplier<Class<? extends ConstraintValidator<A, ?>>> {

    private static class TypeWrapper {
        final Class<?> componentType;
        final int arrayDepth;

        TypeWrapper(Class<?> type) {
            Class<?> c = type;
            int d = 0;
            while (Object[].class.isAssignableFrom(c)) {
                d++;
                c = c.getComponentType();
            }
            this.componentType = c;
            this.arrayDepth = d;
        }

        Class<?> unwrap(Class<?> t) {
            Exceptions.raiseUnless(t.isAssignableFrom(componentType), IllegalArgumentException::new,
                "%s not assignable from %s", t, componentType);
            if (arrayDepth == 0) {
                return t;
            }
            return Array.newInstance(t, new int[arrayDepth]).getClass();
        }
    }

    private static final String CV = ConstraintValidator.class.getSimpleName();
    private static final WildcardType UNBOUNDED = TypeUtils.wildcardType().build();

    private static Class<?> getValidatedType(Class<? extends ConstraintValidator<?, ?>> validatorType) {
        final Type result = TypeUtils.getTypeArguments(validatorType, ConstraintValidator.class)
            .get(ConstraintValidator.class.getTypeParameters()[1]);
        Exceptions.raiseUnless(isSupported(result), ConstraintDefinitionException::new,
            "Validated type %s declared by %s %s is unsupported", result, CV, validatorType.getName());
        return TypeUtils.getRawType(result, null);
    }

    private static boolean isSupported(Type validatedType) {
        if (validatedType instanceof Class<?>) {
            return true;
        }
        if (validatedType instanceof ParameterizedType) {
            return Stream.of(((ParameterizedType) validatedType).getActualTypeArguments())
                .allMatch(arg -> TypeUtils.equals(arg, UNBOUNDED));
        }
        return false;
    }

    private final ApacheValidatorFactory validatorFactory;
    private final Class<?> validatedType;
    private final Collection<ValidationTarget> validationTargets;
    private final A constraint;
    private final boolean composed;

    ComputeConstraintValidatorClass(ApacheValidatorFactory validatorFactory,
                                    Collection<ValidationTarget> validationTargets,
        A constraint, Class<?> validatedType) {
        super();
        this.validatorFactory = Validate.notNull(validatorFactory, "validatorFactory");
        this.validationTargets = Validate.notNull(validationTargets, "validationTargets");
        this.constraint = Validate.notNull(constraint, "constraint");
        this.validatedType = Validate.notNull(validatedType, "validatedType");
        this.composed = validatorFactory.getAnnotationsManager().isComposed(constraint);
    }

    @Override
    public Class<? extends ConstraintValidator<A, ?>> get() {
        @SuppressWarnings("unchecked")
        final Class<A> constraintType = (Class<A>) constraint.annotationType();
        return findValidator(validatorFactory.getConstraintsCache().getConstraintValidatorInfo(constraintType));
    }

    private Class<? extends ConstraintValidator<A, ?>> findValidator(Set<ConstraintValidatorInfo<A>> infos) {
        RuntimeException error = null;
        for (ValidationTarget vt : validationTargets) { // TODO: to rework, perf are not correct
            try {
                switch (vt) {
                    case PARAMETERS:
                        return findCrossParameterValidator(infos);
                    case ANNOTATED_ELEMENT:
                        return findAnnotatedElementValidator(infos);
                    default:
                }
            } catch (final RuntimeException re) {
                error = re;
            }
        }
        if (error != null) {
            throw error;
        }
        return null;
    }

    private Class<? extends ConstraintValidator<A, ?>> findCrossParameterValidator(
        Set<ConstraintValidatorInfo<A>> infos) {

        final Set<ConstraintValidatorInfo<A>> set =
            infos.stream().filter(info -> info.getSupportedTargets().contains(ValidationTarget.PARAMETERS))
                .collect(Collectors.toSet());

        @SuppressWarnings("unchecked")
        final Class<A> constraintType = (Class<A>) constraint.annotationType();

        Exceptions.raiseIf(set.size() > 1 || !composed && set.isEmpty(), ConstraintDefinitionException::new,
            "%d cross-parameter %ss found for constraint type %s", set.size(), CV, constraintType);

        final Class<? extends ConstraintValidator<A, ?>> result = set.iterator().next().getType();
        Exceptions.raiseUnless(TypeUtils.isAssignable(Object[].class, getValidatedType(result)),
            ConstraintDefinitionException::new,
            "Cross-parameter %s %s does not support the validation of an object array", CV, result.getName());

        return result;
    }

    private Class<? extends ConstraintValidator<A, ?>> findAnnotatedElementValidator(
        Set<ConstraintValidatorInfo<A>> infos) {

        final Map<Class<?>, Class<? extends ConstraintValidator<?, ?>>> validators = infos.stream()
            .filter(info -> info.getSupportedTargets().contains(ValidationTarget.ANNOTATED_ELEMENT))
            .map(ConstraintValidatorInfo::getType).collect(
                Collectors.toMap(ComputeConstraintValidatorClass::getValidatedType, Function.identity(), (v1, v2) -> {
                    Exceptions.raiseUnless(Objects.equals(v1, v2), UnexpectedTypeException::new,
                        "Detected collision of constraint and target type between %s and %s", v1, v2);
                    return v1;
                }));

        final Map<Type, Class<? extends ConstraintValidator<?, ?>>> candidates = new HashMap<>();

        walkHierarchy().filter(validators::containsKey).forEach(type -> {
            // if we haven't already found a candidate whose validated type
            // is a subtype of the current evaluated type, save:
            if (!candidates.keySet().stream().anyMatch(k -> TypeUtils.isAssignable(k, type))) {
                candidates.put(type, validators.get(type));
            }
        });
        final String cond;
        switch (candidates.size()) {
        case 1:
            @SuppressWarnings("unchecked")
            final Class<? extends ConstraintValidator<A, ?>> result =
                (Class<? extends ConstraintValidator<A, ?>>) candidates.values().iterator().next();
            return result;
        case 0:
            if (composed) {
                return null;
            }
            cond = "No compliant";
            break;
        default:
            cond = "> 1 maximally specific";
            break;
        }
        throw Exceptions.create(UnexpectedTypeException::new, "%s %s %s found for annotated element of type %s", cond,
            constraint.annotationType().getName(), CV, TypeUtils.toString(validatedType));
    }

    // account for validated array types by unwrapping and rewrapping component
    // type hierarchy:
    private Stream<Class<?>> walkHierarchy() {
        final TypeWrapper w = new TypeWrapper(Reflection.primitiveToWrapper(validatedType));
        Stream.Builder<Class<?>> hierarchy = Stream.builder();
        Reflection.hierarchy(w.componentType, Interfaces.INCLUDE).forEach(hierarchy);
        return hierarchy.build().map(w::unwrap);
    }
}
