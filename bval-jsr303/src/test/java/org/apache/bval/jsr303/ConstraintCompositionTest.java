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

import junit.framework.Assert;
import junit.framework.TestCase;

import javax.validation.*;
import javax.validation.constraints.NotNull;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Locale;
import java.util.Set;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

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

    /**
     * Checks that the groups() value of the constraint annotations are
     * correctly set to the inherited ones.
     */
    public void testAnnotationGroupsAreInherited() {
        Validator validator = getValidator();
        
        // Check that the groups() value is right when querying the metadata
        ConstraintDescriptor<?> manNameDesc = getValidator().getConstraintsForClass(Man.class).getConstraintsForProperty("name").getConstraintDescriptors().iterator().next();
        ConstraintDescriptor<?> personNameDesc = manNameDesc.getComposingConstraints().iterator().next();
        ConstraintDescriptor<?> notNullDesc = personNameDesc.getComposingConstraints().iterator().next();
        Assert.assertEquals("There should only be 1 group", 1, manNameDesc.getGroups().size());
        Assert.assertTrue("Group1 should be present", manNameDesc.getGroups().contains(Group1.class));
        Assert.assertEquals("There should only be 1 group", 1, personNameDesc.getGroups().size());
        Assert.assertTrue("Group1 should be present", personNameDesc.getGroups().contains(Group1.class));
        Assert.assertEquals("There should only be 1 group", 1, personNameDesc.getGroups().size());
        Assert.assertTrue("Group1 should be present", notNullDesc.getGroups().contains(Group1.class));
        
        // Check that the groups() value is right when accesing it from an error
        Set<ConstraintViolation<Man>> violations = validator.validate(new Man(), Group1.class);
        Set<Class<?>> notNullGroups = violations.iterator().next().getConstraintDescriptor().getGroups();
        Assert.assertEquals("There should only be 1 group", 1, notNullGroups.size());
        Assert.assertTrue("Group1 should be the only group", notNullGroups.contains(Group1.class));
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
