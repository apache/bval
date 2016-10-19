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


import org.apache.bval.constraints.HasValue;

import javax.validation.Valid;

/**
 * Description: <br/>
 */
public class AccessTestBusinessObject {
    // test that field-access is used, not method-access 
    @HasValue({"1", "3"})
    protected String var1;

    // test that field-access is used, not method-access
    @Valid
    private AccessTestBusinessObject next;

    // not annotated with @Valid, not validated!!
    private AccessTestBusinessObject toBeIgnored;
    private AccessTestBusinessObject _next;

    public AccessTestBusinessObject(String var1) {
        this.var1 = var1;
    }

    @HasValue("3")
    public String getVar1() {
        return "3";
    }

    public void next(AccessTestBusinessObject next) {
        this._next = next;
    }


    public void setNext(AccessTestBusinessObject next) {
        this.next = next;
    }

    @Valid
    public AccessTestBusinessObject getNext() {
        return _next; // method returns '_next', not the field 'next'
    }

    public AccessTestBusinessObject getToBeIgnored() {
        return toBeIgnored;
    }

    public void setToBeIgnored(AccessTestBusinessObject toBeIgnored) {
        this.toBeIgnored = toBeIgnored;
    }
}
