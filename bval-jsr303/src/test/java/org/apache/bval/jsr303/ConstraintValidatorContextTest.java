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
package org.apache.bval.jsr303;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.bval.jsr303.groups.Group;
import org.apache.bval.jsr303.groups.Groups;
import org.apache.bval.jsr303.util.PathImpl;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.ValidationListener;
import org.apache.bval.model.ValidationListener.Error;
import org.apache.bval.util.AccessStrategy;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;


/**
 * Checks to validate the correct implementation of
 * {@link ConstraintValidatorContext} and its sub-interfaces.
 * 
 * @author Carlos Vara
 */
public class ConstraintValidatorContextTest extends TestCase {

    private ConstraintValidatorContextImpl cvc;
    private ConstraintViolationBuilder cvb;

    private void resetConstraintValidatorContext() {
        this.cvc = new ConstraintValidatorContextImpl(
                new DummyContext<ValidationListener>(), null);
        this.cvc.disableDefaultConstraintViolation();
        this.cvb = cvc.buildConstraintViolationWithTemplate("dummy.msg.tpl");
    }

    // Test that builds a path and checks it against the expected one
    public void testPathBuilding() {

        resetConstraintValidatorContext();

        // persons[1]
        cvb.addNode("person").addNode(null).inIterable().atIndex(1)
                .addConstraintViolation();
        Error error = cvc.getErrorMessages().iterator().next();
        PathImpl errorPath = (PathImpl) error.getOwner();
        Assert.assertEquals("Incorrect path created", "person[1]", errorPath.toString());

        resetConstraintValidatorContext();

        // persons[lawyer].name
        cvb.addNode("person").addNode("name").inIterable().atKey("john")
                .addConstraintViolation();
        error = cvc.getErrorMessages().iterator().next();
        errorPath = (PathImpl) error.getOwner();
        Assert.assertEquals("Incorrect path created", "person[john].name", errorPath.toString());

        resetConstraintValidatorContext();

        // [0].name[]
        cvb.addNode(null).addNode("name").inIterable().atIndex(0).addNode(null)
                .inIterable().addConstraintViolation();
        error = cvc.getErrorMessages().iterator().next();
        errorPath = (PathImpl) error.getOwner();
        Assert.assertEquals("Incorrect path created", "[0].name[]", errorPath.toString());

        resetConstraintValidatorContext();

        // []
        cvb.addNode(null).addNode(null).inIterable().addConstraintViolation();
        error = cvc.getErrorMessages().iterator().next();
        errorPath = (PathImpl) error.getOwner();
        Assert.assertEquals("Incorrect path created", "[]", errorPath.toString());
        
        resetConstraintValidatorContext();
        
        // Adding only nulls should still give a root path
        cvb.addNode(null).addNode(null).addNode(null).addNode(null).addConstraintViolation();
        error = cvc.getErrorMessages().iterator().next();
        errorPath = (PathImpl) error.getOwner();
        Assert.assertTrue("Created path must be a root path", errorPath.isRootPath());

    }

    // TODO: mock
    public static class DummyContext<T> implements
            GroupValidationContext<T> {

        public boolean collectValidated(ConstraintValidator constraint) {
            throw new IllegalStateException("Unexpected call");
        }

        public ConstraintValidation getConstraintValidation() {
            throw new IllegalStateException("Unexpected call");
        }

        public Group getCurrentGroup() {
            throw new IllegalStateException("Unexpected call");
        }

        public Groups getGroups() {
            throw new IllegalStateException("Unexpected call");
        }

        public MessageInterpolator getMessageResolver() {
            throw new IllegalStateException("Unexpected call");
        }

        public PathImpl getPropertyPath() {
            return PathImpl.createPathFromString("");
        }

        public MetaBean getRootMetaBean() {
            throw new IllegalStateException("Unexpected call");
        }

        public TraversableResolver getTraversableResolver() {
            throw new IllegalStateException("Unexpected call");
        }

        public Object getValidatedValue() {
            throw new IllegalStateException("Unexpected call");
        }

        public void setConstraintValidation(ConstraintValidation constraint) {
            throw new IllegalStateException("Unexpected call");
        }

        public void setCurrentGroup(Group group) {
            throw new IllegalStateException("Unexpected call");
        }

        public void setFixedValue(Object value) {
            throw new IllegalStateException("Unexpected call");
        }

        public boolean collectValidated() {
            throw new IllegalStateException("Unexpected call");
        }

        public AccessStrategy getAccess() {
            throw new IllegalStateException("Unexpected call");
        }

        public Object getBean() {
            throw new IllegalStateException("Unexpected call");
        }

        public ConstraintValidationListener<T> getListener() {
            throw new IllegalStateException("Unexpected call");
        }

        public MetaBean getMetaBean() {
            throw new IllegalStateException("Unexpected call");
        }

        public MetaProperty getMetaProperty() {
            throw new IllegalStateException("Unexpected call");
        }

        public String getPropertyName() {
            throw new IllegalStateException("Unexpected call");
        }

        public Object getPropertyValue() {
            throw new IllegalStateException("Unexpected call");
        }

        public Object getPropertyValue(AccessStrategy access) {
            throw new IllegalStateException("Unexpected call");
        }

        public void moveDown(MetaProperty prop, AccessStrategy access) {
            throw new IllegalStateException("Unexpected call");
        }

        public void moveUp(Object bean, MetaBean metaBean) {
            throw new IllegalStateException("Unexpected call");
        }

        public void setBean(Object bean) {
            throw new IllegalStateException("Unexpected call");
        }

        public void setBean(Object aBean, MetaBean aMetaBean) {
            throw new IllegalStateException("Unexpected call");
        }

        public void setCurrentIndex(Integer index) {
            throw new IllegalStateException("Unexpected call");
        }

        public void setCurrentKey(Object key) {
            throw new IllegalStateException("Unexpected call");
        }

        public void setMetaBean(MetaBean metaBean) {
            throw new IllegalStateException("Unexpected call");
        }

        public void setMetaProperty(MetaProperty metaProperty) {
            throw new IllegalStateException("Unexpected call");
        }

        // @Override - not allowed in 1.5 for Interface methods
        public Class<?> getCurrentOwner() {
            throw new IllegalStateException("Unexpected call");
        }

        // @Override - not allowed in 1.5 for Interface methods
        public void setCurrentOwner(Class<?> currentOwner) {
            throw new IllegalStateException("Unexpected call");
        }

    }

}
