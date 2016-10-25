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
import org.apache.bval.model.MetaBean;

import javax.validation.metadata.ParameterDescriptor;
import java.util.List;

/**
 * Description: superinterface of {@link javax.validation.metadata.ConstructorDescriptor} and {@link org.apache.bval.jsr.MethodDescriptor}.<br/>
 */
public interface ProcedureDescriptor {
    /**
     * Get the owning metabean.
     * @return MetaBean
     */
    MetaBean getMetaBean();

    /**
     * Set whether this procedure should be validated.
     * @param b
     */
    void setCascaded(boolean b);

    /**
     * Get the parameter descriptors of this procedure.
     * @return {@link java.util.List} of {@link javax.validation.metadata.ParameterDescriptor}
     */
    List<ParameterDescriptor> getParameterDescriptors();

    void addGroupMapping(Group from, Group to);

    Group mapGroup(Group current);
}
