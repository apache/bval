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


import org.apache.bval.jsr303.groups.GroupsComputer;
import org.apache.bval.jsr303.util.SecureActions;
import org.apache.bval.jsr303.xml.AnnotationProxyBuilder;
import org.apache.bval.util.AccessStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * Description: helper class that builds a {@link ConstraintValidation} or its
 * composite constraint validations by parsing the jsr303-annotations
 * and providing information (e.g. for @OverridesAttributes) <br/>
 */
final class AnnotationConstraintBuilder<A extends Annotation> {
    private static final Logger log = LoggerFactory.getLogger(AnnotationConstraintBuilder.class);

    private final ConstraintValidation<?> constraintValidation;
    private List<ConstraintOverrides> overrides;

    /**
     * Create a new AnnotationConstraintBuilder instance.
     * @param validatorClasses
     * @param constraintValidator
     * @param annotation
     * @param owner
     * @param access
     */
    public AnnotationConstraintBuilder(
          Class<? extends ConstraintValidator<A, ?>>[] validatorClasses,
          ConstraintValidator<A, ?> constraintValidator, A annotation, Class<?> owner,
          AccessStrategy access) {
        boolean reportFromComposite = annotation != null && annotation.annotationType()
              .isAnnotationPresent(ReportAsSingleViolation.class);
        constraintValidation = new ConstraintValidation<A>(validatorClasses,
              constraintValidator, annotation, owner, access, reportFromComposite);
        buildFromAnnotation();
    }

    /** build attributes, payload, groups from 'annotation' */
    private void buildFromAnnotation() {
        if (constraintValidation.getAnnotation() != null) {
            SecureActions.run(new PrivilegedAction<Object>() {
                public Object run() {
                    for (Method method : constraintValidation.getAnnotation()
                          .annotationType()
                          .getDeclaredMethods()) {
                        // groups + payload must also appear in attributes (also checked by TCK-Tests)
                        if (method.getParameterTypes().length == 0) {
                            try {
                                if (Jsr303MetaBeanFactory.ANNOTATION_PAYLOAD.equals(method.getName())) {
                                    buildPayload(method);
                                } else if (Jsr303MetaBeanFactory.ANNOTATION_GROUPS.equals(method.getName())) {
                                    buildGroups(method);
                                } else {
                                    constraintValidation.getAttributes()
                                          .put(method.getName(), method.invoke(
                                                constraintValidation.getAnnotation()));
                                }
                            } catch (Exception e) { // do nothing
                                log.warn("error processing annotation: " +
                                      constraintValidation.getAnnotation(), e);
                            }
                        }
                    }
                    return null;
                }
            });
        }
    }

    private void buildGroups(Method method)
          throws IllegalAccessException, InvocationTargetException {
        Object raw = method.invoke(constraintValidation.getAnnotation());
        Class<?>[] garr;
        if (raw instanceof Class<?>) {
            garr = new Class[]{(Class<?>) raw};
        } else if (raw instanceof Class<?>[]) {
            garr = (Class<?>[]) raw;
        } else {
            garr = null;
        }

        if (garr == null || garr.length == 0) {
            garr = GroupsComputer.getDefaultGroupArray();
        }
        constraintValidation.setGroups(new HashSet<Class<?>>(Arrays.asList(garr)));
    }

    @SuppressWarnings("unchecked")
    private void buildPayload(Method method)
          throws IllegalAccessException, InvocationTargetException {
        Class<? extends Payload>[] payload_raw =
              (Class<? extends Payload>[]) method.invoke(constraintValidation.getAnnotation());
        Set<Class<? extends Payload>> payloadSet;
        if (payload_raw == null) {
            payloadSet = Collections.<Class<? extends Payload>>emptySet();
        } else {
            payloadSet = new HashSet<Class<? extends Payload>>(payload_raw.length);
            payloadSet.addAll(Arrays.asList(payload_raw));
        }
        constraintValidation.setPayload(payloadSet);
    }

    /**
     * Get the configured {@link ConstraintValidation}.
     * @return {@link ConstraintValidation}
     */
    public ConstraintValidation<?> getConstraintValidation() {
        return constraintValidation;
    }

