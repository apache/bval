/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.bval.jsr303;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Locale;
import java.util.Set;

import javax.validation.Constraint;
import javax.validation.ConstraintViolation;
import javax.validation.Payload;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Checks that groups are correctly inherited from the root constraint to its
 * compositing constraints.
 * 
 * @author Carlos Vara
 */
public class ConstraintCompositionTest extends TestCase {

    static ValidatorFactory factory;

    static {
        factory = Validation.buildDefaultValidatorFactory();
        ((DefaultMessageInterpolator) factory.getMessageInterpolator()).setLocale(Locale.ENGLISH);
    }

    private Validator getValidator() {
        return factory.getValidator();
    }
    
    /**
     * Check correct group inheritance on constraint composition on a 1 level
     * hierarchy.
     */
    public void test1LevelInheritance() {
        Validator validator = getValidator();
        Set<ConstraintViolation<Person>> violations = validator.validate(new Person());
        
        Assert.assertEquals("Wrong number of violations detected", 1, violations.size());
        String msg = violations.iterator().next().getMessage();
        Assert.assertEquals("Incorrect violation message", "A person needs a non null name", msg);
        
        violations = validator.validate(new Person(), Group1.class);
        Assert.assertEquals("Wrong number of violations detected", 0, violations.size());
    }
    
    /**
     * Check correct group inheritance on constraint composition on a 2 level
     * hierarchy.
     */
    public void test2LevelInheritance() {
        Validator validator = getValidator();
        Set<ConstraintViolation<Man>> violations = validator.validate(new Man());
        
        Assert.assertEquals("Wrong number of violations detected", 0, violations.size());
        
        violations = validator.validate(new Man(), Group1.class);
        Assert.assertEquals("Wrong number of violations detected", 1, violations.size());
        String msg = violations.iterator().next().getMessage();
        Assert.assertEquals("Incorrect violation message", "A person needs a non null name", msg);
    }
    
    
    
    public static class Person {
        @PersonName
        String name;
    }
    
    public static class Man {
        @ManName(groups={Group1.class})
        String name;
    }
    
    @NotNull(message="A person needs a non null name", groups={Group1.class})
    @Constraint(validatedBy = {})
    @Documented
    @Target({ METHOD, FIELD, TYPE })
    @Retention(RUNTIME)
    public static @interface PersonName {
        String message() default "Wrong person name";
        Class<?>[] groups() default { };
        Class<? extends Payload>[] payload() default {};
    }
    
    @PersonName(groups={Group2.class})
    @Constraint(validatedBy = {})
    @Documented
    @Target({ METHOD, FIELD, TYPE })
    @Retention(RUNTIME)
    public static @interface ManName {
        String message() default "Wrong man name";
        Class<?>[] groups() default { };
        Class<? extends Payload>[] payload() default {};
    }
    
    public static interface Group1 {
    }
    
    public static interface Group2 {
    }
    
}
