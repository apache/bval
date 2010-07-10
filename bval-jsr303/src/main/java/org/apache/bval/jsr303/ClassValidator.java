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


import org.apache.bval.AbstractBeanValidator;
import org.apache.bval.MetaBeanFinder;
import org.apache.bval.jsr303.groups.Group;
import org.apache.bval.jsr303.groups.Groups;
import org.apache.bval.jsr303.groups.GroupsComputer;
import org.apache.bval.jsr303.util.ClassHelper;
import org.apache.bval.jsr303.util.NodeImpl;
import org.apache.bval.jsr303.util.PathImpl;
import org.apache.bval.jsr303.util.SecureActions;
import org.apache.bval.model.Features;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;
import org.apache.bval.util.AccessStrategy;
import org.apache.commons.lang.ClassUtils;

import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.groups.Default;
import javax.validation.metadata.BeanDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Objects of this class are able to validate bean instances (and the associated
 * object graphs).
 * <p>
 * Implementation is thread-safe.
 * <p>
 * API class
 * 
 * @author Roman Stumm
 * @author Carlos Vara
 * <br/>
 */
public class ClassValidator extends AbstractBeanValidator implements Validator {
  /**
   * {@link ApacheFactoryContext} used
   */
  protected final ApacheFactoryContext factoryContext;
  /**
   * {@link GroupsComputer} used
   */
  protected final GroupsComputer groupsComputer = new GroupsComputer();

  /**
   * Create a new ClassValidator instance.
   * @param factoryContext
   */
  public ClassValidator(ApacheFactoryContext factoryContext) {
    this.factoryContext = factoryContext;
  }

  /**
   * Create a new ClassValidator instance.
   * @param factory
   * @deprecated provided for backward compatibility
   */
  public ClassValidator(ApacheValidatorFactory factory) {
    this(factory.usingContext());
  }

  /**
   * Get the metabean finder associated with this validator.
   *
   * @return a MetaBeanFinder
   * @see org.apache.bval.MetaBeanManagerFactory#getFinder()
   */
  public MetaBeanFinder getMetaBeanFinder() {
    return factoryContext.getMetaBeanFinder();
  }

  // Validator implementation --------------------------------------------------
  
  /**
   * {@inheritDoc}
   * Validates all constraints on <code>object</code>.
   * 
   * @param object
   *            object to validate
   * @param groups
   *            group or list of groups targeted for validation
   *            (default to {@link javax.validation.groups.Default})
   * 
   * @return constraint violations or an empty Set if none
   * 
   * @throws IllegalArgumentException
   *             if object is null
   *             or if null is passed to the varargs groups
   * @throws ValidationException
   *             if a non recoverable error happens
   *             during the validation process
   */
  // @Override - not allowed in 1.5 for Interface methods
  @SuppressWarnings("unchecked")
  public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
    if (object == null) throw new IllegalArgumentException("cannot validate null");
    checkGroups(groups);

