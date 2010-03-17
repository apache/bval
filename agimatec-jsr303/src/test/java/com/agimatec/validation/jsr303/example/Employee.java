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
package com.agimatec.validation.jsr303.example;

/**
 * Description: <br/>
 * User: roman <br/>
 * Date: 06.10.2009 <br/>
 * Time: 13:41:43 <br/>
 * Copyright: Agimatec GmbH
 */
public class Employee implements Person {
    private String firstName, lastName;

    public Employee(String firstN, String lastN) {
        this.firstName = firstN;
        this.lastName = lastN;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return null;  // not supported
    }

    public String getLastName() {
        return lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
