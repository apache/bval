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
package org.apache.bval.jsr.parameter;

import javax.validation.ParameterNameProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DefaultParameterNameProvider implements ParameterNameProvider {
    private static final String ARG = "arg";

    @Override
    public List<String> getParameterNames(Constructor<?> constructor) {
        return names(constructor.getParameterTypes().length);
    }

    @Override
    public List<String> getParameterNames(Method method) {
        return names(method.getParameterTypes().length);
    }

    private static List<String> names(final int length) {
        final List<String> list = new ArrayList<String>();
        for (int i = 0; i < length; i++) {
            list.add(ARG + i);
        }
        return list;
    }
}
