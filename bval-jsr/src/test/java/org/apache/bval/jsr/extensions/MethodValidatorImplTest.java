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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.bval.jsr.ApacheValidationProvider;
import org.apache.bval.jsr.ClassValidator;
import org.apache.bval.jsr.extensions.ExampleMethodService.Person;
import org.junit.Ignore;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.executable.ExecutableValidator;
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
        assertTrue(v == cv);
        assertTrue(v == v.unwrap(Validator.class));
        ExecutableValidator mv = v.forExecutables();
        assertNotNull(mv);
    }

    public void testValidateMethodParameters() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);
        Method method = service.getClass().getMethod("concat", new Class[] { String.class, String.class });
        String[] params = new String[2];
        params[0] = "Hello ";
        params[1] = "world";
        Set results = mv.validateParameters(service, method, params);
        assertEquals(true, results.isEmpty());

        params[0] = "";
        results = mv.validateParameters(service, method, params);
        assertEquals(1, results.size());

        params[1] = null;
        results = mv.validateParameters(service, method, params);
        assertEquals(2, results.size());
    }

    public void testValidateMoreMethodParameters() throws NoSuchMethodException {

        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);
        Method saveMethod = service.getClass().getMethod("save", new Class[] { String.class });

        String[] saveParams = new String[1];
        saveParams[0] = "abcd";

        Set results = mv.validateParameters(service, saveMethod, saveParams);
        assertTrue(results.isEmpty());

        saveParams[0] = "zzzz";
        results = mv.validateParameters(service, saveMethod, saveParams);
        assertEquals(1, results.size());

        Method echoMethod = service.getClass().getMethod("echo", new Class[] { String.class });

        String[] echoParams = new String[1];
        echoParams[0] = "hello";

        results = mv.validateParameters(service, echoMethod, echoParams);
        assertTrue(results.isEmpty());

        echoParams[0] = "h";
        results = mv.validateParameters(service, echoMethod, echoParams);
        assertEquals(1, results.size());

        echoParams[0] = null;
        results = mv.validateParameters(service, echoMethod, echoParams);
        assertEquals(1, results.size());

    }

    public void testValidateConstructorParameters() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);
        Constructor constructor = service.getClass().getConstructor(String.class, String.class);
        String[] params = new String[2];
        params[0] = "Hello ";
        params[1] = "world";
        Set results = mv.validateConstructorParameters(constructor, params);
        assertEquals(true, results.isEmpty());

        params[0] = "";
        results = mv.validateConstructorParameters(constructor, params);
        assertEquals(1, results.size());

        params[1] = null;
        results = mv.validateConstructorParameters(constructor, params);
        assertEquals(2, results.size());
    }

    public void testValidateReturnValue() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);
        Method method = service.getClass().getMethod("concat", new Class[] { String.class, String.class });

        Set results;

        results = mv.validateReturnValue(service, method, "test");
        assertEquals(true, results.isEmpty());

        results = mv.validateReturnValue(service, method, "");
        assertEquals(1, results.size());
    }

    public void testValidateMoreReturnValue() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);
        Method echoMethod = service.getClass().getMethod("echo", new Class[] { String.class });

        String returnedValue = "a too long string";
        Set results = mv.validateReturnValue(service, echoMethod, returnedValue);
        assertEquals(1, results.size());

        returnedValue = null;
        results = mv.validateReturnValue(service, echoMethod, returnedValue);
        assertEquals(1, results.size());

        returnedValue = "valid";
        results = mv.validateReturnValue(service, echoMethod, returnedValue);
        assertTrue(results.isEmpty());
    }

    public void testValidateValidParam() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);

        Method personOp1 = service.getClass().getMethod("personOp1", new Class[] { Person.class });

        // Validate with invalid person
        Person p = new ExampleMethodService.Person();
        Set<?> results = mv.validateParameters(service, personOp1, new Object[] { p });
        assertEquals("Expected 1 violation", 1, results.size());

        // validate with valid person
        p.name = "valid name";
        results = mv.validateParameters(service, personOp1, new Object[] { p });
        assertTrue("No violations expected", results.isEmpty());

        // validate with null person
        results = mv.validateParameters(service, personOp1, new Object[] { null });
        assertTrue("No violations expected", results.isEmpty());
    }

    public void testValidateNotNullValidParam() throws NoSuchMethodException {
        ExampleMethodService service = new ExampleMethodService();
        ExecutableValidator mv = getValidator().unwrap(ExecutableValidator.class);

        Method personOp2 = service.getClass().getMethod("personOp2", new Class[] { Person.class });

        // Validate with null person
        Set<?> results = mv.validateParameters(service, personOp2, new Object[] { null });
        assertEquals("Expected 1 violation", 1, results.size());

        // Validate with invalid person
        Person p = new ExampleMethodService.Person();
        results = mv.validateParameters(service, personOp2, new Object[] { p });
        assertEquals("Expected 1 violation", 1, results.size());

        // validate with valid person
        p.name = "valid name";
        results = mv.validateParameters(service, personOp2, new Object[] { p });
        assertTrue("No violations expected", results.isEmpty());
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

        Method classMethod =
            um.getClass().getMethod("findUser", new Class[] { String.class, String.class, Integer.class });
        Method ifaceMethod =
            UserMethods.class.getMethod("findUser", new Class[] { String.class, String.class, Integer.class });

        Set<?> results;

        // Validate from class (should create violations)
        results = mv.validateParameters(um, classMethod, new Object[] { "", "valid", null });
        assertEquals("Invalid number of violations", 2, results.size());
    }

    public static interface UserMethods {
        void findUser(String param1, String param2, Integer param3);
    }

    public static class UserMethodsImpl implements UserMethods {
        // @Override - not allowed in 1.5 for Interface methods
        public void findUser(@Size(min = 1) String param1, @NotNull String param2, @NotNull Integer param3) {
            return;
        }
    }

    private Validator getValidator() {
        return Validation.byProvider(ApacheValidationProvider.class).configure()
            /*
             * .addProperty(ApacheValidatorConfiguration.Properties.
             * METABEAN_FACTORY_CLASSNAMES,
             * MethodValidatorMetaBeanFactory.class.getName())
             */
            .buildValidatorFactory().getValidator();
    }
}
