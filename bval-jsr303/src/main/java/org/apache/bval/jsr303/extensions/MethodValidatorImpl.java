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

import org.apache.bval.MetaBeanFactory;
import org.apache.bval.MetaBeanManager;
import org.apache.bval.jsr303.*;
import org.apache.bval.jsr303.groups.Group;
import org.apache.bval.jsr303.groups.Groups;
import org.apache.bval.model.MetaBean;
import org.apache.bval.util.ValidationHelper;

import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Description: experimental implementation of method-level-validation <br/>
 */
class MethodValidatorImpl extends ClassValidator implements MethodValidator {
    /**
     * Create a new MethodValidatorImpl instance.
     * 
     * @param factoryContext
     */
    public MethodValidatorImpl(ApacheFactoryContext factoryContext) {
        super(factoryContext);
        patchFactoryContextForMethodValidation(factoryContext);
    }

    /**
     * experimental: replace the Jsr303MetaBeanFactory with a
     * MethodValidatorMetaBeanFactory in the factoryContext.
     * 
     * @param factoryContext
     */
    private void patchFactoryContextForMethodValidation(ApacheFactoryContext factoryContext) {
        MetaBeanFactory[] factories = ((MetaBeanManager) getMetaBeanFinder()).getBuilder().getFactories();
        for (int i = 0; i < factories.length; i++) {
            if (factories[i] instanceof Jsr303MetaBeanFactory
                && !(factories[i] instanceof MethodValidatorMetaBeanFactory)) {
                factories[i] = new MethodValidatorMetaBeanFactory(factoryContext);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BeanDescriptorImpl createBeanDescriptor(MetaBean metaBean) {
        MethodBeanDescriptorImpl descriptor =
            new MethodBeanDescriptorImpl(factoryContext, metaBean, metaBean.getValidations());
        MethodValidatorMetaBeanFactory factory = new MethodValidatorMetaBeanFactory(factoryContext);
        factory.buildMethodDescriptor(descriptor);
        return descriptor;
    }

    /**
     * {@inheritDoc} enhancement: method-level-validation not yet completly
     * implemented
     * 
     * <pre>
     * example:
     * <code>
     * public @NotNull String saveItem(@Valid @NotNull Item item, @Max(23) BigDecimal
     * </code>
     * </pre>
     * 
     * spec: The constraints declarations evaluated are the constraints hosted
     * on the parameters of the method or constructor. If @Valid is placed on a
     * parameter, constraints declared on the object itself are considered.
     * 
     * @throws IllegalArgumentException
     *             enhancement: if the method does not belong to <code>T</code>
     *             or if the Object[] does not match the method signature
     */
    public <T> Set<ConstraintViolation<T>> validateParameters(Class<T> clazz, Method method, Object[] parameters,
        Class<?>... groupArray) {
        MethodBeanDescriptorImpl beanDesc = (MethodBeanDescriptorImpl) getConstraintsForClass(clazz);
        MethodDescriptorImpl methodDescriptor = (MethodDescriptorImpl) beanDesc.getConstraintsForMethod(method);
        if (methodDescriptor == null) {
            throw new ValidationException("Method " + method + " doesn't belong to class " + clazz);
        }
        return validateParameters(methodDescriptor.getMetaBean(), methodDescriptor.getParameterDescriptors(),
            parameters, groupArray);
    }

    /**
     * {@inheritDoc}
     */
    public <T> Set<ConstraintViolation<T>> validateParameter(Class<T> clazz, Method method, Object parameter,
        int parameterIndex, Class<?>... groupArray) {
        MethodBeanDescriptorImpl beanDesc = (MethodBeanDescriptorImpl) getConstraintsForClass(clazz);
        MethodDescriptorImpl methodDescriptor = (MethodDescriptorImpl) beanDesc.getConstraintsForMethod(method);
        if (methodDescriptor == null) {
            throw new ValidationException("Method " + method + " doesn't belong to class " + clazz);
        }
        ParameterDescriptorImpl paramDesc =
            (ParameterDescriptorImpl) methodDescriptor.getParameterDescriptors().get(parameterIndex);
        return validateParameter(paramDesc, parameter, groupArray);
    }

    /**
     * {@inheritDoc}
     */
    public <T> Set<ConstraintViolation<T>> validateParameters(Class<T> clazz, Constructor<T> constructor,
        Object[] parameters, Class<?>... groupArray) {
        MethodBeanDescriptorImpl beanDesc = (MethodBeanDescriptorImpl) getConstraintsForClass(clazz);
        ConstructorDescriptorImpl constructorDescriptor =
            (ConstructorDescriptorImpl) beanDesc.getConstraintsForConstructor(constructor);
        if (constructorDescriptor == null) {
            throw new ValidationException("Constructor " + constructor + " doesn't belong to class " + clazz);
        }
        return validateParameters(constructorDescriptor.getMetaBean(), constructorDescriptor.getParameterDescriptors(),
            parameters, groupArray);
    }

    /**
     * {@inheritDoc}
     */
    public <T> Set<ConstraintViolation<T>> validateParameter(Class<T> clazz, Constructor<T> constructor,
        Object parameter, int parameterIndex, Class<?>... groupArray) {
        MethodBeanDescriptorImpl beanDesc = (MethodBeanDescriptorImpl) getConstraintsForClass(clazz);
        ConstructorDescriptorImpl constructorDescriptor =
            (ConstructorDescriptorImpl) beanDesc.getConstraintsForConstructor(constructor);
        if (constructorDescriptor == null) {
            throw new ValidationException("Constructor " + constructor + " doesn't belong to class " + clazz);
        }
        ParameterDescriptorImpl paramDesc =
            (ParameterDescriptorImpl) constructorDescriptor.getParameterDescriptors().get(parameterIndex);
        return validateParameter(paramDesc, parameter, groupArray);
    }

    /**
     * {@inheritDoc} If @Valid is placed on the method, the constraints declared
     * on the object itself are considered.
     */
    @SuppressWarnings("unchecked")
    public <T> Set<ConstraintViolation<T>> validateReturnedValue(Class<T> clazz, Method method, Object returnedValue,
        Class<?>... groupArray) {
        MethodBeanDescriptorImpl beanDesc = (MethodBeanDescriptorImpl) getConstraintsForClass(clazz);
        MethodDescriptorImpl methodDescriptor = (MethodDescriptorImpl) beanDesc.getConstraintsForMethod(method);
        if (methodDescriptor == null) {
            throw new ValidationException("Method " + method + " doesn't belong to class " + clazz);
        }
        final GroupValidationContext<Object> context =
            createContext(methodDescriptor.getMetaBean(), returnedValue, null, groupArray);
        validateReturnedValueInContext(context, methodDescriptor);
        ConstraintValidationListener<T> result = (ConstraintValidationListener<T>) context.getListener();
        return result.getConstraintViolations();
    }

    @SuppressWarnings("unchecked")
    private <T> Set<ConstraintViolation<T>> validateParameters(MetaBean metaBean,
        List<ParameterDescriptor> paramDescriptors, Object[] parameters, Class<?>... groupArray) {
        if (parameters == null)
            throw new IllegalArgumentException("cannot validate null");
        if (parameters.length > 0) {
            try {
                GroupValidationContext<ConstraintValidationListener<Object[]>> context =
                    createContext(metaBean, null, null, groupArray);
                for (int i = 0; i < parameters.length; i++) {
                    ParameterDescriptorImpl paramDesc = (ParameterDescriptorImpl) paramDescriptors.get(i);
                    context.setBean(parameters[i]);
                    validateParameterInContext(context, paramDesc);
                }
                ConstraintValidationListener<T> result = (ConstraintValidationListener<T>) context.getListener();
                return result.getConstraintViolations();
            } catch (RuntimeException ex) {
                throw unrecoverableValidationError(ex, parameters);
            }
        } else {
            return Collections.<ConstraintViolation<T>> emptySet();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Set<ConstraintViolation<T>> validateParameter(ParameterDescriptorImpl paramDesc, Object parameter,
        Class<?>... groupArray) {
        try {
            final GroupValidationContext<Object> context =
                createContext(paramDesc.getMetaBean(), parameter, null, groupArray);
            final ConstraintValidationListener<T> result = (ConstraintValidationListener<T>) context.getListener();
            validateParameterInContext(context, paramDesc);
            return result.getConstraintViolations();
        } catch (RuntimeException ex) {
            throw unrecoverableValidationError(ex, parameter);
        }
    }

    /**
     * validate constraints hosted on parameters of a method
     */
    private <T> void validateParameterInContext(GroupValidationContext<T> context, ParameterDescriptorImpl paramDesc) {

        final Groups groups = context.getGroups();

        for (ConstraintDescriptor<?> consDesc : paramDesc.getConstraintDescriptors()) {
            ConstraintValidation<?> validation = (ConstraintValidation<?>) consDesc;
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
                     * if one of the group process in the sequence leads to one
                     * or more validation failure, the groups following in the
                     * sequence must not be processed
                     */
                    if (!context.getListener().isEmpty())
                        break;
                }
            }
        }
        if (paramDesc.isCascaded() && context.getValidatedValue() != null) {
            context
                .setMetaBean(factoryContext.getMetaBeanFinder().findForClass(context.getValidatedValue().getClass()));
            // 1. process groups
            for (Group current : groups.getGroups()) {
                context.setCurrentGroup(current);
                ValidationHelper
                    .validateContext(context, new Jsr303ValidationCallback(context), isTreatMapsLikeBeans());
            }
            // 2. process sequences
            for (List<Group> eachSeq : groups.getSequences()) {
                for (Group current : eachSeq) {
                    context.setCurrentGroup(current);
                    ValidationHelper.validateContext(context, new Jsr303ValidationCallback(context),
                        isTreatMapsLikeBeans());
                    /**
                     * if one of the group process in the sequence leads to one
                     * or more validation failure, the groups following in the
                     * sequence must not be processed
                     */
                    if (!context.getListener().isEmpty())
                        break;
                }
            }
        }
    }

    /**
     * validate constraints hosted on parameters of a method
     */
    private <T> void validateReturnedValueInContext(GroupValidationContext<T> context,
        MethodDescriptorImpl methodDescriptor) {

        final Groups groups = context.getGroups();

        for (ConstraintDescriptor<?> consDesc : methodDescriptor.getConstraintDescriptors()) {
            ConstraintValidation<?> validation = (ConstraintValidation<?>) consDesc;
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
                     * if one of the group process in the sequence leads to one
                     * or more validation failure, the groups following in the
                     * sequence must not be processed
                     */
                    if (!context.getListener().isEmpty())
                        break;
                }
            }
        }
        if (methodDescriptor.isCascaded() && context.getValidatedValue() != null) {
            context
                .setMetaBean(factoryContext.getMetaBeanFinder().findForClass(context.getValidatedValue().getClass()));
            // 1. process groups
            for (Group current : groups.getGroups()) {
                context.setCurrentGroup(current);
                ValidationHelper
                    .validateContext(context, new Jsr303ValidationCallback(context), isTreatMapsLikeBeans());
            }
            // 2. process sequences
            for (List<Group> eachSeq : groups.getSequences()) {
                for (Group current : eachSeq) {
                    context.setCurrentGroup(current);
                    ValidationHelper.validateContext(context, new Jsr303ValidationCallback(context),
                        isTreatMapsLikeBeans());
                    /**
                     * if one of the group process in the sequence leads to one
                     * or more validation failure, the groups following in the
                     * sequence must not be processed
                     */
                    if (!context.getListener().isEmpty())
                        break;
                }
            }
        }
    }
}
