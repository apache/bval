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
package org.apache.bval.jsr303;


import org.apache.bval.BeanValidationContext;
import org.apache.bval.jsr303.util.NodeImpl;
import org.apache.bval.jsr303.util.PathImpl;
import org.apache.bval.model.Validation;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;
import org.apache.bval.util.AccessStrategy;

import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintValidator;
import javax.validation.Payload;
import javax.validation.ValidationException;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Description: Adapter between Constraint (JSR303) and Validation (Agimatec)<br/>
 * this instance is immutable!<br/>
 */
public class ConstraintValidation<T extends Annotation>
      implements Validation, ConstraintDescriptor<T> {
    private static final String ANNOTATION_MESSAGE = "message";
    private final ConstraintValidator validator;
    private T annotation; // for metadata request API
    private final AccessStrategy access;
    private final boolean reportFromComposite;
    private final Map<String, Object> attributes;

    private Set<ConstraintValidation<?>> composedConstraints;

    /**
     * the owner is the type where the validation comes from.
     * it is used to support implicit grouping.
     */
    private final Class owner;
    private Set<Class<?>> groups;
    private Set<Class<? extends Payload>> payload;
    private Class<? extends ConstraintValidator<T, ?>>[] validatorClasses;

    /**
     * @param validator  - the constraint validator
     * @param annotation - the annotation of the constraint
     * @param owner      - the type where the annotated element is placed
     *                   (class, interface, annotation type)
     * @param access     - how to access the value
     */
    public ConstraintValidation(
          Class<? extends ConstraintValidator<T, ?>>[] validatorClasses,
          ConstraintValidator validator, T annotation, Class owner,
          AccessStrategy access, boolean reportFromComposite) {
        this.attributes = new HashMap();
        this.validatorClasses = validatorClasses;
        this.validator = validator;
        this.annotation = annotation;
        this.owner = owner;
        this.access = access;
        this.reportFromComposite = reportFromComposite;
    }            

    public ConstraintDescriptor<T> asSerializableDescriptor() {
      return new ConstraintDescriptorImpl(this);
    }

    void setGroups(Set<Class<?>> groups) {
        this.groups = groups;
        this.attributes.put("groups", groups.toArray(new Class[groups.size()]));
    }

    void setPayload(Set<Class<? extends Payload>> payload) {
        this.payload = payload;
        this.attributes.put("payload", groups.toArray(new Class[groups.size()]));
    }

    public boolean isReportAsSingleViolation() {
        return reportFromComposite;
    }

    public void addComposed(ConstraintValidation aConstraintValidation) {
        if (composedConstraints == null) {
            composedConstraints = new HashSet();
        }
        composedConstraints.add(aConstraintValidation);
    }

    public void validate(ValidationContext context) {
        validate((GroupValidationContext) context);
    }

    public void validate(GroupValidationContext context) {
        context.setConstraintValidation(this);
        /**
         * execute unless the given validation constraint has already been processed
         * during this validation routine (as part of a previous group match)
         */
        if (!isMemberOf(context.getCurrentGroup().getGroup())) {
            return; // do not validate in the current group
        }
        if (validator != null && !context.collectValidated(validator))
            return; // already done

        if (context.getMetaProperty() != null && !isCascadeEnabled(context)) {
            return;
        }

        // process composed constraints
        if (isReportAsSingleViolation()) {
            BeanValidationContext gctx = (BeanValidationContext) context;
            ConstraintValidationListener oldListener =
                  ((ConstraintValidationListener) gctx.getListener());
            ConstraintValidationListener listener =
                  new ConstraintValidationListener(oldListener.getRootBean(), oldListener.getRootBeanType());
            gctx.setListener(listener);
            try {
                for (ConstraintValidation composed : getComposingValidations()) {
                    composed.validate(context);
                }
            } finally {
                gctx.setListener(oldListener);
            }
            
            // Restore current constraint validation
            context.setConstraintValidation(this);
            
            // stop validating when already failed and ReportAsSingleInvalidConstraint = true ?
            if (!listener.getConstaintViolations().isEmpty()) {
                // TODO RSt - how should the composed constraint error report look like?
                ConstraintValidatorContextImpl jsrContext =
                      new ConstraintValidatorContextImpl(context, this);
                addErrors(context, jsrContext); // add defaultErrorMessage only*/
                return;
            }
        } else {
            for (ConstraintValidation composed : getComposingValidations()) {
                composed.validate(context);
            }
            
            // Restore current constraint validation
            context.setConstraintValidation(this);
        }
        
        if (validator != null) {
            ConstraintValidatorContextImpl jsrContext =
                  new ConstraintValidatorContextImpl(context, this);
            if (!validator.isValid(context.getValidatedValue(), jsrContext)) {
                addErrors(context, jsrContext);
            }
        }
    }

    /**
     * Initialize the validator (if not <code>null</code>) with the stored
     * annotation.
     */
    public void initialize() {
        if (null != validator) {
            try {
                validator.initialize(annotation);
            } catch (RuntimeException e) {
                // Either a "legit" problem initializing the validator or a
                // ClassCastException if the validator associated annotation is 
                // not a supertype of the validated annotation.
                throw new ConstraintDefinitionException("Incorrect validator [" + validator.getClass().getCanonicalName() + "] for annotation " + annotation.annotationType().getCanonicalName(), e);
            }
        }
    }

    private boolean isCascadeEnabled(GroupValidationContext context) {
        PathImpl path = context.getPropertyPath();
        NodeImpl node = path.getLeafNode();
        PathImpl beanPath = path.getPathWithoutLeafNode();
        if (beanPath == null) {
            beanPath = PathImpl.create(null);
        }
        try {
            if (!context.getTraversableResolver()
                  .isReachable(context.getBean(), node,
                        context.getRootMetaBean().getBeanClass(), beanPath,
                        access.getElementType())) return false;
        } catch (RuntimeException e) {
            throw new ValidationException(
                  "Error in TraversableResolver.isReachable() for " + context.getBean(), e);
        }

        try {
            if (!context.getTraversableResolver()
                  .isCascadable(context.getBean(), node,
                        context.getRootMetaBean().getBeanClass(), beanPath,
                        access.getElementType())) return false;
        } catch (RuntimeException e) {
            throw new ValidationException(
                  "Error TraversableResolver.isCascadable() for " + context.getBean(), e);
        }

        return true;
    }

    private void addErrors(GroupValidationContext context,
                           ConstraintValidatorContextImpl jsrContext) {
        for (ValidationListener.Error each : jsrContext.getErrorMessages()) {
            context.getListener().addError(each, context);
        }
    }

    public String toString() {
        return "ConstraintValidation{" + validator + '}';
    }

    public String getMessageTemplate() {
        return (String) attributes.get(ANNOTATION_MESSAGE);
    }

    public ConstraintValidator getValidator() {
        return validator;
    }

    protected boolean isMemberOf(Class<?> reqGroup) {
        return groups.contains(reqGroup);
    }

    public Class getOwner() {
        return owner;
    }

    public T getAnnotation() {
        return annotation;
    }

    public AccessStrategy getAccess() {
        return access;
    }
    
    public void setAnnotation(T annotation) {
        this.annotation = annotation;
    }

    /////////////////////////// ConstraintDescriptor implementation


    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Set<ConstraintDescriptor<?>> getComposingConstraints() {
        return composedConstraints == null ? Collections.EMPTY_SET : composedConstraints;
    }

    Set<ConstraintValidation<?>> getComposingValidations() {
        return composedConstraints == null ? Collections.EMPTY_SET : composedConstraints;
    }

    public Set<Class<?>> getGroups() {
        return groups;
    }

    public Set<Class<? extends Payload>> getPayload() {
        return payload;
    }

    public List<Class<? extends ConstraintValidator<T, ?>>> getConstraintValidatorClasses() {
        if ( validatorClasses == null ) {
            return Collections.emptyList();
        }
        return Arrays.asList(validatorClasses);
    }

}
