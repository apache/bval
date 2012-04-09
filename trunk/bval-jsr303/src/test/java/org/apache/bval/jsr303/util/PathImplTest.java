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

import java.util.Iterator;

import javax.validation.Path;
import javax.validation.ValidationException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * PathImpl Tester.
 *
 * @version 1.0
 * @since <pre>10/01/2009</pre>
 */
public class PathImplTest extends TestCase {
    public PathImplTest(String name) {
        super(name);
    }

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

    public void testNull() {
        assertEquals(PathImpl.createPathFromString(null), PathImpl.create(null));

        assertEquals("", PathImpl.create(null).toString());
        Path path = PathImpl.create(null);
        Path.Node node = path.iterator().next();
        assertEquals(null, node.getName());
    }

    public void testUnbalancedBraces() {
        try {
            PathImpl.createPathFromString("foo[.bar");
            fail();
        } catch (ValidationException ex) {
        }
    }

    public void testIndexInMiddleOfProperty() {
        try {
            PathImpl.createPathFromString("f[1]oo.bar");
            fail();
        } catch (ValidationException ex) {
        }
    }

    public void testTrailingPathSeparator() {
        try {
            PathImpl.createPathFromString("foo.bar.");
            fail();
        } catch (ValidationException ex) {
        }
    }

    public void testLeadingPathSeparator() {
        try {
            PathImpl.createPathFromString(".foo.bar");
            fail();
        } catch (ValidationException ex) {
        }
    }

    public void testEmptyString() {
        Path path = PathImpl.createPathFromString("");
        assertEquals(null, path.iterator().next().getName());
    }

    public void testToString() {
        PathImpl path = PathImpl.create(null);
        path.addNode(new NodeImpl("firstName"));
        assertEquals("firstName", path.toString());

        path = PathImpl.create(null);
        path.getLeafNode().setIndex(2);
        assertEquals("[2]", path.toString());
        path.addNode(new NodeImpl("firstName"));
        assertEquals("[2].firstName", path.toString());
    }

    public void testAddRemoveNodes() {
        PathImpl path = PathImpl.createPathFromString("");
        assertTrue(path.isRootPath());
        assertEquals(1, countNodes(path));
        path.addNode(new NodeImpl("foo"));
        assertFalse(path.isRootPath());
        assertEquals(1, countNodes(path));
        path.removeLeafNode();
        assertTrue(path.isRootPath());
        assertEquals(1, countNodes(path));
    }

    private int countNodes(Path path) {
        int result = 0;
        for (Iterator<Path.Node> iter = path.iterator(); iter.hasNext();) {
            iter.next();
            result++;
        }
        return result;
    }

    public static Test suite() {
        return new TestSuite(PathImplTest.class);
    }
}
