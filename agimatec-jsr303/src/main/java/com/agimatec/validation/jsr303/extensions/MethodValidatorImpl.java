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
package com.agimatec.validation.jsr303.extensions;

import com.agimatec.validation.jsr303.*;
import com.agimatec.validation.jsr303.groups.Group;
import com.agimatec.validation.jsr303.groups.Groups;
import com.agimatec.validation.model.MetaBean;

import javax.validation.ConstraintViolation;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Description: experimental implementation of method-level-validation <br/>
 * User: roman <br/>
 * Date: 11.11.2009 <br/>
 * Time: 12:36:20 <br/>
 * Copyright: Agimatec GmbH
 */
class MethodValidatorImpl extends ClassValidator implements MethodValidator {

    public MethodValidatorImpl(AgimatecFactoryContext factoryContext) {
        super(factoryContext);
    }

    @Override
    protected BeanDescriptorImpl createBeanDescriptor(MetaBean metaBean) {
        MethodBeanDescriptorImpl descriptor = new MethodBeanDescriptorImpl(factoryContext,
              metaBean, metaBean.getValidations());
        MethodValidatorMetaBeanFactory factory =
              new MethodValidatorMetaBeanFactory(factoryContext);
        factory.buildMethodDescriptor(descriptor);
        return descriptor;
    }

    /**
     * enhancement: method-level-validation not yet completly implemented
     * <pre>example:
     * <code>
     * public @NotNull String saveItem(@Valid @NotNull Item item, @Max(23) BigDecimal
     * </code></pre>
     * spec:
     * The constraints declarations evaluated are the constraints hosted on the
     * parameters of the method or constructor. If @Valid is placed on a parameter,
     * constraints declared on the object itself are considered.
     *
     * @throws IllegalArgumentException enhancement: if the method does not belong to <code>T</code>
     *                                  or if the Object[] does not match the method signature
     */
    public <T> Set<ConstraintViolation<T>> validateParameters(Class<T> clazz, Method method,
                                                              Object[] parameters,
                                                              Class<?>... groupArray) {
        MethodBeanDescriptorImpl beanDesc =
              (MethodBeanDescriptorImpl) getConstraintsForClass(clazz);
        MethodDescriptorImpl methodDescriptor =
              (MethodDescriptorImpl) beanDesc.getConstraintsForMethod(method);
        return validateParameters(methodDescriptor.getMetaBean(),
              methodDescriptor.getParameterDescriptors(), parameters, groupArray);
    }

    public <T> Set<ConstraintViolation<T>> validateParameter(Class<T> clazz, Method method,
                                                             Object parameter,
                                                             int parameterIndex,
                                                             Class<?>... groupArray) {
        MethodBeanDescriptorImpl beanDesc =
              (MethodBeanDescriptorImpl) getConstraintsForClass(clazz);
        MethodDescriptorImpl methodDescriptor =
              (MethodDescriptorImpl) beanDesc.getConstraintsForMethod(method);
        ParameterDescriptorImpl paramDesc = (ParameterDescriptorImpl) methodDescriptor
              .getParameterDescriptors().get(parameterIndex);
        return validateParameter(paramDesc, parameter, groupArray);
    }

    public <T> Set<ConstraintViolation<T>> validateParameters(Class<T> clazz,
                                                              Constructor constructor,
                                                              Object[] parameters,
                                                              Class<?>... groupArray) {
        MethodBeanDescriptorImpl beanDesc =
              (MethodBeanDescriptorImpl) getConstraintsForClass(clazz);
        ConstructorDescriptorImpl constructorDescriptor =
              (ConstructorDescriptorImpl) beanDesc.getConstraintsForConstructor(constructor);
        return validateParameters(constructorDescriptor.getMetaBean(),
              constructorDescriptor.getParameterDescriptors(), parameters, groupArray);
    }

    public <T> Set<ConstraintViolation<T>> validateParameter(Class<T> clazz,
                                                             Constructor constructor,
                                                             Object parameter,
                                                             int parameterIndex,
                                                             Class<?>... groupArray) {
        MethodBeanDescriptorImpl beanDesc =
              (MethodBeanDescriptorImpl) getConstraintsForClass(clazz);
        ConstructorDescriptorImpl methodDescriptor =
              (ConstructorDescriptorImpl) beanDesc.getConstraintsForConstructor(constructor);
        ParameterDescriptorImpl paramDesc = (ParameterDescriptorImpl) methodDescriptor
              .getParameterDescriptors().get(parameterIndex);
        return validateParameter(paramDesc, parameter, groupArray);
    }

    /**
     * If @Valid  is placed on the method, the constraints declared on the object
     * itself are considered.
     */
    public <T> Set<ConstraintViolation<T>> validateReturnedValue(Class<T> clazz, Method method,
                                                                 Object returnedValue,
                                                                 Class<?>... groupArray) {
        MethodBeanDescriptorImpl beanDesc =
              (MethodBeanDescriptorImpl) getConstraintsForClass(clazz);
        MethodDescriptorImpl methodDescriptor =
              (MethodDescriptorImpl) beanDesc.getConstraintsForMethod(method);
        final GroupValidationContext<ConstraintValidationListener<Object>> context =
              createContext(methodDescriptor.getMetaBean(), returnedValue, groupArray);
        validateReturnedValueInContext(context, methodDescriptor);
        ConstraintValidationListener result = context.getListener();
        return result.getConstaintViolations();
    }

