/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.validation;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
/*import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.validation.ResponseConstraintViolationException;*/

//@Provider
public class ValidationExceptionMapper {


    //@Override
    public String toResponse(ValidationException exception) {
        //if (exception instanceof ConstraintViolationException) {

            /*final ConstraintViolationException constraint = (ConstraintViolationException) exception;
            final boolean isResponseException = constraint instanceof ResponseConstraintViolationException;

            for (final ConstraintViolation< ? > violation: constraint.getConstraintViolations()) {
                LOG.log(Level.WARNING,
                    violation.getRootBeanClass().getSimpleName()
                        + "." + violation.getPropertyPath()
                        + ": " + violation.getMessage());
            }

            if (isResponseException) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }

            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }*/return "";


    }
}
