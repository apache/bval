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
import org.apache.bval.model.MetaConstructor;
import org.apache.bval.model.Validation;

import javax.validation.metadata.ConstructorDescriptor;

/**
 * Description: {@link javax.validation.metadata.ConstructorDescriptor} implementation.<br/>
 */
public class ConstructorDescriptorImpl extends InvocableElementDescriptor
      implements ConstructorDescriptor, ProcedureDescriptor {
    /**
     * Create a new ConstructorDescriptorImpl instance.
     * @param metaBean
     * @param validations
     */
    protected ConstructorDescriptorImpl(MetaBean metaBean, Validation[] validations) {
        super(metaBean, metaBean.getBeanClass(), validations);
    }

    public ConstructorDescriptorImpl(final MetaBean metaBean, final MetaConstructor metaMethod) {
        super(metaBean, metaBean.getBeanClass(), new Validation[0]);
        setCascaded(false);
    }

    public String getName() {
        return elementClass.getSimpleName();
    }

    @Override
    public boolean hasConstraints() {
        return false;
    }

    public boolean hasConstrainedParameters() {
        return super.hasConstrainedParameters();
    }

    public boolean hasConstrainedReturnValue() {
        return super.hasConstrainedReturnValue();
    }
}
