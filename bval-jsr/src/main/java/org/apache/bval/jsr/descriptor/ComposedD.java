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
package org.apache.bval.jsr.descriptor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.metadata.CascadableDescriptor;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.ContainerDescriptor;
import jakarta.validation.metadata.ContainerElementTypeDescriptor;
import jakarta.validation.metadata.ElementDescriptor;
import jakarta.validation.metadata.GroupConversionDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;

import org.apache.bval.jsr.groups.GroupsComputer;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Validate;

public abstract class ComposedD<D extends ElementD<?, ?>> implements ElementDescriptor {

    static abstract class ForCascadableContainer<D extends CascadableContainerD<?, ?>> extends ComposedD<D>
        implements CascadableDescriptor, ContainerDescriptor {

        ForCascadableContainer(List<D> delegates) {
            super(delegates);
        }

        @Override
        public Set<ContainerElementTypeDescriptor> getConstrainedContainerElementTypes() {
            return delegates.stream().map(ContainerDescriptor::getConstrainedContainerElementTypes)
                .flatMap(Collection::stream).collect(ToUnmodifiable.set());
        }

        @Override
        public boolean isCascaded() {
            return delegates.stream().anyMatch(CascadableDescriptor::isCascaded);
        }

        @Override
        public Set<GroupConversionDescriptor> getGroupConversions() {
            return delegates.stream().map(CascadableDescriptor::getGroupConversions).flatMap(Collection::stream)
                .collect(ToUnmodifiable.set());
        }
    }

    static class ForProperty extends ComposedD.ForCascadableContainer<PropertyD<?>> implements PropertyDescriptor {

        ForProperty(List<PropertyD<?>> delegates) {
            super(delegates);
        }

        @Override
        public String getPropertyName() {
            return delegates.stream().map(PropertyDescriptor::getPropertyName).findFirst()
                .orElseThrow(IllegalStateException::new);
        }
    }

    public static <T extends ElementD<?, ?>> Stream<T> unwrap(ElementDescriptor descriptor, Class<T> delegateType) {
        final Stream<?> s;

        if (descriptor instanceof ComposedD<?>) {
            s = ((ComposedD<?>) descriptor).delegates.stream()
                // unwrap recursively:
                .flatMap(d -> unwrap(d, delegateType));
        } else {
            s = Stream.of(descriptor);
        }
        return s.map(delegateType::cast);
    }

    protected final List<D> delegates;

    ComposedD(List<D> delegates) {
        super();
        this.delegates = delegates;

        Validate.notNull(delegates, "delegates");
        Validate.isTrue(!delegates.isEmpty(), "At least one delegate is required");
        Validate.isTrue(delegates.stream().noneMatch(Objects::isNull), "null delegates not permitted");
    }

    @Override
    public boolean hasConstraints() {
        return delegates.stream().anyMatch(ElementDescriptor::hasConstraints);
    }

    @Override
    public Class<?> getElementClass() {
        return delegates.stream().map(ElementDescriptor::getElementClass).findFirst()
            .orElseThrow(IllegalStateException::new);
    }

    @Override
    public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        return delegates.stream().map(ElementDescriptor::getConstraintDescriptors).flatMap(Collection::stream)
            .collect(ToUnmodifiable.set());
    }

    @Override
    public ConstraintFinder findConstraints() {
        final GroupsComputer groupsComputer =
            unwrap(this, ElementD.class).findFirst().orElseThrow(IllegalStateException::new).groupsComputer;

        return new Finder(groupsComputer, this);
    }
}
