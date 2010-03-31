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
package org.apache.bval.jsr303;


import javax.validation.ConstraintValidator;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;

import org.apache.bval.jsr303.groups.Group;
import org.apache.bval.jsr303.groups.Groups;
import org.apache.bval.jsr303.util.PathImpl;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;

/**
 * Description: <br/>
 */
public interface GroupValidationContext<T extends ValidationListener>
      extends ValidationContext<T> {
    /** the groups in their sequence for validation */
    Groups getGroups();

    void setCurrentGroup(Group group);

    Group getCurrentGroup();

    PathImpl getPropertyPath();

    MetaBean getRootMetaBean();

    void setConstraintDescriptor(ConstraintValidation constraint);

    public ConstraintValidation getConstraintDescriptor();

    public Object getValidatedValue();

    void setFixedValue(Object value);

    MessageInterpolator getMessageResolver();

    TraversableResolver getTraversableResolver();

    boolean collectValidated(Object bean, ConstraintValidator constraint);

}
