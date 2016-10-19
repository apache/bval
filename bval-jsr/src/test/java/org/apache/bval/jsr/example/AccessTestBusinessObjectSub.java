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

/**
 * Description: <br/>
 */
public class AccessTestBusinessObjectSub extends AccessTestBusinessObject {
    private String var2, _var2;

    public void setVar2(String var2) {
        this.var2 = var2;
    }

    public void setvar2(String _var2) {
        this._var2 = _var2;
    }

    public AccessTestBusinessObjectSub(String var1) {
        super(var1);
    }

    // getVar1() is called on subclass, although annotated on superclass    
    @Override
    public String getVar1() {
        return var1;
    }

    //// test that getvar2() is called, not getVar2()

    @HasValue("5")
    public String getvar2() {
        return _var2;
    }

    @HasValue("6")
    public String getVar2() {
        return var2;
    }
}
