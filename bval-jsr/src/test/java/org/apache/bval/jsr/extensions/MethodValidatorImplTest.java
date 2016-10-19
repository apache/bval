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
package org.apache.bval.jsr.extensions;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.executable.ExecutableValidator;

import org.apache.bval.jsr.ApacheValidationProvider;
import org.apache.bval.jsr.ClassValidator;
import org.apache.bval.jsr.extensions.ExampleMethodService.Person;
import org.junit.Ignore;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * MethodValidatorImpl Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>11/11/2009</pre>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
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
        assertSame(v, cv);
        assertSame(v, v.unwrap(Validator.class));
        assertNotNull(v.forExecutables());
    }

    public void testValidateMethodParameters() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);
        Method method = service.getClass().getMethod("concat", String.class, String.class);
        String[] params = new String[] { "Hello ", "world" };
        assertTrue(mv.validateParameters(service, method, params).isEmpty());

        params[0] = "";
        assertEquals(1, mv.validateParameters(service, method, params).size());

        params[1] = null;
        assertEquals(2, mv.validateParameters(service, method, params).size());
    }

    public void testValidateMoreMethodParameters() throws NoSuchMethodException {

        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);
        Method saveMethod = service.getClass().getMethod("save", String.class);

        String[] saveParams = new String[1];
        saveParams[0] = "abcd";
        assertTrue(mv.validateParameters(service, saveMethod, saveParams).isEmpty());

        saveParams[0] = "zzzz";
        assertEquals(1, mv.validateParameters(service, saveMethod, saveParams).size());

        Method echoMethod = service.getClass().getMethod("echo", String.class);

        String[] echoParams = new String[1];
        echoParams[0] = "hello";
        assertTrue(mv.validateParameters(service, echoMethod, echoParams).isEmpty());

        echoParams[0] = "h";
        assertEquals(1, mv.validateParameters(service, echoMethod, echoParams).size());

        echoParams[0] = null;
        assertEquals(1, mv.validateParameters(service, echoMethod, echoParams).size());
    }

    public void testValidateConstructorParameters() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);
        Constructor constructor = service.getClass().getConstructor(String.class, String.class);
        String[] params = new String[] {"Hello ", "world" };
        
        assertTrue(mv.<ExampleMethodService>validateConstructorParameters(constructor, params).isEmpty());

        params[0] = "";
        assertEquals(1, mv.validateConstructorParameters(constructor, params).size());

        params[1] = null;
        assertEquals(2, mv.validateConstructorParameters(constructor, params).size());
    }

    public void testValidateReturnValue() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);
        Method method = service.getClass().getMethod("concat", String.class, String.class);

        assertTrue(mv.validateReturnValue(service, method, "test").isEmpty());

        assertEquals(1, mv.validateReturnValue(service, method, "").size());
    }

    public void testValidateMoreReturnValue() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);
        Method echoMethod = service.getClass().getMethod("echo", String.class);

        String returnedValue = "a too long string";
        assertEquals(1, mv.validateReturnValue(service, echoMethod, returnedValue).size());

        returnedValue = null;
        assertEquals(1, mv.validateReturnValue(service, echoMethod, returnedValue).size());

        returnedValue = "valid";
        assertTrue(mv.validateReturnValue(service, echoMethod, returnedValue).isEmpty());
    }

    public void testValidateValidParam() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);

        Method personOp1 = service.getClass().getMethod("personOp1", Person.class);

        // Validate with invalid person
        Person p = new ExampleMethodService.Person();
        assertEquals("Expected 1 violation", 1, mv.validateParameters(service, personOp1, new Object[] { p }).size());

        // validate with valid person
        p.name = "valid name";
        assertTrue("No violations expected", mv.validateParameters(service, personOp1, new Object[] { p }).isEmpty());

        // validate with null person
        assertTrue("No violations expected", mv.validateParameters(service, personOp1, new Object[] { null }).isEmpty());
    }

    public void testValidateNotNullValidParam() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);

        Method personOp2 = service.getClass().getMethod("personOp2", Person.class);

        // Validate with null person
        assertEquals("Expected 1 violation", 1, mv.validateParameters(service, personOp2, new Object[] { null }).size());

        // Validate with invalid person
        Person p = new ExampleMethodService.Person();
        assertEquals("Expected 1 violation", 1, mv.validateParameters(service, personOp2, new Object[] { p }).size());

        // validate with valid person
        p.name = "valid name";
        assertTrue("No violations expected", mv.validateParameters(service, personOp2, new Object[] { p }).isEmpty());
    }

    /**
     * Validate a method defined in an interface using the following
     * combinations:
     * <ul>
     * <li>impl.class + impl.method</li>
     * <li>interface.class + interface.method</li>
     * <li>impl.class + interface.method</li>
     * <li>interface.class + impl.method</li>
     * </ul>
     */
    @Ignore("violates Liskov principle, forbidden by the spec - 4.5.5")
    public void validateImplementedMethod() throws NoSuchMethodException {
        UserMethodsImpl um = new UserMethodsImpl();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);

        Method classMethod = um.getClass().getMethod("findUser", String.class, String.class, Integer.class);
        UserMethods.class.getMethod("findUser", String.class, String.class, Integer.class);

        assertEquals("Invalid number of violations", 2,
            mv.validateParameters(um, classMethod, new Object[] { "", "valid", null }).size());
    }

    public static interface UserMethods {
        void findUser(String param1, String param2, Integer param3);
    }

    public static class UserMethodsImpl implements UserMethods {
        @Override
        public void findUser(@Size(min = 1) String param1, @NotNull String param2, @NotNull Integer param3) {
            return;
        }
    }

    private Validator getValidator() {
        return Validation.byProvider(ApacheValidationProvider.class).configure()
            .buildValidatorFactory().getValidator();
    }
}
