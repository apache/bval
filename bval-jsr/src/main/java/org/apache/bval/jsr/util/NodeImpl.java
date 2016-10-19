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
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class NodeImpl implements Path.Node, Serializable {

    private static final long serialVersionUID = 1L;
    private static final String INDEX_OPEN = "[";
    private static final String INDEX_CLOSE = "]";
    private List<Class<?>> parameterTypes;

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
        NodeImpl result = new NodeImpl();
        result.setIndex(index);
        return result;
    }

    /**
     * Get a NodeImpl keyed from the preceding node (or root).
     * @param key
     * @return NodeImpl
     */
    public static NodeImpl atKey(Object key) {
        NodeImpl result = new NodeImpl();
        result.setKey(key);
        return result;
    }

    private String name;
    private boolean inIterable;
    private Integer index;
    private int parameterIndex;
    private Object key;
    private ElementKind kind;

    /**
     * Create a new NodeImpl instance.
     * @param name
     */
    public  NodeImpl(String name) {
        this.name = name;
    }

    /**
     * Create a new NodeImpl instance.
     * @param node
     */
    NodeImpl(Path.Node node) {
        this.name = node.getName();
        this.inIterable = node.isInIterable();
        this.index = node.getIndex();
        this.key = node.getKey();
        this.kind = node.getKind();
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
    public ElementKind getKind() {
        return kind;
    }

    public void setKind(ElementKind kind) {
        this.kind = kind;
    }

    @Override
    public <T extends Node> T as(final Class<T> nodeType) {
        if (nodeType.isInstance(this)) {
            return nodeType.cast(this);
        }
        throw new ClassCastException("Type " + nodeType + " not supported");
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NodeImpl node = (NodeImpl) o;

        if (inIterable != node.inIterable) {
            return false;
        }
        if (index != null ? !index.equals(node.index) : node.index != null) {
            return false;
        }
        if (key != null ? !key.equals(node.key) : node.key != null) {
            return false;
        }
        if (name != null ? !name.equals(node.name) : node.name != null) {
            return false;
        }
        if (kind != null ? !kind.equals(node.kind) : node.kind != null) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (inIterable ? 1 : 0);
        result = 31 * result + (index != null ? index.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        return result;
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

    public static class ParameterNodeImpl extends NodeImpl implements Path.ParameterNode {
        public ParameterNodeImpl(final Node cast) {
            super(cast);
            if (ParameterNodeImpl.class.isInstance(cast)) {
                setParameterIndex(ParameterNodeImpl.class.cast(cast).getParameterIndex());
            }
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

    public static class ConstructorNodeImpl extends NodeImpl implements Path.ConstructorNode {
        public ConstructorNodeImpl(final Node cast) {
            super(cast);
            if (NodeImpl.class.isInstance(cast)) {
                setParameterTypes(NodeImpl.class.cast(cast).parameterTypes);
            }
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

    public static class MethodNodeImpl extends NodeImpl implements Path.MethodNode {
        public MethodNodeImpl(final Node cast) {
            super(cast);
            if (MethodNodeImpl.class.isInstance(cast)) {
                setParameterTypes(MethodNodeImpl.class.cast(cast).getParameterTypes());
            }
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

    public static class PropertyNodeImpl extends NodeImpl implements Path.PropertyNode {
        public PropertyNodeImpl(final String name) {
            super(name);
        }

        public PropertyNodeImpl(final Node cast) {
            super(cast);
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.PROPERTY;
        }
    }

    public static class BeanNodeImpl extends NodeImpl implements Path.BeanNode {
        public BeanNodeImpl() {
            // no-op
        }

        public BeanNodeImpl(final Node cast) {
            super(cast);
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.BEAN;
        }
    }
}
