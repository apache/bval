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
package org.apache.bval.cdi;

import org.apache.bval.jsr303.util.Proxies;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.MethodDescriptor;
import java.lang.reflect.Method;
import java.util.Set;

@Interceptor
@BValBinding
public class BValInterceptor {
    @Inject
    private Validator validator;

    @AroundInvoke
    public Object around(final InvocationContext context) throws Throwable {
        final ExecutableValidator ev = validator.forExecutables();

        final Method method = context.getMethod();
        final MethodDescriptor constraintsForMethod = validator.getConstraintsForClass(Proxies.classFor(method.getDeclaringClass())).getConstraintsForMethod(method.getName(), method.getParameterTypes());
        if (constraintsForMethod == null) {
            return context.proceed();
        }

        {
            final Set<ConstraintViolation<Object>> violations = ev.validateParameters(context.getTarget(), method, context.getParameters());
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        final Object result = context.proceed();

        {
            final Set<ConstraintViolation<Object>> violations = ev.validateReturnValue(context.getTarget(), method, result);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        return result;
    }
}
