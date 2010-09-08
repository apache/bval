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
package org.apache.bval.example;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * Description: <br/>
 */
public class BusinessObject {
    private long userId;
    private int numericValue;
    private String firstName, lastName, title;
    private Date dateBirth;
    private Timestamp validTo;
    private String email;
    private BusinessEnum choice;
    private BusinessObjectAddress address;
    private List<BusinessObjectAddress> addresses;
    private List properties;

    public BusinessEnum getChoice() {
        return choice;
    }

    public void setChoice(BusinessEnum choice) {
        this.choice = choice;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getDateBirth() {
        return dateBirth;
    }

    public void setDateBirth(Date dateBirth) {
        this.dateBirth = dateBirth;
    }

    public Timestamp getValidTo() {
        return validTo;
    }

    public void setValidTo(Timestamp validTo) {
        this.validTo = validTo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BusinessObjectAddress getAddress() {
        return address;
    }

    public void setAddress(BusinessObjectAddress address) {
        this.address = address;
    }

    public void setNumericValue(int newNumericValue) {
        numericValue = newNumericValue;
    }

    public int getNumericValue() {
        return numericValue;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public List<BusinessObjectAddress> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<BusinessObjectAddress> addresses) {
        this.addresses = addresses;
    }

    public List getProperties() {
        return properties;
    }

    public void setProperties(List properties) {
        this.properties = properties;
    }
}