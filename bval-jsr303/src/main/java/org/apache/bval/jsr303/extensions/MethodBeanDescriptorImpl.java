/**
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
 * Description: <br/>
 */
class MethodBeanDescriptorImpl extends BeanDescriptorImpl
      implements MethodBeanDescriptor {
    private Map<Method, MethodDescriptor> methodConstraints;
    private Map<Constructor<?>, ConstructorDescriptor> constructorConstraints;

    protected MethodBeanDescriptorImpl(ApacheFactoryContext factoryContext,
                                       MetaBean metaBean, Validation[] validations) {
        super(factoryContext, metaBean, validations);
    }

    public void setMethodConstraints(Map<Method, MethodDescriptor> methodConstraints) {
        this.methodConstraints = methodConstraints;
    }

    public void setConstructorConstraints(
          Map<Constructor<?>, ConstructorDescriptor> constructorConstraints) {
        this.constructorConstraints = constructorConstraints;
    }

    public MethodDescriptor getConstraintsForMethod(Method method) {
        return methodConstraints.get(method);
    }

    public ConstructorDescriptor getConstraintsForConstructor(Constructor<?> constructor) {
        return constructorConstraints.get(constructor);
    }

    public Set<MethodDescriptor> getConstrainedMethods() {
        return new HashSet<MethodDescriptor>(methodConstraints.values());
    }

    public void putMethodDescriptor(Method method, MethodDescriptor desc) {
        methodConstraints.put(method, desc);
    }

    public Set<ConstructorDescriptor> getConstrainedConstructors() {
        return new HashSet<ConstructorDescriptor>(this.constructorConstraints.values());
    }

    public void putConstructorDescriptor(Constructor<?> cons, ConstructorDescriptor desc) {
        constructorConstraints.put(cons, desc);
    }

    public Map<Method, MethodDescriptor> getMethodConstraints() {
        return methodConstraints;
    }

    public Map<Constructor<?>, ConstructorDescriptor> getConstructorConstraints() {
        return constructorConstraints;
    }
}
