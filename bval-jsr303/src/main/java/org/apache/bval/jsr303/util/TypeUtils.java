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
package org.apache.bval.jsr303.util;

import javax.validation.ConstraintValidator;
import javax.validation.ValidationException;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;


/**
 * Description: Helper methods to determine the type for a {@link ConstraintValidator} class.
 * Alternative: could also do this by using <pre>
 *  &lt;groupId&gt;com.googlecode.jtype&lt;/groupId&gt;
 *  &lt;artifactId&gt;jtype&lt;/artifactId&gt;
 *  &lt;version&gt;0.1.1&lt;/version&gt;
 * </pre> but tried to reduce dependencies, so here is some code, that
 * handles java5 generic types to find suitable implementations for
 * ConstraintValidators.
 * <br/>
 * User: roman <br/>
 * Date: 17.11.2009 <br/>
 * Time: 16:36:02 <br/>
 * Copyright: Agimatec GmbH
 */
public class TypeUtils {
    private static final Map<Class<?>, Set<Class<?>>> SUBTYPES_BY_PRIMITIVE;
    private static final int VALIDATOR_TYPE_INDEX = 1;

    static {
        Map<Class<?>, Set<Class<?>>> subtypesByPrimitive =
              new HashMap<Class<?>, Set<Class<?>>>();

        putPrimitiveSubtypes(subtypesByPrimitive, Void.TYPE);
        putPrimitiveSubtypes(subtypesByPrimitive, Boolean.TYPE);
        putPrimitiveSubtypes(subtypesByPrimitive, Byte.TYPE);
        putPrimitiveSubtypes(subtypesByPrimitive, Character.TYPE);
        putPrimitiveSubtypes(subtypesByPrimitive, Short.TYPE, Byte.TYPE);
        putPrimitiveSubtypes(subtypesByPrimitive, Integer.TYPE, Character.TYPE, Short.TYPE);
        putPrimitiveSubtypes(subtypesByPrimitive, Long.TYPE, Integer.TYPE);
        putPrimitiveSubtypes(subtypesByPrimitive, Float.TYPE, Long.TYPE);
        putPrimitiveSubtypes(subtypesByPrimitive, Double.TYPE, Float.TYPE);

        SUBTYPES_BY_PRIMITIVE = subtypesByPrimitive;
    }

    private static void putPrimitiveSubtypes(Map<Class<?>, Set<Class<?>>> subtypesByPrimitive,
                                             Class<?> primitiveType,
                                             Class<?>... directSubtypes) {
        Set<Class<?>> subtypes = new HashSet<Class<?>>();

        for (Class<?> directSubtype : directSubtypes) {
            subtypes.add(directSubtype);
            subtypes.addAll(subtypesByPrimitive.get(directSubtype));
        }

        subtypesByPrimitive.put(primitiveType, subtypes);
    }

    private static Type getArrayType(final Type componentType) {
        if (componentType instanceof Class<?>) {
            return getClassArrayType((Class<?>) componentType);
        } else {
            return getGenericArrayType(componentType);
        }
    }

    private static Type getGenericArrayType(final Type componentType) {
        return new GenericArrayType() {
            public Type getGenericComponentType() {
                return componentType;
            }

            public int hashCode() {
                return componentType.hashCode();
            }

            public boolean equals(Object object) {
                return object instanceof GenericArrayType && componentType
                      .equals(((GenericArrayType) object).getGenericComponentType());
            }
        };
    }

    private static Class<?> getClassArrayType(Class<?> componentType) {
        return Array.newInstance(componentType, 0).getClass();
    }

