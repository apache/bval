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

import org.junit.Test;

public class ELFacadeTest {

    @Test
    public void disarmsUnescapedDeferredExpressions() {
        assertEquals("no deferred ${x}", ELFacade.disarmDeferredExpressions("no deferred ${x}"));
        assertEquals("a $0x} b", ELFacade.disarmDeferredExpressions("a #{x} b"));
        assertEquals("$0x}", ELFacade.disarmDeferredExpressions("#{x}"));
        assertEquals("a \\#{x} b", ELFacade.disarmDeferredExpressions("a \\#{x} b"));
        assertEquals("a \\\\$0x} b", ELFacade.disarmDeferredExpressions("a \\\\#{x} b"));
        assertEquals("a \\\\\\#{x} b", ELFacade.disarmDeferredExpressions("a \\\\\\#{x} b"));
        assertEquals("$0a} $0b}", ELFacade.disarmDeferredExpressions("#{a} #{b}"));
    }
}
