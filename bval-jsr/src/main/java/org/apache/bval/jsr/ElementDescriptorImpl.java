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

import org.apache.bval.jsr.groups.Group;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.Validation;

import javax.validation.ConstraintDeclarationException;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.GroupConversionDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Description: MetaData class<br/>
 */
public abstract class ElementDescriptorImpl implements ElementDescriptor {
    private final Set<GroupConversionDescriptor> groupConversions =
        new CopyOnWriteArraySet<GroupConversionDescriptor>();
    private boolean cascaded;
    private final Collection<Object> validated = new CopyOnWriteArraySet<Object>();

    /**
     * Get a set of {@link ConstraintDescriptor}s from the specified array of
     * {@link Validation}s.
     * 
     * @param validations
     * @return {@link ConstraintDescriptor} set
     */
    protected static Set<ConstraintDescriptor<?>> getConstraintDescriptors(final Validation[] validations) {
        final Set<ConstraintDescriptor<?>> result = new HashSet<ConstraintDescriptor<?>>(validations.length);
        for (Validation validation : validations) {
            if (validation instanceof ConstraintValidation<?>) {
                result.add((ConstraintValidation<?>) validation);
            }
        }
        return result;
    }

    /** the MetaBean of this element */
    protected final MetaBean metaBean;

    /** the raw type of this element */
    protected final Class<?> elementClass;

    private Set<ConstraintDescriptor<?>> constraintDescriptors;

    private final Map<Group, Group> groupMapping = new HashMap<Group, Group>();

    /**
     * Create a new ElementDescriptorImpl instance.
     * 
     * @param metaBean
     * @param elementClass
     * @param validations
     */
    protected ElementDescriptorImpl(MetaBean metaBean, Class<?> elementClass, Validation[] validations) {
        this.metaBean = metaBean;
        this.elementClass = elementClass;
        setConstraintDescriptors(getConstraintDescriptors(validations));
    }

    /**
     * Create a new ElementDescriptorImpl instance.
     *
     * @param elementClass
     * @param validations
     */
    protected ElementDescriptorImpl(Class<?> elementClass, Validation[] validations) {
        this(null, elementClass, validations);
    }

    /**
     * {@inheritDoc}
     * 
     * @return Statically defined returned type.
     */
    @Override
    public Class<?> getElementClass() {
        return elementClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ElementDescriptor.ConstraintFinder findConstraints() {
        return new ConstraintFinderImpl(metaBean, new HashSet(constraintDescriptors));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        return constraintDescriptors.isEmpty() ? Collections.<ConstraintDescriptor<?>> emptySet()
            : Collections.unmodifiableSet(constraintDescriptors);
    }

    /**
     * Get the mutable {@link ConstraintDescriptor} {@link Set}.
     * 
     * @return Set of {@link ConstraintDescriptor}
     */
    public Set<ConstraintDescriptor<?>> getMutableConstraintDescriptors() {
        return constraintDescriptors;
    }

    /**
     * {@inheritDoc} return true if at least one constraint declaration is
     * present on the element.
     */
    @Override
    public boolean hasConstraints() {
        return !getConstraintDescriptors().isEmpty();
    }

    /**
     * Set the constraintDescriptors for this element.
     * 
     * @param constraintDescriptors
     *            to set
     */
    public void setConstraintDescriptors(Set<ConstraintDescriptor<?>> constraintDescriptors) {
        this.constraintDescriptors = constraintDescriptors;
    }

    /**
     * Get the model {@link MetaBean} used.
     * 
     * @return MetaBean
     */
    public MetaBean getMetaBean() {
        return metaBean;
    }

    public void addGroupMapping(final Group from, final Group to) {
        groupMapping.put(from, to);
    }

    public Group mapGroup(final Group current) {
        final Group mapping = groupMapping.get(current);
        if (mapping != null) {
            return mapping;
        }
        return current;
    }

    public Set<GroupConversionDescriptor> getGroupConversions() {
        return groupConversions;
    }

    public void addGroupConversion(final GroupConversionDescriptor descriptor) {
        groupConversions.add(descriptor);
        final Group from = new Group(descriptor.getFrom());
        if (mapGroup(from) != from) { // ref == is fine
            throw new ConstraintDeclarationException("You can't map twice from the same group");
        }
        addGroupMapping(from, new Group(descriptor.getTo()));
    }

    public boolean isCascaded() {
        return cascaded;
    }

    public void setCascaded(final boolean cascaded) {
        this.cascaded = cascaded;
    }

    public boolean isValidated(final Object object) {
        return validated.contains(object);
    }

    public void setValidated(final Object object) {
        this.validated.add(object);
    }
}
