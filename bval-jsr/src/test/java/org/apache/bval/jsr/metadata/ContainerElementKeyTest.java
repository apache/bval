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
package org.apache.bval.jsr.metadata;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ContainerElementKeyTest {
    public static abstract class HasList {
        public List<String> strings;
    }

    private Field stringsField;

    @Before
    public void setup() throws Exception {
        stringsField = HasList.class.getField("strings");
    }

    @Test
    public void testBasic() {
        final ContainerElementKey containerElementKey =
            new ContainerElementKey(stringsField.getAnnotatedType(), Integer.valueOf(0));

        assertEquals(List.class, containerElementKey.getContainerClass());
        assertEquals(0, containerElementKey.getTypeArgumentIndex().intValue());
        assertEquals(String.class, containerElementKey.getAnnotatedType().getType());
    }

    @Test
    public void testAssignableKeys() {
        final ContainerElementKey containerElementKey =
            new ContainerElementKey(stringsField.getAnnotatedType(), Integer.valueOf(0));

        final Iterator<ContainerElementKey> iterator = containerElementKey.getAssignableKeys().iterator();
        {
            assertTrue(iterator.hasNext());
            final ContainerElementKey assignableKey = iterator.next();
            assertEquals(Collection.class, assignableKey.getContainerClass());
            assertEquals(0, assignableKey.getTypeArgumentIndex().intValue());
            assertTrue(assignableKey.getAnnotatedType().getType() instanceof TypeVariable<?>);
        }
        {
            assertTrue(iterator.hasNext());
            final ContainerElementKey assignableKey = iterator.next();
            assertEquals(Iterable.class, assignableKey.getContainerClass());
            assertEquals(0, assignableKey.getTypeArgumentIndex().intValue());
            assertTrue(assignableKey.getAnnotatedType().getType() instanceof TypeVariable<?>);
        }
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testTypeVariableInheritance() {
        final ContainerElementKey containerElementKey =
            new ContainerElementKey(stringsField.getAnnotatedType(), Integer.valueOf(0));

        assertTrue(containerElementKey.represents(List.class.getTypeParameters()[0]));
        assertTrue(containerElementKey.represents(Collection.class.getTypeParameters()[0]));
        assertTrue(containerElementKey.represents(Iterable.class.getTypeParameters()[0]));
    }
}
