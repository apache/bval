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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;

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
 * methods with {@code cascade == false}.
 * 
 * @version $Rev: 993539 $ $Date: 2010-09-07 16:27:50 -0500 (Tue, 07 Sep 2010) $
 */
public interface CascadingPropertyValidator extends Validator {

    /**
     * {@inheritDoc} Validates all constraints placed on the property of {@code object} named {@code propertyName}.
     *
     * @param object       object to validate
     * @param propertyName property to validate (i.e. field and getter constraints). Nested
     *                     properties may be referenced (e.g. prop[2].subpropA.subpropB)
     * @param groups       group or list of groups targeted for validation (default to
     *                     {@link jakarta.validation.groups.Default})
     * @return constraint violations or an empty {@link Set} if none
     * @throws IllegalArgumentException if {@code object} is {@code null}, if {@code propertyName null},
     *                                  empty or not a valid object property or if {@code null} is
     *                                  passed to the varargs {@code groups}
     * @throws ValidationException      if a non recoverable error happens during the validation process
     */
    @Override
    default <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
        return validateProperty(object, propertyName, false, groups);
    }

    /**
     * Validates all constraints placed on the property of {@code object} named {@code propertyName}.
     *
     * @param object       object to validate
     * @param propertyName property to validate (i.e. field and getter constraints). Nested
     *                     properties may be referenced (e.g. prop[2].subpropA.subpropB)
     * @param cascade      whether to cascade along {@link Valid} properties
     * @param groups       group or list of groups targeted for validation (default to
     *                     {@link jakarta.validation.groups.Default})
     * @return constraint violations or an empty {@link Set} if none
     * @throws IllegalArgumentException if {@code object} is {@code null}, if {@code propertyName null},
     *                                  empty or not a valid object property or if {@code null} is
     *                                  passed to the varargs {@code groups}
     * @throws ValidationException      if a non recoverable error happens during the validation process
     */
    <T> Set<jakarta.validation.ConstraintViolation<T>> validateProperty(T object, String propertyName, boolean cascade,
        Class<?>... groups);

    /**
     * {@inheritDoc} Validates all constraints placed on the property named {@code propertyName} of the class
     * {@code beanType} would the property value be {@code value}.
     * <p/>
     * {@link ConstraintViolation} objects return {@code null} for {@link ConstraintViolation#getRootBean()} and
     * {@link ConstraintViolation#getLeafBean()}.
     *
     * @param beanType     the bean type
     * @param propertyName property to validate
     * @param value        property value to validate
     * @param groups       group or list of groups targeted for validation (default to
     *                     {@link jakarta.validation.groups.Default})
     * @return constraint violations or an empty {@link Set} if none
     * @throws IllegalArgumentException if {@code beanType} is {@code null}, if
     *                                  {@code propertyName null}, empty or not a valid object
     *                                  property or if {@code null} is passed to the varargs {@code groups}
     * @throws ValidationException      if a non recoverable error happens during the validation process
     */
    @Override
    default <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value,
        Class<?>... groups) {
        return validateValue(beanType, propertyName, value, false, groups);
    }

    /**
     * {@inheritDoc} Validates all constraints placed on the property named {@code propertyName} of the class
     * {@code beanType} would the property value be {@code value}.
     * <p/>
     * {@link ConstraintViolation} objects return {@code null} for {@link ConstraintViolation#getRootBean()} and
     * {@link ConstraintViolation#getLeafBean()}.
     *
     * @param beanType     the bean type
     * @param propertyName property to validate
     * @param value        property value to validate
     * @param groups       group or list of groups targeted for validation (default to
     *                     {@link jakarta.validation.groups.Default})
     * @return constraint violations or an empty {@link Set} if none
     * @throws IllegalArgumentException if {@code beanType} is {@code null}, if
     *                                  {@code propertyName null}, empty or not a valid object
     *                                  property or if {@code null} is passed to the varargs {@code groups}
     * @throws ValidationException      if a non recoverable error happens during the validation process
     */
    <T> Set<jakarta.validation.ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value,
        boolean cascade, Class<?>... groups);
}
