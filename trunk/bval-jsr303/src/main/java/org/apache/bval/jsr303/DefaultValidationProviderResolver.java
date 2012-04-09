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
package org.apache.bval.jsr303;


import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.spi.ValidationProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class DefaultValidationProviderResolver implements ValidationProviderResolver {

    //TODO - Spec recommends caching per classloader
    private static final String SPI_CFG =
            "META-INF/services/javax.validation.spi.ValidationProvider";

    /**
     * {@inheritDoc}
     */
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
                URL url = cfgs.nextElement();
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
                                final Class<?> provider = cl.loadClass(line);
                                // create an instance to return
                                ValidationProvider<?> vp =
                                        AccessController.doPrivileged(new PrivilegedAction<ValidationProvider<?>>() {
                                            public ValidationProvider<?> run() {
                                                try {
                                                    return (ValidationProvider<?>) provider.newInstance();
                                                } catch (final Exception ex) {
                                                    throw new ValidationException("Cannot instantiate : " + provider, ex);
                                                }
                                            }
                                        });
                                 providers.add(vp);

                            } catch (ClassNotFoundException e) {
                                throw new ValidationException("Failed to load provider " +
                                        line + " configured in file " + url, e);
                            }
                        }
                        line = br.readLine();
                    }
                    br.close();
                } catch (IOException e) {
                    throw new ValidationException("Error trying to read " + url, e);
                } finally {
                    if (br != null)
                        br.close();
                }
            }
        } catch (IOException e) {
            throw new ValidationException("Error trying to read a " + SPI_CFG, e);
        }
        // caller must handle the case of no providers found
        return providers;
    }
}
