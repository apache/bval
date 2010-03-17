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
package com.agimatec.validation.model;

/**
 * Description: Interface for a single validation <br/>
 * User: roman.stumm <br/>
 * Date: 06.07.2007 <br/>
 * Time: 10:04:39 <br/>
 * Copyright: Agimatec GmbH 2008
 */
public interface Validation {
    /**
     * Perform a single validation routine.
     * Validate the object or property according to the current ValidationContext.
     *
     * @param context - to access the property, value, constraints
     */
    void validate(ValidationContext context);
}