    private <T> Set<ConstraintViolation<T>> validateParameters(MetaBean metaBean,
                                                               List<ParameterDescriptor> paramDescriptors,
                                                               Object[] parameters,
                                                               Class<?>... groupArray) {
        if (parameters == null) throw new IllegalArgumentException("cannot validate null");
        if (parameters.length > 0) {
            try {
                GroupValidationContext<ConstraintValidationListener<Object[]>> context =
                      createContext(metaBean, null, groupArray);
                for (int i = 0; i < parameters.length; i++) {
                    ParameterDescriptorImpl paramDesc =
                          (ParameterDescriptorImpl) paramDescriptors.get(i);
                    context.setBean(parameters[i]);
                    validateParameterInContext(context, paramDesc);
                }
                ConstraintValidationListener result = context.getListener();
                return result.getConstaintViolations();
            } catch (RuntimeException ex) {
                throw unrecoverableValidationError(ex, parameters);
            }
        } else {
            return Collections.EMPTY_SET;
        }
    }

    private <T> Set<ConstraintViolation<T>> validateParameter(
          ParameterDescriptorImpl paramDesc, Object parameter, Class<?>... groupArray) {
        try {
            final GroupValidationContext<ConstraintValidationListener<Object>> context =
                  createContext(paramDesc.getMetaBean(), parameter, groupArray);
            final ConstraintValidationListener result = context.getListener();
            validateParameterInContext(context, paramDesc);
            return result.getConstaintViolations();
        } catch (RuntimeException ex) {
            throw unrecoverableValidationError(ex, parameter);
        }
    }

    /** validate constraints hosted on parameters of a method */
    private <T> void validateParameterInContext(
          GroupValidationContext<ConstraintValidationListener<T>> context,
          ParameterDescriptorImpl paramDesc) {

        final Groups groups = context.getGroups();

        for (ConstraintDescriptor consDesc : paramDesc.getConstraintDescriptors()) {
            ConstraintValidation validation = (ConstraintValidation) consDesc;
            // 1. process groups
            for (Group current : groups.getGroups()) {
                context.setCurrentGroup(current);
                validation.validate(context);
            }
            // 2. process sequences
            for (List<Group> eachSeq : groups.getSequences()) {
                for (Group current : eachSeq) {
                    context.setCurrentGroup(current);
                    validation.validate(context);
                    /**
                     * if one of the group process in the sequence leads to one or more validation failure,
                     * the groups following in the sequence must not be processed
                     */
                    if (!context.getListener().isEmpty()) break;
                }
            }
        }
        if (paramDesc.isCascaded() && context.getValidatedValue() != null) {
            context.setMetaBean(factoryContext.getMetaBeanFinder().
                  findForClass(context.getValidatedValue().getClass()));
            // 1. process groups
            for (Group current : groups.getGroups()) {
                context.setCurrentGroup(current);
                validateContext(context);
            }
            // 2. process sequences
            for (List<Group> eachSeq : groups.getSequences()) {
                for (Group current : eachSeq) {
                    context.setCurrentGroup(current);
                    validateContext(context);
                    /**
                     * if one of the group process in the sequence leads to one or more validation failure,
                     * the groups following in the sequence must not be processed
                     */
                    if (!context.getListener().isEmpty()) break;
                }
            }
        }
    }

    /** validate constraints hosted on parameters of a method */
    private <T> void validateReturnedValueInContext(
          GroupValidationContext<ConstraintValidationListener<T>> context,
          MethodDescriptorImpl methodDescriptor) {

        final Groups groups = context.getGroups();

        for (ConstraintDescriptor consDesc : methodDescriptor.getConstraintDescriptors()) {
            ConstraintValidation validation = (ConstraintValidation) consDesc;
            // 1. process groups
            for (Group current : groups.getGroups()) {
                context.setCurrentGroup(current);
                validation.validate(context);
            }
            // 2. process sequences
            for (List<Group> eachSeq : groups.getSequences()) {
                for (Group current : eachSeq) {
                    context.setCurrentGroup(current);
                    validation.validate(context);
                    /**
                     * if one of the group process in the sequence leads to one or more validation failure,
                     * the groups following in the sequence must not be processed
                     */
                    if (!context.getListener().isEmpty()) break;
                }
            }
        }
        if (methodDescriptor.isCascaded() && context.getValidatedValue() != null) {
            context.setMetaBean(factoryContext.getMetaBeanFinder().
                  findForClass(context.getValidatedValue().getClass()));
            // 1. process groups
            for (Group current : groups.getGroups()) {
                context.setCurrentGroup(current);
                validateContext(context);
            }
            // 2. process sequences
            for (List<Group> eachSeq : groups.getSequences()) {
                for (Group current : eachSeq) {
                    context.setCurrentGroup(current);
                    validateContext(context);
                    /**
                     * if one of the group process in the sequence leads to one or more validation failure,
                     * the groups following in the sequence must not be processed
                     */
                    if (!context.getListener().isEmpty()) break;
                }
            }
        }
    }
}
