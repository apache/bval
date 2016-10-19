/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.bval.jsr;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.spi.ValidationProvider;

import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

@Privilizing(@CallTo(Reflection.class))
public class DefaultValidationProviderResolver implements ValidationProviderResolver {

    //TODO - Spec recommends caching per classloader
    private static final String SPI_CFG =
        "META-INF/services/javax.validation.spi.ValidationProvider";

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ValidationProvider<?>> getValidationProviders() {
        List<ValidationProvider<?>> providers = new ArrayList<ValidationProvider<?>>();
        try {
            // get our classloader
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null)
                cl = DefaultValidationProviderResolver.class.getClassLoader();
            // find all service provider cfgs
            Enumeration<URL> cfgs = cl.getResources(SPI_CFG);
            while (cfgs.hasMoreElements()) {
                final URL url = cfgs.nextElement();
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(url.openStream()), 256);
                    String line = br.readLine();
                    // cfgs may contain multiple providers and/or comments
                    while (line != null) {
                        line = line.trim();
                        if (!line.startsWith("#")) {
                            try {
                                // try loading the specified class
                                @SuppressWarnings("rawtypes")
                                final Class<? extends ValidationProvider> providerType =
                                    cl.loadClass(line).asSubclass(ValidationProvider.class);
                                // create an instance to return
                                providers.add(Reflection.newInstance(providerType.asSubclass(ValidationProvider.class)));

                            } catch (ClassNotFoundException e) {
                                throw new ValidationException("Failed to load provider " +
                                    line + " configured in file " + url, e);
                            }
                        }
                        line = br.readLine();
                    }
                } catch (IOException e) {
                    throw new ValidationException("Error trying to read " + url, e);
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
            }
        } catch (IOException e) {
            throw new ValidationException("Error trying to read a " + SPI_CFG, e);
        }
        // caller must handle the case of no providers found
        return providers;
    }
}
