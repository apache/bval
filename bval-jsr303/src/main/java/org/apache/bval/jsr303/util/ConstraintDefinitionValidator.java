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

import org.apache.bval.jsr303.ConstraintAnnotationAttributes;

import javax.validation.Constraint;
import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintTarget;
import java.lang.annotation.Annotation;

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

        ConstraintAnnotationAttributes.GROUPS.analyze(type).valid();
        ConstraintAnnotationAttributes.PAYLOAD.analyze(type).valid();
        ConstraintAnnotationAttributes.MESSAGE.analyze(type).valid();

        final ConstraintAnnotationAttributes.Worker<? extends Annotation> validationAppliesToWorker = ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO.analyze(type);

        if (validationAppliesToWorker.isValid() && !ConstraintTarget.IMPLICIT.equals(validationAppliesToWorker.defaultValue)) {
            throw new ConstraintDefinitionException("validationAppliesTo default value should be IMPLICIT");
        }
    }
}
