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
package org.apache.bval.el;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

public class ELFacadeTest {
    private final ELFacade facade = new ELFacade();

    @Test
    public void disarmsUnescapedDeferredExpressions() {
        assertInterpolates("a no deferred", "${'a'} no deferred");
        assertInterpolates("a $0x} b", "${'a'} #{x} b");
        assertInterpolates("$0x} a", "#{x} ${'a'}");
        assertInterpolates("a $0x} $0y}", "${'a'} #{x} #{y}");
    }

    @Test
    public void keepsDeferredExpressionsEscapedByAnOddNumberOfBackslashes() {
        assertInterpolates("a #{x} b", "${'a'} \\#{x} b");
        assertInterpolates("a \\\\$0x} b", "${'a'} \\\\#{x} b");
        assertInterpolates("a \\\\#{x} b", "${'a'} \\\\\\#{x} b");
    }

    @Test
    public void leavesMessagesWithoutImmediateExpressionsUntouched() {
        assertInterpolates("#{x} without immediate", "#{x} without immediate");
    }

    private void assertInterpolates(String expected, String message) {
        assertEquals(expected, facade.interpolate(message, Collections.emptyMap(), null));
    }
}
