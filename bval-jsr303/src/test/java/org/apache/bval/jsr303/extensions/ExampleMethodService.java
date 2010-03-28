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
package org.apache.bval.jsr303.extensions;


import javax.validation.constraints.NotNull;

import org.apache.bval.constraints.NotEmpty;

/**
 * Description: class with annotated methods to demonstrate
 * method-level-validation<br/>
 * User: roman <br/>
 * Date: 01.02.2010 <br/>
 * Time: 10:05:12 <br/>
 * Copyright: Agimatec GmbH
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
}
