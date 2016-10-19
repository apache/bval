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
import static org.junit.Assert.assertTrue;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.model.ValidationListener;
import org.apache.bval.model.ValidationListener.Error;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * Checks to validate the correct implementation of
 * {@link ConstraintValidatorContext} and its sub-interfaces.
 * 
 * @author Carlos Vara
 */
@RunWith(MockitoJUnitRunner.class)
public class ConstraintValidatorContextTest {

    private ConstraintValidatorContextImpl cvc;
    private ConstraintViolationBuilder cvb;

    @Mock
    private GroupValidationContext<ValidationListener> groupValidationContext;

    /**
     * {@inheritDoc}
     */
    @Before
    public void setUp() throws Exception {
        Mockito.when(groupValidationContext.getPropertyPath()).thenAnswer(new Answer<PathImpl>() {

            @Override
            public PathImpl answer(InvocationOnMock invocation) throws Throwable {
                return PathImpl.createPathFromString("");
            }
        });
        this.cvc = new ConstraintValidatorContextImpl(groupValidationContext, null);
        this.cvc.disableDefaultConstraintViolation();
        this.cvb = cvc.buildConstraintViolationWithTemplate("dummy.msg.tpl");
    }

    @Test
    public void testPerson1() {
        cvb.addNode("person").addNode(null).inIterable().atIndex(1).addConstraintViolation();
        final Error error = cvc.getErrorMessages().iterator().next();
        final PathImpl errorPath = (PathImpl) error.getOwner();
        assertEquals("Incorrect path created", "person[1]", errorPath.toString());
    }

    @Test
    public void testPersonLawyerName() {
        cvb.addNode("person").addNode("name").inIterable().atKey("john")
                .addConstraintViolation();
        Error error = cvc.getErrorMessages().iterator().next();
        PathImpl errorPath = (PathImpl) error.getOwner();
        assertEquals("Incorrect path created", "person[john].name", errorPath.toString());
    }

    @Test
    public void test0Name() {
        cvb.addNode(null).addNode("name").inIterable().atIndex(0).addNode(null)
                .inIterable().addConstraintViolation();
        Error error = cvc.getErrorMessages().iterator().next();
        PathImpl errorPath = (PathImpl) error.getOwner();
        assertEquals("Incorrect path created", "[0].name[]", errorPath.toString());
    }

    @Test
    public void testEmptyIndex() {
        cvb.addNode(null).addNode(null).inIterable().addConstraintViolation();
        Error error = cvc.getErrorMessages().iterator().next();
        PathImpl errorPath = (PathImpl) error.getOwner();
        assertEquals("Incorrect path created", "[]", errorPath.toString());
    }

    @Test
    public void testRootPath() {
        // Adding only nulls should still give a root path
        cvb.addNode(null).addNode(null).addNode(null).addNode(null)
                .addConstraintViolation();
        Error error = cvc.getErrorMessages().iterator().next();
        PathImpl errorPath = (PathImpl) error.getOwner();
        assertTrue("Created path must be a root path", errorPath.isRootPath());
    }

}
