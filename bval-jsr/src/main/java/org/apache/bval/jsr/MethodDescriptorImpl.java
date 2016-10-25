/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval.jsr;

import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaMethod;
import org.apache.bval.model.Validation;

import java.lang.reflect.Method;

/**
 * Description: {@link MethodDescriptor} implementation.<br/>
 */
public class MethodDescriptorImpl extends InvocableElementDescriptor
    implements javax.validation.metadata.MethodDescriptor, ProcedureDescriptor {
    private static final Validation[] EMPTY_VALIDATION = new Validation[0];

    private final String name;

    protected MethodDescriptorImpl(final MetaBean metaBean, final Validation[] validations, final Method method) {
        super(metaBean, method.getReturnType(), validations);
        name = method.getName();
    }

    public MethodDescriptorImpl(final MetaBean bean, final MetaMethod metaMethod) {
        super(bean, metaMethod.getMethod().getReturnType(), EMPTY_VALIDATION);
        setCascaded(false);
        this.name = metaMethod.getMethod().getName();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean hasConstrainedParameters() {
        return super.hasConstrainedParameters();
    }

    @Override
    public boolean hasConstrainedReturnValue() {
        return super.hasConstrainedReturnValue();
    }

    @Override
    public boolean hasConstraints() {
        return false;
    }
}
