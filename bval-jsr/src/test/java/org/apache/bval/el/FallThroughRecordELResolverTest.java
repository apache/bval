/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.bval.el;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.PropertyNotWritableException;
import jakarta.el.StandardELContext;

import org.junit.Test;

public class FallThroughRecordELResolverTest {

    public record Item(String name, AtomicInteger invocations, String optional) {
        public Item(String name) {
            this(name, new AtomicInteger(), null);
        }

        public String getDisplayName() {
            return "Display " + name;
        }

        public boolean isActive() {
            return true;
        }

        public String getOptional() {
            return "getter must not shadow a null component";
        }

        public String sideEffect() {
            invocations.incrementAndGet();
            return "invoked";
        }
    }

    public interface Data {
        boolean isGood();
    }

    public record DataImpl(int status) implements Data {
        @Override
        public boolean isGood() {
            return status == 1;
        }
    }

    private final FallThroughRecordELResolver resolver = new FallThroughRecordELResolver();

    private static ELContext newContext() {
        return new StandardELContext(ExpressionFactory.newInstance());
    }

    @Test
    public void resolvesComponentsReadOnly() {
        final Item item = new Item("widget");

        ELContext context = newContext();
        assertEquals("widget", resolver.getValue(context, item, "name"));
        assertTrue(context.isPropertyResolved());

        context = newContext();
        assertNull(resolver.getType(context, item, "name"));
        assertTrue(context.isPropertyResolved());

        context = newContext();
        assertTrue(resolver.isReadOnly(context, item, "name"));
        assertTrue(context.isPropertyResolved());

        final ELContext writeContext = newContext();
        assertThrows(PropertyNotWritableException.class, () -> resolver.setValue(writeContext, item, "name", "x"));
        assertTrue(writeContext.isPropertyResolved());
    }

    @Test
    public void resolvesNullComponent() {
        final ELContext context = newContext();
        assertNull(resolver.getValue(context, new Item("widget"), "optional"));
        assertTrue(context.isPropertyResolved());
    }

    @Test
    public void leavesNonComponentsUnresolved() {
        final Item item = new Item("widget");
        for (final Object[] baseAndProperty : new Object[][] { { item, "displayName" }, { item, "sideEffect" },
            { item, "hashCode" }, { item, "missing" }, { item, null }, { null, "name" }, { "widget", "bytes" } }) {
            final Object base = baseAndProperty[0];
            final Object property = baseAndProperty[1];

            final ELContext context = newContext();
            assertNull(resolver.getValue(context, base, property));
            assertNull(resolver.getType(context, base, property));
            assertFalse(resolver.isReadOnly(context, base, property));
            resolver.setValue(context, base, property, "x");
            assertFalse(base + "." + property + " must stay unresolved", context.isPropertyResolved());
        }
        assertEquals(0, item.invocations().get());
    }

    /**
     * Interface property implemented by a record, see https://github.com/jakartaee/expression-language/issues/489.
     */
    @Test
    public void interpolatesInterfacePropertyOfRecord() {
        final ELFacade el = new ELFacade();

        assertEquals("true", el.interpolate("${validatedValue.good}", Collections.emptyMap(), new DataImpl(1)));
        assertEquals("1", el.interpolate("${validatedValue.status}", Collections.emptyMap(), new DataImpl(1)));
    }

    @Test
    public void rejectsNullContext() {
        assertThrows(NullPointerException.class, () -> resolver.getValue(null, null, null));
        assertThrows(NullPointerException.class, () -> resolver.getType(null, null, null));
        assertThrows(NullPointerException.class, () -> resolver.isReadOnly(null, null, null));
        assertThrows(NullPointerException.class, () -> resolver.setValue(null, null, null, null));
    }

    @Test
    public void interpolatesRecordProperties() {
        final ELFacade el = new ELFacade();
        final Item item = new Item("widget");

        assertEquals("widget", el.interpolate("${validatedValue.name}", Collections.emptyMap(), item));
        assertEquals("widget", el.interpolate("${validatedValue['name']}", Collections.emptyMap(), item));
        assertEquals("widget", el.interpolate("${validatedValue.name()}", Collections.emptyMap(), item));
        assertEquals("", el.interpolate("${validatedValue.optional}", Collections.emptyMap(), item));
        assertEquals("Display widget", el.interpolate("${validatedValue.displayName}", Collections.emptyMap(), item));
        assertEquals("true", el.interpolate("${validatedValue.active}", Collections.emptyMap(), item));
        assertEquals("${validatedValue.sideEffect}",
            el.interpolate("${validatedValue.sideEffect}", Collections.emptyMap(), item));
        assertEquals("${validatedValue.hashCode}",
            el.interpolate("${validatedValue.hashCode}", Collections.emptyMap(), item));
        assertEquals("${validatedValue.missing}",
            el.interpolate("${validatedValue.missing}", Collections.emptyMap(), item));
        assertEquals(0, item.invocations().get());
    }
}
