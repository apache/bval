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
package com.agimatec.validation.jsr303.util;

import javax.validation.Path;
import java.io.Serializable;

/**
 * Description: a node (property) as part of a Path.
 * (Implementation based on reference implementation) <br/>
 * User: roman <br/>
 * Date: 28.09.2009 <br/>
 * Time: 11:56:35 <br/>
 * Copyright: Agimatec GmbH
 */
public final class NodeImpl implements Path.Node, Serializable {

    private static final String INDEX_OPEN = "[";
    private static final String INDEX_CLOSE = "]";

    private final String name;
    private boolean isInIterable;
    private Integer index;
    private Object key;

    public NodeImpl(String name) {
        this.name = name;
    }

    NodeImpl(Path.Node node) {
        this.name = node.getName();
        this.isInIterable = node.isInIterable();
        this.index = node.getIndex();
        this.key = node.getKey();
    }

    public String getName() {
        return name;
    }

    public boolean isInIterable() {
        return isInIterable;
    }

    public void setInIterable(boolean inIterable) {
        isInIterable = inIterable;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        isInIterable = true;
        this.index = index;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        isInIterable = true;
        this.key = key;
    }

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

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (isInIterable ? 1 : 0);
        result = 31 * result + (index != null ? index.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
