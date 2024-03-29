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

import org.apache.bval.constraints.ZipCodeCityCoherence;
import org.apache.bval.jsr.example.ZipCodeCityCarrier;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Example 3.6. Redefining Default group for Address:
 * To redefine Default for a class, place a @GroupSequence annotation on the class ; 
 * this sequence expresses the sequence of groups that does
 * substitute Default for this class.
 */
@GroupSequence({ Address.class, Address.HighLevelCoherence.class, Address.ExtraCareful.class })
@ZipCodeCityCoherence(groups = Address.HighLevelCoherence.class)
public class Address implements ZipCodeCityCarrier {

    /**
     * check coherence on the overall object
     * Needs basic checking to be green first
     */
    public interface HighLevelCoherence {
    }

    /**
     * Extra-careful validation group.
     */
    public interface ExtraCareful {
    }

    @NotNull
    @Size(max = 50, min = 1, groups = ExtraCareful.class)
    private String street1;

    @NotNull
    private String zipCode;

    @NotNull
    @Size(max = 30)
    private String city;

    public String getStreet1() {
        return street1;
    }

    public void setStreet1(String street1) {
        this.street1 = street1;
    }

    @Override
    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    @Override
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}