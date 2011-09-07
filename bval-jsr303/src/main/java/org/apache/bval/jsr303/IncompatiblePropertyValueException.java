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
package org.apache.bval.jsr303;

import javax.validation.ValidationException;

/**
 * Internal exception thrown when trying to validate a value for a property for which it is not assignment-compatible.
 * 
 * @version $Rev: 1031833 $ $Date: 2010-11-05 16:53:03 -0500 (Fri, 05 Nov 2010) $
 * 
 * @author Matt Benson
 */
public class IncompatiblePropertyValueException extends ValidationException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new {@link IncompatiblePropertyValueException} instance.
     * 
     * @param message
     */
    public IncompatiblePropertyValueException(String message) {
        super(message);
    }

    /**
     * Create a new IncompatiblePropertyValueException instance.
     */
    public IncompatiblePropertyValueException() {
        super();
    }

    /**
     * Create a new IncompatiblePropertyValueException instance.
     * 
     * @param message
     * @param cause
     */
    public IncompatiblePropertyValueException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new IncompatiblePropertyValueException instance.
     * 
     * @param cause
     */
    public IncompatiblePropertyValueException(Throwable cause) {
        super(cause);
    }

}
