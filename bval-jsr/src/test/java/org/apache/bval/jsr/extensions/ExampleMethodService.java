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
package org.apache.bval.jsr.extensions;

import org.apache.bval.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Description: class with annotated methods to demonstrate
 * method-level-validation<br/>
 */
public class ExampleMethodService {
    public ExampleMethodService() {
    }

    public ExampleMethodService(@NotNull @NotEmpty String s1, @NotNull String s2) {
    }

    @NotNull
    @NotEmpty
    public String concat(@NotNull @NotEmpty String s1, @NotNull String s2) {
        return s1 + s2;
    }

    public void save(@Pattern(regexp = "[a-f0-9]{4}") String data) {
        return;
    }

    @NotNull
    @Size(min = 3, max = 10)
    public String echo(@NotNull @Size(min = 3, max = 10) String str) {
        return str;
    }

    public void personOp1(@Valid Person p) {
        return;
    }

    public void personOp2(@NotNull @Valid Person p) {
        return;
    }

    public static class Person {
        @NotNull
        String name;
    }

}
