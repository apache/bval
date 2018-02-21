/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval.jsr.util;

import java.beans.Introspector;
import java.lang.reflect.Method;

import org.apache.bval.util.Validate;

public final class Methods {
    public static boolean isGetter(Method m) {
        if (m.getParameterCount() > 0) {
            return false;
        }
        // TODO look for capital letter after verb?
        if (Boolean.TYPE.equals(m.getReturnType()) && m.getName().startsWith("is")) {
            return true;
        }
        return !Void.TYPE.equals(m.getReturnType()) && m.getName().startsWith("get");
    }

    public static String propertyName(Method getter) {
        Validate.isTrue(isGetter(getter), "%s is not a getter", getter);
        final String name = getter.getName();
        final String suffix = name.startsWith("is") ? name.substring(2) : name.substring(3);
        return Introspector.decapitalize(suffix);
    }

    private Methods() {
    }
}
