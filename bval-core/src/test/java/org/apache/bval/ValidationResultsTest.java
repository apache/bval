/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.bval.model.MetaProperty;

/**
 * ValidationResults Tester.
 */
public class ValidationResultsTest extends TestCase {
    private ValidationResults results;

    public ValidationResultsTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        results = new ValidationResults();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testValidationResults() throws Exception {
        assertTrue(results.isEmpty());
        BeanValidationContext<ValidationResults> ctx = new BeanValidationContext<ValidationResults>(results);
        ctx.setBean(this);
        ctx.setMetaProperty(new MetaProperty());
        ctx.getMetaProperty().setName("prop");
        results.addError("test", ctx);
        assertFalse(results.isEmpty());
        assertTrue(results.hasErrorForReason("test"));
        assertTrue(results.hasError(this, "prop"));
        assertTrue(results.hasError(this, null));
        assertFalse(results.hasError(this, "prop2"));
    }

    public static Test suite() {
        return new TestSuite(ValidationResultsTest.class);
    }
}
