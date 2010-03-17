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

import com.agimatec.validation.jsr303.groups.Group;
import com.agimatec.validation.jsr303.groups.Groups;
import com.agimatec.validation.jsr303.util.PathImpl;
import com.agimatec.validation.model.MetaBean;
import com.agimatec.validation.model.ValidationContext;
import com.agimatec.validation.model.ValidationListener;

import javax.validation.ConstraintValidator;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;

/**
 * Description: <br/>
 * User: roman.stumm <br/>
 * Date: 28.04.2008 <br/>
 * Time: 10:15:08 <br/>
 * Copyright: Agimatec GmbH
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
