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

import org.apache.bval.jsr.groups.Group;
import org.apache.bval.jsr.groups.GroupConversionDescriptorImpl;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.Validation;

import javax.validation.metadata.GroupConversionDescriptor;
import javax.validation.metadata.ParameterDescriptor;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * Description: {@link javax.validation.metadata.ParameterDescriptor} implementation.<br/>
 */
public class ParameterDescriptorImpl extends ElementDescriptorImpl implements ParameterDescriptor {
    private final Set<GroupConversionDescriptor> groupConversions = new CopyOnWriteArraySet<GroupConversionDescriptor>();
    private final String name;
    private int index;

    /**
     * Create a new ParameterDescriptorImpl instance.
     * @param metaBean
     * @param validations
     */
    public ParameterDescriptorImpl(MetaBean metaBean, Validation[] validations, String name) {
        super(metaBean, metaBean.getBeanClass(), validations);
        this.name = name;
    }

    /**
     * Create a new ParameterDescriptorImpl instance.
     * @param elementClass
     * @param validations
     */
    public ParameterDescriptorImpl(Class<?> elementClass, Validation[] validations, String name) {
        super(elementClass, validations);
        this.name = name;
    }

    @Override
    public Set<GroupConversionDescriptor> getGroupConversions() {
        return groupConversions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Set the index of the referenced parameter.
     * @param index
     */
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public void addGroupMapping(final Group from, final Group to) {
        groupConversions.add(new GroupConversionDescriptorImpl(from, to));
        super.addGroupMapping(from, to);
    }
}
