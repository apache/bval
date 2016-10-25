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
package org.apache.bval.jsr;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;
import java.util.Set;

/**
 * Per the bean validation spec, {@link Valid} is not honored by the
 * {@link #validateProperty(Object, String, Class...)} and
 * {@link #validateValue(Class, String, Object, Class...)} methods. The
 * {@link CascadingPropertyValidator} interface thus defines a {@link Validator} that
 * provides corresponding methods that <em>may</em> honor {@link Valid}.
 * It should be noted that {@link Validator#validateProperty(Object, String, Class...)}
 * and {@link Validator#validateValue(Class, String, Object, Class...)} are assumed
 * semantically equivalent to calling the {@link CascadingPropertyValidator}-defined
 * methods with <code>cascade == false</code>.
 * 
 * @version $Rev: 993539 $ $Date: 2010-09-07 16:27:50 -0500 (Tue, 07 Sep 2010) $
 */
public interface CascadingPropertyValidator extends Validator {

    /**
     * Validates all constraints placed on <code>object</code>'s
     * <code>propertyName</code> property, with optional validation cascading.
     * 
     * @param <T>
     * @param object
     * @param propertyName
     * @param cascade
     * @param groups
     * @return the resulting {@link Set} of {@link ConstraintViolation}s.
     */
    <T> Set<javax.validation.ConstraintViolation<T>> validateProperty(T object, String propertyName, boolean cascade,
        Class<?>... groups);

    /**
     * Validates all constraints placed on <code>object</code>'s
     * <code>propertyName</code> property, with optional validation cascading,
     * given a hypothetical property <code>value</code>.
     * 
     * @param <T>
     * @param beanType
     * @param propertyName
     * @param value
     * @param cascade
     * @param groups
     * @return the resulting {@link Set} of {@link ConstraintViolation}s.
     */
    <T> Set<javax.validation.ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value,
        boolean cascade, Class<?>... groups);
}
