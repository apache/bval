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
import java.util.Arrays;
import java.util.List;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;

import org.apache.bval.util.Validate;

public class AnnotationDeclaredValidatorMappingProvider extends ValidatorMappingProvider {
    public static final AnnotationDeclaredValidatorMappingProvider INSTANCE =
        new AnnotationDeclaredValidatorMappingProvider();

    @Override
    protected <A extends Annotation> ValidatorMapping<A> doGetValidatorMapping(Class<A> constraintType) {
        Validate.notNull(constraintType);
        Validate.isTrue(constraintType.isAnnotationPresent(Constraint.class),
            "%s does not represent a validation constraint", constraintType);
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final List<Class<? extends ConstraintValidator<A, ?>>> validatorTypes =
            (List) Arrays.asList(constraintType.getAnnotation(Constraint.class).validatedBy());
        return new ValidatorMapping<>("@Constraint.validatedBy()", validatorTypes);
    }
}
