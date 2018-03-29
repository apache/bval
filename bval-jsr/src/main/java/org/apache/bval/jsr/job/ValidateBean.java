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
package org.apache.bval.jsr.job;

import org.apache.bval.jsr.ApacheFactoryContext;
import org.apache.bval.jsr.ConstraintViolationImpl;
import org.apache.bval.jsr.GraphContext;
import org.apache.bval.jsr.descriptor.BeanD;
import org.apache.bval.jsr.descriptor.ConstraintD;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.util.Validate;

public final class ValidateBean<T> extends ValidationJob<T> {

    private final T bean;

    ValidateBean(ApacheFactoryContext validatorContext, T bean, Class<?>[] groups) {
        super(validatorContext, groups);
        this.bean = Validate.notNull(bean, IllegalArgumentException::new, "bean");
    }

    @Override
    protected Frame<BeanD<T>> computeBaseFrame() {
        return new BeanFrame<T>(new GraphContext(validatorContext, PathImpl.create(), bean));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<T> getRootBeanClass() {
        return (Class<T>) bean.getClass();
    }

    @Override
    ConstraintViolationImpl<T> createViolation(String messageTemplate, String message,
        ConstraintValidatorContextImpl<T> context, PathImpl propertyPath) {
        return new ConstraintViolationImpl<>(messageTemplate, message, bean,
            context.getFrame().getBean(), propertyPath, context.getFrame().context.getValue(),
            context.getConstraintDescriptor(), getRootBeanClass(),
            context.getConstraintDescriptor().unwrap(ConstraintD.class).getDeclaredOn(), null, null);
    }
}
