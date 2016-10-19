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
package org.apache.bval.jsr.example;


import org.apache.bval.constraints.ZipCodeCityCoherence;

import javax.validation.GroupSequence;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;

@ZipCodeCityCoherence
public class Address implements ZipCodeCityCarrier {
    @NotNull
    @Size(max = 30)
    private String addressline1;
    @Size(max = 30)
    private String addressline2;
    @Size(max = 11)
    private String zipCode;
    @NotNull
    @Valid
    private Country country;
    private String city;

    public String getAddressline1() {
        return addressline1;
    }

    public void setAddressline1(String addressline1) {
        this.addressline1 = addressline1;
    }

    public String getAddressline2() {
        return addressline2;
    }

    public void setAddressline2(String addressline2) {
        this.addressline2 = addressline2;
    }

    @Override
    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    @Override
    @Size(max = 30)
    @NotNull
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    /**
     * Check coherence on the overall object
     * Needs basic checking to be green first
     */
    public interface HighLevelCoherence {
    }

    /**
     * Check both basic constraints and high level ones.
     * High level constraints are not checked if basic constraints fail.
     */
    @GroupSequence(value = {Default.class, HighLevelCoherence.class})
    public interface Complete {
    }
}
