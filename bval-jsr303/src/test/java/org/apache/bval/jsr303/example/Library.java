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
package org.apache.bval.jsr303.example;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Description: <br/>
 * User: roman <br/>
 * Date: 06.10.2009 <br/>
 * Time: 12:35:42 <br/>
 * Copyright: Agimatec GmbH
 */
public class Library {
    @NotNull
    private String libraryName;
    @Valid
    private final Map<String,Book> taggedBooks = new HashMap();
    
    private Person[] persons;

    public String getLibraryName() {
        return libraryName;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    public Map<String, Book> getTaggedBooks() {
        return taggedBooks;
    }

    public Person[] getPersons() {
        return persons;
    }

    public void setPersons(Person[] persons) {
        this.persons = persons;
    }

    @Valid
    public List<Employee> getEmployees() {
        if(persons == null) return Collections.emptyList();

        ArrayList<Employee> emps = new ArrayList(persons.length);
        for(Person each : persons) {
            if(each instanceof Employee) emps.add((Employee) each);
        }
        return emps;
    }
}
