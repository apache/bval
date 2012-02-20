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

import org.apache.bval.jsr303.ElementDescriptorImpl;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.Validation;


/**
 * Description: {@link ParameterDescriptor} implementation.<br/>
 */
public class ParameterDescriptorImpl extends ElementDescriptorImpl
      implements ParameterDescriptor {
    private boolean cascaded;
    private int index;

    /**
     * Create a new ParameterDescriptorImpl instance.
     * @param metaBean
     * @param validations
     */
    public ParameterDescriptorImpl(MetaBean metaBean, Validation[] validations) {
        super(metaBean, metaBean.getClass(), validations);
    }

    /**
     * Create a new ParameterDescriptorImpl instance.
     * @param elementClass
     * @param validations
     */
    public ParameterDescriptorImpl(Class<?> elementClass, Validation[] validations) {
        super(elementClass, validations);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCascaded() {
        return cascaded;
    }

    /**
     * Set whether the referenced parameter descriptor should be validated.
     * @param cascaded
     */
    public void setCascaded(boolean cascaded) {
        this.cascaded = cascaded;
    }

    /**
     * {@inheritDoc}
     */
    public int getIndex() {
        return index;
    }

    /**
     * Set the index of the referenced parameter.
     * @param index
     */
    public void setIndex(int index) {
        this.index = index;
    }
}
