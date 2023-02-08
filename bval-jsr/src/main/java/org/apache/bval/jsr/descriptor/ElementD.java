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

import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Set;

import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.ElementDescriptor;

import org.apache.bval.jsr.groups.GroupStrategy;
import org.apache.bval.jsr.groups.GroupsComputer;
import org.apache.bval.jsr.metadata.Meta;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.TypeUtils;

public abstract class ElementD<E extends AnnotatedElement, R extends MetadataReader.ForElement<E, ?>>
    implements ElementDescriptor {

    public static abstract class NonRoot<P extends ElementD<?, ?>, E extends AnnotatedElement, R extends MetadataReader.ForElement<E, ?>>
        extends ElementD<E, R> {

        protected final P parent;

        protected NonRoot(R reader, P parent) {
            super(reader);
            this.parent = Validate.notNull(parent, "parent");
        }

        public P getParent() {
            return parent;
        }

        @Override
        public final Type getGenericType() {
            if (TypeUtils.containsTypeVariables(genericType)) {
                final Map<TypeVariable<?>, Type> args =
                    TypeUtils.getTypeArguments(parent.getGenericType(), Object.class);
                return TypeUtils.unrollVariables(args, genericType);
            }
            return genericType;
        }

        @Override
        final protected BeanD<?> getBean() {
            return parent.getBean();
        }

        @Override
        public final GroupStrategy getGroupStrategy() {
            return getBean().getGroupStrategy();
        }
    }

    protected final Type genericType;
    final GroupsComputer groupsComputer;

    private final Meta<E> meta;
    private final Set<ConstraintD<?>> constraints;

    protected ElementD(R reader) {
        super();
        Validate.notNull(reader, "reader");
        this.meta = reader.meta;
        this.genericType = reader.meta.getType();
        this.constraints = reader.getConstraints();
        this.groupsComputer = reader.getValidatorFactory().getGroupsComputer();
    }

    @Override
    public boolean hasConstraints() {
        return !constraints.isEmpty();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        return (Set) constraints;
    }

    @Override
    public final ConstraintFinder findConstraints() {
        return new Finder(groupsComputer, this);
    }

    public final ElementType getElementType() {
        return meta.getElementType();
    }

    public final E getTarget() {
        return meta.getHost();
    }

    public final Class<?> getDeclaringClass() {
        return meta.getDeclaringClass();
    }

    public abstract Type getGenericType();

    public abstract GroupStrategy getGroupStrategy();

    @Override
    public String toString() {
        return String.format("%s: %s", getClass().getSimpleName(), meta.describeHost());
    }

    protected abstract BeanD<?> getBean();
}
