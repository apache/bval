/**
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
package org.apache.bval.jsr303.extensions;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.bval.jsr303.ApacheValidatorFactory;
import org.apache.bval.jsr303.ClassValidator;

import javax.validation.Validator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * MethodValidatorImpl Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>11/11/2009</pre>
 */
public class MethodValidatorImplTest extends TestCase {
    public MethodValidatorImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MethodValidatorImplTest.class);
    }

    public void testUnwrap() {
        Validator v = getValidator();
        ClassValidator cv = v.unwrap(ClassValidator.class);
        assertTrue(v == cv);
        assertTrue(v == v.unwrap(Validator.class));
        MethodValidatorImpl mvi = v.unwrap(MethodValidatorImpl.class);
        assertNotNull(mvi);
        MethodValidator mv = v.unwrap(MethodValidator.class);
        assertNotNull(mv);
        assertTrue(mv == mv.unwrap(MethodValidatorImpl.class));
        assertTrue(mv == mv.unwrap(ClassValidator.class));
    }

    public void testValidateMethodParameters() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        MethodValidator mv = getValidator().unwrap(MethodValidator.class);
        Method method =
              service.getClass().getMethod("concat", new Class[]{String.class, String.class});
        String[] params = new String[2];
        params[0] = "Hello ";
        params[1] = "world";
        Set results = mv.validateParameters(service.getClass(), method, params);
        assertEquals(true, results.isEmpty());

        params[0] = "";
        results = mv.validateParameters(service.getClass(), method, params);
        assertEquals(1, results.size());

        params[1] = null;
        results = mv.validateParameters(service.getClass(), method, params);
        assertEquals(2, results.size());

        results = mv.validateParameter(service.getClass(), method,  params[0], 0);
        assertEquals(1, results.size());

        results = mv.validateParameter(service.getClass(), method,  "ok", 0);
        assertEquals(0, results.size());
    }
    
    public void testValidateMoreMethodParameters() throws NoSuchMethodException {
    	
    	ExampleMethodService service = new ExampleMethodService();
    	MethodValidator mv = getValidator().unwrap(MethodValidator.class);
    	Method saveMethod = service.getClass().getMethod("save", new Class[]{String.class});
    	
    	String[] saveParams = new String[1];
    	saveParams[0] = "abcd";
    	
    	Set results = mv.validateParameters(service.getClass(), saveMethod, saveParams);
    	assertTrue(results.isEmpty());
    	
    	saveParams[0] = "zzzz";
    	results = mv.validateParameters(service.getClass(), saveMethod, saveParams);
    	assertEquals(1, results.size());
    	
    	Method echoMethod = service.getClass().getMethod("echo", new Class[]{String.class});
    	
    	String[] echoParams = new String[1];
    	echoParams[0] = "hello";
    	
    	results = mv.validateParameters(service.getClass(), echoMethod, echoParams);
    	assertTrue(results.isEmpty());
    	
    	echoParams[0] = "h";
    	results = mv.validateParameters(service.getClass(), echoMethod, echoParams);
    	assertEquals(1, results.size());
    	
    	echoParams[0] = null;
    	results = mv.validateParameters(service.getClass(), echoMethod, echoParams);
    	assertEquals(1, results.size());
    	
    }

    public void testValidateConstructorParameters() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        MethodValidator mv = getValidator().unwrap(MethodValidator.class);
        Constructor constructor =
              service.getClass().getConstructor(String.class, String.class);
        String[] params = new String[2];
        params[0] = "Hello ";
        params[1] = "world";
        Set results = mv.validateParameters(service.getClass(), constructor, params);
        assertEquals(true, results.isEmpty());

        params[0] = "";
        results = mv.validateParameters(service.getClass(), constructor, params);
        assertEquals(1, results.size());

        params[1] = null;
        results = mv.validateParameters(service.getClass(), constructor, params);
        assertEquals(2, results.size());

        results = mv.validateParameter(service.getClass(), constructor,  params[0], 0);
        assertEquals(1, results.size());

        results = mv.validateParameter(service.getClass(), constructor,  "ok", 0);
        assertEquals(0, results.size());
    }

    public void testValidateReturnValue() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        MethodValidator mv = getValidator().unwrap(MethodValidator.class);
        Method method =
              service.getClass().getMethod("concat", new Class[]{String.class, String.class});
        Set results;
        results = mv.validateReturnedValue(service.getClass(), method, "test");
        assertEquals(true, results.isEmpty());

        results = mv.validateReturnedValue(service.getClass(), method, "");
        assertEquals(1, results.size());
    }
    
    public void testValidateMoreReturnValue() throws NoSuchMethodException {
    	
    	ExampleMethodService service = new ExampleMethodService();
    	MethodValidator mv = getValidator().unwrap(MethodValidator.class);
    	Method echoMethod = service.getClass().getMethod("echo", new Class[]{String.class});
    	
    	String returnedValue = "a too long string";
    	Set results = mv.validateReturnedValue(service.getClass(), echoMethod, returnedValue);
    	assertEquals(1, results.size());
    	
    	returnedValue = null;
    	results = mv.validateReturnedValue(service.getClass(), echoMethod, returnedValue);
    	assertEquals(1, results.size());
    	
    	returnedValue = "valid";
    	results = mv.validateReturnedValue(service.getClass(), echoMethod, returnedValue);
    	assertTrue(results.isEmpty());
    	
    	
    }

    private Validator getValidator() {
        return ApacheValidatorFactory.getDefault().getValidator();
    }
}
