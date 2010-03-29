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


import org.apache.bval.BeanValidator;
import org.apache.bval.jsr303.groups.Group;
import org.apache.bval.jsr303.groups.Groups;
import org.apache.bval.jsr303.groups.GroupsComputer;
import org.apache.bval.jsr303.util.SecureActions;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.ValidationContext;
import org.apache.commons.lang.ClassUtils;

import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;
import java.util.List;
import java.util.Set;

/**
 * API class -
 * Description:
 * instance is able to validate bean instances (and the associated object graphs).
 * concurrent, multithreaded access implementation is safe.
 * It is recommended to cache the instance.
 * <br/>
 * User: roman.stumm <br/>
 * Date: 01.04.2008 <br/>
 * Time: 13:36:33 <br/>
 * Copyright: Agimatec GmbH 2008
 */
public class ClassValidator extends BeanValidator implements Validator {
    protected final ApacheFactoryContext factoryContext;
    protected final GroupsComputer groupsComputer = new GroupsComputer();

    public ClassValidator(ApacheFactoryContext factoryContext) {
        super(factoryContext.getMetaBeanFinder());
        this.factoryContext = factoryContext;
    }

    /** @deprecated provided for backward compatibility */
    public ClassValidator(ApacheValidatorFactory factory) {
        this(factory.usingContext());
    }

