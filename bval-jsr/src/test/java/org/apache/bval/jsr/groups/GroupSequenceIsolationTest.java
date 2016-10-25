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
package org.apache.bval.jsr.groups;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.GroupSequence;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.apache.bval.jsr.ValidationTestBase;
import org.junit.Test;

/**
 * Additional tests to check the correct processing of {@link GroupSequence}s
 * by the validator.
 * 
 * @author Carlos Vara
 */
public class GroupSequenceIsolationTest extends ValidationTestBase {

    /**
     * When validating the {@link Default} group in a bean whose class doesn't
     * define a {@link GroupSequence}, all the classes in the hierarchy must be
     * checked for group sequence definitions and they must be evaluated in
     * order for the constraints defined on those classes.
     */
    @Test
    public void testGroupSequencesInHierarchyClasses() {
        HolderWithNoGS h = new HolderWithNoGS();

        assertEquals(set("a1", "b2"), violationPaths(validator.validate(h)));

        h.a1 = "good";
        assertEquals(set("a2", "b2"), violationPaths(validator.validate(h)));

        h.b2 = "good";
        assertEquals(set("a2", "b1"), violationPaths(validator.validate(h)));

        h.b1 = "good";
        assertEquals(set("a2"), violationPaths(validator.validate(h)));
    }

    /**
     * When validating the {@link Default} group in a bean whose class defines
     * a group sequence, that group sequence is used for all the constraints.
     */
    @Test
    public void testGroupSequenceOfBeanClass() {
        HolderWithGS h = new HolderWithGS();

        assertEquals(Collections.singleton("a1"), violationPaths(validator.validate(h)));

        h.a1 = "good";
        assertEquals(set("a2", "b2"), violationPaths(validator.validate(h)));

        h.a2 = "good";
        h.b2 = "good";
        assertEquals(Collections.singleton("b1"), violationPaths(validator.validate(h)));
    }

    private static <T> Set<T> set(T... elements) {
        return new HashSet<T>(Arrays.asList(elements));
    }

    private static Set<String> violationPaths(Set<? extends ConstraintViolation<?>> violations) {
        if (violations == null || violations.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<String> result = new LinkedHashSet<String>(violations.size());
        for (ConstraintViolation<?> constraintViolation : violations) {
            result.add(constraintViolation.getPropertyPath().toString());
        }
        return result;
    }

    @GroupSequence({ GroupA1.class, A.class })
    public static class A {
        @NotNull(groups = { GroupA1.class })
        public String a1;
        @NotNull
        public String a2;
    }

    public interface GroupA1 {
    }

    @GroupSequence({ B.class, GroupB1.class })
    public static class B extends A {
        @NotNull(groups = { GroupB1.class })
        public String b1;
        @NotNull
        public String b2;
    }

    public interface GroupB1 {
    }

    // No group sequence definition
    public static class HolderWithNoGS extends B {
    }

    @GroupSequence({ GroupA1.class, HolderWithGS.class, GroupB1.class })
    public static class HolderWithGS extends B {
    }
}
