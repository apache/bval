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
package org.apache.bval.jsr303.util;

import javax.validation.Path;
import javax.validation.Path.Node;

import java.io.Serializable;
import java.util.Map;

/**
 * Description: a node (property) as part of a Path.
 * (Implementation based on reference implementation) <br/>
 */
public final class NodeImpl implements Path.Node, Serializable {

    private static final long serialVersionUID = 1L;
    private static final String INDEX_OPEN = "[";
    private static final String INDEX_CLOSE = "]";

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
    private Object key;

    /**
     * Create a new NodeImpl instance.
     * @param name
     */
    public NodeImpl(String name) {
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
    }

    private NodeImpl() {
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
        return result;
    }
}