    private static Type getComponentType(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            return clazz.isArray() ? clazz.getComponentType() : null;
        } else if (type instanceof GenericArrayType) {
            return ((GenericArrayType) type).getGenericComponentType();
        } else {
            return null;
        }
    }

    public static Map<Type, Class<? extends ConstraintValidator<?, ?>>> getValidatorsTypes(
          Class<? extends ConstraintValidator<?, ?>>[] validators) {
        Map<Type, Class<? extends ConstraintValidator<?, ?>>> validatorsTypes =
              new HashMap<Type, Class<? extends ConstraintValidator<?, ?>>>();
        for (Class<? extends ConstraintValidator<?, ?>> validator : validators) {
            validatorsTypes.put(getValidatorType(validator), validator);
        }
        return validatorsTypes;
    }

    // ((ParameterizedType)validator.getGenericInterfaces()[0]).getActualTypeArguments()[1]
    private static Type getValidatorType(
          Class<? extends ConstraintValidator<?, ?>> validator) {
        Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
        Type constraintValidatorType = resolveTypes(resolvedTypes, validator);
        Type validatorType = ((ParameterizedType) constraintValidatorType)
              .getActualTypeArguments()[VALIDATOR_TYPE_INDEX];
        if (validatorType == null) {
            throw new ValidationException("null is an invalid type for a ConstraintValidator");
        } else if (validatorType instanceof GenericArrayType) {
            validatorType = getArrayType(getComponentType(validatorType));
        }
        while (resolvedTypes.containsKey(validatorType)) {
            validatorType = resolvedTypes.get(validatorType);
        }
        return validatorType;
    }

    private static Type resolveTypes(Map<Type, Type> resolvedTypes, Type type) {
        if (type == null) {
            return null;
        } else if (type instanceof Class) {
            Class clazz = (Class) type;
            final Type returnedType = resolveTypeForHierarchy(resolvedTypes, clazz);
            if (returnedType != null) {
                return returnedType;
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            if (!(paramType.getRawType() instanceof Class)) {
                return null;
            }
            Class<?> rawType = (Class<?>) paramType.getRawType();

            TypeVariable<?>[] originalTypes = rawType.getTypeParameters();
            Type[] partiallyResolvedTypes = paramType.getActualTypeArguments();
            int nbrOfParams = originalTypes.length;
            for (int i = 0; i < nbrOfParams; i++) {
                resolvedTypes.put(originalTypes[i], partiallyResolvedTypes[i]);
            }

            if (rawType.equals(ConstraintValidator.class)) {
                return type;  // we found what we were looking for
            } else {
                Type returnedType = resolveTypeForHierarchy(resolvedTypes, rawType);
                if (returnedType != null) {
                    return returnedType;
                }
            }
        }
        return null;
    }

    private static Type resolveTypeForHierarchy(Map<Type, Type> resolvedTypes,
                                                Class<?> clazz) {
        Type returnedType = resolveTypes(resolvedTypes, clazz.getGenericSuperclass());
        if (returnedType != null) {
            return returnedType;
        }
        for (Type genericInterface : clazz.getGenericInterfaces()) {
            returnedType = resolveTypes(resolvedTypes, genericInterface);
            if (returnedType != null) {
                return returnedType;
            }
        }
        return null;
    }

    public static boolean isAssignable(Type supertype, Type type) {
        if (supertype.equals(type)) {
            return true;
        }

        if (supertype instanceof Class<?>) {
            if (type instanceof Class<?>) {
                return isClassAssignable((Class<?>) supertype, (Class<?>) type);
            }

            if (type instanceof ParameterizedType) {
                return isAssignable(supertype, ((ParameterizedType) type).getRawType());
            }

            if (type instanceof TypeVariable<?>) {
                return isTypeVariableAssignable(supertype, (TypeVariable<?>) type);
            }

            if (type instanceof GenericArrayType) {
                if (((Class<?>) supertype).isArray()) {
                    return isAssignable(getComponentType(supertype), getComponentType(type));
                }

                return isArraySupertype((Class<?>) supertype);
            }

            return false;
        }

        if (supertype instanceof ParameterizedType) {
            if (type instanceof Class<?>) {
                return isSuperAssignable(supertype, type);
            }

            return type instanceof ParameterizedType && isParameterizedTypeAssignable(
                  (ParameterizedType) supertype, (ParameterizedType) type);

        }

        if (type instanceof TypeVariable<?>) {
            return isTypeVariableAssignable(supertype, (TypeVariable<?>) type);
        }

        if (supertype instanceof GenericArrayType) {
            return isArray(type) &&
                  isAssignable(getComponentType(supertype), getComponentType(type));

        }

        return supertype instanceof WildcardType &&
              isWildcardTypeAssignable((WildcardType) supertype, type);

    }

    private static boolean isClassAssignable(Class<?> supertype, Class<?> type) {
        if (supertype.isPrimitive() && type.isPrimitive()) {
            return SUBTYPES_BY_PRIMITIVE.get(supertype).contains(type);
        }

        return supertype.isAssignableFrom(type);
    }

    private static boolean isParameterizedTypeAssignable(ParameterizedType supertype,
                                                         ParameterizedType type) {
        Type rawSupertype = supertype.getRawType();
        Type rawType = type.getRawType();


        if (!rawSupertype.equals(rawType)) {
            return !(rawSupertype instanceof Class<?> && rawType instanceof Class<?> &&
                  !(((Class<?>) rawSupertype).isAssignableFrom((Class<?>) rawType))) &&
                  isSuperAssignable(supertype, type);
        }

        Type[] supertypeArgs = supertype.getActualTypeArguments();
        Type[] typeArgs = type.getActualTypeArguments();

        if (supertypeArgs.length != typeArgs.length) {
            return false;
        }

        for (int i = 0; i < supertypeArgs.length; i++) {
            Type supertypeArg = supertypeArgs[i];
            Type typeArg = typeArgs[i];

            if (supertypeArg instanceof WildcardType) {
                if (!isWildcardTypeAssignable((WildcardType) supertypeArg, typeArg)) {
                    return false;
                }
            } else if (!supertypeArg.equals(typeArg)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isTypeVariableAssignable(Type supertype, TypeVariable<?> type) {
        for (Type bound : type.getBounds()) {
            if (isAssignable(supertype, bound)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isWildcardTypeAssignable(WildcardType supertype, Type type) {
        for (Type upperBound : supertype.getUpperBounds()) {
            if (!isAssignable(upperBound, type)) {
                return false;
            }
        }

        for (Type lowerBound : supertype.getLowerBounds()) {
            if (!isAssignable(type, lowerBound)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isSuperAssignable(Type supertype, Type type) {
        Type superclass = getResolvedSuperclass(type);

        if (superclass != null && isAssignable(supertype, superclass)) {
            return true;
        }

        for (Type interphace : getResolvedInterfaces(type)) {
            if (isAssignable(supertype, interphace)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isArraySupertype(Class<?> type) {
        return Object.class.equals(type) || Cloneable.class.equals(type) ||
              Serializable.class.equals(type);
    }

    private static Type getResolvedSuperclass(Type type) {
        Class<?> rawType = getErasedReferenceType(type);
        Type supertype = rawType.getGenericSuperclass();

        if (supertype == null) {
            return null;
        }

        return resolveTypeVariables(supertype, type);
    }

    private static Class<?> getErasedReferenceType(Type type) {
        return (Class<?>) getErasedType(type);
    }


    private static Type getErasedType(Type type) {
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();

            return getErasedType(rawType);
        }

        if (isArray(type)) {
            Type componentType = getComponentType(type);
            Type erasedComponentType = getErasedType(componentType);

            return getArrayType(erasedComponentType);
        }

        if (type instanceof TypeVariable<?>) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();

            return getErasedType(bounds[0]);
        }

        return type;
    }

    private static boolean isArray(Type type) {
        return (type instanceof Class<?> && ((Class<?>) type).isArray()) ||
              (type instanceof GenericArrayType);
    }

    private static Type[] getResolvedInterfaces(Type type) {

        Class<?> rawType = getErasedReferenceType(type);
        Type[] interfaces = rawType.getGenericInterfaces();
        Type[] resolvedInterfaces = new Type[interfaces.length];

        for (int i = 0; i < interfaces.length; i++) {
            resolvedInterfaces[i] = resolveTypeVariables(interfaces[i], type);
        }

        return resolvedInterfaces;
    }

    private static Type resolveTypeVariables(Type type, Type subtype) {
        if (!(type instanceof ParameterizedType)) {
            return type;
        }

        Map<Type, Type> actualTypeArgumentsByParameter =
              getActualTypeArgumentsByParameter(type, subtype);
        Class<?> rawType = getErasedReferenceType(type);

        return parameterizeClass(rawType, actualTypeArgumentsByParameter);
    }

    private static Map<Type, Type> getActualTypeArgumentsByParameter(Type... types) {
        Map<Type, Type> actualTypeArgumentsByParameter = new LinkedHashMap<Type, Type>();

        for (Type type : types) {
            actualTypeArgumentsByParameter
                  .putAll(getActualTypeArgumentsByParameterInternal(type));
        }

        return normalize(actualTypeArgumentsByParameter);
    }

    private static <K> Map<K, K> normalize(Map<K, K> map) {
        for (Map.Entry<K, K> entry : map.entrySet()) {
            K key = entry.getKey();
            K value = entry.getValue();

            while (map.containsKey(value)) {
                value = map.get(value);
            }
            map.put(key, value);
        }
        return map;
    }

    private static Map<Type, Type> getActualTypeArgumentsByParameterInternal(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return Collections.emptyMap();
        }

        TypeVariable<?>[] typeParameters = getErasedReferenceType(type).getTypeParameters();
        Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();

        if (typeParameters.length != typeArguments.length) {
            throw new MalformedParameterizedTypeException();
        }

        Map<Type, Type> actualTypeArgumentsByParameter = new LinkedHashMap<Type, Type>();

        for (int i = 0; i < typeParameters.length; i++) {
            actualTypeArgumentsByParameter.put(typeParameters[i], typeArguments[i]);
        }

        return actualTypeArgumentsByParameter;
    }

    private static ParameterizedType parameterizeClass(Class<?> type,
                                                       Map<Type, Type> actualTypeArgumentsByParameter) {
        return parameterizeClassCapture(type, actualTypeArgumentsByParameter);
    }

    private static <T> ParameterizedType parameterizeClassCapture(Class<T> type,
                                                                  Map<Type, Type> actualTypeArgumentsByParameter) {
        TypeVariable<Class<T>>[] typeParameters = type.getTypeParameters();
        Type[] actualTypeArguments = new Type[typeParameters.length];

        for (int i = 0; i < typeParameters.length; i++) {
            TypeVariable<Class<T>> typeParameter = typeParameters[i];
            Type actualTypeArgument = actualTypeArgumentsByParameter.get(typeParameter);

            if (actualTypeArgument == null) {
                throw new IllegalArgumentException(
                      "Missing actual type argument for type parameter: " + typeParameter);
            }

            actualTypeArguments[i] = actualTypeArgument;
        }

        return parameterizedType(getErasedReferenceType(type), actualTypeArguments);
    }

    private static ParameterizedType parameterizedType(final Class<?> rawType,
                                                       final Type... actualTypeArguments) {
        return new ParameterizedType() {
            public Type getOwnerType() {
                return null;
            }

            public Type getRawType() {
                return rawType;
            }

            public Type[] getActualTypeArguments() {
                return actualTypeArguments.clone();
            }
        };
    }
}
