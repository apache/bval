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

import javax.validation.Constraint;
import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintTarget;

import org.apache.bval.jsr303.ConstraintAnnotationAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

/**
 * Internal validator that ensures the correct definition of constraint
 * annotations.
 * 
 * @author Carlos Vara
 */
public class ConstraintDefinitionValidator {

    /**
     * Ensures that the constraint definition is valid.
     * 
     * @param annotation
     *            An annotation which is annotated with {@link Constraint}.
     * @throws ConstraintDefinitionException
     *             In case the constraint is invalid.
     */
    public static void validateConstraintDefinition(final Annotation annotation) {
        final Class<? extends Annotation> type = annotation.annotationType();

        ConstraintAnnotationAttributes.GROUPS.validateOn(type);
        ConstraintAnnotationAttributes.PAYLOAD.validateOn(type);
        ConstraintAnnotationAttributes.MESSAGE.validateOn(type);
        ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO.validateOn(type);

        final Object defaultValidationApplies = ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO.getDefaultValue(type);
        if (ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO.isDeclaredOn(type) && !ConstraintTarget.IMPLICIT.equals(defaultValidationApplies)) {
            throw new ConstraintDefinitionException("validationAppliesTo default value should be IMPLICIT");
        }

        validAttributes(annotation);
    }

    /**
     * Check that the annotation has no methods that start with "valid".
     * 
     * @param annotation
     *            The annotation to check.
     */
    private static void validAttributes(final Annotation annotation) {
        /*
        final Method[] methods = run(SecureActions.getDeclaredMethods(annotation.annotationType()));
        for (Method method : methods ){
            // Currently case insensitive, the spec is unclear about this
            if (method.getName().toLowerCase(Locale.ENGLISH).startsWith("valid")) {
                throw new ConstraintDefinitionException(
                    "A constraint annotation cannot have methods which start with 'valid'");
            }
        }
        */
    }

    private static <T> T run(PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
    }
}
