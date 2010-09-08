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
package org.apache.bval.jsr303.extensions;


import java.util.ArrayList;
import java.util.List;

import org.apache.bval.jsr303.ElementDescriptorImpl;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.Validation;

/**
 * Description: {@link ConstructorDescriptor} implementation.<br/>
 */
public class ConstructorDescriptorImpl extends ElementDescriptorImpl
      implements ConstructorDescriptor, ProcedureDescriptor {
    private final List<ParameterDescriptor> parameterDescriptors = new ArrayList<ParameterDescriptor>();
    private boolean cascaded;

    /**
     * Create a new ConstructorDescriptorImpl instance.
     * @param metaBean
     * @param validations
     */
    protected ConstructorDescriptorImpl(MetaBean metaBean, Validation[] validations) {
        super(metaBean, metaBean.getBeanClass(), validations);
    }

    /**
     * Create a new ConstructorDescriptorImpl instance.
     * @param elementClass
     * @param validations
     */
    protected ConstructorDescriptorImpl(Class<?> elementClass, Validation[] validations) {
        super(elementClass, validations);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCascaded() {
        return cascaded;
    }

    /**
     * {@inheritDoc}
     */
    public void setCascaded(boolean cascaded) {
        this.cascaded = cascaded;
    }

    /**
     * {@inheritDoc}
     */
    public List<ParameterDescriptor> getParameterDescriptors() //index aligned
    {
        return parameterDescriptors;
    }
}
