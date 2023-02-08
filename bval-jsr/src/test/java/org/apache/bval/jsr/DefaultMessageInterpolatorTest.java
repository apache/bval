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

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.el.ExpressionFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.metadata.ConstraintDescriptor;

import org.apache.bval.constraints.NotEmpty;
import org.apache.bval.jsr.example.Author;
import org.apache.bval.jsr.example.PreferredGuest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

/**
 * MessageResolverImpl Tester.
 */
@RunWith(Parameterized.class)
public class DefaultMessageInterpolatorTest {
    @Parameters(name="{0}")
    public static List<Object[]> generateParameters(){
        return Arrays.asList(new Object[] { "default", null },
            new Object[] { "ri", "org.glassfish.expressly.ExpressionFactoryImpl" },
            new Object[] { "tomcat", "org.apache.el.ExpressionFactoryImpl" },
            new Object[] { "invalid", "java.lang.String" });
    }

    @AfterClass
    public static void cleanup() {
        System.clearProperty(ExpressionFactory.class.getName());
    }

    private static Predicate<ConstraintDescriptor<?>> forConstraintType(Class<? extends Annotation> type) {
        return d -> Objects.equals(type, d.getAnnotation().annotationType());
    }

    private String elImpl;
    private String elFactory;
    private DefaultMessageInterpolator interpolator;
    private Validator validator;
    private boolean elAvailable;
    private ClassLoader originalClassLoader;

    public DefaultMessageInterpolatorTest(String elImpl, String elFactory) {
        this.elImpl = elImpl;
        this.elFactory = elFactory;
    }

