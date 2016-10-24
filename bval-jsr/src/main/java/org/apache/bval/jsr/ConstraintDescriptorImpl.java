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

import javax.validation.ConstraintTarget;
import javax.validation.Payload;
import javax.validation.metadata.ConstraintDescriptor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, {@link Serializable} {@link ConstraintDescriptor} implementation.
 *
 * User: roman.stumm<br>
 * Date: 22.04.2010<br>
 * Time: 10:21:23<br>
 */
public class ConstraintDescriptorImpl<T extends Annotation> implements ConstraintDescriptor<T>, Serializable {
    /** Serialization version */
    private static final long serialVersionUID = 1L;

    private final T annotation;
    private final Set<Class<?>> groups;
    private final Set<Class<? extends javax.validation.Payload>> payload;
    private final List<java.lang.Class<? extends javax.validation.ConstraintValidator<T, ?>>> constraintValidatorClasses;
    private final Map<String, Object> attributes;
    private final Set<ConstraintDescriptor<?>> composingConstraints;
    private final boolean reportAsSingleViolation;
    private final ConstraintTarget validationAppliesTo;
    private final String template;
    private final int hashCode;

    /**
     * Create a new ConstraintDescriptorImpl instance.
     * 
     * @param descriptor
     */
    public ConstraintDescriptorImpl(final ConstraintDescriptor<T> descriptor) {
        this(descriptor.getAnnotation(), descriptor.getGroups(), descriptor.getPayload(), descriptor
            .getConstraintValidatorClasses(), descriptor.getAttributes(), descriptor.getComposingConstraints(),
            descriptor.isReportAsSingleViolation(), descriptor.getValidationAppliesTo(), descriptor.getMessageTemplate());
    }

    /**
     * Create a new ConstraintDescriptorImpl instance.
     * 
     * @param annotation
     * @param groups
     * @param payload
     * @param constraintValidatorClasses
     * @param attributes
     * @param composingConstraints
     * @param reportAsSingleViolation
     */
    public ConstraintDescriptorImpl(T annotation, Set<Class<?>> groups,
        Set<Class<? extends javax.validation.Payload>> payload,
        List<java.lang.Class<? extends javax.validation.ConstraintValidator<T, ?>>> constraintValidatorClasses,
        Map<String, Object> attributes, Set<ConstraintDescriptor<?>> composingConstraints,
        boolean reportAsSingleViolation, ConstraintTarget validationAppliesTo, String messageTemplate) {
        this.annotation = annotation;
        this.groups = groups;
        this.payload = payload;
        this.constraintValidatorClasses = constraintValidatorClasses;
        this.attributes = attributes;
        this.composingConstraints = composingConstraints;
        this.reportAsSingleViolation = reportAsSingleViolation;
        this.validationAppliesTo = validationAppliesTo;
        this.template = messageTemplate;
        this.hashCode = computeHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getAnnotation() {
        return annotation;
    }

    @Override
    public String getMessageTemplate() {
        return template;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<?>> getGroups() {
        return groups;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<? extends Payload>> getPayload() {
        return payload;
    }

    @Override
    public ConstraintTarget getValidationAppliesTo() {
        return validationAppliesTo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<java.lang.Class<? extends javax.validation.ConstraintValidator<T, ?>>> getConstraintValidatorClasses() {
        return constraintValidatorClasses;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ConstraintDescriptor<?>> getComposingConstraints() {
        return composingConstraints;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReportAsSingleViolation() {
        return reportAsSingleViolation;
    }


    /**
     * generated equals on all fields except hashCode
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        ConstraintDescriptorImpl<?> that = (ConstraintDescriptorImpl<?>) o;

        if (reportAsSingleViolation != that.reportAsSingleViolation) { return false; }
        if (annotation != null ? !annotation.equals(that.annotation) : that.annotation != null) { return false; }
        if (groups != null ? !groups.equals(that.groups) : that.groups != null) { return false; }
        if (payload != null ? !payload.equals(that.payload) : that.payload != null) { return false; }
        if (constraintValidatorClasses != null ? !constraintValidatorClasses.equals(that.constraintValidatorClasses) : that.constraintValidatorClasses != null) { return false; }
        if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null) { return false; }
        if (composingConstraints != null ? !composingConstraints.equals(that.composingConstraints) : that.composingConstraints != null) { return false; }
        if (validationAppliesTo != that.validationAppliesTo) { return false; }
        return template != null ? template.equals(that.template) : that.template == null;

    }


    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * generated hashCode on all fields except hashCode
     */
    private int computeHashCode() {
        int result = annotation != null ? annotation.hashCode() : 0;
        result = 31 * result + (groups != null ? groups.hashCode() : 0);
        result = 31 * result + (payload != null ? payload.hashCode() : 0);
        result = 31 * result + (constraintValidatorClasses != null ? constraintValidatorClasses.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + (composingConstraints != null ? composingConstraints.hashCode() : 0);
        result = 31 * result + (reportAsSingleViolation ? 1 : 0);
        result = 31 * result + (validationAppliesTo != null ? validationAppliesTo.hashCode() : 0);
        result = 31 * result + (template != null ? template.hashCode() : 0);
        return result;
    }
}
