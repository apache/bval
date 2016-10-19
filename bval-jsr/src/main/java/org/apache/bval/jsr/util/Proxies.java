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
package org.apache.bval.jsr.util;

public final class Proxies {
    // get rid of proxies which probably contains wrong annotation metamodel
    public static <T> Class<?> classFor(final Class<?> clazz) { // TODO: do we want a SPI with impl for guice, owb, openejb, ...?
        if (isProxyClass(clazz)) {
            final Class<?> parent = clazz.getSuperclass();
            if (parent != null) {
                return classFor(clazz.getSuperclass());
            }
        }
        return clazz;
    }

    private static boolean isProxyClass(Class<?> clazz) {
        return clazz.getSimpleName().contains("$$");// a lot of proxies use this convention to avoid conflicts with inner/anonymous classes
    }

    private Proxies() {
        // no-op
    }
}
