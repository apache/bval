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
 * Checks correct behaviour of {@link AssertTrueValidator}.
 * <p>
 * Per the spec:
 * <ul>
 * <li>The annotated element must be true.</li>
 * <li><code>null</code> elements are considered valid.</li>
 * </ul>
 * 
 * TODO: Mock context and verify that it's not used during validation.
 * 
 * @see "bean_validation-1_0_CR1-pfd-spec#Chapter6#Example6.3"
 * 
 * @author Carlos Vara
 */
public class AssertTrueValidatorTest {
	
    /**
     * Test {@link AssertTrueValidator} with null context.
     */
    @Test
    public void testAssertTrueValidator() {
    	AssertTrueValidator atv = new AssertTrueValidator();
    	assertTrue("True value validation must succeed", atv.isValid(true, null));
    	assertFalse("False value validation must fail", atv.isValid(false, null));
    	assertTrue("Null value validation must succeed", atv.isValid(null, null));
    }

}
