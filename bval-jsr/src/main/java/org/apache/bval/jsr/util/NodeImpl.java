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
package org.apache.bval.jsr.util;

import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.Path.Node;

import org.apache.bval.util.Exceptions;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class NodeImpl implements Path.Node, Serializable {

    private static final long serialVersionUID = 1L;
    private static final String INDEX_OPEN = "[";
    private static final String INDEX_CLOSE = "]";

    private static <T extends Path.Node> Optional<T> optional(Class<T> type, Object o) {
        return Optional.ofNullable(o).filter(type::isInstance).map(type::cast);
    }

    /**
     * Append a Node to the specified StringBuilder.
     * @param node
     * @param to
     * @return to
     */
    public static StringBuilder appendNode(Node node, StringBuilder to) {
        if (node.isInIterable()) {
            to.append(INDEX_OPEN);
            if (node.getIndex() != null) {
                to.append(node.getIndex());
            } else if (node.getKey() != null) {
                to.append(node.getKey());
            }
            to.append(INDEX_CLOSE);
        }
        if (node.getName() != null) {
            if (to.length() > 0) {
                to.append(PathImpl.PROPERTY_PATH_SEPARATOR);
            }
            to.append(node.getName());
        }
        return to;
    }

    /**
     * Get a NodeImpl indexed from the preceding node (or root).
     * @param index
     * @return NodeImpl
     */
    public static NodeImpl atIndex(Integer index) {
        final NodeImpl result = new NodeImpl.PropertyNodeImpl((String) null);
        result.setIndex(index);
        return result;
    }

    /**
     * Get a NodeImpl keyed from the preceding node (or root).
     * @param key
     * @return NodeImpl
     */
    public static NodeImpl atKey(Object key) {
        final NodeImpl result = new NodeImpl.PropertyNodeImpl((String) null);
        result.setKey(key);
        return result;
    }

    private String name;
    private boolean inIterable;
    private Integer index;
    private int parameterIndex;
    private Object key;
    private List<Class<?>> parameterTypes;
    private Class<?> containerType;
    private Integer typeArgumentIndex;

    /**
     * Create a new NodeImpl instance.
     * @param name
     */
    private NodeImpl(String name) {
        this.name = name;
    }

    /**
     * Create a new NodeImpl instance.
     * @param node
     */
    NodeImpl(Path.Node node) {
        this(node.getName());
        this.inIterable = node.isInIterable();
        this.index = node.getIndex();
        this.key = node.getKey();

        if (node instanceof NodeImpl) {
            final NodeImpl n = (NodeImpl) node;
            this.parameterIndex = n.parameterIndex;
            this.parameterTypes = n.parameterTypes;
            this.containerType = n.containerType;
            this.typeArgumentIndex = n.typeArgumentIndex;
        }
    }

    <T extends Path.Node> NodeImpl(Path.Node node, Class<T> nodeType, Consumer<T> handler) {
        this(node);
        Optional.of(node).filter(nodeType::isInstance).map(nodeType::cast).ifPresent(handler);
    }

    private NodeImpl() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInIterable() {
        return inIterable;
    }

    /**
     * Set whether this node represents a contained value of an {@link Iterable} or {@link Map}.
     * @param inIterable
     */
    public void setInIterable(boolean inIterable) {
        this.inIterable = inIterable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getIndex() {
        return index;
    }

    /**
     * Set the index of this node, implying <code>inIterable</code>.
     * @param index
     */
    public void setIndex(Integer index) {
        inIterable = true;
        this.index = index;
        this.key = null;
    }

    public void setParameterIndex(final Integer parameterIndex) {
        this.parameterIndex = parameterIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getKey() {
        return key;
    }

    /**
     * Set the map key of this node, implying <code>inIterable</code>.
     * @param key
     */
    public void setKey(Object key) {
        inIterable = true;
        this.key = key;
        this.index = null;
    }

    @Override
    public <T extends Node> T as(final Class<T> nodeType) {
        Exceptions.raiseUnless(nodeType.isInstance(this), ClassCastException::new, "Type %s not supported by %s",
            nodeType, getClass());
        return nodeType.cast(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return appendNode(this, new StringBuilder()).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        final NodeImpl node = (NodeImpl) o;

        return inIterable == node.inIterable && Objects.equals(index, node.index) && Objects.equals(key, node.key)
            && Objects.equals(name, node.name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, Boolean.valueOf(inIterable), index, key, getKind());
    }

    public int getParameterIndex() {
        return parameterIndex;
    }

    public List<Class<?>> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(final List<Class<?>> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Class<?> getContainerClass() {
        return containerType;
    }

    public Integer getTypeArgumentIndex() {
        return typeArgumentIndex;
    }

    public NodeImpl inContainer(Class<?> containerType, Integer typeArgumentIndex) {
        this.containerType = containerType;
        this.typeArgumentIndex = typeArgumentIndex;
        return this;
    }

    @SuppressWarnings("serial")
    public static class ParameterNodeImpl extends NodeImpl implements Path.ParameterNode {
        public ParameterNodeImpl(final Node cast) {
            super(cast);
            optional(Path.ParameterNode.class, cast).ifPresent(n -> setParameterIndex(n.getParameterIndex()));
        }

        public ParameterNodeImpl(final String name, final int idx) {
            super(name);
            setParameterIndex(idx);
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.PARAMETER;
        }
    }

    @SuppressWarnings("serial")
    public static class ConstructorNodeImpl extends NodeImpl implements Path.ConstructorNode {
        public ConstructorNodeImpl(final Node cast) {
            super(cast);
            optional(Path.ConstructorNode.class, cast).ifPresent(n -> setParameterTypes(n.getParameterTypes()));
        }

        public ConstructorNodeImpl(final String simpleName, List<Class<?>> paramTypes) {
            super(simpleName);
            setParameterTypes(paramTypes);
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.CONSTRUCTOR;
        }
    }

    @SuppressWarnings("serial")
    public static class CrossParameterNodeImpl extends NodeImpl implements Path.CrossParameterNode {
        public CrossParameterNodeImpl() {
            super("<cross-parameter>");
        }

        public CrossParameterNodeImpl(final Node cast) {
            super(cast);
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.CROSS_PARAMETER;
        }
    }

    @SuppressWarnings("serial")
    public static class MethodNodeImpl extends NodeImpl implements Path.MethodNode {
        public MethodNodeImpl(final Node cast) {
            super(cast);
            optional(Path.MethodNode.class, cast).ifPresent(n -> setParameterTypes(n.getParameterTypes()));
        }

        public MethodNodeImpl(final String name, final List<Class<?>> classes) {
            super(name);
            setParameterTypes(classes);
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.METHOD;
        }
    }

    @SuppressWarnings("serial")
    public static class ReturnValueNodeImpl extends NodeImpl implements Path.ReturnValueNode {
        public ReturnValueNodeImpl(final Node cast) {
            super(cast);
        }

        public ReturnValueNodeImpl() {
            super("<return value>");
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.RETURN_VALUE;
        }
    }

    @SuppressWarnings("serial")
    public static class PropertyNodeImpl extends NodeImpl implements Path.PropertyNode {
        public PropertyNodeImpl(final String name) {
            super(name);
        }

        public PropertyNodeImpl(final Node cast) {
            super(cast);
            optional(Path.PropertyNode.class, cast)
                .ifPresent(n -> inContainer(n.getContainerClass(), n.getTypeArgumentIndex()));
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.PROPERTY;
        }
    }

    @SuppressWarnings("serial")
    public static class BeanNodeImpl extends NodeImpl implements Path.BeanNode {
        public BeanNodeImpl() {
            // no-op
        }

        public BeanNodeImpl(final Node cast) {
            super(cast);
            optional(Path.BeanNode.class, cast)
                .ifPresent(n -> inContainer(n.getContainerClass(), n.getTypeArgumentIndex()));
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.BEAN;
        }
    }

    @SuppressWarnings("serial")
    public static class ContainerElementNodeImpl extends NodeImpl implements Path.ContainerElementNode {

        public ContainerElementNodeImpl(String name) {
            super(name);
        }

        public ContainerElementNodeImpl(String name, Class<?> containerType, Integer typeArgumentIndex) {
            this(name);
            inContainer(containerType, typeArgumentIndex);
        }

        public ContainerElementNodeImpl(final Node cast) {
            super(cast);
            optional(Path.ContainerElementNode.class, cast)
                .ifPresent(n -> inContainer(n.getContainerClass(), n.getTypeArgumentIndex()));
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.CONTAINER_ELEMENT;
        }
    }
}
