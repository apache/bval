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

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Objects;

import javax.validation.Path;
import javax.validation.ValidationException;

import org.apache.bval.jsr.metadata.ContainerElementKey;
import org.apache.bval.jsr.util.NodeImpl;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.reflection.TypeUtils;

public class GraphContext {

    private final ApacheFactoryContext validatorContext;
    private final PathImpl path;
    private final Object value;
    private final GraphContext parent;

    public GraphContext(ApacheFactoryContext validatorContext, PathImpl path, Object value) {
        this(validatorContext, path, value, null);
    }

    private GraphContext(ApacheFactoryContext validatorContext, PathImpl path, Object value, GraphContext parent) {
        super();
        this.validatorContext = validatorContext;
        this.path = path;
        this.value = value;
        this.parent = parent;
    }

    public ApacheFactoryContext getValidatorContext() {
        return validatorContext;
    }

    public PathImpl getPath() {
        return PathImpl.copy(path);
    }

    public Object getValue() {
        return value;
    }

    public GraphContext child(NodeImpl node, Object value) {
        // Validate.notNull(node, "node");
        final PathImpl p = PathImpl.copy(path);
        p.addNode(node);
        return new GraphContext(validatorContext, p, value, this);
    }

    public GraphContext child(Path p, Object value) {
        // Validate.notNull(p, "Path");
        final PathImpl impl = PathImpl.of(p);
        // Validate.isTrue(impl.isSubPathOf(path), "%s is not a subpath of %s", p, path);
        return new GraphContext(validatorContext, impl == p ? PathImpl.copy(impl) : impl, value, this);
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isRecursive() {
        GraphContext c = parent;
        while (c != null) {
            if (c.value == value) {
                return true;
            }
            c = c.parent;
        }
        return false;
    }

    public GraphContext getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return String.format("%s: %s at '%s'", getClass().getSimpleName(), value, path);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !obj.getClass().equals(getClass())) {
            return false;
        }
        final GraphContext other = (GraphContext) obj;
        return other.validatorContext == validatorContext && other.value == value && other.getPath().equals(path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validatorContext, value, path);
    }

    public ContainerElementKey runtimeKey(ContainerElementKey key) {
        if (value != null) {
            final Class<?> containerClass = key.getContainerClass();
            final Class<? extends Object> runtimeType = value.getClass();
            if (!runtimeType.equals(containerClass)) {
                Exceptions.raiseUnless(containerClass.isAssignableFrom(runtimeType), ValidationException::new,
                    "Value %s is not assignment-compatible with %s", value, containerClass);

                if (key.getTypeArgumentIndex() == null) {
                    return new ContainerElementKey(runtimeType, null);
                }
                final Map<TypeVariable<?>, Type> typeArguments =
                    TypeUtils.getTypeArguments(runtimeType, containerClass);

                Type type =
                    typeArguments.get(containerClass.getTypeParameters()[key.getTypeArgumentIndex().intValue()]);

                while (type instanceof TypeVariable<?>) {
                    final TypeVariable<?> var = (TypeVariable<?>) type;
                    final Type nextType = typeArguments.get(var);
                    if (nextType instanceof TypeVariable<?>) {
                        type = nextType;
                    } else {
                        return ContainerElementKey.forTypeVariable(var);
                    }
                }
            }
        }
        return key;
    }
}
