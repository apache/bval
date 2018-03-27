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
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

@Privilizing(@CallTo(Reflection.class))
public final class Methods {
    public static boolean isGetter(Method m) {
        if (Modifier.isStatic(m.getModifiers()) || m.getParameterCount() > 0) {
            return false;
        }
        // TODO look for capital letter after verb?
        if (Boolean.TYPE.equals(m.getReturnType()) && m.getName().startsWith("is")) {
            return true;
        }
        return !Void.TYPE.equals(m.getReturnType()) && m.getName().startsWith("get");
    }

    public static boolean isGetter(String methodName) {
        Validate.notNull(methodName);
        final int len = methodName.length();
        return len > 2 && methodName.startsWith("is") || len > 3 && methodName.startsWith("get");
    }

    public static String propertyName(Method getter) {
        Validate.isTrue(isGetter(getter), "%s is not a getter", getter);
        return propertyName(getter.getName());
    }

    public static String propertyName(String methodName) {
        Validate.isTrue(isGetter(methodName), "%s does not represent a property getter");
        final String suffix = methodName.startsWith("is") ? methodName.substring(2) : methodName.substring(3);
        return Introspector.decapitalize(suffix);
    }

    public static Method getter(Class<?> clazz, String property) {
        return Reflection.find(clazz, t -> Stream.of(Reflection.getDeclaredMethods(t)).filter(Methods::isGetter)
            .filter(m -> property.equals(Methods.propertyName(m))).findFirst().orElse(null));
    }

    private Methods() {
    }
}
