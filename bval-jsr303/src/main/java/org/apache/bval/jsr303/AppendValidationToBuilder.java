/**
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
package org.apache.bval.jsr303;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Description: <br/>
 */
public class AppendValidationToBuilder implements AppendValidation {
    private final AnnotationConstraintBuilder builder;

    public AppendValidationToBuilder(AnnotationConstraintBuilder builder) {
        this.builder = builder;
    }

    public <T extends Annotation> void append(ConstraintValidation<T> validation) {
        // JSR-303 2.3:
        // Groups from the main constraint annotation are inherited by the composing annotations.
        // Any groups definition on a composing annotation is ignored.
        validation.setGroups(builder.getConstraintValidation().getGroups());
        builder.addComposed(validation);
    }
    
    /**
     * @return The set of groups from the parent constraint.
     */
    public Set<?> getInheritedGroups() {
        return builder.getConstraintValidation().getGroups();
    }
    
}
