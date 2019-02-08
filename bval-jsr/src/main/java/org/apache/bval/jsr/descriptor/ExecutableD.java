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
package org.apache.bval.jsr.descriptor;

import java.lang.reflect.Executable;
import java.util.List;

import javax.validation.metadata.CrossParameterDescriptor;
import javax.validation.metadata.ExecutableDescriptor;
import javax.validation.metadata.ParameterDescriptor;
import javax.validation.metadata.ReturnValueDescriptor;

public abstract class ExecutableD<E extends Executable, R extends MetadataReader.ForExecutable<E, R>, SELF extends ExecutableD<E, R, SELF>>
    extends ElementD.NonRoot<BeanD<?>, E, R> implements ExecutableDescriptor {

    private final String name;
    private final ReturnValueD<SELF, E> returnValue;
    private final List<ParameterD<SELF>> parameters;
    private final CrossParameterD<SELF, E> crossParameter;
    private final boolean parametersAreConstrained;
    private final boolean returnValueIsConstrained;

    @SuppressWarnings("unchecked")
    protected ExecutableD(R reader, BeanD<?> parent) {
        super(reader, parent);

        name = reader.meta.getName();

        returnValue = reader.getReturnValueDescriptor((SELF) this);
        parameters = reader.getParameterDescriptors((SELF) this);
        crossParameter = reader.getCrossParameterDescriptor((SELF) this);
        parametersAreConstrained = parameters.stream().anyMatch(DescriptorManager::isConstrained) || crossParameter.hasConstraints();
        returnValueIsConstrained = DescriptorManager.isConstrained(returnValue);
    }

    @Override
    public final String getName() {
        return name;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public final List<ParameterDescriptor> getParameterDescriptors() {
        return (List) parameters;
    }

    @Override
    public final CrossParameterDescriptor getCrossParameterDescriptor() {
        return crossParameter;
    }

    @Override
    public final ReturnValueDescriptor getReturnValueDescriptor() {
        return returnValue;
    }

    @Override
    public final boolean hasConstrainedParameters() {
        return parametersAreConstrained;
    }

    @Override
    public final boolean hasConstrainedReturnValue() {
        return returnValueIsConstrained;
    }
}
