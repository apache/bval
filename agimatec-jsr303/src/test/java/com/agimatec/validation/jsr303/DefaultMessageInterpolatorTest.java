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
package com.agimatec.validation.jsr303;

import com.agimatec.validation.jsr303.example.Author;
import com.agimatec.validation.jsr303.example.PreferredGuest;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.validation.MessageInterpolator;
import javax.validation.Validator;
import javax.validation.metadata.ConstraintDescriptor;

/**
 * MessageResolverImpl Tester.
 *
 * @author ${USER}
 * @version 1.0
 * @since <pre>04/02/2008</pre>
 *        Copyright: Agimatec GmbH 2008
 */
public class DefaultMessageInterpolatorTest extends TestCase {
    DefaultMessageInterpolator interpolator = new DefaultMessageInterpolator();

    public DefaultMessageInterpolatorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DefaultMessageInterpolatorTest.class);
    }

    public void testCreateResolver() {

        final Validator gvalidator = getValidator();

        assertTrue(!gvalidator.getConstraintsForClass(PreferredGuest.class)
              .getConstraintsForProperty("guestCreditCardNumber")
              .getConstraintDescriptors().isEmpty());

        MessageInterpolator.Context ctx = new MessageInterpolator.Context() {

            public ConstraintDescriptor<?> getConstraintDescriptor() {
                return (ConstraintDescriptor<?>) gvalidator
                      .getConstraintsForClass(PreferredGuest.class).
                      getConstraintsForProperty("guestCreditCardNumber")
                      .getConstraintDescriptors().toArray()[0];
            }

            public Object getValidatedValue() {
                return "12345678";
            }
        };
        String msg = interpolator.interpolate("{validator.creditcard}", ctx);
        Assert.assertEquals("credit card is not valid", msg);

        ctx = new MessageInterpolator.Context() {
            public ConstraintDescriptor<?> getConstraintDescriptor() {
                return (ConstraintDescriptor) gvalidator
                      .getConstraintsForClass(Author.class).
                      getConstraintsForProperty("lastName")
                      .getConstraintDescriptors().toArray()[0];
            }

            public Object getValidatedValue() {
                return "";
            }
        };


        msg = interpolator.interpolate("{com.agimatec.validation.constraints.NotEmpty.message}", ctx);
        Assert.assertEquals("may not be empty", msg);
    }


    private Validator getValidator() {
        return AgimatecValidatorFactory.getDefault().getValidator();
    }
}
