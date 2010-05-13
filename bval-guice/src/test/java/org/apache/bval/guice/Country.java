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
package org.apache.bval.guice;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 
 *
 * @version $Id$
 */
public class Country {

    @NotNull(groups = { Update.class })
    private Long id;

    @NotNull(groups = { Insert.class })
    private String name;

    @NotNull(groups = { Insert.class })
    @Size(max = 2, groups = { Insert.class, Update.class })
    private String iso2Code;

    @NotNull(groups = { Insert.class })
    @Size(max = 3, groups = { Insert.class, Update.class })
    private String iso3Code;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIso2Code() {
        return iso2Code;
    }

    public void setIso2Code(String iso2Code) {
        this.iso2Code = iso2Code;
    }

    public String getIso3Code() {
        return iso3Code;
    }

    public void setIso3Code(String iso3Code) {
        this.iso3Code = iso3Code;
    }

}