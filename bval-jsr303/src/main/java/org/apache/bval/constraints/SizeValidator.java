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

import javax.validation.ValidationException;
import javax.validation.constraints.Size;

/**
 * Description: Abstract validator impl. for @Size annotation<br/>
 */
public abstract class SizeValidator {
    protected int min;
    protected int max;

    /**
     * Configure the constraint validator based on the elements
     * specified at the time it was defined.
     *
     * @param constraint the constraint definition
     */
    public void initialize(Size constraint) {
        min = constraint.min();
        max = constraint.max();
        if (min < 0) throw new ValidationException("Min cannot be negative");
        if (max < 0) throw new ValidationException("Max cannot be negative");
        if (max < min) throw new ValidationException("Max cannot be less than Min");
    }
}