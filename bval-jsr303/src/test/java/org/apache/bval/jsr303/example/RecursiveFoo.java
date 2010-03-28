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

import org.apache.bval.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Description: <br/>
 * User: roman.stumm <br/>
 * Date: 05.09.2008 <br/>
 * Time: 11:02:24 <br/>
 * Copyright: Agimatec GmbH
 */
public class RecursiveFoo {
    @NotEmpty
    @Valid
    Collection<RecursiveFoo> foos = new ArrayList();

    public Collection<RecursiveFoo> getFoos() {
        return foos;
    }

    public void setFoos(Collection<RecursiveFoo> foos) {
        this.foos = foos;
    }
}
