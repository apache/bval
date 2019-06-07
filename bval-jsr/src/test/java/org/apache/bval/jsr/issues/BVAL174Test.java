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
package org.apache.bval.jsr.issues;

import java.lang.reflect.Method;
import javax.validation.UnexpectedTypeException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.metadata.MethodDescriptor;
import org.apache.bval.jsr.ApacheValidationProvider;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BVAL174Test {
    
    private Validator getValidator() {
        return Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory().getValidator();
    }
    
    @Test(expected = UnexpectedTypeException.class)
    public void testValidateReturnValue() throws NoSuchMethodException {
        Validator validator = getValidator();        
        
        BVAL174 service = new BVAL174();
        Method getMovie = service.getClass().getMethod("getMovie");
        Method addMovie = service.getClass().getMethod("addMovie", String.class);
        
        MethodDescriptor getMovieConstraints = validator.getConstraintsForClass(service.getClass())
            .getConstraintsForMethod(getMovie.getName(), getMovie.getParameterTypes());
        
        assertTrue(getMovieConstraints == null);
        
        MethodDescriptor addMovieConstraints = validator.getConstraintsForClass(service.getClass())
            .getConstraintsForMethod(addMovie.getName(), addMovie.getParameterTypes());
        
        assertTrue(addMovieConstraints == null);
    }

}
