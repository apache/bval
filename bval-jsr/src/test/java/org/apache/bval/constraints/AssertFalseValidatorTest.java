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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Checks correct behaviour of {@link AssertFalseValidator}.
 * <p>
 * Per the spec:
 * <ul>
 * <li>The annotated element must be false.</li>
 * <li><code>null</code> elements are considered valid.</li>
 * </ul>
 * 
 * TODO: Mock context and verify that it's not used during validation.
 * 
 * @see "bean_validation-1_0_CR1-pfd-spec#Chapter6#Example6.4"
 * 
 * @author Carlos Vara
 */
public class AssertFalseValidatorTest extends TestCase {
	
    public static Test suite() {
        return new TestSuite(AssertFalseValidatorTest.class);
    }
    
    public AssertFalseValidatorTest(String name) {
    	super(name);
    }
    
    /**
     * Test {@link AssertFalseValidator} with <code>null</code> context.
     */
    public void testAssertFalseValidator() {
    	AssertFalseValidator afv = new AssertFalseValidator();
    	assertFalse("True value validation must fail", afv.isValid(true, null));
    	assertTrue("False value validation must succeed", afv.isValid(false, null));
    	assertTrue("Null value validation must succeed", afv.isValid(null, null));
    }

}
