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
package org.apache.bval.jsr.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;

import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintValidator;

import org.apache.bval.util.Exceptions;
import org.apache.bval.util.reflection.TypeUtils;

public abstract class ValidatorMappingProvider {

    public final <A extends Annotation> ValidatorMapping<A> getValidatorMapping(Class<A> constraintType) {
        final Optional<ValidatorMapping<A>> result =
            Optional.ofNullable(this.<A> doGetValidatorMapping(constraintType));
        if (result.isPresent()) {
            for (Class<? extends ConstraintValidator<A, ?>> t : result.get().getValidatorTypes()) {
                final Type constraintParameter = TypeUtils.getTypeArguments(t, ConstraintValidator.class)
                    .get(ConstraintValidator.class.getTypeParameters()[0]);

                Exceptions.raiseUnless(constraintType.equals(constraintParameter), ConstraintDefinitionException::new,
                    "%s %s expected first type parameter of %s, %s; source %s", ConstraintValidator.class, t,
                    constraintType, constraintParameter, result.get().getSource());
            }
            return result.get();
        }
        return null;
    }

    protected abstract <A extends Annotation> ValidatorMapping<A> doGetValidatorMapping(Class<A> constraintType);
}
