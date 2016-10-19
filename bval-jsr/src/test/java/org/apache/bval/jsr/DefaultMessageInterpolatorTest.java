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

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.bval.jsr.example.Author;
import org.apache.bval.jsr.example.PreferredGuest;

import javax.validation.MessageInterpolator;
import javax.validation.Validator;
import javax.validation.constraints.Pattern;
import javax.validation.metadata.ConstraintDescriptor;
import java.util.Locale;

/**
 * MessageResolverImpl Tester.
 */
public class DefaultMessageInterpolatorTest extends TestCase {

    private DefaultMessageInterpolator interpolator;

    public DefaultMessageInterpolatorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DefaultMessageInterpolatorTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp(); // call super!
        interpolator = new DefaultMessageInterpolator();
        interpolator.setLocale(Locale.ENGLISH);
    }

    public void testCreateResolver() {

        final Validator gvalidator = getValidator();

        assertTrue(!gvalidator.getConstraintsForClass(PreferredGuest.class).getConstraintsForProperty(
            "guestCreditCardNumber").getConstraintDescriptors().isEmpty());

        MessageInterpolator.Context ctx = new MessageInterpolator.Context() {

            @Override
            public ConstraintDescriptor<?> getConstraintDescriptor() {
                return gvalidator.getConstraintsForClass(PreferredGuest.class)
                    .getConstraintsForProperty("guestCreditCardNumber").getConstraintDescriptors().iterator().next();
            }

            @Override
            public Object getValidatedValue() {
                return "12345678";
            }

            @Override
            public <T> T unwrap(Class<T> type) {
                return null;
            }
        };
        String msg = interpolator.interpolate("{validator.creditcard}", ctx);
        Assert.assertEquals("credit card is not valid", msg);

        ctx = new MessageInterpolator.Context() {
            @Override
            public ConstraintDescriptor<?> getConstraintDescriptor() {
                return gvalidator.getConstraintsForClass(Author.class).getConstraintsForProperty("lastName")
                    .getConstraintDescriptors().iterator().next();
            }

            @Override
            public Object getValidatedValue() {
                return "";
            }

            @Override
            public <T> T unwrap(Class<T> type) {
                return null;
            }
        };

        msg = interpolator.interpolate("{org.apache.bval.constraints.NotEmpty.message}", ctx);
        Assert.assertEquals("may not be empty", msg);
    }

    /**
     * Checks that strings containing special characters are correctly
     * substituted when interpolating.
     */
    public void testReplacementWithSpecialChars() {

        final Validator validator = getValidator();
        MessageInterpolator.Context ctx;

        // Try to interpolate an annotation attribute containing $
        ctx = new MessageInterpolator.Context() {

            @Override
            public ConstraintDescriptor<?> getConstraintDescriptor() {
                return validator.getConstraintsForClass(Person.class)
                    .getConstraintsForProperty("idNumber").getConstraintDescriptors().iterator().next();
            }

            @Override
            public Object getValidatedValue() {
                return "12345678";
            }

            @Override
            public <T> T unwrap(Class<T> type) {
                return null;
            }
        };

        String result = this.interpolator.interpolate("Id number should match {regexp}", ctx);
        Assert.assertEquals("Incorrect message interpolation when $ is in an attribute",
            "Id number should match ....$", result);

        // Try to interpolate an annotation attribute containing \
        ctx = new MessageInterpolator.Context() {

            @Override
            public ConstraintDescriptor<?> getConstraintDescriptor() {
                return validator.getConstraintsForClass(Person.class)
                    .getConstraintsForProperty("otherId").getConstraintDescriptors().iterator().next();
            }

            @Override
            public Object getValidatedValue() {
                return "12345678";
            }

            @Override
            public <T> T unwrap(Class<T> type) {
                return null;
            }
        };

        result = this.interpolator.interpolate("Other id should match {regexp}", ctx);
        Assert.assertEquals("Incorrect message interpolation when \\ is in an attribute value",
            "Other id should match .\\n", result);

    }

    public static class Person {

        @Pattern(message = "Id number should match {regexp}", regexp = "....$")
        public String idNumber;

        @Pattern(message = "Other id should match {regexp}", regexp = ".\\n")
        public String otherId;

    }

    private Validator getValidator() {
        return ApacheValidatorFactory.getDefault().getValidator();
    }
}
