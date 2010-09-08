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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.validation.Path;

import org.apache.bval.jsr303.util.NodeImpl;
import org.apache.bval.jsr303.util.PathImpl;

import java.util.Iterator;

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
        assertEquals("order", elem.getName());
        assertTrue(elem.isInIterable());
        assertEquals(new Integer(3), elem.getIndex());

        assertTrue(propIter.hasNext());
        elem = propIter.next();
        assertEquals("deliveryAddress", elem.getName());
        assertFalse(elem.isInIterable());
        assertEquals(null, elem.getIndex());

        assertTrue(propIter.hasNext());
        elem = propIter.next();
        assertEquals("addressline", elem.getName());
        assertTrue(elem.isInIterable());
        assertEquals(new Integer(1), elem.getIndex());

        assertFalse(propIter.hasNext());
    }

    public void testParseMapBasedProperty() {
        String property = "order[foo].deliveryAddress";
        Path path = PathImpl.createPathFromString(property);
        Iterator<Path.Node> propIter = path.iterator();

        assertTrue(propIter.hasNext());
        Path.Node elem = propIter.next();
        assertEquals("order", elem.getName());
        assertTrue(elem.isInIterable());
        assertEquals("foo", elem.getKey());

        assertTrue(propIter.hasNext());
        elem = propIter.next();
        assertEquals("deliveryAddress", elem.getName());
        assertFalse(elem.isInIterable());
        assertEquals(null, elem.getIndex());

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
        } catch (IllegalArgumentException ex) {
        }
    }

    public void testIndexInMiddleOfProperty() {
        try {
            PathImpl.createPathFromString("f[1]oo.bar");
            fail();
        } catch (IllegalArgumentException ex) {
        }
    }

    public void testTrailingPathSeperator() {
        try {
            PathImpl.createPathFromString("foo.bar.");
            fail();
        } catch (IllegalArgumentException ex) {
        }
    }

    public void testLeadingPathSeperator() {
        try {
            PathImpl.createPathFromString(".foo.bar");
            fail();
        } catch (IllegalArgumentException ex) {
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

    public static Test suite() {
        return new TestSuite(PathImplTest.class);
    }
}
