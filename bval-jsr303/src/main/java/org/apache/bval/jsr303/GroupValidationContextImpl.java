/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.bval.jsr303;


import org.apache.bval.BeanValidationContext;
import org.apache.bval.jsr303.groups.Group;
import org.apache.bval.jsr303.groups.Groups;
import org.apache.bval.jsr303.resolver.CachingTraversableResolver;
import org.apache.bval.jsr303.util.NodeImpl;
import org.apache.bval.jsr303.util.PathImpl;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.ValidationListener;
import org.apache.bval.util.AccessStrategy;

import javax.validation.ConstraintValidator;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.metadata.ConstraintDescriptor;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Description: instance per validation process, not thread-safe<br/>
 */
final class GroupValidationContextImpl<T extends ValidationListener>
    extends BeanValidationContext<T>
    implements GroupValidationContext<T>, MessageInterpolator.Context {

  private final MessageInterpolator messageResolver;
  private final PathImpl path;
  private final MetaBean rootMetaBean;
  /**
   * the groups in the sequence of validation to take place
   */
  private Groups groups;
  /**
   * the current group during the validation process
   */
  private Group currentGroup;

  /**
   * contains the validation constraints that have already been processed during
   * this validation routine (as part of a previous group match)
   */
  private IdentityHashMap<Object, IdentityHashMap<ConstraintValidator, Object>> validatedConstraints =
      new IdentityHashMap();
  private ConstraintValidation constraintValidation;
  private final TraversableResolver traversableResolver;

  public GroupValidationContextImpl(T listener, MessageInterpolator aMessageResolver,
                                    TraversableResolver traversableResolver,
                                    MetaBean rootMetaBean) {
    super(listener);
    this.messageResolver = aMessageResolver;
    this.traversableResolver = CachingTraversableResolver.cacheFor(traversableResolver);
    this.rootMetaBean = rootMetaBean;
    this.path = PathImpl.create(null);
  }

  @Override
  public void setCurrentIndex(int index) {
    path.getLeafNode().setIndex(index);
  }

  @Override
  public void setCurrentKey(Object key) {
    path.getLeafNode().setKey(key);
  }

  @Override
  public void moveDown(MetaProperty prop, AccessStrategy access) {
    path.addNode(new NodeImpl(prop.getName()));
    super.moveDown(prop, access);
  }

  @Override
  public void moveUp(Object bean, MetaBean metaBean) {
    path.removeLeafNode();
    super.moveUp(bean, metaBean); // call super!
  }

  /**
   * add the object in the current group
   * to the collection of validated objects to keep
   * track of them to avoid endless loops during validation.
   *
   * @return true when the object was not already validated in this context
   */
  @Override
  public boolean collectValidated() {
    Set<Group> groupSet = (Set<Group>) validatedObjects.get(getBean());
    if (groupSet == null) {
      groupSet = new HashSet(10);
      validatedObjects.put(getBean(), groupSet);
    }
    return groupSet.add(getCurrentGroup());
  }

  /**
   * @return true when the constraint for this object was not already validated in this context
   */
  public boolean collectValidated(Object bean, ConstraintValidator constraint) {
    IdentityHashMap<ConstraintValidator, Object> beanConstraints =
        validatedConstraints.get(bean);
    if (beanConstraints == null) {
      beanConstraints = new IdentityHashMap();
      validatedConstraints.put(bean, beanConstraints);
    }
    return beanConstraints.put(constraint, Boolean.TRUE) == null;
  }

  public boolean isValidated(Object bean, ConstraintValidator constraint) {
    IdentityHashMap<ConstraintValidator, Object> beanConstraints =
        validatedConstraints.get(bean);
    return beanConstraints != null && beanConstraints.containsKey(constraint);
  }

  public void resetValidatedConstraints() {
    validatedConstraints.clear();
  }

  /**
   * if an associated object is validated,
   * add the association field or JavaBeans property name and a dot ('.') as a prefix
   * to the previous rules.
   * uses prop[index] in property path for elements in to-many-relationships.
   *
   * @return the path in dot notation
   */
  public PathImpl getPropertyPath() {
    PathImpl currentPath = PathImpl.copy(path);
    if (getMetaProperty() != null) {
      currentPath.addNode(new NodeImpl(getMetaProperty().getName()));
    }
    return currentPath;
  }

  public MetaBean getRootMetaBean() {
    return rootMetaBean;
  }

  public void setGroups(Groups groups) {
    this.groups = groups;
  }

  public Groups getGroups() {
    return groups;
  }

  public Group getCurrentGroup() {
    return currentGroup;
  }

  public void setCurrentGroup(Group currentGroup) {
    this.currentGroup = currentGroup;
  }

  public void setConstraintValidation(ConstraintValidation constraint) {
    constraintValidation = constraint;
  }

  public ConstraintValidation getConstraintValidation() {
    return constraintValidation;
  }

  public ConstraintDescriptor getConstraintDescriptor() {
    return constraintValidation;
  }

  /**
   * @return value being validated
   */
  public Object getValidatedValue() {
    if (getMetaProperty() != null) {
      return getPropertyValue(constraintValidation.getAccess());
    } else {
      return getBean();
    }
  }

  public MessageInterpolator getMessageResolver() {
    return messageResolver;
  }

  public TraversableResolver getTraversableResolver() {
    return traversableResolver;
  }
}
