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
package org.apache.bval.jsr;

import org.apache.bval.jsr.groups.Group;
import org.apache.bval.jsr.groups.Groups;
import org.apache.bval.jsr.groups.GroupsComputer;
import org.apache.bval.model.MetaBean;

import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.ElementDescriptor.ConstraintFinder;
import javax.validation.metadata.Scope;
import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Description: Implementation of the fluent {@link ConstraintFinder} interface.<br/>
 */
final class ConstraintFinderImpl implements ElementDescriptor.ConstraintFinder {
    private final MetaBean metaBean;
    private final Set<Scope> findInScopes;
    private Set<ConstraintValidation<?>> constraintDescriptors;

    /**
     * Create a new ConstraintFinderImpl instance.
     * 
     * @param metaBean
     * @param constraintDescriptors
     */
    ConstraintFinderImpl(MetaBean metaBean, Set<ConstraintValidation<?>> constraintDescriptors) {
        this.metaBean = metaBean;
        this.constraintDescriptors = constraintDescriptors;
        this.findInScopes = new HashSet<Scope>(Arrays.asList(Scope.values()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ElementDescriptor.ConstraintFinder unorderedAndMatchingGroups(Class<?>... groups) {
        final Set<ConstraintValidation<?>> matchingDescriptors =
            new HashSet<ConstraintValidation<?>>(constraintDescriptors.size());
        final Groups groupChain = new GroupsComputer().computeGroups(groups);
        for (Group group : groupChain.getGroups()) {
            if (group.isDefault()) {
                // If group is default, check if it gets redefined
                for (Group defaultGroupMember : metaBean.<List<Group>> getFeature(JsrFeatures.Bean.GROUP_SEQUENCE)) {
                    for (ConstraintValidation<?> descriptor : constraintDescriptors) {
                        if (isInScope(descriptor) && isInGroup(descriptor, defaultGroupMember)) {
                            matchingDescriptors.add(descriptor);
                        }
                    }
                }
            } else {
                for (ConstraintValidation<?> descriptor : constraintDescriptors) {
                    if (isInScope(descriptor) && isInGroup(descriptor, group)) {
                        matchingDescriptors.add(descriptor);
                    }
                }
            }
        }
        return thisWith(matchingDescriptors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ElementDescriptor.ConstraintFinder lookingAt(Scope scope) {
        if (scope == Scope.LOCAL_ELEMENT) {
            findInScopes.remove(Scope.HIERARCHY);
            for (Iterator<ConstraintValidation<?>> it = constraintDescriptors.iterator(); it.hasNext();) {
                if (!it.next().getOwner().equals(metaBean.getBeanClass())) {
                    it.remove();
                }
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ElementDescriptor.ConstraintFinder declaredOn(ElementType... elementTypes) {
        final Set<ConstraintValidation<?>> matchingDescriptors =
            new HashSet<ConstraintValidation<?>>(constraintDescriptors.size());
        for (ElementType each : elementTypes) {
            for (ConstraintValidation<?> descriptor : constraintDescriptors) {
                if (isInScope(descriptor) && isAtElement(descriptor, each)) {
                    matchingDescriptors.add(descriptor);
                }
            }
        }
        return thisWith(matchingDescriptors);
    }

    private boolean isAtElement(ConstraintValidation<?> descriptor, ElementType each) {
        return descriptor.getAccess().getElementType() == each;
    }

    private boolean isInScope(ConstraintValidation<?> descriptor) {
        if (findInScopes.size() == Scope.values().length) {
            return true; // all scopes
        }
        if (metaBean != null) {
            final boolean isOwner = descriptor.getOwner().equals(metaBean.getBeanClass());
            for (Scope scope : findInScopes) {
                switch (scope) {
                case LOCAL_ELEMENT:
                    if (isOwner) {
                        return true;
                    }
                    break;
                case HIERARCHY:
                    if (!isOwner) {
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    private boolean isInGroup(ConstraintValidation<?> descriptor, Group group) {
        return descriptor.getGroups().contains(group.getGroup());
    }

    private ElementDescriptor.ConstraintFinder thisWith(Set<ConstraintValidation<?>> matchingDescriptors) {
        constraintDescriptors = matchingDescriptors;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        if (constraintDescriptors.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.<ConstraintDescriptor<?>> unmodifiableSet(constraintDescriptors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasConstraints() {
        return !constraintDescriptors.isEmpty();
    }
}
