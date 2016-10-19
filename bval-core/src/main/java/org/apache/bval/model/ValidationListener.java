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
package org.apache.bval.model;


import java.io.Serializable;


/**
 * Description: The interface to collect errors found during validation<br/>
 */
public interface ValidationListener {
    /**
     * Simple API to add an error reason during validation.
     * Error notification added from a {@link org.apache.bval.model.Validation} with context information
     * taken from the given {@link org.apache.bval.model.ValidationContext}.
     *
     * @param reason  a constant describing the reason. This is normally the key of the
     *                feature that was violated in the object 'owner' for property 'propertyName'
     * @param context - contains
     *                bean =         the object that contains the error (owner)
     *                propertyName = the Name of the attribute that caused the error
     */
    <T extends ValidationListener> void addError(String reason, ValidationContext<T> context);

    /** Alternative method to add a fully initialized {@link ValidationListener.Error} object. */
    <T extends ValidationListener> void addError(Error error, ValidationContext<T> context);

    /**
     * An object holding a single validation constraint violation
     * found during the validation process.
     */
    public class Error implements Serializable {
        private static final long serialVersionUID = 1L;

        /** Reason */
        final String reason;
        /** Owner */
        final Object owner;
        /** Property name*/
        final String propertyName;

        /**
         * Create a new Error instance.
         * @param aReason
         * @param aOwner
         * @param aPropertyName
         */
        public Error(String aReason, Object aOwner, String aPropertyName) {
            this.reason = aReason;
            this.owner = aOwner;
            this.propertyName = aPropertyName;
        }

        /**
         * Get the reason.
         * @return String
         */
        public String getReason() {
            return reason;
        }

        /**
         * Get the owner.
         * @return Object
         */
        public Object getOwner() {
            return owner;
        }

        /**
         * Get the propertyName.
         * @return String
         */
        public String getPropertyName() {
            return propertyName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Error{" + "reason='" + reason + '\'' + ", propertyName='" +
                  propertyName + '\'' + '}';
        }
    }
}
