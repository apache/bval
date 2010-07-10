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
import org.apache.bval.util.AccessStrategy;

import javax.validation.ConstraintValidator;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.metadata.ConstraintDescriptor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Description: instance per validation process, not thread-safe<br/>
 */
final class GroupValidationContextImpl<T>
    extends BeanValidationContext<ConstraintValidationListener<T>>
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

  private Class<?> currentOwner;

  /**
   * contains the validation constraints that have already been processed during
   * this validation routine (as part of a previous group match)
   */
  private HashSet<ConstraintValidatorIdentity> validatedConstraints =
      new HashSet<ConstraintValidatorIdentity>();

  private ConstraintValidation<?> constraintValidation;
  private final TraversableResolver traversableResolver;

  /**
   * Create a new GroupValidationContextImpl instance.
   * @param listener
   * @param aMessageResolver
   * @param traversableResolver
   * @param rootMetaBean
   */
  public GroupValidationContextImpl(ConstraintValidationListener<T> listener, MessageInterpolator aMessageResolver,
                                    TraversableResolver traversableResolver,
                                    MetaBean rootMetaBean) {
    // inherited variable 'validatedObjects' is of type: HashMap<GraphBeanIdentity, Set<PathImpl>> in this class 
    super(listener, new HashMap<GraphBeanIdentity, Set<PathImpl>>());
    this.messageResolver = aMessageResolver;
    this.traversableResolver = CachingTraversableResolver.cacheFor(traversableResolver);
    this.rootMetaBean = rootMetaBean;
    this.path = PathImpl.create(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCurrentIndex(Integer index) {
    path.getLeafNode().setIndex(index);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCurrentKey(Object key) {
    path.getLeafNode().setKey(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void moveDown(MetaProperty prop, AccessStrategy access) {
    path.addNode(new NodeImpl(prop.getName()));
    super.moveDown(prop, access);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void moveUp(Object bean, MetaBean metaBean) {
    path.removeLeafNode();
    super.moveUp(bean, metaBean); // call super!
  }

  /**
   * {@inheritDoc}
   * Here, state equates to bean identity + group.
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean collectValidated() {

    // Combination of bean+group+owner (owner is currently ignored)
    GraphBeanIdentity gbi = new GraphBeanIdentity(getBean(), getCurrentGroup().getGroup(), getCurrentOwner());

    Set<PathImpl> validatedPathsForGBI = (Set<PathImpl>) validatedObjects.get(gbi);
    if (validatedPathsForGBI == null) {
      validatedPathsForGBI = new HashSet<PathImpl>();
      validatedObjects.put(gbi, validatedPathsForGBI);
    }

    // If any of the paths is a subpath of the current path, there is a
    // circular dependency, so return false
    for (PathImpl validatedPath : validatedPathsForGBI) {
      if (path.isSubPathOf(validatedPath)) {
        return false;
      }
    }

    // Else, add the currentPath to the set of validatedPaths
    validatedPathsForGBI.add(PathImpl.copy(path));
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public boolean collectValidated(ConstraintValidator<?, ?> constraint) {
    ConstraintValidatorIdentity cvi = new ConstraintValidatorIdentity(getBean(), getPropertyPath(), constraint);
    return this.validatedConstraints.add(cvi);
  }

  /**
   * Reset the validated constraints.
   */
  public void resetValidatedConstraints() {
    validatedConstraints.clear();
  }

  /**
   * {@inheritDoc}
   * If an associated object is validated,
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

  /**
   * {@inheritDoc}
   */
  public MetaBean getRootMetaBean() {
    return rootMetaBean;
  }

  /**
   * Set the Groups.
   * @param groups
   */
  public void setGroups(Groups groups) {
    this.groups = groups;
  }

  /**
   * {@inheritDoc}
   */
  public Groups getGroups() {
    return groups;
  }

  /**
   * {@inheritDoc}
   */
  public Group getCurrentGroup() {
    return currentGroup;
  }

  /**
   * {@inheritDoc}
   */
  public void setCurrentGroup(Group currentGroup) {
    this.currentGroup = currentGroup;
  }

  /**
   * {@inheritDoc}
   */
  public void setConstraintValidation(ConstraintValidation<?> constraint) {
    constraintValidation = constraint;
  }

  /**
   * {@inheritDoc}
   */
  public ConstraintValidation<?> getConstraintValidation() {
    return constraintValidation;
  }

  /**
   * {@inheritDoc}
   */
  public ConstraintDescriptor<?> getConstraintDescriptor() {
    return constraintValidation;
  }

  /**
   * {@inheritDoc}
   */
  public Object getValidatedValue() {
    if (getMetaProperty() != null) {
      return getPropertyValue(constraintValidation.getAccess());
    } else {
      return getBean();
    }
  }

  /**
   * {@inheritDoc}
   */
  public MessageInterpolator getMessageResolver() {
    return messageResolver;
  }

  /**
   * {@inheritDoc}
   */
  public TraversableResolver getTraversableResolver() {
    return traversableResolver;
  }

  /**
   * {@inheritDoc}
   */
  public Class<?> getCurrentOwner() {
    return this.currentOwner;
  }

  /**
   * {@inheritDoc}
   */
  public void setCurrentOwner(Class<?> currentOwner) {
    this.currentOwner = currentOwner;
  }
}
