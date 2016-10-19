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

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.model.ValidationListener;
import org.apache.bval.model.ValidationListener.Error;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

/**
 * Checks to validate the correct implementation of
 * {@link ConstraintValidatorContext} and its sub-interfaces.
 * 
 * @author Carlos Vara
 */
public class ConstraintValidatorContextTest extends TestCase {

    private ConstraintValidatorContextImpl cvc;
    private ConstraintViolationBuilder cvb;

    @Mock
    private GroupValidationContext<ValidationListener> groupValidationContext;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        Mockito.when(groupValidationContext.getPropertyPath()).thenAnswer(new Answer<PathImpl>() {

            @Override
            public PathImpl answer(InvocationOnMock invocation) throws Throwable {
                return PathImpl.createPathFromString("");
            }
        });
        this.cvc = new ConstraintValidatorContextImpl(groupValidationContext,
                null);
        this.cvc.disableDefaultConstraintViolation();
        this.cvb = cvc.buildConstraintViolationWithTemplate("dummy.msg.tpl");
    }

    public void testPerson1() {
        cvb.addNode("person").addNode(null).inIterable().atIndex(1).addConstraintViolation();
        final Error error = cvc.getErrorMessages().iterator().next();
        final PathImpl errorPath = (PathImpl) error.getOwner();
        Assert.assertEquals("Incorrect path created", "person[1]", errorPath.toString());
    }

    public void testPersonLawyerName() {
        cvb.addNode("person").addNode("name").inIterable().atKey("john")
                .addConstraintViolation();
        Error error = cvc.getErrorMessages().iterator().next();
        PathImpl errorPath = (PathImpl) error.getOwner();
        Assert.assertEquals("Incorrect path created", "person[john].name",
                errorPath.toString());
    }

    public void test0Name() {
        cvb.addNode(null).addNode("name").inIterable().atIndex(0).addNode(null)
                .inIterable().addConstraintViolation();
        Error error = cvc.getErrorMessages().iterator().next();
        PathImpl errorPath = (PathImpl) error.getOwner();
        Assert.assertEquals("Incorrect path created", "[0].name[]", errorPath
                .toString());
    }

    public void testEmptyIndex() {
        cvb.addNode(null).addNode(null).inIterable().addConstraintViolation();
        Error error = cvc.getErrorMessages().iterator().next();
        PathImpl errorPath = (PathImpl) error.getOwner();
        Assert.assertEquals("Incorrect path created", "[]", errorPath
                .toString());
    }

    public void testRootPath() {
        // Adding only nulls should still give a root path
        cvb.addNode(null).addNode(null).addNode(null).addNode(null)
                .addConstraintViolation();
        Error error = cvc.getErrorMessages().iterator().next();
        PathImpl errorPath = (PathImpl) error.getOwner();
        Assert.assertTrue("Created path must be a root path", errorPath
                .isRootPath());

    }

}
