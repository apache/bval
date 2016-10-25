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
package org.apache.bval.jsr;

import org.apache.bval.jsr.xml.AnnotationProxyBuilder;

import javax.validation.Payload;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Description: Adapt {@link AnnotationConstraintBuilder} to the {@link AppendValidation} interface.<br/>
 */
public class AppendValidationToBuilder extends BaseAppendValidation {
    private final AnnotationConstraintBuilder<?> builder;

    /**
     * Create a new AppendValidationToBuilder instance.
     * @param builder
     */
    public AppendValidationToBuilder(AnnotationConstraintBuilder<?> builder) {
        this.builder = builder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Annotation> void preProcessValidation(ConstraintValidation<T> validation) {
        // JSR-303 2.3:
        // Groups from the main constraint annotation are inherited by the composing annotations.
        // Any groups definition on a composing annotation is ignored.
        final Set<Class<?>> inheritedGroups = builder.getConstraintValidation().getGroups();
        validation.setGroups(inheritedGroups);

        // JSR-303 2.3 p:
        // Payloads are also inherited
        final Set<Class<? extends Payload>> inheritedPayload = builder.getConstraintValidation().getPayload();
        validation.setPayload(inheritedPayload);

        // Inherited groups and payload values must also be replicated in the 
        // annotation, so it has to be substituted with a new proxy.
        final T originalAnnot = validation.getAnnotation();
        final AnnotationProxyBuilder<T> apb = new AnnotationProxyBuilder<T>(originalAnnot);
        apb.putValue(ConstraintAnnotationAttributes.GROUPS.getAttributeName(),
            inheritedGroups.toArray(new Class[inheritedGroups.size()]));
        apb.putValue(ConstraintAnnotationAttributes.PAYLOAD.getAttributeName(),
            inheritedPayload.toArray(new Class[inheritedPayload.size()]));
        final T newAnnot = apb.createAnnotation();
        validation.setAnnotation(newAnnot);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Annotation> void performAppend(ConstraintValidation<T> validation) {
        builder.addComposed(validation);
    }

    /**
     * Get inherited groups.
     * @return The set of groups from the parent constraint.
     */
    public Set<Class<?>> getInheritedGroups() {
        return builder.getConstraintValidation().getGroups();
    }

    /**
     * Get inherited payload.
     * @return The set of payloads from the parent constraint.
     */
    public Set<Class<? extends Payload>> getInheritedPayload() {
        return builder.getConstraintValidation().getPayload();
    }

}
