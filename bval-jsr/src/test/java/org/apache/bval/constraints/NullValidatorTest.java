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
package org.apache.bval.constraints;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Checks correct behaviour of {@link NullValidator}.
 * <p>
 * Per the spec:
 * <ul>
 * <li>The annotated element must be null.</li>
 * </ul>
 * 
 * TODO: Mock context and verify that it's not used during validation.
 * 
 * @see "bean_validation-1_0_CR1-pfd-spec#Chapter6#Example6.1"
 * 
 * @author Carlos Vara
 */
public class NullValidatorTest {

    /**
     * Test {@link AssertFalseValidator} with null context.
     */
    @Test
    public void testNullValidator() {
        NullValidator nv = new NullValidator();
        assertTrue("Null value validation must succeed", nv.isValid(null, null));
        assertFalse("Non null value validation must fail", nv.isValid("hello", null));
    }

}
