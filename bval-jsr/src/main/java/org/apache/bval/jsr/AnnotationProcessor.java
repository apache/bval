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

import org.apache.bval.model.Features;
import org.apache.bval.model.Meta;
import org.apache.bval.model.MetaBean;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.ObjectUtils;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.Valid;
import javax.validation.constraintvalidation.SupportedValidationTarget;
import javax.validation.constraintvalidation.ValidationTarget;
import javax.validation.groups.ConvertGroup;
import javax.validation.groups.Default;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Description: implements uniform handling of JSR303 {@link Constraint}
 * annotations, including composed constraints and the resolution of
 * {@link ConstraintValidator} implementations.
 */
@Privilizing(@CallTo(Reflection.class))
public final class AnnotationProcessor {
    /** {@link ApacheFactoryContext} used */
    private final ApacheValidatorFactory factory;

    /**
     * Create a new {@link AnnotationProcessor} instance.
     * 
     * @param factory the validator factory.
     */
    public AnnotationProcessor(ApacheValidatorFactory factory) {
        this.factory = factory;
    }

    /**
     * Process JSR303 annotations.
     * 
     * @param prop
     *            potentially null
     * @param owner
     *            bean type
     * @param element
     *            whose annotations to read
     * @param access
     *            strategy for <code>prop</code>
     * @param appender
     *            handling accumulation
     * @return whether any processing took place
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public boolean processAnnotations(Meta prop, Class<?> owner, AnnotatedElement element, AccessStrategy access,
        AppendValidation appender) throws IllegalAccessException, InvocationTargetException {

        boolean changed = false;
        for (final Annotation annotation : element.getDeclaredAnnotations()) {
            final Class<?> type = annotation.annotationType();
            if (type.getName().startsWith("java.lang.annotation.")) {
                continue;
            }
            changed = processAnnotation(annotation, prop, owner, access, appender, true) || changed;
        }
        return changed;
    }

    /**
     * Process a single annotation.
     * 
     * @param <A>
     *            annotation type
     * @param annotation
     *            to process
     * @param prop
     *            potentially null
     * @param owner
     *            bean type
     * @param access
     *            strategy for <code>prop</code>
     * @param appender
     *            handling accumulation
     * @return whether any processing took place
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public <A extends Annotation> boolean processAnnotation(A annotation, Meta prop, Class<?> owner,
        AccessStrategy access, AppendValidation appender, boolean reflection)
        throws IllegalAccessException, InvocationTargetException {
        if (annotation instanceof Valid) {
            return addAccessStrategy(prop, access);
        }

        if (ConvertGroup.class.isInstance(annotation) || ConvertGroup.List.class.isInstance(annotation)) {
            if (!reflection) {
                Collection<Annotation> annotations = prop.getFeature(JsrFeatures.Property.ANNOTATIONS_TO_PROCESS);
                if (annotations == null) {
                    annotations = new ArrayList<Annotation>();
                    prop.putFeature(JsrFeatures.Property.ANNOTATIONS_TO_PROCESS, annotations);
                }
                annotations.add(annotation);
            }
            return true;
        }

        /**
         * An annotation is considered a constraint definition if its retention
         * policy contains RUNTIME and if the annotation itself is annotated
         * with javax.validation.Constraint.
         */
        final Constraint vcAnno = annotation.annotationType().getAnnotation(Constraint.class);
        if (vcAnno != null) {
            Class<? extends ConstraintValidator<A, ?>>[] validatorClasses;
            validatorClasses = findConstraintValidatorClasses(annotation, vcAnno);
            return applyConstraint(annotation, validatorClasses, prop, owner, access, appender);
        }
        /**
         * Multi-valued constraints: To support this requirement, the bean
         * validation provider treats regular annotations (annotations not
         * annotated by @Constraint) whose value element has a return type of an
         * array of constraint annotations in a special way.
         */
        final Object result =
            Reflection.getAnnotationValue(annotation, ConstraintAnnotationAttributes.VALUE.getAttributeName());
        if (result instanceof Annotation[]) {
            boolean changed = false;
            for (final Annotation each : (Annotation[]) result) {
                if (each.annotationType().getName().startsWith("java.lang.annotation")) {
                    continue;
                }

                changed |= processAnnotation(each, prop, owner, access, appender, reflection);
            }
            return changed;
        }
        return false;
    }

    /**
     * Add the specified {@link AccessStrategy} to <code>prop</code>; noop if
     * <code>prop == null</code>.
     * 
     * @param prop
     * @param access
     * @return whether anything took place.
     */
    public boolean addAccessStrategy(Meta prop, AccessStrategy access) {
        if (prop == null) {
            return false;
        }
        AccessStrategy[] strategies = prop.getFeature(Features.Property.REF_CASCADE);
        if (ObjectUtils.arrayContains(strategies, access)) {
            return false;
        }
        if (strategies == null) {
            strategies = new AccessStrategy[] { access };
        } else {
            strategies = ObjectUtils.arrayAdd(strategies, access);
        }
        prop.putFeature(Features.Property.REF_CASCADE, strategies);
        return true;
    }

    /**
     * Find available {@link ConstraintValidation} classes for a given
     * constraint annotation.
     * 
     * @param annotation
     * @param vcAnno
     * @return {@link ConstraintValidation} implementation class array
     */
    @SuppressWarnings("unchecked")
    private <A extends Annotation> Class<? extends ConstraintValidator<A, ?>>[] findConstraintValidatorClasses(
        A annotation, Constraint vcAnno) {
        if (vcAnno == null) {
            vcAnno = annotation.annotationType().getAnnotation(Constraint.class);
        }
        final Class<A> annotationType = (Class<A>) annotation.annotationType();
        Class<? extends ConstraintValidator<A, ?>>[] validatorClasses =
            factory.getConstraintsCache().getConstraintValidators(annotationType);
        if (validatorClasses == null) {
            validatorClasses = (Class<? extends ConstraintValidator<A, ?>>[]) vcAnno.validatedBy();
            if (validatorClasses.length == 0) {
                validatorClasses = factory.getDefaultConstraints().getValidatorClasses(annotationType);
            }
        }
        return validatorClasses;
    }

    /**
     * Apply a constraint to the specified <code>appender</code>.
     * 
     * @param annotation
     *            constraint annotation
     * @param rawConstraintClasses
     *            known {@link ConstraintValidator} implementation classes for
     *            <code>annotation</code>
     * @param prop
     *            meta-property
     * @param owner
     *            type
     * @param access
     *            strategy
     * @param appender
     * @return success flag
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private <A extends Annotation> boolean applyConstraint(A annotation,
        Class<? extends ConstraintValidator<A, ?>>[] rawConstraintClasses, Meta prop, Class<?> owner,
        AccessStrategy access, AppendValidation appender) throws IllegalAccessException, InvocationTargetException {

        final Class<? extends ConstraintValidator<A, ?>>[] constraintClasses = select(rawConstraintClasses, access);
        if (constraintClasses != null && constraintClasses.length == 0 && rawConstraintClasses.length > 0) {
            return false;
        }

        final AnnotationConstraintBuilder<A> builder =
            new AnnotationConstraintBuilder<A>(constraintClasses, annotation, owner, access, null);

        // JSR-303 3.4.4: Add implicit groups
        if (prop != null && prop.getParentMetaBean() != null) {
            final MetaBean parentMetaBean = prop.getParentMetaBean();
            // If:
            // - the owner is an interface
            // - the class of the metabean being build is different than the
            // owner
            // - and only the Default group is defined
            // Then: add the owner interface as implicit groups
            if (builder.getConstraintValidation().getOwner().isInterface()
                && parentMetaBean.getBeanClass() != builder.getConstraintValidation().getOwner()
                && builder.getConstraintValidation().getGroups().size() == 1
                && builder.getConstraintValidation().getGroups().contains(Default.class)) {
                Set<Class<?>> groups = builder.getConstraintValidation().getGroups();
                groups.add(builder.getConstraintValidation().getOwner());
                builder.getConstraintValidation().setGroups(groups);
            }
        }

        // If already building a constraint composition tree, ensure that:
        // - the parent groups are inherited
        // - the parent payload is inherited
        if (appender instanceof AppendValidationToBuilder) {
            AppendValidationToBuilder avb = (AppendValidationToBuilder) appender;
            builder.getConstraintValidation().setGroups(avb.getInheritedGroups());
            builder.getConstraintValidation().setPayload(avb.getInheritedPayload());
        }

        // process composed constraints:
        // here are not other superclasses possible, because annotations do not
        // inherit!
        processAnnotations(prop, owner, annotation.annotationType(), access, new AppendValidationToBuilder(builder));

        // Even if the validator is null, it must be added to mimic the RI impl
        appender.append(builder.getConstraintValidation());
        return true;
    }

    private static <A extends Annotation> Class<? extends ConstraintValidator<A, ?>>[] select(
        final Class<? extends ConstraintValidator<A, ?>>[] rawConstraintClasses, final AccessStrategy access) {
        final boolean isReturn = ReturnAccess.class.isInstance(access);
        final boolean isParam = ParametersAccess.class.isInstance(access);
        if (rawConstraintClasses != null && (isReturn || isParam)) {
            final Collection<Class<? extends ConstraintValidator<A, ?>>> selected =
                new ArrayList<Class<? extends ConstraintValidator<A, ?>>>();
            for (final Class<? extends ConstraintValidator<A, ?>> constraint : rawConstraintClasses) {
                final SupportedValidationTarget target = constraint.getAnnotation(SupportedValidationTarget.class);
                if (target == null && isReturn) {
                    selected.add(constraint);
                } else if (target != null) {
                    for (final ValidationTarget validationTarget : target.value()) {
                        if (isReturn && ValidationTarget.ANNOTATED_ELEMENT == validationTarget) {
                            selected.add(constraint);
                        } else if (isParam && ValidationTarget.PARAMETERS == validationTarget) {
                            selected.add(constraint);
                        }
                    }
                }
            }
            @SuppressWarnings("unchecked")
            final Class<? extends ConstraintValidator<A, ?>>[] result = selected.toArray(new Class[selected.size()]);
            return result;
        }
        return rawConstraintClasses;
    }

}