    @Before
    public void setUp() throws Exception {
        // store and replace CCL to sidestep EL factory caching
        originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[] {}, originalClassLoader));
        
        try {
            Class<?> elFactoryClass;
            if (elFactory == null) {
                elFactoryClass = ExpressionFactory.class;
                System.clearProperty(ExpressionFactory.class.getName());
            } else {
                elFactoryClass = Class.forName(elFactory);
                System.setProperty(ExpressionFactory.class.getName(), elFactory);
            }
            elAvailable = ExpressionFactory.class.isAssignableFrom(elFactoryClass);
        } catch (Exception e) {
            elAvailable = false;
        }
        interpolator = new DefaultMessageInterpolator();
        interpolator.setLocale(Locale.ENGLISH);
        validator = ApacheValidatorFactory.getDefault().getValidator();
    }

    @After
    public void tearDownEL() {
        assumeTrue(originalClassLoader != null);
        Thread.currentThread().setContextClassLoader(originalClassLoader);
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

    @Test
    public void testRecursiveInterpolation() {
        String msg = this.interpolator.interpolate("{recursive.interpolation.1}",
            context("12345678",
                () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("idNumber")
                    .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
                    .orElseThrow(() -> new AssertionError("expected constraint missing"))));

        assertEquals("must match \"....$\"", msg);
    }

    @Test
    public void testNoELAvailable() {
        assumeThat(elImpl, equalTo("invalid"));
        assertFalse(elAvailable);
        
        ApacheMessageContext context = context("12345678",
            () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("idNumber")
            .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
            .orElseThrow(() -> new AssertionError("expected constraint missing")));

        when(context
            .getConfigurationProperty(ApacheValidatorConfiguration.Properties.CUSTOM_TEMPLATE_EXPRESSION_EVALUATION))
                .thenAnswer(invocation -> Boolean.toString(true));

        assertEquals("${regexp.charAt(4)}", interpolator.interpolate("${regexp.charAt(4)}",
            context));
    }

    @Test
    public void testDisallowCustomTemplateExpressionEvaluationByDefault() {
        assumeTrue(elAvailable);

        assertEquals("${regexp.charAt(4)}", interpolator.interpolate("${regexp.charAt(4)}",
            context("12345678",
                () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("idNumber")
                .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
                .orElseThrow(() -> new AssertionError("expected constraint missing")))));
    }

    @Test
    public void testExpressionLanguageEvaluation() {
        assumeTrue(elAvailable);
        
        final MessageInterpolator.Context context = context("12345678",
            () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("anotherValue")
            .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
            .orElseThrow(() -> new AssertionError("expected constraint missing")));
        
        assertEquals("Another value should match ....$",
            interpolator.interpolate(context.getConstraintDescriptor().getMessageTemplate(), context));
    }

    @Test
    public void testMixedEvaluation() {
        assumeTrue(elAvailable);

        final MessageInterpolator.Context context = context("12345678",
            () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("mixedMessageValue")
            .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
            .orElseThrow(() -> new AssertionError("expected constraint missing")));
        
        assertEquals("Mixed message value of length 8 should match ....$",
            interpolator.interpolate(context.getConstraintDescriptor().getMessageTemplate(), context));
    }

    @Test
    public void testELEscapingTomcatJuel() {
        assumeTrue(elAvailable);
        assumeThat(elImpl, anyOf(equalTo("tomcat")));

        // not so much a test as an illustration that the specified EL implementations are seemingly confused by leading
        // backslashes and treats the whole expression as literal. We could skip any literal text before the first
        // non-escaped $, but that would only expose us to inconsistency for composite expressions containing more
        // than one component EL expression

        ApacheMessageContext context = context("12345678",
            () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("idNumber")
            .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
            .orElseThrow(() -> new AssertionError("expected constraint missing")));

        when(context
            .getConfigurationProperty(ApacheValidatorConfiguration.Properties.CUSTOM_TEMPLATE_EXPRESSION_EVALUATION))
                .thenAnswer(invocation -> Boolean.toString(true));

        assertEquals("${regexp.charAt(4)}", interpolator.interpolate("\\${regexp.charAt(4)}",
            context));

        assertEquals("${regexp.charAt(4)}", interpolator.interpolate("\\\\${regexp.charAt(4)}",
            context));
    }

    @Test
    public void testELEscapingRI() {
        assumeTrue(elAvailable);
            assumeThat(elImpl, equalTo("ri"));

        ApacheMessageContext context = context("12345678",
            () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("idNumber")
                .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
                .orElseThrow(() -> new AssertionError("expected constraint missing")));

        when(context
            .getConfigurationProperty(ApacheValidatorConfiguration.Properties.CUSTOM_TEMPLATE_EXPRESSION_EVALUATION))
        .thenAnswer(invocation -> Boolean.toString(true));

        assertEquals("returns literal", "${regexp.charAt(4)}",
            interpolator.interpolate("\\${regexp.charAt(4)}",
                context));

        assertEquals("returns literal \\ followed by $, later interpreted as an escape sequence", "$",
            interpolator.interpolate("\\\\${regexp.charAt(4)}",
                context));

        assertEquals("returns literal \\ followed by .", "\\.",
            interpolator.interpolate("\\\\${regexp.charAt(3)}",
                context));
    }

    @Test
    public void testEscapedELPattern() {
        assertEquals("$must match \"....$\"",
            interpolator.interpolate("\\${jakarta.validation.constraints.Pattern.message}",
                context("12345678",
                    () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("idNumber")
                        .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
                        .orElseThrow(() -> new AssertionError("expected constraint missing")))));

        assertEquals("$must match \"....$\"",
            interpolator.interpolate("\\${jakarta.validation.constraints.Pattern.message}",
                context("12345678",
                    () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("idNumber")
                    .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
                    .orElseThrow(() -> new AssertionError("expected constraint missing")))));

        assertEquals("\\$must match \"....$\"",
            interpolator.interpolate("\\\\\\${jakarta.validation.constraints.Pattern.message}",
                context("12345678",
                    () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("idNumber")
                    .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
                    .orElseThrow(() -> new AssertionError("expected constraint missing")))));
    }

    @SuppressWarnings("unchecked")
    private ApacheMessageContext context(Object validatedValue, Supplier<ConstraintDescriptor<?>> descriptor) {
        final ApacheMessageContext result = Mockito.mock(ApacheMessageContext.class);
        when(result.unwrap(any(Class.class)))
            .thenAnswer(invocation -> invocation.getArgumentAt(0, Class.class).cast(result));
        when(result.getValidatedValue()).thenReturn(validatedValue);
        when(result.getConstraintDescriptor()).thenAnswer(invocation -> descriptor.get());
        return result;
    }

    public static class Person {

        @Pattern(message = "Id number should match {regexp}", regexp = "....$")
        public String idNumber;

        @Pattern(message = "Other id should match {regexp}", regexp = ".\\n")
        public String otherId;

        @Pattern(message = "Another value should match ${regexp.intern()}", regexp = "....$")
        public String anotherValue;
        
        @Pattern(message = "Mixed message value of length ${validatedValue.length()} should match {regexp}", regexp = "....$")
        public String mixedMessageValue;
    }
}