    /**
     * initialize a child composite 'validation' with @OverridesAttribute
     * from 'constraintValidation' and add to composites.
     */
    public void addComposed(ConstraintValidation<?> composite) {
        applyOverridesAttributes(composite);
        constraintValidation.addComposed(composite); // add AFTER apply()
    }

    private void applyOverridesAttributes(ConstraintValidation<?> composite) {
        if (null == overrides) {
            buildOverridesAttributes();
        }
        if (!overrides.isEmpty()) {
            int index = computeIndex(composite);

            // Search for the overrides to apply
            ConstraintOverrides generalOverride = findOverride(composite.getAnnotation().annotationType(), -1);
            if ( generalOverride != null ) {
                if ( index > 0 ) {
                    throw new ConstraintDeclarationException("Wrong OverridesAttribute declaration for " + generalOverride.constraintType + ", it needs a defined index when there is a list of constraints");
                }
                generalOverride.applyOn(composite);
            }

            ConstraintOverrides override = findOverride(composite.getAnnotation().annotationType(), index);
            if ( override != null ) {
                override.applyOn(composite);
            }

        }
    }


    /**
     * Calculates the index of the composite constraint. The index represents
     * the order in which it is added in reference to other constraints of the
     * same type.
     *
     * @param composite
     *            The composite constraint (not yet added).
     * @return An integer index always >= 0
     */
    private int computeIndex(ConstraintValidation<?> composite) {
        int idx = 0;
        for (ConstraintValidation<?> each : constraintValidation.getComposingValidations()) {
            if (each.getAnnotation().annotationType() == composite.getAnnotation().annotationType()) {
                idx++;
            }
        }
        return idx;
    }

    /** read overridesAttributes from constraintValidation.annotation */
    private void buildOverridesAttributes() {
        overrides = new LinkedList<ConstraintOverrides>();
        for (Method method : constraintValidation.getAnnotation()
              .annotationType()
              .getDeclaredMethods()) {
            OverridesAttribute.List annoOAL =
                  method.getAnnotation(OverridesAttribute.List.class);
            if (annoOAL != null) {
                for (OverridesAttribute annoOA : annoOAL.value()) {
                    parseConstraintOverride(method.getName(), annoOA);
                }
            }
            OverridesAttribute annoOA = method.getAnnotation(OverridesAttribute.class);
            if (annoOA != null) {
                parseConstraintOverride(method.getName(), annoOA);
            }
        }
    }

    private void parseConstraintOverride(String methodName, OverridesAttribute oa) {
        ConstraintOverrides target = findOverride(oa.constraint(), oa.constraintIndex());
        if (target == null) {
            target = new ConstraintOverrides(oa.constraint(), oa.constraintIndex());
            overrides.add(target);
        }
        target.values
              .put(oa.name(), constraintValidation.getAttributes().get(methodName));
    }

    private ConstraintOverrides findOverride(Class<? extends Annotation> constraint,
                                             int constraintIndex) {
        for (ConstraintOverrides each : overrides) {
            if (each.constraintType == constraint &&
                  each.constraintIndex == constraintIndex) {
                return each;
            }
        }
        return null;
    }

    /**
     * Holds the values to override in a composed constraint
     * during creation of a composed ConstraintValidation
     */
    private static final class ConstraintOverrides {
        final Class<? extends Annotation> constraintType;
        final int constraintIndex;

        /** key = attributeName, value = overridden value */
        final Map<String, Object> values;

        private ConstraintOverrides(Class<? extends Annotation> constraintType,
                                    int constraintIndex) {
            this.constraintType = constraintType;
            this.constraintIndex = constraintIndex;
            values = new HashMap<String, Object>();
        }

        @SuppressWarnings("unchecked")
        private void applyOn(ConstraintValidation<?> composite) {
            // Update the attributes
            composite.getAttributes().putAll(values);

            // And the annotation
            Annotation originalAnnot = composite.getAnnotation();
            AnnotationProxyBuilder<Annotation> apb = new AnnotationProxyBuilder<Annotation>(originalAnnot);
            for ( String key : values.keySet() ) {
                apb.putValue(key, values.get(key));
            }
            Annotation newAnnot = apb.createAnnotation();
            ((ConstraintValidation<Annotation>) composite).setAnnotation(newAnnot);
        }
    }
}
