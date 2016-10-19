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
package org.apache.bval.jsr;

import java.lang.annotation.Annotation;

/**
 * Abstract base validation appender that initializes the
 * {@link ConstraintValidation#getValidator()} on post-processing.
 * 
 * @author Carlos Vara
 */
public abstract class BaseAppendValidation implements AppendValidation {

    /**
     * {@inheritDoc}
     *
     * Append operation divided in 3 stages: pre & post processing and the
     * "real" append process.
     */
    @Override
    public final <T extends Annotation> void append(final ConstraintValidation<T> validation) {
        preProcessValidation(validation);
        performAppend(validation);
        postProcessValidation(validation);
    }

    /**
     * Performs the actual "appending" operation to the underlying data
     * structure that holds the validations. Implementations shouldn't try to do
     * more than that in this step.
     * 
     * @param <T>
     *            The type of the validation.
     * @param validation
     *            The validation to be appended.
     */
    public abstract <T extends Annotation> void performAppend(final ConstraintValidation<T> validation);

    /**
     * Pre-process the validation before appending it.
     * 
     * @param <T>
     *            The type of the validation.
     * @param validation
     *            The validation to be appended.
     */
    public <T extends Annotation> void preProcessValidation(final ConstraintValidation<T> validation) {
        // No generic pre-processing
    }

    /**
     * Post-process the validation once it postProcessValidationhas been appended.
     * 
     * @param <T>
     *            The type of the validation.
     * @param validation
     *            The validation to be appended.
     */
    public <T extends Annotation> void postProcessValidation(final ConstraintValidation<T> validation) {
    }

}
