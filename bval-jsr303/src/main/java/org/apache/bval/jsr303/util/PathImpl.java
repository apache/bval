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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description: object holding the property path as a list of nodes.
 * (Implementation based on reference implementation)
 * <br/>
 * This class is not synchronized.
 */
public class PathImpl implements Path, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Regular expression used to split a string path into its elements.
     *
     * @see <a href="http://www.regexplanet.com/simple/index.jsp">Regular expression tester</a>
     */
    private static final Pattern pathPattern =
          Pattern.compile("(\\w+)(\\[(\\w*)\\])?(\\.(.*))*");

    private static final String PROPERTY_PATH_SEPARATOR = ".";

    private final List<Node> nodeList;

    /**
     * Returns a {@code Path} instance representing the path described by the given string. To create a root node the empty string should be passed.
     * Note:  This signature is to maintain pluggability with the RI impl.
     *
     * @param propertyPath the path as string representation.
     * @return a {@code Path} instance representing the path described by the given string.
     */
    public static PathImpl createPathFromString(String propertyPath) {
        if (propertyPath == null || propertyPath.length() == 0) {
            return create(null);
        }

        return parseProperty(propertyPath);
    }

    /**
     * Create a {@link PathImpl} instance representing the specified path.
     * @param name
     * @return PathImpl
     */
    public static PathImpl create(String name) {
        PathImpl path = new PathImpl();
        NodeImpl node = new NodeImpl(name);
        path.addNode(node);
        return path;
    }

    /**
     * Copy another Path.
     * @param path
     * @return new {@link PathImpl}
     */
    public static PathImpl copy(Path path) {
        return path == null ? null : new PathImpl(path);
    }

    private PathImpl(Path path) {
        this.nodeList = new ArrayList<Node>();
        for (Object aPath : path) {
            nodeList.add(new NodeImpl((Node) aPath));
        }
    }

    private PathImpl() {
        nodeList = new ArrayList<Node>();
    }

    private PathImpl(List<Node> nodeList) {
        this.nodeList = new ArrayList<Node>();
        for (Node node : nodeList) {
            this.nodeList.add(new NodeImpl(node));
        }
    }

    /**
     * Learn whether this {@link PathImpl} points to the root of its graph.
     * @return true if no child nodes
     */
    //our implementation stores a nameless root node.
    public boolean isRootPath() {
        return nodeList.size() == 1 && nodeList.get(0).getName() == null;
    }

    /**
     * Return a new {@link PathImpl} that represents <code>this</code> minus its leaf node (if present). 
     * @return PathImpl
     */
    public PathImpl getPathWithoutLeafNode() {
        List<Node> nodes = new ArrayList<Node>(nodeList);
        PathImpl path = null;
        if (nodes.size() > 1) {
            nodes.remove(nodes.size() - 1);
            path = new PathImpl(nodes);
        }
        return path;
    }

    /**
     * Add a node to this {@link PathImpl}.
     * @param node to add
     */
    public void addNode(Node node) {
    	if ( isRootPath() && nodeList.get(0).getIndex() == null ) {
    		nodeList.set(0, node);
    	}
    	else {
    		nodeList.add(node);
    	}
    }

    /**
     * Trim the leaf node from this {@link PathImpl}.
     * @return the node removed
     * @throws IllegalStateException if no nodes are found
     */
    public Node removeLeafNode() {
        if (isRootPath() || nodeList.size() == 0) {
            throw new IllegalStateException("No nodes in path!");
        }
        try {
            return nodeList.remove(nodeList.size() - 1);
        } finally {
            if (nodeList.isEmpty()) {
                nodeList.add(new NodeImpl((String) null));
            }
        }
    }

    /**
     * Get the leaf node (if any) from this {@link PathImpl}
     * @return {@link NodeImpl}
     */
    public NodeImpl getLeafNode() {
        if (nodeList.size() == 0) {
            return null;
        }
        return (NodeImpl) nodeList.get(nodeList.size() - 1);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Path.Node> iterator() {
        return nodeList.iterator();
    }

    /**
     * Learn whether <code>path</code> is a parent to <code>this</code>.
     * @param path
     * @return <code>true</code> if our nodes begin with nodes equal to those found in <code>path</code>
     */
    public boolean isSubPathOf(Path path) {
        if (path instanceof PathImpl && ((PathImpl) path).isRootPath()) {
            return true;
        }
        Iterator<Node> pathIter = path.iterator();
        Iterator<Node> thisIter = iterator();
        while (pathIter.hasNext()) {
            Node pathNode = pathIter.next();
            if (!thisIter.hasNext()) {
                return false;
            }
            Node thisNode = thisIter.next();
            if (!thisNode.equals(pathNode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Iterator<Path.Node> iter = iterator();
        while (iter.hasNext()) {
            Node node = iter.next();
            builder.append(node.toString());
            if (iter.hasNext() && builder.length() > 0) {
                builder.append(PROPERTY_PATH_SEPARATOR);
            }
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

        PathImpl path = (PathImpl) o;
        return !(nodeList != null && !nodeList.equals(path.nodeList)) &&
              !(nodeList == null && path.nodeList != null);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return nodeList != null ? nodeList.hashCode() : 0;
    }

    private static PathImpl parseProperty(String property) {
        PathImpl path = new PathImpl();
        String tmp = property;
        do {
            Matcher matcher = pathPattern.matcher(tmp);
            if (matcher.matches()) {
                String value = matcher.group(1);
                String indexed = matcher.group(2);
                String index = matcher.group(3);
                NodeImpl node = new NodeImpl(value);
                if (indexed != null) {
                    node.setInIterable(true);
                }
                if (index != null && index.length() > 0) {
                    try {
                        Integer i = Integer.valueOf(index);
                        node.setIndex(i);
                    } catch (NumberFormatException e) {
                        node.setKey(index);
                    }
                }
                path.addNode(node);
                tmp = matcher.group(5);
            } else {
                throw new IllegalArgumentException(
                      "Unable to parse property path " + property);
            }
        } while (tmp != null);
        return path;
    }

}
