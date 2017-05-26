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
final class GroupValidationContextImpl<T> extends BeanValidationContext<ConstraintValidationListener<T>>
    implements GroupValidationContext<T>, MessageInterpolator.Context {

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
        ConstraintValidatorFactory constraintValidatorFactory, MetaBean rootMetaBean) {
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

    @Override
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

    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    public Groups getGroups() {
        return groups;
    }

    @Override
    public void setCurrentGroups(final Groups g) {
        groups = g;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Group getCurrentGroup() {
        return currentGroup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentGroup(Group currentGroup) {
        this.currentGroup = currentGroup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConstraintValidation(ConstraintValidation<?> constraint) {
        constraintValidation = constraint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintValidation<?> getConstraintValidation() {
        return constraintValidation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return constraintValidation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValidatedValue() {
        if (getMetaProperty() != null) {
            return getPropertyValue(constraintValidation.getAccess());
        } else {
            return getBean();
        }
    }

    @Override
    public <U> U unwrap(Class<U> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new ValidationException("Type " + type + " not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageInterpolator getMessageResolver() {
        return messageResolver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public Class<?> getCurrentOwner() {
        return this.currentOwner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentOwner(Class<?> currentOwner) {
        this.currentOwner = currentOwner;
    }

    @Override
    public ElementKind getElementKind() {
        return path.getLeafNode().getKind();
    }

    @Override
    public Object getReturnValue() {
        return returnValue;
    }

    @Override
    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(final Object[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public void setReturnValue(final Object returnValue) {
        this.returnValue = returnValue;
    }

    @Override
    public ParameterNameProvider getParameterNameProvider() {
        return parameterNameProvider;
    }

    @Override
    public void setMethod(final Method method) {
        this.method = method;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Constructor<?> getConstructor() {
        return constructor;
    }

    @Override
    public void setConstructor(final Constructor<?> constructor) {
        this.constructor = constructor;
    }
}
