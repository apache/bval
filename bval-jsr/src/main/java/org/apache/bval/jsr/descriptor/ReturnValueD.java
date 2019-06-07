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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.UnexpectedTypeException;
import javax.validation.metadata.ConstraintDescriptor;

import javax.validation.metadata.ReturnValueDescriptor;
import org.apache.bval.util.ValidatorUtils;
import org.apache.bval.util.reflection.TypeUtils;

public class ReturnValueD<P extends ExecutableD<?, ?, P>, E extends Executable> extends CascadableContainerD<P, E>
    implements ReturnValueDescriptor {

    private final Set<ConstraintD<?>> constraints;

    ReturnValueD(MetadataReader.ForContainer<E> reader, P parent) {
        super(reader, parent);
        this.constraints = new HashSet<>(reader.getConstraints());

        Class<?> validatedType;
        if (reader.meta.getHost() instanceof Constructor)
        {
            validatedType = reader.meta.getDeclaringClass();
        }
        else
        {
            validatedType = ((Method) reader.meta.getHost()).getReturnType();
        }
        
        for (ConstraintDescriptor<?> c : constraints)
        {
            if (!hasValidatorForType(validatedType, c)
                    && (!c.getConstraintValidatorClasses().isEmpty() || !c.getComposingConstraints().isEmpty()))
            {
                String msg = "No validator found for (composition) constraint @"
                        + c.getAnnotation().annotationType().getSimpleName()
                        + " declared on \"" + reader.meta.getHost().toString()
                        + "\" for validated type \"" + validatedType.getName() + "\"";
                throw new UnexpectedTypeException(msg);
            }
        }
    }

    private boolean hasValidatorForType(Class<?> validatedType, ConstraintDescriptor<?> c)
    {
        for (Class<? extends ConstraintValidator<?, ?>> validatorClass : c.getConstraintValidatorClasses())
        {
            if (TypeUtils.isAssignable(validatedType, ValidatorUtils.getValidatedType(validatorClass)))
            {
                return true;
            }
        }
        
        for (ConstraintDescriptor<?> composite : c.getComposingConstraints())
        {
            if (hasValidatorForType(validatedType, composite))
            {
                return true;
            }
        }

        return false;
    }
    
    @Override
    public boolean hasConstraints() {
        return !constraints.isEmpty();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        return (Set) constraints;
    }
}
