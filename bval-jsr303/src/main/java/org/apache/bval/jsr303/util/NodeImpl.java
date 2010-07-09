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

    private final String name;
    private boolean isInIterable;
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
        this.isInIterable = node.isInIterable();
        this.index = node.getIndex();
        this.key = node.getKey();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInIterable() {
        return isInIterable;
    }

    /**
     * Set whether this node represents a contained value of an {@link Iterable} or {@link Map}.
     * @param inIterable
     */
    public void setInIterable(boolean inIterable) {
        isInIterable = inIterable;
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
        isInIterable = true;
        this.index = index;
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
        isInIterable = true;
        this.key = key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(name == null ? "" : name);
        if (isInIterable) {
            builder.append(INDEX_OPEN);
            if (getIndex() != null) {
                builder.append(getIndex());
            } else if (getKey() != null) {
                builder.append(getKey());
            }
            builder.append(INDEX_CLOSE);
        }
        return builder.toString();
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

        if (isInIterable != node.isInIterable) {
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
        result = 31 * result + (isInIterable ? 1 : 0);
        result = 31 * result + (index != null ? index.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
