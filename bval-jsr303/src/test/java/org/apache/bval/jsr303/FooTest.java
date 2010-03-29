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
package org.apache.bval.jsr303;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import junit.framework.TestCase;

/**
 * Description: <br/>
 * User: roman.stumm <br/>
 * Date: 08.09.2008 <br/>
 * Time: 13:44:58 <br/>
 * Copyright: Agimatec GmbH
 */
public class FooTest extends TestCase {

    @Valid
    private Collection<Foo> foos = new ArrayList<Foo>();

    public FooTest() {
        foos.add(new Foo("foo1"));
        foos.add(null);
        foos.add(new Foo("foo3"));
    }


    public class Foo {
        @NotNull
        public String bar;

        public Foo(String bar) {
            this.bar = bar;
        }

    }

    public void testValidation() {
        FooTest t = new FooTest();

        Validator v = ApacheValidatorFactory.getDefault().getValidator();
        Set<ConstraintViolation<FooTest>> errors = v.validate(t);
        System.out.println("got errors:");
        for (ConstraintViolation error : errors) {
            System.out.println(error.getPropertyPath());
        }
    }
}