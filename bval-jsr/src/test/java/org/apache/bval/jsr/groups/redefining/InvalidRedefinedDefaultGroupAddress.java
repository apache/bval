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
package org.apache.bval.jsr.groups.redefining;

import javax.validation.GroupSequence;
import javax.validation.constraints.NotNull;


/**
 * If a @GroupSequence redefining the Default group for a class A does not
 * contain the group A, a GroupDefinitionException is raised when the class is
 * validated or when its metadata is requested.
 */
@GroupSequence({Address.class, Address.HighLevelCoherence.class})
public class InvalidRedefinedDefaultGroupAddress {
    @NotNull(groups = Address.HighLevelCoherence.class)
    private String street;

    @NotNull
    private String city;

    /**
     * check coherence on the overall object
     * Needs basic checking to be green first
     */
    public interface HighLevelCoherence {}

}
