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
package org.apache.bval.jsr303.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.validation.ValidationException;

import org.apache.bval.jsr303.util.ConverterUtils;

/**
 * ConverterUtils Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>11/25/2009</pre>
 */
public class ConverterUtilsTest extends TestCase {
    public ConverterUtilsTest(String name) {
        super(name);
    }

    public void testLong() {
        long lng = (Long) ConverterUtils.fromStringToType("444", long.class);
        assertEquals(444L, lng);

        try {
            ConverterUtils.fromStringToType("hallo", long.class);
            fail();
        } catch (ValidationException ve) {
            // yes
        }
    }

    public void testClass() {
        assertEquals(getClass(),
              ConverterUtils.fromStringToType(getClass().getName(), Class.class));
    }

    public void testEnum() {
        Thread.State state = (Thread.State) ConverterUtils
              .fromStringToType(Thread.State.TERMINATED.name(), Thread.State.class);
        assertEquals(Thread.State.TERMINATED, state);
    }

    public static Test suite() {
        return new TestSuite(ConverterUtilsTest.class);
    }
}
