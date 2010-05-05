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
package org.apache.bval.jsr303.util;

import javax.validation.Constraint;
import javax.validation.ConstraintDefinitionException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Internal validator that ensures the correct definition of constraint
 * annotations.
 * 
 * @author Carlos Vara
 */
public class ConstraintDefinitionValidator {

    /**
     * Ensures that the constraint definition is valid.
     * 
     * @param annotation
     *            An annotation which is annotated with {@link Constraint}.
     * @throws ConstraintDefinitionException
     *             In case the constraint is invalid.
     */
    public static void validateConstraintDefinition(Annotation annotation) {
        validGroups(annotation);
        validPayload(annotation);
        validMessage(annotation);
        validAttributes(annotation);
    }
    
    /**
     * Check that the annotation:
     * <ul>
     * <li>Has a groups() method.</li>
     * <li>Whose default value is an empty Class[] array.</li>
     * </ul>
     * 
     * @param annotation
     *            The annotation to check.
     */
    private static void validGroups(Annotation annotation) {
        // Ensure that it has a groups() method...
        Method groupsMethod = SecureActions.getMethod(annotation.annotationType(), "groups");
        if ( groupsMethod == null ) {
            throw new ConstraintDefinitionException("Constraint definition " + annotation + " has no groups() method");
        }
        
        // ...whose default value is an empty array
        Object defaultGroupsValue = groupsMethod.getDefaultValue();
        if ( defaultGroupsValue instanceof Class<?>[] ) {
            if ( ((Class[]) defaultGroupsValue).length != 0 ) {
                throw new ConstraintDefinitionException("Default value for groups() must be an empty array");
            }
        }
        else {
            throw new ConstraintDefinitionException("Return type for groups() must be of type Class<?>[]");
        }
    }
    
    /**
     * Check that the annotation:
     * <ul>
     * <li>Has a payload() method.</li>
     * <li>Whose default value is an empty Class[] array.</li>
     * </ul>
     * 
     * @param annotation
     *            The annotation to check.
     */
    private static void validPayload(Annotation annotation) {
        // Ensure that it has a payload() method...
        Method payloadMethod = SecureActions.getMethod(annotation.annotationType(), "payload");
        if ( payloadMethod == null ) {
            throw new ConstraintDefinitionException("Constraint definition " + annotation + " has no payload() method");
        }
        
        // ...whose default value is an empty array
        Object defaultPayloadValue = payloadMethod.getDefaultValue();
        if ( defaultPayloadValue instanceof Class<?>[] ) {
            if ( ((Class[]) defaultPayloadValue).length != 0 ) {
                throw new ConstraintDefinitionException("Default value for payload() must be an empty array");
            }
        }
        else {
            throw new ConstraintDefinitionException("Return type for payload() must be of type Class<? extends Payload>[]");
        }
    }
    
    /**
     * Check that the annotation:
     * <ul>
     * <li>Has a message() method.</li>
     * <li>Whose default value is a {@link String}.</li>
     * </ul>
     * 
     * @param annotation
     *            The annotation to check.
     */
    private static void validMessage(Annotation annotation) {
        // Ensure that it has a message() method...
        Method messageMethod = SecureActions.getMethod(annotation.annotationType(), "message");
        if ( messageMethod == null ) {
            throw new ConstraintDefinitionException("Constraint definition " + annotation + " has no message() method");
        }
        
        // ...whose default value is a String
        Object defaultMessageValue = messageMethod.getDefaultValue();
        if ( !(defaultMessageValue instanceof String) ) {
            throw new ConstraintDefinitionException("Return type for message() must be of type String");
        }
    }
    
    /**
     * Check that the annotation has no methods that start with "valid".
     * 
     * @param annotation
     *            The annotation to check.
     */
    private static void validAttributes(Annotation annotation) {
        Method[] methods = SecureActions.getDeclaredMethods(annotation.annotationType());
        for ( Method method : methods ) {
            // Currently case insensitive, the spec is unclear about this
            if ( method.getName().toLowerCase(Locale.ENGLISH).startsWith("valid") ) {
                throw new ConstraintDefinitionException("A constraint annotation cannot have methods which start with 'valid'");
            }
        }
    }

}
