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
package org.apache.bval.util;

import java.net.URL;

/**
 * Basic classloader to wrap multiple classloader into one.
 *
 * This class is intended to support resource and class lookups in multiple classloaders instead of just one (TCCL or caller class loader),
 * because not all environments (ie. OSGi) honor or use thread context class loaders which are common in web development.
 */
public class CompoundClassLoader extends ClassLoader {

    private final ClassLoader[] classLoaders;

    public CompoundClassLoader(Class<?> caller) {
        this(Thread.currentThread().getContextClassLoader(), caller.getClassLoader());
    }

    public CompoundClassLoader(ClassLoader ... classLoaders) {
        this.classLoaders = classLoaders;
    }

    @Override
    public URL getResource(String name) {
        URL resource = super.getResource(name);

        if (resource != null) {
            return resource;
        }

        for (ClassLoader loader : classLoaders) {
            if (loader == null) continue;

            resource = loader.getResource(name);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (ClassLoader loader : classLoaders) {
            if (loader == null) continue;
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                // this may happen
            }
        }

        return super.loadClass(name);
    }
}
