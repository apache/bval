/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.bval.jsr;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.validation.MessageInterpolator;
import javax.validation.Validator;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Pattern;
import javax.validation.metadata.ConstraintDescriptor;

import org.apache.bval.constraints.NotEmpty;
import org.apache.bval.jsr.example.Author;
import org.apache.bval.jsr.example.PreferredGuest;
import org.junit.Before;
import org.junit.Test;

/**
 * MessageResolverImpl Tester.
 */
public class DefaultMessageInterpolatorTest {
    private static Predicate<ConstraintDescriptor<?>> forConstraintType(Class<? extends Annotation> type) {
        return d -> Objects.equals(type, d.getAnnotation().annotationType());
    }

    private static MessageInterpolator.Context context(Object validatedValue, Supplier<ConstraintDescriptor<?>> descriptor){
        return new MessageInterpolator.Context() {
            
            @Override
            public <T> T unwrap(Class<T> type) {
                return null;
            }
            
            @Override
            public Object getValidatedValue() {
                return validatedValue;
            }
            
            @Override
            public ConstraintDescriptor<?> getConstraintDescriptor() {
                return descriptor.get();
            }
        };
    }

    private DefaultMessageInterpolator interpolator;
    private Validator validator;

    @Before
    public void setUp() throws Exception {
        interpolator = new DefaultMessageInterpolator();
        interpolator.setLocale(Locale.ENGLISH);
        validator = ApacheValidatorFactory.getDefault().getValidator();
    }

    @Test
    public void testInterpolateFromValidationResources() {
        String msg = interpolator.interpolate("{validator.creditcard}",
            context("12345678",
                () -> validator.getConstraintsForClass(PreferredGuest.class)
                    .getConstraintsForProperty("guestCreditCardNumber").getConstraintDescriptors().stream()
                    .filter(forConstraintType(Digits.class)).findFirst()
                    .orElseThrow(() -> new AssertionError("expected constraint missing"))));

        assertEquals("credit card is not valid", msg);
    }

    @Test
    public void testInterpolateFromDefaultResources() {
        String msg = interpolator.interpolate("{org.apache.bval.constraints.NotEmpty.message}",
            context("",
                () -> validator.getConstraintsForClass(Author.class).getConstraintsForProperty("lastName")
                    .getConstraintDescriptors().stream().filter(forConstraintType(NotEmpty.class)).findFirst()
                    .orElseThrow(() -> new AssertionError("expected constraint missing"))));

        assertEquals("may not be empty", msg);
    }

    /**
     * Checks that strings containing special characters are correctly
     * substituted when interpolating.
     */
    @Test
    public void testReplacementWithSpecialChars() {
        // Try to interpolate an annotation attribute containing $
        String idNumberResult = this.interpolator.interpolate("Id number should match {regexp}",
            context("12345678",
                () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("idNumber")
                    .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
                    .orElseThrow(() -> new AssertionError("expected constraint missing"))));

        assertEquals("Incorrect message interpolation when $ is in an attribute", "Id number should match ....$",
            idNumberResult);

        // Try to interpolate an annotation attribute containing \
        String otherIdResult = this.interpolator.interpolate("Other id should match {regexp}",
            context("12345678",
                () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("otherId")
                    .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
                    .orElseThrow(() -> new AssertionError("expected constraint missing"))));

        assertEquals("Incorrect message interpolation when \\ is in an attribute value", "Other id should match .\\n",
            otherIdResult);
    }

    public static class Person {

        @Pattern(message = "Id number should match {regexp}", regexp = "....$")
        public String idNumber;

        @Pattern(message = "Other id should match {regexp}", regexp = ".\\n")
        public String otherId;

    }
}
