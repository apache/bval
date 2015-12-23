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
package org.apache.bval.jsr;

import org.apache.bval.BeanValidationContext;
import org.apache.bval.jsr.groups.Group;
import org.apache.bval.jsr.groups.Groups;
import org.apache.bval.jsr.resolver.CachingTraversableResolver;
import org.apache.bval.jsr.util.NodeImpl;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.util.AccessStrategy;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ElementKind;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.Path;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Description: instance per validation process, not thread-safe<br/>
 */
final class GroupValidationContextImpl<T> extends BeanValidationContext<ConstraintValidationListener<T>> implements
    GroupValidationContext<T>, MessageInterpolator.Context {

    private final MessageInterpolator messageResolver;
    private final PathImpl path;
    private final MetaBean rootMetaBean;
    private final ParameterNameProvider parameterNameProvider;

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
     * contains the validation constraints that have already been processed
     * during this validation routine (as part of a previous group match)
     */
    private HashSet<ConstraintValidatorIdentity> validatedConstraints = new HashSet<ConstraintValidatorIdentity>();

    private ConstraintValidation<?> constraintValidation;
    private final TraversableResolver traversableResolver;
    private final ConstraintValidatorFactory constraintValidatorFactory;

    private Object[] parameters;
    private Object returnValue;
    private Method method;
    private Constructor<?> constructor;

    /**
     * Create a new GroupValidationContextImpl instance.
     *
     * @param listener
     * @param aMessageResolver
     * @param traversableResolver
     * @param parameterNameProvider
     * @param rootMetaBean
     */
    public GroupValidationContextImpl(ConstraintValidationListener<T> listener, MessageInterpolator aMessageResolver,
                                      TraversableResolver traversableResolver, ParameterNameProvider parameterNameProvider,
                                      ConstraintValidatorFactory constraintValidatorFactory,
                                      MetaBean rootMetaBean) {
        // inherited variable 'validatedObjects' is of type:
        // HashMap<GraphBeanIdentity, Set<PathImpl>> in this class
        super(listener, new HashMap<GraphBeanIdentity, Set<PathImpl>>());
        this.messageResolver = aMessageResolver;
        this.constraintValidatorFactory = constraintValidatorFactory;
        this.traversableResolver = CachingTraversableResolver.cacheFor(traversableResolver);
        this.parameterNameProvider = parameterNameProvider;
        this.rootMetaBean = rootMetaBean;
        this.path = PathImpl.create();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentIndex(Integer index) {
        NodeImpl leaf = path.getLeafNode();
        if (leaf.getName() == null) {
            leaf.setIndex(index);
        } else {
            path.addNode(NodeImpl.atIndex(index));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentKey(Object key) {
        NodeImpl leaf = path.getLeafNode();
        if (leaf.getName() == null) {
            leaf.setKey(key);
        } else {
            path.addNode(NodeImpl.atKey(key));
        }
    }

    public void setKind(final ElementKind type) {
        path.getLeafNode().setKind(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveDown(MetaProperty prop, AccessStrategy access) {
        moveDown(prop.getName());
        super.moveDown(prop, access);
    }

    @Override
    public void moveDown(final String prop) {
        path.addProperty(prop);
    }

    public void moveDown(final Path.Node node) {
        path.addNode(node);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveUp(Object bean, MetaBean metaBean) {
        NodeImpl leaf = path.getLeafNode();
        if (leaf.isInIterable() && leaf.getName() != null) {
            leaf.setName(null);
        } else {
            path.removeLeafNode();
        }
        super.moveUp(bean, metaBean); // call super!
    }

    /**
     * {@inheritDoc} Here, state equates to bean identity + group.
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
     * {@inheritDoc} If an associated object is validated, add the association
     * field or JavaBeans property name and a dot ('.') as a prefix to the
     * previous rules. uses prop[index] in property path for elements in
     * to-many-relationships.
     * 
     * @return the path in dot notation
     */
    public PathImpl getPropertyPath() {
        PathImpl currentPath = PathImpl.copy(path);
        if (getMetaProperty() != null) {
            currentPath.addProperty(getMetaProperty().getName());
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
     * 
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

    public void setCurrentGroups(final Groups g) {
        groups = g;
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

    public <T> T unwrap(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new ValidationException("Type " + type + " not supported");
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

    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return constraintValidatorFactory;
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

    public ElementKind getElementKind() {
        return path.getLeafNode().getKind();
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(final Object[] parameters) {
        this.parameters = parameters;
    }

    public void setReturnValue(final Object returnValue) {
        this.returnValue = returnValue;
    }

    public ParameterNameProvider getParameterNameProvider() {
        return parameterNameProvider;
    }

    public void setMethod(final Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public void setConstructor(final Constructor<?> constructor) {
        this.constructor = constructor;
    }
}
