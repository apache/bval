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


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bval.jsr303.ApacheFactoryContext;
import org.apache.bval.jsr303.BeanDescriptorImpl;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.Validation;

/**
 * Description: {@link MethodBeanDescriptor} implementation.<br/>
 */
class MethodBeanDescriptorImpl extends BeanDescriptorImpl
      implements MethodBeanDescriptor {
    private Map<Method, MethodDescriptor> methodConstraints;
    private Map<Constructor<?>, ConstructorDescriptor> constructorConstraints;

    /**
     * Create a new MethodBeanDescriptorImpl instance.
     * @param factoryContext
     * @param metaBean
     * @param validations
     */
    protected MethodBeanDescriptorImpl(ApacheFactoryContext factoryContext,
                                       MetaBean metaBean, Validation[] validations) {
        super(factoryContext, metaBean, validations);
    }

    /**
     * Set the map of method constraints for this bean.
     * @param methodConstraints
     */
    public void setMethodConstraints(Map<Method, MethodDescriptor> methodConstraints) {
        this.methodConstraints = methodConstraints;
    }

    /**
     * Set the map of constructor constraints for this bean.
     * @param constructorConstraints
     */
    public void setConstructorConstraints(
          Map<Constructor<?>, ConstructorDescriptor> constructorConstraints) {
        this.constructorConstraints = constructorConstraints;
    }

    /**
     * {@inheritDoc}
     */
    public MethodDescriptor getConstraintsForMethod(Method method) {
        return methodConstraints.get(method);
    }

    /**
     * {@inheritDoc}
     */
    public ConstructorDescriptor getConstraintsForConstructor(Constructor<?> constructor) {
        return constructorConstraints.get(constructor);
    }

    /**
     * {@inheritDoc}
     */
    public Set<MethodDescriptor> getConstrainedMethods() {
        return new HashSet<MethodDescriptor>(methodConstraints.values());
    }

    /**
     * Add a {@link MethodDescriptor} to this {@link MethodBeanDescriptorImpl}.
     * @param method
     * @param desc
     */
    public void putMethodDescriptor(Method method, MethodDescriptor desc) {
        methodConstraints.put(method, desc);
    }

    /**
     * {@inheritDoc}
     */
    public Set<ConstructorDescriptor> getConstrainedConstructors() {
        return new HashSet<ConstructorDescriptor>(this.constructorConstraints.values());
    }

    /**
     * Add a {@link ConstructorDescriptor} to this {@link MethodBeanDescriptorImpl}.
     * @param cons
     * @param desc
     */
    public void putConstructorDescriptor(Constructor<?> cons, ConstructorDescriptor desc) {
        constructorConstraints.put(cons, desc);
    }

    /**
     * Get the configured method constraints.
     * @return {@link Map} of {@link Method} : {@link MethodDescriptor}
     */
    public Map<Method, MethodDescriptor> getMethodConstraints() {
        return methodConstraints;
    }

    /**
     * Get the configured constructor constraints.
     * @return {@link Map} of {@link Constructor} : {@link ConstructorDescriptor}
     */
    public Map<Constructor<?>, ConstructorDescriptor> getConstructorConstraints() {
        return constructorConstraints;
    }
}
