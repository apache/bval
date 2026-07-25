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
package org.apache.bval.jsr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.junit.Test;

/**
 * Regression tests guarding the path-handling optimizations (copy-on-write node sharing, lazy path
 * materialization) and the removal of the per-constraint validation de-duplication. These specifically exercise
 * cases where a corrupted/shared path node or an over-eager de-duplication would surface as a wrong or missing
 * violation path.
 */
public class PathSharingRegressionTest extends ValidationTestBase {

    // ---- beans -------------------------------------------------------------

    public static class Item {
        @NotBlank
        private final String code;

        public Item(String code) {
            this.code = code;
        }
    }

    public static class Container {
        @Valid
        private final List<Item> items = new ArrayList<>();
        @Valid
        private final Map<String, Item> byKey = new LinkedHashMap<>();
        @Valid
        private final Set<Item> set = new HashSet<>();
    }

    public static class Child {
        @NotBlank
        private final String name;

        public Child(String name) {
            this.name = name;
        }
    }

    public static class Parent {
        @Valid
        private Child a;
        @Valid
        private Child b;
    }

    // ---- container-element paths (copy-on-write / lazy path core risk) ------

    @Test
    public void listElementPathsAreDistinctAndCorrect() {
        final Container c = new Container();
        c.items.add(new Item("")); // items[0].code invalid
        c.items.add(new Item("ok"));
        c.items.add(new Item("")); // items[2].code invalid

        final Set<String> paths = pathsOf(validator.validate(c));

        assertEquals(new TreeSet<>(java.util.Arrays.asList("items[0].code", "items[2].code")), paths);
    }

    @Test
    public void mapValuePathsUseKeys() {
        final Container c = new Container();
        c.byKey.put("alpha", new Item(""));
        c.byKey.put("beta", new Item("ok"));
        c.byKey.put("gamma", new Item(""));

        final Set<String> paths = pathsOf(validator.validate(c));

        assertEquals(new TreeSet<>(java.util.Arrays.asList("byKey[alpha].code", "byKey[gamma].code")), paths);
    }

    @Test
    public void setElementsAreEachValidated() {
        final Container c = new Container();
        c.set.add(new Item("")); // invalid
        c.set.add(new Item("")); // distinct instance, also invalid
        c.set.add(new Item("ok"));

        // Set elements are indexless; each invalid element must still yield its own violation.
        assertEquals(2, validator.validate(c).size());
    }

    // ---- shared object reached via two paths (diamond) ---------------------

    @Test
    public void sharedObjectYieldsDistinctPathsForEachReference() {
        final Child shared = new Child(""); // invalid
        final Parent p = new Parent();
        p.a = shared;
        p.b = shared;

        final Set<String> paths = pathsOf(validator.validate(p));

        assertEquals(new TreeSet<>(java.util.Arrays.asList("a.name", "b.name")), paths);
    }

    // ---- de-duplication removal: no duplicate violations -------------------

    public interface G1 {
    }

    public interface G2 {
    }

    public static class MultiGroup {
        @NotNull(groups = { G1.class, G2.class })
        private String value;
    }

    @Test
    public void constraintInMultipleTargetedGroupsIsReportedOnce() {
        final Set<ConstraintViolation<MultiGroup>> violations =
            validator.validate(new MultiGroup(), G1.class, G2.class);
        assertEquals(1, violations.size());
        assertEquals("value", violations.iterator().next().getPropertyPath().toString());
    }

    // ---- custom violation node builders (mutableLeafNode) ------------------

    @Documented
    @Constraint(validatedBy = CustomPathValidator.class)
    @Target({ FIELD, METHOD, ANNOTATION_TYPE, TYPE_USE })
    @Retention(RUNTIME)
    public @interface CustomPath {
        String message() default "custom";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    /**
     * Builds violations whose paths use keyed and indexed iterable nodes, exercising the
     * {@code getLeafNode()}-mutating builders (now routed through {@code PathImpl#mutableLeafNode()}).
     */
    public static class CustomPathValidator implements ConstraintValidator<CustomPath, Object> {
        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("keyed")
                .addPropertyNode("prop").inIterable().atKey("myKey").addConstraintViolation();
            context.buildConstraintViolationWithTemplate("indexed")
                .addPropertyNode("other").inIterable().atIndex(3).addConstraintViolation();
            return false;
        }
    }

    public static class Holder {
        @CustomPath
        private final String field = "x";
    }

    @Test
    public void customBuiltViolationPathsWithKeyAndIndexAreCorrect() {
        final Set<String> paths = pathsOf(validator.validate(new Holder()));
        // per the Bean Validation node-builder API the key/index attaches to the node preceding the added one
        assertTrue("expected keyed path, got " + paths, paths.contains("field[myKey].prop"));
        assertTrue("expected indexed path, got " + paths, paths.contains("field[3].other"));
        assertEquals(2, paths.size());
    }

    // ---- stability under repeated validation with a reused validator -------

    @Test
    public void repeatedValidationIsStable() {
        final Container c = new Container();
        c.items.add(new Item(""));
        c.items.add(new Item("ok"));
        c.byKey.put("k", new Item(""));

        final Set<String> expected = pathsOf(validator.validate(c));
        assertNotNull(expected);
        for (int i = 0; i < 100; i++) {
            assertEquals("path set changed on iteration " + i, expected, pathsOf(validator.validate(c)));
        }
    }

    // ---- helper ------------------------------------------------------------

    private static <T> Set<String> pathsOf(Set<ConstraintViolation<T>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString())
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
