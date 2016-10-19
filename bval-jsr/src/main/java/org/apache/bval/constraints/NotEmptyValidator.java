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
package org.apache.bval.constraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Description:  Check the non emptyness of an
 * any object that has a public isEmpty():boolean or a valid toString() method
 */
public class NotEmptyValidator implements ConstraintValidator<NotEmpty, Object> {
    @Override
    public void initialize(NotEmpty constraintAnnotation) {
        // do nothing
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) > 0;
        }
        try {
            final Method isEmptyMethod = value.getClass().getMethod("isEmpty");
            if (isEmptyMethod != null) {
                return !((Boolean) isEmptyMethod.invoke(value)).booleanValue();
            }
        } catch (IllegalAccessException iae) {
            // do nothing
        } catch (NoSuchMethodException nsme) {
            // do nothing
        } catch (InvocationTargetException ite) {
            // do nothing
        }
        return !value.toString().isEmpty();
    }
}
