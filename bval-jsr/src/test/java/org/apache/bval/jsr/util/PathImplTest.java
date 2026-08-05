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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Iterator;

import jakarta.validation.Path;
import jakarta.validation.ValidationException;

import org.junit.Test;

/**
 * PathImpl Tester.
 *
 * @version 1.0
 * @since <pre>10/01/2009</pre>
 */
public class PathImplTest {
    @Test
    public void testParsing() {
        String property = "order[3].deliveryAddress.addressline[1]";
        Path path = PathImpl.createPathFromString(property);
        assertEquals(property, path.toString());

        Iterator<Path.Node> propIter = path.iterator();

        assertTrue(propIter.hasNext());
        Path.Node elem = propIter.next();
        assertFalse(elem.isInIterable());
        assertEquals("order", elem.getName());

        assertTrue(propIter.hasNext());
        elem = propIter.next();
        assertTrue(elem.isInIterable());
        assertEquals(new Integer(3), elem.getIndex());
        assertEquals("deliveryAddress", elem.getName());

        assertTrue(propIter.hasNext());
        elem = propIter.next();
        assertFalse(elem.isInIterable());
        assertEquals(null, elem.getIndex());
        assertEquals("addressline", elem.getName());

        assertTrue(propIter.hasNext());
        elem = propIter.next();
        assertTrue(elem.isInIterable());
        assertEquals(new Integer(1), elem.getIndex());
        assertNull(elem.getName());

        assertFalse(propIter.hasNext());
    }

    @Test
    public void testParseMapBasedProperty() {
        String property = "order[foo].deliveryAddress";
        Path path = PathImpl.createPathFromString(property);
        Iterator<Path.Node> propIter = path.iterator();

        assertTrue(propIter.hasNext());
        Path.Node elem = propIter.next();
        assertFalse(elem.isInIterable());
        assertEquals("order", elem.getName());

        assertTrue(propIter.hasNext());
        elem = propIter.next();
        assertTrue(elem.isInIterable());
        assertEquals("foo", elem.getKey());
        assertEquals("deliveryAddress", elem.getName());

        assertFalse(propIter.hasNext());
    }

    //some of the examples from the 1.0 bean validation spec, section 4.2
    @Test
    public void testSpecExamples() {
        String fourthAuthor = "authors[3]";
        Path path = PathImpl.createPathFromString(fourthAuthor);
        Iterator<Path.Node> propIter = path.iterator();

        assertTrue(propIter.hasNext());
        Path.Node elem = propIter.next();
        assertFalse(elem.isInIterable());
        assertEquals("authors", elem.getName());

        assertTrue(propIter.hasNext());
        elem = propIter.next();
        assertTrue(elem.isInIterable());
        assertEquals(3, elem.getIndex().intValue());
        assertNull(elem.getName());
        assertFalse(propIter.hasNext());

        String firstAuthorCompany = "authors[0].company";
        path = PathImpl.createPathFromString(firstAuthorCompany);
        propIter = path.iterator();

        assertTrue(propIter.hasNext());
        elem = propIter.next();
        assertFalse(elem.isInIterable());
        assertEquals("authors", elem.getName());

        assertTrue(propIter.hasNext());
        elem = propIter.next();
        assertTrue(elem.isInIterable());
        assertEquals(0, elem.getIndex().intValue());
        assertEquals("company", elem.getName());
        assertFalse(propIter.hasNext());
    }

    @Test
    public void testNull() {
        assertEquals(PathImpl.createPathFromString(null), PathImpl.create());

        assertEquals("", PathImpl.create().toString());
        Path path = PathImpl.create();
        Path.Node node = path.iterator().next();
        assertEquals(null, node.getName());
    }

    @Test(expected = ValidationException.class)
    public void testUnbalancedBraces() {
        PathImpl.createPathFromString("foo[.bar");
    }

    @Test(expected = ValidationException.class)
    public void testIndexInMiddleOfProperty() {
        PathImpl.createPathFromString("f[1]oo.bar");
    }

    @Test(expected = ValidationException.class)
    public void testTrailingPathSeparator() {
        PathImpl.createPathFromString("foo.bar.");
    }

    @Test(expected = ValidationException.class)
    public void testLeadingPathSeparator() {
        PathImpl.createPathFromString(".foo.bar");
    }

    @Test
    public void testEmptyString() {
        Path path = PathImpl.createPathFromString("");
        assertEquals(null, path.iterator().next().getName());
    }

    @Test
    public void testToString() {
        PathImpl path = PathImpl.create();
        path.addNode(new NodeImpl.PropertyNodeImpl("firstName"));
        assertEquals("firstName", path.toString());

        path = PathImpl.create();
        path.getLeafNode().setIndex(2);
        assertEquals("[2]", path.toString());
        path.addNode(new NodeImpl.PropertyNodeImpl("firstName"));
        assertEquals("[2].firstName", path.toString());
    }

    @Test
    public void testAddRemoveNodes() {
        PathImpl path = PathImpl.createPathFromString("");
        assertTrue(path.isRootPath());
        assertEquals(1, countNodes(path));
        path.addNode(new NodeImpl.PropertyNodeImpl("foo"));
        assertFalse(path.isRootPath());
        assertEquals(1, countNodes(path));
        path.removeLeafNode();
        assertTrue(path.isRootPath());
        assertEquals(1, countNodes(path));
    }

    @Test
    public void testLength() {
        assertEquals(1, PathImpl.create().length());
        assertEquals(4, PathImpl.createPathFromString("order[3].deliveryAddress.addressline[1]").length());
        assertEquals(0, emptyPath().length());
    }

    @Test
    public void testGetNode() {
        final PathImpl path = PathImpl.createPathFromString("order[3].deliveryAddress.addressline[1]");

        assertEquals("order", path.getNode(0).getName());
        assertEquals("deliveryAddress", path.getNode(1).getName());
        assertEquals("addressline", path.getNode(2).getName());
        assertNull(path.getNode(3).getName());

        assertThrows(IndexOutOfBoundsException.class, () -> path.getNode(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> path.getNode(4));
    }

    @Test
    public void testGetRootNodeAndGetLeafNode() {
        final PathImpl path = PathImpl.createPathFromString("order[3].deliveryAddress.addressline[1]");

        assertSame(path.getNode(0), path.getRootNode());
        assertSame(path.getNode(path.length() - 1), path.getLeafNode());

        final PathImpl root = PathImpl.create();
        assertSame(root.getRootNode(), root.getLeafNode());
    }

    @Test
    public void testAccessorsOnEmptyPath() {
        final PathImpl path = emptyPath();

        assertThrows(IndexOutOfBoundsException.class, () -> path.getNode(0));
        assertThrows(IndexOutOfBoundsException.class, path::getRootNode);
        assertThrows(IndexOutOfBoundsException.class, path::getLeafNode);
    }

    /**
     * A {@link PathImpl} holding no nodes at all. {@link PathImpl#create()} always seeds a root node and
     * {@link PathImpl#removeLeafNode()} restores one, so the only way to reach this state is to copy a foreign
     * empty {@link Path}.
     */
    private PathImpl emptyPath() {
        return PathImpl.copy(Collections.<Path.Node> emptyList()::iterator);
    }

    private int countNodes(Path path) {
        int result = 0;
        for (Iterator<Path.Node> iter = path.iterator(); iter.hasNext();) {
            iter.next();
            result++;
        }
        return result;
    }

}
