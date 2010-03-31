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
package org.apache.bval.model;


import java.io.Serializable;


/**
 * Description: The interface to collect errors found during validation<br/>
 */
public interface ValidationListener {
    /**
     * Simple API to add an error reason during validation.
     * Error notification added from a {@link Validation} with context information
     * taken from the given {@link ValidationContext}.
     *
     * @param reason  a constant describing the reason. This is normally the key of the
     *                feature that was violated in the object 'owner' for property 'propertyName'
     * @param context - contains
     *                bean =         the object that contains the error (owner)
     *                propertyName = the Name of the attribute that caused the error
     */
    void addError(String reason, ValidationContext context);

    /** Alternative method to add a fully initialized {@link ValidationListener.Error} object. */
    void addError(Error error, ValidationContext context);

    /**
     * an object holding a single validation constraint violation
     * found during the validation process.
     */
    public class Error implements Serializable {
        final String reason;
        final Object owner;
        final String propertyName;

        public Error(String aReason, Object aOwner, String aPropertyName) {
            this.reason = aReason;
            this.owner = aOwner;
            this.propertyName = aPropertyName;
        }

        public String getReason() {
            return reason;
        }

        public Object getOwner() {
            return owner;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String toString() {
            return "Error{" + "reason='" + reason + '\'' + ", propertyName='" +
                  propertyName + '\'' + '}';
        }
    }
}
