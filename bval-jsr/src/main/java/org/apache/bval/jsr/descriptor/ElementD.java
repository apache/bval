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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;

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
        final protected BeanD getBean() {
            return parent.getBean();
        }

        @Override
        public final List<Class<?>> getGroupSequence() {
            return getBean().getGroupSequence();
        }
    }

    protected final Type genericType;

    private final E target;
    private final ElementType elementType;
    private final Set<ConstraintD<?>> constraints;

    protected ElementD(R reader) {
        super();
        Validate.notNull(reader, "reader");
        this.genericType = reader.meta.getType();
        this.target = reader.meta.getHost();
        this.elementType = reader.meta.getElementType();
        this.constraints = reader.getConstraints();
    }

    @Override
    public final boolean hasConstraints() {
        return !constraints.isEmpty();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public final Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        return (Set) constraints;
    }

    @Override
    public final ConstraintFinder findConstraints() {
        return new Finder(this);
    }

    public final ElementType getElementType() {
        return elementType;
    }

    public final E getTarget() {
        return target;
    }

    public abstract Type getGenericType();

    public abstract List<Class<?>> getGroupSequence();

    protected abstract BeanD getBean();

    @Override
    public String toString() {
        return String.format("%s: %s", getClass().getSimpleName(), target);
    }
}
