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

import java.util.Collection;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.Assert;

/**
 * Description: <br/>
 */
public class TestUtils {
    /**
     * @param violations
     * @param propertyPath - string format of a propertyPath
     * @return the constraintViolation with the propertyPath's string representation given
     */
    public static ConstraintViolation getViolation(Set violations, String propertyPath)
    {
        for(ConstraintViolation each : (Set<ConstraintViolation>)violations) {
            if(each.getPropertyPath().toString().equals(propertyPath)) return each;
        }
        return null;
    }

    public static ConstraintViolation getViolationWithMessage(Set violations, String message)
    {
        for(ConstraintViolation each : (Set<ConstraintViolation>)violations) {
            if(each.getMessage().equals(message)) return each;
        }
        return null;
    }

    /**
     * assume set addition either does nothing, returning false per collection
     * contract, or throws an Exception; in either case size should remain
     * unchanged
     * 
     * @param collection
     */
    public static void failOnModifiable(Collection<?> collection, String description) {
        int size = collection.size();
        try {
            Assert
                .assertFalse(String.format("should not permit modification to %s", description), collection.add(null));
        } catch (Exception e) {
            // okay
        }
        Assert.assertEquals("constraint descriptor set size changed", size, collection.size());
    }

}