    try {
      
      Class<T> objectClass = (Class<T>) object.getClass();
      MetaBean objectMetaBean = factoryContext.getMetaBeanFinder().findForClass(objectClass);
      
      final GroupValidationContext<T> context =
          createContext(objectMetaBean, object, objectClass, groups);
      final ConstraintValidationListener<T> result = context.getListener();
      final Groups sequence = context.getGroups();
      
      // 1. process groups
      for (Group current : sequence.getGroups()) {
        context.setCurrentGroup(current);
        validateBeanNet(context);
      }
      
      // 2. process sequences
      for (List<Group> eachSeq : sequence.getSequences()) {
        for (Group current : eachSeq) {
          context.setCurrentGroup(current);
          validateBeanNet(context);
          // if one of the group process in the sequence leads to one or more validation failure,
          // the groups following in the sequence must not be processed
          if (!result.isEmpty()) break;
        }
        if (!result.isEmpty()) break;
      }
      return result.getConstraintViolations();
    } catch (RuntimeException ex) {
      throw unrecoverableValidationError(ex, object);
    }
  }

  /**
   * {@inheritDoc}
   * Validates all constraints placed on the property of <code>object</code>
   * named <code>propertyName</code>.
   * 
   * @param object
   *            object to validate
   * @param propertyName
   *            property to validate (ie field and getter constraints). Nested
   *            properties may be referenced (e.g. prop[2].subpropA.subpropB)
   * @param groups
   *            group or list of groups targeted for validation
   *            (default to {@link javax.validation.groups.Default})
   * 
   * @return constraint violations or an empty Set if none
   * 
   * @throws IllegalArgumentException
   *             if <code>object</code> is null,
   *             if <code>propertyName</code> null, empty or not a valid
   *             object property
   *             or if null is passed to the varargs groups
   * @throws ValidationException
   *             if a non recoverable error happens
   *             during the validation process
   */
  // @Override - not allowed in 1.5 for Interface methods
  @SuppressWarnings("unchecked")
  public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName,
                                                          Class<?>... groups) {
    if (object == null) throw new IllegalArgumentException("cannot validate null");

    checkPropertyName(propertyName);
    checkGroups(groups);

    try {
      
      Class<T> objectClass = (Class<T>) object.getClass();
      MetaBean objectMetaBean = factoryContext.getMetaBeanFinder().findForClass(objectClass);

      GroupValidationContext<T> context =
          createContext(objectMetaBean, object, objectClass, groups);
      ConstraintValidationListener<T> result = context.getListener();
      NestedMetaProperty nestedProp = getNestedProperty(objectMetaBean, object, propertyName);
      context.setMetaProperty(nestedProp.getMetaProperty());
      if (nestedProp.isNested()) {
        context.setFixedValue(nestedProp.getValue());
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
        if (!result.isEmpty()) break;
      }
      return result.getConstraintViolations();
    } catch (RuntimeException ex) {
      throw unrecoverableValidationError(ex, object);
    }
  }

  /**
   * {@inheritDoc}
   * Validates all constraints placed on the property named
   * <code>propertyName</code> of the class <code>beanType</code> would the
   * property value be <code>value</code>
   * <p/>
   * <code>ConstraintViolation</code> objects return null for
   * {@link ConstraintViolation#getRootBean()} and
   * {@link ConstraintViolation#getLeafBean()}
   * 
   * @param beanType
   *            the bean type
   * @param propertyName
   *            property to validate
   * @param value
   *            property value to validate
   * @param groups
   *            group or list of groups targeted for validation
   *            (default to {@link javax.validation.groups.Default})
   * 
   * @return constraint violations or an empty Set if none
   * 
   * @throws IllegalArgumentException
   *             if <code>beanType</code> is null,
   *             if <code>propertyName</code> null, empty or not a valid
   *             object property
   *             or if null is passed to the varargs groups
   * @throws ValidationException
   *             if a non recoverable error happens
   *             during the validation process
   */
  // @Override - not allowed in 1.5 for Interface methods
  public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType,
                                                       String propertyName, Object value,
                                                       Class<?>... groups) {

    checkBeanType(beanType);
    checkPropertyName(propertyName);
    checkGroups(groups);

    try {
      MetaBean metaBean = factoryContext.getMetaBeanFinder().findForClass(beanType);
      GroupValidationContext<T> context =
          createContext(metaBean, null, beanType, groups);
      ConstraintValidationListener<T> result = context.getListener();
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
          // if one of the group process in the sequence leads to one or more validation failure,
          // the groups following in the sequence must not be processed
          if (!result.isEmpty()) break;
        }
        if (!result.isEmpty()) break;
      }
      return result.getConstraintViolations();
    } catch (RuntimeException ex) {
      throw unrecoverableValidationError(ex, value);
    }
  }
  
  /**
   * {@inheritDoc}
   * Return the descriptor object describing bean constraints.
   * The returned object (and associated objects including
   * <code>ConstraintDescriptor<code>s) are immutable.
   * 
   * @param clazz
   *            class or interface type evaluated
   * 
   * @return the bean descriptor for the specified class.
   * 
   * @throws IllegalArgumentException
   *             if clazz is null
   * @throws ValidationException
   *             if a non recoverable error happens
   *             during the metadata discovery or if some
   *             constraints are invalid.
   */
  // @Override - not allowed in 1.5 for Interface methods
  public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
    if (clazz == null) {
      throw new IllegalArgumentException("Class cannot be null");
    }
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
  
  /**
   * {@inheritDoc}
   * Return an instance of the specified type allowing access to
   * provider-specific APIs. If the Bean Validation provider
   * implementation does not support the specified class,
   * <code>ValidationException</code> is thrown.
   * 
   * @param type
   *            the class of the object to be returned.
   * 
   * @return an instance of the specified class
   * 
   * @throws ValidationException
   *             if the provider does not support the call.
   */
  @SuppressWarnings("unchecked")
  // @Override - not allowed in 1.5 for Interface methods
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
  
  
  // Helpers -------------------------------------------------------------------
  
  /**
   * Validates a bean and all its cascaded related beans for the currently
   * defined group.
   * <p>
   * Special code is present to manage the {@link Default} group.
   * 
   * @param validationContext
   *            The current context of this validation call. Must have its
   *            {@link GroupValidationContext#getCurrentGroup()} field set.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected <VL extends ValidationListener> void validateBeanNet(ValidationContext<VL> validationContext) {

    GroupValidationContext<VL> context = (GroupValidationContext<VL>) validationContext;

    // If reached a cascaded bean which is null
    if (context.getBean() == null) {
      return;
    }

    // If reached a cascaded bean which has already been validated for the current group
    if (!context.collectValidated()) {
      return;
    }


    // ### First, validate the bean

    // Default is a special case
    if (context.getCurrentGroup().isDefault()) {

      List<Group> defaultGroups = expandDefaultGroup(context);
      final ConstraintValidationListener<VL> result = (ConstraintValidationListener<VL>) context.getListener();

      // If the rootBean defines a GroupSequence
      if (defaultGroups.size() > 1) {

        int numViolations = result.violationsSize();

        // Validate the bean for each group in the sequence
        Group currentGroup = context.getCurrentGroup();
        for (Group each : defaultGroups) {
          context.setCurrentGroup(each);
          validateBean(context);
          // Spec 3.4.3 - Stop validation if errors already found
          if (result.violationsSize() > numViolations) {
            break;
          }
        }
        context.setCurrentGroup(currentGroup);
      } else {

        // For each class in the hierarchy of classes of rootBean,
        // validate the constraints defined in that class according
        // to the GroupSequence defined in the same class

        // Obtain the full class hierarchy
        List<Class<?>> classHierarchy = new ArrayList<Class<?>>();
        ClassHelper.fillFullClassHierarchyAsList(classHierarchy, context.getMetaBean().getBeanClass());
        Class<?> initialOwner = context.getCurrentOwner();

        // For each owner in the hierarchy
        for (Class<?> owner : classHierarchy) {
          context.setCurrentOwner(owner);

          int numViolations = result.violationsSize();

          // Obtain the group sequence of the owner, and use it for the constraints that belong to it
          List<Group> ownerDefaultGroups =
              context.getMetaBean().getFeature("{GroupSequence:" + owner.getCanonicalName() + "}");
          for (Group each : ownerDefaultGroups) {
            context.setCurrentGroup(each);
            validateBean(context);
            // Spec 3.4.3 - Stop validation if errors already found
            if (result.violationsSize() > numViolations) {
              break;
            }
          }

        }
        context.setCurrentOwner(initialOwner);
        context.setCurrentGroup(Group.DEFAULT);

      }

    }
    // if not the default group, proceed as normal
    else {
      validateBean(context);
    }


    // ### Then, the cascaded beans (@Valid)
    for (MetaProperty prop : context.getMetaBean().getProperties()) {
      validateCascadedBean(context, prop);
    }

  }

  /**
   * Checks if the the meta property <code>prop</code> defines a cascaded
   * bean, and in case it does, validates it.
   * 
   * @param context
   *            The current validation context.
   * @param prop
   *            The property to cascade from (in case it is possible).
   */
  private void validateCascadedBean(GroupValidationContext<?> context, MetaProperty prop) {
    AccessStrategy[] access = prop.getFeature(Features.Property.REF_CASCADE);
    if (access != null) { // different accesses to relation
      // save old values from context
      final Object bean = context.getBean();
      final MetaBean mbean = context.getMetaBean();
      for (AccessStrategy each : access) {
        if (isCascadable(context, prop, each)) {
          // modify context state for relationship-target bean
          context.moveDown(prop, each);
          // Now, if the related bean is an instance of Map/Array/etc,
          validateContext(context);
          // restore old values in context
          context.moveUp(bean, mbean);
        }
      }
    }
  }

  /**
   * Before accessing a related bean (marked with {@link javax.validation.Valid}), the
   * validator has to check if it is reachable and cascadable.
   *
   * @param context The current validation context.
   * @param prop    The property of the related bean.
   * @param access  The access strategy used to get the related bean value.
   * @return <code>true</code> if the validator can access the related bean,
   *         <code>false</code> otherwise.
   */
  private boolean isCascadable(GroupValidationContext<?> context, MetaProperty prop, AccessStrategy access) {

    PathImpl beanPath = context.getPropertyPath();
    NodeImpl node = new NodeImpl(prop.getName());
    if (beanPath == null) {
      beanPath = PathImpl.create(null);
    }
    try {
      if (!context.getTraversableResolver().isReachable(
          context.getBean(), node,
          context.getRootMetaBean().getBeanClass(), beanPath,
          access.getElementType()))
        return false;
    } catch (RuntimeException e) {
      throw new ValidationException("Error in TraversableResolver.isReachable() for " + context.getBean(), e);
    }

    try {
      if (!context.getTraversableResolver().isCascadable(
          context.getBean(), node,
          context.getRootMetaBean().getBeanClass(), beanPath,
          access.getElementType()))
        return false;
    } catch (RuntimeException e) {
      throw new ValidationException("Error TraversableResolver.isCascadable() for " + context.getBean(), e);
    }

    return true;
  }

  /**
   * in case of a default group return the list of groups
   * for a redefined default GroupSequence
   *
   * @return null when no in default group or default group sequence not redefined
   */
  private List<Group> expandDefaultGroup(GroupValidationContext<?> context) {
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

  /**
   * Generate an unrecoverable validation error
   * @param ex
   * @param object
   * @return a {@link RuntimeException} of the appropriate type
   */
  protected static RuntimeException unrecoverableValidationError(RuntimeException ex,
                                                          Object object) {
    if (ex instanceof UnknownPropertyException) {
      // Convert to IllegalArgumentException
      return new IllegalArgumentException(ex.getMessage(), ex);
    } else if (ex instanceof ValidationException) {
      return ex; // do not wrap specific ValidationExceptions (or instances from subclasses)
    } else {
      return new ValidationException("error during validation of " + object, ex);
    }
  }

  private void validatePropertyInGroup(GroupValidationContext<?> context) {
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
   * Create a {@link GroupValidationContext}.
   * @param <T>
   * @param metaBean
   * @param object
   * @param objectClass
   * @param groups
   * @return {@link GroupValidationContext} instance
   */
  protected <T> GroupValidationContext<T> createContext(
      MetaBean metaBean, T object, Class<T> objectClass, Class<?>[] groups) {
    ConstraintValidationListener<T> listener = new ConstraintValidationListener<T>(object, objectClass);
    GroupValidationContextImpl<T> context =
        new GroupValidationContextImpl<T>(listener,
            this.factoryContext.getMessageInterpolator(),
            this.factoryContext.getTraversableResolver(), metaBean);
    context.setBean(object, metaBean);
    context.setGroups(groupsComputer.computeGroups(groups));
    return context;
  }

  /**
   * Create a {@link BeanDescriptorImpl}
   * @param metaBean
   * @return {@link BeanDescriptorImpl} instance
   */
  protected BeanDescriptorImpl createBeanDescriptor(MetaBean metaBean) {
    return new BeanDescriptorImpl(factoryContext, metaBean, metaBean.getValidations());
  }

  /**
   * Checks that beanType is valid according to spec Section 4.1.1 i. Throws
   * an {@link IllegalArgumentException} if it is not.
   *
   * @param beanType Bean type to check.
   */
  private void checkBeanType(Class<?> beanType) {
    if (beanType == null) {
      throw new IllegalArgumentException("Bean type cannot be null.");
    }
  }

  /**
   * Checks that the property name is valid according to spec Section 4.1.1 i.
   * Throws an {@link IllegalArgumentException} if it is not.
   *
   * @param propertyName Property name to check.
   */
  private void checkPropertyName(String propertyName) {
    if (propertyName == null || propertyName.trim().length() == 0) {
      throw new IllegalArgumentException("Property path cannot be null or empty.");
    }
  }

  /**
   * Checks that the groups array is valid according to spec Section 4.1.1 i.
   * Throws an {@link IllegalArgumentException} if it is not.
   *
   * @param groups The groups to check.
   */
  private void checkGroups(Class<?>[] groups) {
    if (groups == null) {
      throw new IllegalArgumentException("Groups cannot be null.");
    }
  }
}
