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
package org.apache.bval.jsr303.extensions;


import org.apache.bval.jsr303.AppendValidation;
import org.apache.bval.jsr303.BaseAppendValidation;
import org.apache.bval.jsr303.ConstraintValidation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Description: {@link AppendValidation} implementation that acts as an intermediate
 * cache of validations for further processing.<br/>
 */
public class AppendValidationToList extends BaseAppendValidation {
    private final List<ConstraintValidation<? extends Annotation>> validations = new ArrayList<ConstraintValidation<? extends Annotation>>();

    /**
     * Create a new AppendValidationToList instance.
     */
    public AppendValidationToList() {
    }

    /**
     * {@inheritDoc}
     */
    public <T extends Annotation> void performAppend(ConstraintValidation<T> validation) {
        validations.add(validation);
    }

    /**
     * Get the list of cached validations.
     * @return {@link List} of {@link ConstraintValidation}
     */
    public List<ConstraintValidation<? extends Annotation>> getValidations() {
        return validations;
    }
}
