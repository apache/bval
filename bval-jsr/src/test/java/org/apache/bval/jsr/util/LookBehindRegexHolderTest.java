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
package org.apache.bval.jsr.util;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class LookBehindRegexHolderTest {
    private static final String MESSAGE_PARAMETER_PATTERN =
        "(?<!(?:^|[^\\\\])(?:\\\\\\\\){0,%1$d}\\\\)\\{((?:[\\w\\.]|\\\\[\\{\\$\\}\\\\])+)\\}";

    private LookBehindRegexHolder messageParameter;

    @Before
    public void setup() {
        messageParameter = new LookBehindRegexHolder(MESSAGE_PARAMETER_PATTERN, 5, 5, this::computeInjectedRepetition);
    }

    @Test
    public void testLookBehind() {
        assertFound("{foo}");
        assertFound("${foo}");
        assertNotFound("\\{foo}");
        assertNotFound("{foo\\}");
        assertFound("\\\\{foo}");
        assertFound("{foo\\\\}");
        assertNotFound("\\\\\\{foo}");
        assertNotFound("{foo\\\\\\}");
        assertFound("\\${foo}");
        assertFound("\\\\${foo}");
        assertFound("\\\\\\${foo}");
        assertFound("\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\${foo}");
        assertFound("{foo\\\\\\\\\\\\\\\\}");
    }

    private void assertFound(String msg) {
        assertTrue(messageParameter.matcher(msg).find());
    }

    private void assertNotFound(String msg) {
        assertFalse(messageParameter.matcher(msg).find());
    }

    @Test
    public void testGrowth() {
        assertEquals(5, messageParameter.getMaximumLength());
        assertMessageSizeYieldsMaximumSize(5, 5);
        assertMessageSizeYieldsMaximumSize(10, 6);
        assertMessageSizeYieldsMaximumSize(10, 5);
        assertMessageSizeYieldsMaximumSize(10, 9);
        assertMessageSizeYieldsMaximumSize(35, 31);
    }

    private void assertMessageSizeYieldsMaximumSize(int max, int msg) {
        messageParameter.matcher(new String(new byte[msg]));
        assertEquals(max, messageParameter.getMaximumLength());
        assertEquals(String.format(MESSAGE_PARAMETER_PATTERN, computeInjectedRepetition(max)),
            messageParameter.getPattern());
    }

    private int computeInjectedRepetition(int maximumLength) {
        return (maximumLength - 5) / 2;
    }
}