    /**
     * validate all constraints on object
     *
     * @throws javax.validation.ValidationException
     *          if a non recoverable error happens during the validation process
     */
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groupArray) {
        if (object == null) throw new IllegalArgumentException("cannot validate null");
        try {
            final GroupValidationContext<ConstraintValidationListener<T>> context =
                  createContext(factoryContext.getMetaBeanFinder()
                        .findForClass(object.getClass()), object, groupArray);
            final ConstraintValidationListener result = context.getListener();
            final Groups groups = context.getGroups();
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
                    if (!result.isEmpty()) break;
                }
//                if (!result.isEmpty()) break; // ?? TODO RSt - clarify!
            }
            return result.getConstaintViolations();
        } catch (RuntimeException ex) {
            throw unrecoverableValidationError(ex, object);
        }
    }

    @Override
    public void validateBeanNet(ValidationContext vcontext) {
        GroupValidationContext context = (GroupValidationContext) vcontext;
        List<Group> defaultGroups = expandDefaultGroup(context);
        if (defaultGroups != null) {
            Group currentGroup = context.getCurrentGroup();
            for (Group each : defaultGroups) {
                context.setCurrentGroup(each);
                super.validateBeanNet(context);
                // continue validation, even if errors already found: if (!result.isEmpty())
            }
            context.setCurrentGroup(currentGroup); // restore  (finally{} not required)
        } else {
            super.validateBeanNet(context);
        }
    }

    /**
     * in case of a default group return the list of groups
     * for a redefined default GroupSequence
     *
     * @return null when no in default group or default group sequence not redefined
     */
    private List<Group> expandDefaultGroup(GroupValidationContext context) {
        if (context.getCurrentGroup().isDefault()) {
            // mention if metaBean redefines the default group
            List<Group> groupSeq =
                  context.getMetaBean().getFeature(Jsr303Features.Bean.GROUP_SEQUENCE);
            if (groupSeq != null) {
                context.getGroups().assertDefaultGroupSequenceIsExpandable(groupSeq);
            }
            return groupSeq;
        } else {
            return null;
        }
    }

    protected ValidationException unrecoverableValidationError(RuntimeException ex,
                                                               Object object) {
        if (ex instanceof ValidationException) {
            throw ex; // do not wrap specific ValidationExceptions (or instances from subclasses)
        } else {
            throw new ValidationException("error during validation of " + object, ex);
        }
    }

    /**
     * validate all constraints on <code>propertyName</code> property of object
     *
     * @param propertyName - the attribute name, or nested property name (e.g. prop[2].subpropA.subpropB)
     * @throws javax.validation.ValidationException
     *          if a non recoverable error happens
     *          during the validation process
     */
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName,
                                                            Class<?>... groups) {
        if (object == null) throw new IllegalArgumentException("cannot validate null");
        try {
            MetaBean metaBean =
                  factoryContext.getMetaBeanFinder().findForClass(object.getClass());
            GroupValidationContext<ConstraintValidationListener<T>> context =
                  createContext(metaBean, object, groups);
            ConstraintValidationListener result = context.getListener();
            NestedMetaProperty nestedProp = getNestedProperty(metaBean, object, propertyName);
            context.setMetaProperty(nestedProp.getMetaProperty());
            if (nestedProp.isNested()) {
                context.setFixedValue(nestedProp.getValue());
            } else {
                context.setMetaProperty(nestedProp.getMetaProperty());
            }
            if (context.getMetaProperty() == null) throw new IllegalArgumentException(
                  "Unknown property " + object.getClass().getName() + "." + propertyName);
            Groups sequence = context.getGroups();
            // 1. process groups
            for (Group current : sequence.getGroups()) {
                context.setCurrentGroup(current);
                validatePropertyInGroup(context);
            }
            // 2. process sequences
            for (List<Group> eachSeq : sequence.getSequences()) {
                for (Group current : eachSeq) {
                    context.setCurrentGroup(current);
                    validatePropertyInGroup(context);
                    /**
                     * if one of the group process in the sequence leads to one or more validation failure,
                     * the groups following in the sequence must not be processed
                     */
                    if (!result.isEmpty()) break;
                }
//                if (!result.isEmpty()) break; // ?? TODO RSt - clarify!
            }
            return result.getConstaintViolations();
        } catch (RuntimeException ex) {
            throw unrecoverableValidationError(ex, object);
        }
    }

    private void validatePropertyInGroup(GroupValidationContext context) {
        Group currentGroup = context.getCurrentGroup();
        List<Group> defaultGroups = expandDefaultGroup(context);
        if (defaultGroups != null) {
            for (Group each : defaultGroups) {
                context.setCurrentGroup(each);
                validateProperty(context);
                // continue validation, even if errors already found: if (!result.isEmpty())
            }
            context.setCurrentGroup(currentGroup); // restore
        } else {
            validateProperty(context);
        }
    }

    /**
     * find the MetaProperty for the given propertyName,
     * which could contain a path, following the path on a given object to resolve
     * types at runtime from the instance
     */
    private NestedMetaProperty getNestedProperty(MetaBean metaBean, Object t,
                                                 String propertyName) {
        NestedMetaProperty nested = new NestedMetaProperty(propertyName, t);
        nested.setMetaBean(metaBean);
        nested.parse();
        return nested;
    }

    /**
     * validate all constraints on <code>propertyName</code> property
     * if the property value is <code>value</code>
     *
     * @throws javax.validation.ValidationException
     *          if a non recoverable error happens
     *          during the validation process
     */
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType,
                                                         String propertyName, Object value,
                                                         Class<?>... groups) {
        try {
            MetaBean metaBean = factoryContext.getMetaBeanFinder().findForClass(beanType);
            GroupValidationContext<ConstraintValidationListener<T>> context =
                  createContext(metaBean, null, groups);
            ConstraintValidationListener result = context.getListener();
            context.setMetaProperty(
                  getNestedProperty(metaBean, null, propertyName).getMetaProperty());
            context.setFixedValue(value);
            Groups sequence = context.getGroups();
            // 1. process groups
            for (Group current : sequence.getGroups()) {
                context.setCurrentGroup(current);
                validatePropertyInGroup(context);
            }
            // 2. process sequences
            for (List<Group> eachSeq : sequence.getSequences()) {
                for (Group current : eachSeq) {
                    context.setCurrentGroup(current);
                    validatePropertyInGroup(context);
                    /**
                     * if one of the group process in the sequence leads to one or more validation failure,
                     * the groups following in the sequence must not be processed
                     */
                    if (!result.isEmpty()) break;
                }
//                if (!result.isEmpty()) break; // ?? TODO RSt - clarify!
            }
            return result.getConstaintViolations();
        } catch (RuntimeException ex) {
            throw unrecoverableValidationError(ex, value);
        }
    }

    protected <T> GroupValidationContext<ConstraintValidationListener<T>> createContext(
          MetaBean metaBean, T object, Class<?>[] groups) {
        ConstraintValidationListener<T> listener = new ConstraintValidationListener<T>(object);
        GroupValidationContextImpl<ConstraintValidationListener<T>> context =
              new GroupValidationContextImpl(listener,
                    this.factoryContext.getMessageInterpolator(),
                    this.factoryContext.getTraversableResolver(), metaBean);
        context.setBean(object, metaBean);
        context.setGroups(groupsComputer.computeGroups(groups));
        return context;
    }

    /**
     * Return the descriptor object describing bean constraints
     * The returned object (and associated objects including ConstraintDescriptors)
     * are immutable.
     *
     * @throws ValidationException if a non recoverable error happens
     *                             during the metadata discovery or if some
     *                             constraints are invalid.
     */
    public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
        try {
            MetaBean metaBean = factoryContext.getMetaBeanFinder().findForClass(clazz);
            BeanDescriptorImpl edesc =
                  metaBean.getFeature(Jsr303Features.Bean.BEAN_DESCRIPTOR);
            if (edesc == null) {
                edesc = createBeanDescriptor(metaBean);
                metaBean.putFeature(Jsr303Features.Bean.BEAN_DESCRIPTOR, edesc);
            }
            return edesc;
        } catch (RuntimeException ex) {
            throw new ValidationException("error retrieving constraints for " + clazz, ex);
        }
    }

    protected BeanDescriptorImpl createBeanDescriptor(MetaBean metaBean) {
        return new BeanDescriptorImpl(factoryContext, metaBean, metaBean.getValidations());
    }

    /**
     * Return an object of the specified type to allow access to the
     * provider-specific API.  If the Bean Validation provider
     * implementation does not support the specified class, the
     * ValidationException is thrown.
     *
     * @param type the class of the object to be returned.
     * @return an instance of the specified class
     * @throws ValidationException if the provider does not
     *                             support the call.
     */
    public <T> T unwrap(Class<T> type) {
        if (type.isAssignableFrom(getClass())) {
            return (T) this;
        } else if (!type.isInterface()) {
            return SecureActions.newInstance(type, new Class[]{ApacheFactoryContext.class},
                  new Object[]{factoryContext});
        } else {
            try {
                Class<T> cls = ClassUtils.getClass(type.getName() + "Impl");
                return SecureActions.newInstance(cls,
                      new Class[]{ApacheFactoryContext.class}, new Object[]{factoryContext});
            } catch (ClassNotFoundException e) {
                throw new ValidationException("Type " + type + " not supported");
            }
        }
    }

}
