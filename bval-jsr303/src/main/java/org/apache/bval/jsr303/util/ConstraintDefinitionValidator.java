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

import org.apache.bval.jsr303.ConstraintAnnotationAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
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
    public static void validateConstraintDefinition(Annotation annotation) {
        ConstraintAnnotationAttributes.GROUPS.validateOn(annotation.annotationType());
        ConstraintAnnotationAttributes.PAYLOAD.validateOn(annotation.annotationType());
        ConstraintAnnotationAttributes.MESSAGE.validateOn(annotation.annotationType());
        validAttributes(annotation);
    }

    /**
     * Check that the annotation has no methods that start with "valid".
     * 
     * @param annotation
     *            The annotation to check.
     */
    private static void validAttributes(final Annotation annotation) {
        final Method[] methods = SecureActions.doPrivileged(
            SecureActions.getDeclaredMethods(annotation.annotationType())
        );
        for (Method method : methods ){
            // Currently case insensitive, the spec is unclear about this
            if (method.getName().toLowerCase(Locale.ENGLISH).startsWith("valid")) {
                throw new ConstraintDefinitionException(
                    "A constraint annotation cannot have methods which start with 'valid'");
            }
        }
    }

}
