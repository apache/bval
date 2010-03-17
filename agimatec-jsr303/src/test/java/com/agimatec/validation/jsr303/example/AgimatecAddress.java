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

import com.agimatec.validation.constraints.AgimatecEmail;

/**
 * Description: <br/>
 * User: roman <br/>
 * Date: 28.10.2009 <br/>
 * Time: 11:58:37 <br/>
 * Copyright: Agimatec GmbH
 */
public class AgimatecAddress {
    @AgimatecEmail
    private String email;

    public AgimatecAddress() {
    }

    public AgimatecAddress(String email) {
        this.email = email;
    }

    // do not provided getters & setters to test that value access
    // of combined constraints directly use the private field 'email'
}
