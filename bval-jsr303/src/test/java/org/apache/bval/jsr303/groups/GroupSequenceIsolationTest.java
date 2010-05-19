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
package org.apache.bval.jsr303.groups;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.bval.jsr303.DefaultMessageInterpolator;

import javax.validation.*;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import java.util.Locale;
import java.util.Set;

/**
 * Additional tests to check the correct processing of {@link GroupSequence}s
 * by the validator.
 * 
 * @author Carlos Vara
 */
public class GroupSequenceIsolationTest extends TestCase {
    
    static ValidatorFactory factory;

    static {
        factory = Validation.buildDefaultValidatorFactory();
        ((DefaultMessageInterpolator)factory.getMessageInterpolator()).setLocale(Locale.ENGLISH);
    }

    private Validator getValidator() {
        return factory.getValidator();
    }

    
    /**
     * When validating the {@link Default} group in a bean whose class doesn't
     * define a {@link GroupSequence}, all the classes in the hierarchy must be
     * checked for group sequence definitions and they must be evaluated in
     * order for the constraints defined on those classes.
     */
    public void testGroupSequencesInHierarchyClasses() {
        Validator validator = getValidator();
        
        HolderWithNoGS h = new HolderWithNoGS();
        Set<ConstraintViolation<HolderWithNoGS>> violations;
        
        violations = validator.validate(h);
        Assert.assertEquals("Unexpected number of violations", 2, violations.size());
        for ( ConstraintViolation<HolderWithNoGS> violation : violations ) {
            boolean good = violation.getPropertyPath().toString().equals("a1");
            good |= violation.getPropertyPath().toString().equals("b2");
            Assert.assertTrue("Wrong constraint", good);
        }

        h.a1 = "good";
        violations = validator.validate(h);
        Assert.assertEquals("Unexpected number of violations", 2, violations.size());
        for ( ConstraintViolation<HolderWithNoGS> violation : violations ) {
            boolean good = violation.getPropertyPath().toString().equals("a2");
            good |= violation.getPropertyPath().toString().equals("b2");
            Assert.assertTrue("Wrong constraint", good);
        }
        
        h.b2 = "good";
        violations = validator.validate(h);
        Assert.assertEquals("Unexpected number of violations", 2, violations.size());
        for ( ConstraintViolation<HolderWithNoGS> violation : violations ) {
            boolean good = violation.getPropertyPath().toString().equals("a2");
            good |= violation.getPropertyPath().toString().equals("b1");
            Assert.assertTrue("Wrong constraint", good);
        }
        
        h.b1 = "good";
        violations = validator.validate(h);
        Assert.assertEquals("Unexpected number of violations", 1, violations.size());
        for ( ConstraintViolation<HolderWithNoGS> violation : violations ) {
            boolean good = violation.getPropertyPath().toString().equals("a2");
            Assert.assertTrue("Wrong constraint", good);
        }
    }
    
    /**
     * When validating the {@link Default} group in a bean whose class defines
     * a group sequence, that group sequence is used for all the constraints.
     */
    public void testGroupSequenceOfBeanClass() {
        Validator validator = getValidator();
        
        HolderWithGS h = new HolderWithGS();
        Set<ConstraintViolation<HolderWithGS>> violations;
        
        violations = validator.validate(h);
        Assert.assertEquals("Unexpected number of violations", 1, violations.size());
        for ( ConstraintViolation<HolderWithGS> violation : violations ) {
            boolean good = violation.getPropertyPath().toString().equals("a1");
            Assert.assertTrue("Wrong constraint", good);
        }
        
        h.a1 = "good";
        violations = validator.validate(h);
        Assert.assertEquals("Unexpected number of violations", 2, violations.size());
        for ( ConstraintViolation<HolderWithGS> violation : violations ) {
            boolean good = violation.getPropertyPath().toString().equals("a2");
            good |= violation.getPropertyPath().toString().equals("b2");
            Assert.assertTrue("Wrong constraint", good);
        }
        
        h.a2 = "good";
        h.b2 = "good";
        violations = validator.validate(h);
        Assert.assertEquals("Unexpected number of violations", 1, violations.size());
        for ( ConstraintViolation<HolderWithGS> violation : violations ) {
            boolean good = violation.getPropertyPath().toString().equals("b1");
            Assert.assertTrue("Wrong constraint", good);
        }
    }
    
    @GroupSequence({GroupA1.class, A.class})
    public static class A {
        @NotNull(groups={GroupA1.class})
        public String a1;
        @NotNull
        public String a2;
    }
    
    public static interface GroupA1 {
    }
    
    @GroupSequence({B.class, GroupB1.class})
    public static class B extends A {
        @NotNull(groups={GroupB1.class})
        public String b1;
        @NotNull
        public String b2;
    }
    
    public static interface GroupB1 {
        
    }
    
    // No group sequence definition
    public static class HolderWithNoGS extends B {
        
    }
    
    @GroupSequence({GroupA1.class, HolderWithGS.class, GroupB1.class})
    public static class HolderWithGS extends B {
        
    }
}
