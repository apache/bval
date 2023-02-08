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
package org.apache.bval.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.stream.Stream;
import jakarta.validation.ConstraintDefinitionException;
import jakarta.validation.ConstraintValidator;
import org.apache.bval.util.reflection.TypeUtils;

public class ValidatorUtils {

    private static final WildcardType UNBOUNDED = TypeUtils.wildcardType().build();
    private static final String CV = ConstraintValidator.class.getSimpleName();
    
    public static Class<?> getValidatedType(Class<? extends ConstraintValidator<?, ?>> validatorType) {
        final Type result = TypeUtils.getTypeArguments(validatorType, ConstraintValidator.class)
            .get(ConstraintValidator.class.getTypeParameters()[1]);
        if (!isSupported(result)) {
            Exceptions.raise(ConstraintDefinitionException::new, "Validated type %s declared by %s %s is unsupported",
                result, CV, validatorType.getName());
        }
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
}
