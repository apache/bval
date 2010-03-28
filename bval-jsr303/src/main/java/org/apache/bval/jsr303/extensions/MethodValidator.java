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
package org.apache.bval.jsr303.extensions;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Description: Appendix C. Proposal for method-level validation.
 * This interface contains the APIs added to javax.validation.Validator.
 * It can be removed as soon as the Validator interface contains these methods.
 * The extension is not a part of the JSR303 core specification yet, but could
 * be in a future revision.<br/>
 * You can access the extension via the use of the Validator.unwrap() method.<br/>
 * User: roman <br/>
 * Date: 11.11.2009 <br/>
 * Time: 11:04:56 <br/>
 * Copyright: Agimatec GmbH
 */
public interface MethodValidator extends Validator {
    /**
     * Validate each parameter value based on the constraints described on
     * the parameters of <code>method</code>.
     *
     * @param clazz           class hosting the method
     * @param method          the method whose parameters are currectly validated
     * @param parameterValues the parameter values passed to the method for invocation
     * @param groups          groups targeted for validation
     * @return set of constraint violations
     * @throws IllegalArgumentException if the method does not belong to <code>T</code>
     *                                  or if the Object[] does not match the method signature
     */
    <T> Set<ConstraintViolation<T>> validateParameters(Class<T> clazz, Method method,
                                                       Object[] parameterValues,
                                                       Class<?>... groups);

    /**
     * Validate the parameter value based on the constraints described on
     * the parameterIndex-th parameter of <code>method</code>.
     *
     * @param clazz          class hosting the method
     * @param method         the method whose parameters are currectly validated
     * @param parameterValue the parameter value passed to the parameterIndex-t parameter of method
     * @param parameterIndex parameter index of the parameter validated in method
     * @param groups         groups targeted for validation
     * @return set of constraint violations
     * @throws IllegalArgumentException if the method does not belong to <code>T</code>
     *                                  or if parameterIndex is out of bound
     */
    <T> Set<ConstraintViolation<T>> validateParameter(Class<T> clazz, Method method,
                                                   Object parameterValue,
                                                   int parameterIndex,
                                                   Class<?>... groups);

    /**
     * Validate each parameter value based on the constraints described on
     * <code>method</code>.
     *
     * @param clazz         class hosting the method
     * @param method        the method whose result is validated
     * @param returnedValue the value returned by the method invocation
     * @param groups        groups targeted for validation
     * @return set of constraint violations
     * @throws IllegalArgumentException if the method does not belong to <code>T</code>
     */
    <T> Set<ConstraintViolation<T>> validateReturnedValue(Class<T> clazz, Method method,
                                                       Object returnedValue,
                                                       Class<?>... groups);

    /**
     * Validate each parameter value based on the constraints described on
     * the parameters of <code>constructor</code>.
     *
     * @param clazz           class hosting the constructor
     * @param constructor     the constructor whose parameters are currectly validated
     * @param parameterValues the parameter values passed to the constructor for invocation
     * @param groups          groups targeted for validation
     * @return set of constraint violations
     * @throws IllegalArgumentException if the constructor does not belong to <code>T</code>
     *                                  or if the Object[] does not match the constructor signature
     */
    <T> Set<ConstraintViolation<T>> validateParameters(Class<T> clazz,
                                                    Constructor constructor,
                                                    Object[] parameterValues,
                                                    Class<?>... groups);

    /**
     * Validate the parameter value based on the constraints described on
     * the parameterIndex-th parameter of <code>constructor</code>.
     *
     * @param clazz          class hosting the constructor
     * @param constructor    the method whose parameters are currectly validated
     * @param parameterValue the parameter value passed to the
     *                       parameterIndex-th parameter of constructor
     * @param parameterIndex parameter index of the parameter validated in constructor
     * @param groups         groups targeted for validation
     * @return set of constraint violations
     * @throws IllegalArgumentException if the constructor does not belong to <code>T</code>
     *                                  or if prameterIndex is out of bound
     */
    <T> Set<ConstraintViolation<T>> validateParameter(Class<T> clazz,
                                                   Constructor constructor,
                                                   Object parameterValue,
                                                   int parameterIndex,
                                                   Class<?>... groups);
}
