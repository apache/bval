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


import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.Scope;

import org.apache.bval.jsr303.groups.Group;
import org.apache.bval.jsr303.groups.Groups;
import org.apache.bval.jsr303.groups.GroupsComputer;
import org.apache.bval.model.MetaBean;

import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Description: Implementation to find constraints.<br/>
 * User: roman <br/>
 * Date: 13.10.2009 <br/>
 * Time: 10:44:46 <br/>
 * Copyright: Agimatec GmbH
 */
final class ConstraintFinderImpl implements ElementDescriptor.ConstraintFinder {
    private final MetaBean metaBean;
    private final Set<Scope> findInScopes;
    private Set<ConstraintValidation> constraintDescriptors;

    ConstraintFinderImpl(MetaBean metaBean, Set constraintDescriptors) {
        this.metaBean = metaBean;
        this.constraintDescriptors = constraintDescriptors;
        this.findInScopes = new HashSet<Scope>(Arrays.asList(Scope.values()));
    }

    public ElementDescriptor.ConstraintFinder unorderedAndMatchingGroups(Class<?>... groups) {
        Set<ConstraintValidation> matchingDescriptors =
              new HashSet<ConstraintValidation>(constraintDescriptors.size());
        Groups groupChain = new GroupsComputer().computeGroups(groups);
        for (Group group : groupChain.getGroups()) {
            for (ConstraintValidation descriptor : constraintDescriptors) {
                if (isInScope(descriptor) && isInGroup(descriptor, group)) {
                    matchingDescriptors.add(descriptor);
                }
            }
        }
        return thisWith(matchingDescriptors);
    }

    public ElementDescriptor.ConstraintFinder lookingAt(Scope scope) {
        if (scope.equals(Scope.LOCAL_ELEMENT)) {
            findInScopes.remove(Scope.HIERARCHY);
        }
        return this;
    }

    public ElementDescriptor.ConstraintFinder declaredOn(ElementType... elementTypes) {
        Set<ConstraintValidation> matchingDescriptors =
              new HashSet<ConstraintValidation>(constraintDescriptors.size());
        for (ElementType each : elementTypes) {
            for (ConstraintValidation descriptor : constraintDescriptors) {
                if (isInScope(descriptor) && isAtElement(descriptor, each)) {
                    matchingDescriptors.add(descriptor);
                }
            }
        }
        return thisWith(matchingDescriptors);
    }

    private boolean isAtElement(ConstraintValidation descriptor, ElementType each) {
        return descriptor.getAccess().getElementType() == each;
    }

    private boolean isInScope(ConstraintValidation descriptor) {
        if (findInScopes.size() == Scope.values().length) return true; // all scopes
        if (metaBean != null) {
            Class owner = descriptor.getOwner();
            for (Scope scope : findInScopes) {
                switch (scope) {
                    case LOCAL_ELEMENT:
                        if (owner.equals(metaBean.getBeanClass())) return true;
                        break;
                    case HIERARCHY:
                        if (!owner.equals(metaBean.getBeanClass())) return true;
                        break;
                }
            }
        }
        return false;
    }

    private boolean isInGroup(ConstraintValidation descriptor, Group group) {
        return descriptor.getGroups().contains(group.getGroup());
    }

    private ElementDescriptor.ConstraintFinder thisWith(
          Set<ConstraintValidation> matchingDescriptors) {
        constraintDescriptors = matchingDescriptors;
        return this;
    }

    public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        //noinspection RedundantCast
        return (Set) constraintDescriptors;
    }

    public boolean hasConstraints() {
        return !constraintDescriptors.isEmpty();
    }
}
