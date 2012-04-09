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

import javax.validation.metadata.BeanDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Description: Provides method/constructor-related constraint information
 * for a type.  This class will disappear when such
 * functionality is part of the JSR303 specification.<br/>
 */
public interface MethodBeanDescriptor extends BeanDescriptor {
    /**
     * Get the constraints that apply to a particular method.
     * @param method
     * @return {@link MethodDescriptor}
     */
    MethodDescriptor getConstraintsForMethod(Method method);

    /**
     * Get the constraints that apply to a particular constructor.
     * @param constructor
     * @return {@link ConstructorDescriptor}
     */
    ConstructorDescriptor getConstraintsForConstructor(Constructor<?> constructor);

    /**
     * Get the set of constrained methods.
     * @return {@link Set} of {@link MethodDescriptor}
     */
    Set<MethodDescriptor> getConstrainedMethods();

    /**
     * Get the set of constrained constructors.
     * @return {@link Set} of {@link ConstructorDescriptor}
     */
    Set<ConstructorDescriptor> getConstrainedConstructors();
}
