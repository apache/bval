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
package org.apache.bval.routines;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class EMailValidationUtilsTest {
    private static final String[] TOKENS =
        { "ab", "x", ".", "@", "[", "]", "1", "12", "123", "1234", " ", "é", "\\", "\"", ";", "\n", "\u007f" };

    @Test
    public void matchesLikeDefaultPattern() {
        final String[] examples = { "a@b", "a.b@c.d", "a..b@c", "a.@b", ".a@b", "@b", "a@", "a@b.", "a@.b",
            "a@[1.2.3.4]", "a@[1.2.3.1234]", "a@[1.2.3]", "a@[1.2.3.4]x", "a@b@c", "ä@ö.de", "a b@c",
            "a@b c", "a\"b@c", "a@[123.45.6.789]" };
        for (String example : examples) {
            assertMatchesLikePattern(example);
        }
        final Random random = new Random(42);
        for (int i = 0; i < 100_000; i++) {
            final StringBuilder candidate = new StringBuilder();
            for (int j = 1 + random.nextInt(12); j > 0; j--) {
                candidate.append(TOKENS[random.nextInt(TOKENS.length)]);
            }
            assertMatchesLikePattern(candidate.toString());
        }
    }

    private static void assertMatchesLikePattern(String candidate) {
        assertEquals(candidate, EMailValidationUtils.isValid(candidate, EMailValidationUtils.DEFAULT_EMAIL_PATTERN),
            EMailValidationUtils.isValid(candidate));
    }
}
