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
 * User: roman <br/>
 * Date: 11.11.2009 <br/>
 * Time: 15:18:00 <br/>
 * Copyright: Agimatec GmbH
 */
class MethodBeanDescriptorImpl extends BeanDescriptorImpl
      implements MethodBeanDescriptor {
    private Map<Method, MethodDescriptorImpl> methodConstraints;
    private Map<Constructor, ConstructorDescriptorImpl> constructorConstraints;

    protected MethodBeanDescriptorImpl(ApacheFactoryContext factoryContext,
                                       MetaBean metaBean, Validation[] validations) {
        super(factoryContext, metaBean, validations);
    }

    public void setMethodConstraints(Map<Method, MethodDescriptorImpl> methodConstraints) {
        this.methodConstraints = methodConstraints;
    }

    public void setConstructorConstraints(
          Map<Constructor, ConstructorDescriptorImpl> constructorConstraints) {
        this.constructorConstraints = constructorConstraints;
    }

    public MethodDescriptor getConstraintsForMethod(Method method) {
        return methodConstraints.get(method);
    }

    public ConstructorDescriptor getConstraintsForConstructor(Constructor constructor) {
        return constructorConstraints.get(constructor);
    }

    public Set<MethodDescriptor> getConstrainedMethods() {
        return new HashSet(methodConstraints.values());
    }

    public void putMethodDescriptor(Method method, MethodDescriptorImpl desc) {
        methodConstraints.put(method, desc);
    }

    public Set<ConstructorDescriptor> getConstrainedConstructors() {
        return new HashSet(methodConstraints.values());
    }

    public void putConstructorDescriptor(Constructor cons, ConstructorDescriptorImpl desc) {
        constructorConstraints.put(cons, desc);
    }

    public Map<Method, MethodDescriptorImpl> getMethodConstraints() {
        return methodConstraints;
    }

    public Map<Constructor, ConstructorDescriptorImpl> getConstructorConstraints() {
        return constructorConstraints;
    }
}
