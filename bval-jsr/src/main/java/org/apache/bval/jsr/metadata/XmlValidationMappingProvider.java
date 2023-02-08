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
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidator;

import org.apache.bval.jsr.xml.ValidatedByType;
import org.apache.bval.util.Validate;

public class XmlValidationMappingProvider extends ClassLoadingValidatorMappingProvider {
    private static final Logger log = Logger.getLogger(XmlValidationMappingProvider.class.getName());

    private final Map<Class<? extends Annotation>, ValidatedByType> config;
    private final Function<String, String> classNameTransformer;

    public XmlValidationMappingProvider(Map<Class<? extends Annotation>, ValidatedByType> validatorMappings,
        Function<String, String> classNameTransformer) {
        super();
        this.config = Validate.notNull(validatorMappings, "validatorMappings");
        this.classNameTransformer = Validate.notNull(classNameTransformer, "classNameTransformer");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <A extends Annotation> ValidatorMapping<A> doGetValidatorMapping(Class<A> constraintType) {
        final ValidatedByType validatedByType = config.get(constraintType);
        if (validatedByType == null) {
            return null;
        }
        return new ValidatorMapping<>("XML descriptor",
            load(validatedByType.getValue().stream().map(String::trim).map(classNameTransformer),
                (Class<ConstraintValidator<A, ?>>) (Class) ConstraintValidator.class,
                e -> log.log(Level.SEVERE, "exception loading XML-declared constraint validators", e))
                    .collect(Collectors.toList()),
            toAnnotationBehavior(validatedByType));
    }

    private AnnotationBehavior toAnnotationBehavior(ValidatedByType validatedByType) {
        final Boolean includeExistingValidators = validatedByType.getIncludeExistingValidators();
        return includeExistingValidators == null ? AnnotationBehavior.ABSTAIN
            : includeExistingValidators.booleanValue() ? AnnotationBehavior.INCLUDE : AnnotationBehavior.EXCLUDE;
    }
}
