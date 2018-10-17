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
package org.apache.bval.jsr;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.validation.ValidationException;

import org.apache.bval.cdi.BValExtension;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.util.Validate;
import org.apache.commons.weaver.privilizer.Privileged;

/**
 * Factory object for helper/participant classes. The typical pattern is that this factory loads an instance of a class
 * by name, taking into account whether Apache BVal is operating in a CDI environment.
 */
class ParticipantFactory implements Closeable {
    private static final Logger log = Logger.getLogger(ParticipantFactory.class.getName());
    private static final String META_INF_SERVICES = "META-INF/services/";

    private final Collection<BValExtension.Releasable<?>> releasables = new CopyOnWriteArrayList<>();
    private final List<ClassLoader> loaders;

    ParticipantFactory(ClassLoader... loaders) {
        super();
        this.loaders = Arrays.asList(loaders).stream().filter(Objects::nonNull).collect(ToUnmodifiable.list());
        Validate.validState(!this.loaders.isEmpty(), "no classloaders available");
    }

    @Override
    public void close() throws IOException {
        for (final BValExtension.Releasable<?> releasable : releasables) {
            releasable.release();
        }
        releasables.clear();
    }

    <T> T create(String classname) {
        return newInstance(loadClass(classname));
    }

    <T> Set<T> loadServices(Class<T> type) {
        Validate.notNull(type);
        final Set<URL> resources = new LinkedHashSet<>();
        final String resourceName = META_INF_SERVICES + type.getName();
        for (ClassLoader loader : loaders) {
            try {
                for (Enumeration<URL> urls = loader.getResources(resourceName); urls.hasMoreElements();) {
                    resources.add(urls.nextElement());
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "Error searching for resource(s) " + resourceName, e);
            }
        }
        return resources.stream().map(this::read).flatMap(Collection::stream).<T> map(this::create)
            .collect(ToUnmodifiable.set());
    }

    private Set<String> read(URL url) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return r.lines().map(String::trim).filter(line -> line.charAt(0) != '#').collect(Collectors.toSet());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to read resource " + url, e);
            return Collections.emptySet();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> loadClass(final String className) {
        for (ClassLoader loader : loaders) {
            try {
                return (Class<T>) Class.forName(className, true, loader);
            } catch (final ClassNotFoundException ex) {
            }
        }
        throw new ValidationException("Unable to load class " + className);
    }

    @Privileged
    private <T> T newInstance(final Class<T> cls) {
        try {
            final BValExtension.Releasable<T> releasable = BValExtension.inject(cls);
            releasables.add(releasable);
            return releasable.getInstance();
        } catch (Exception | NoClassDefFoundError e) {
        }
        try {
            return cls.getConstructor().newInstance();
        } catch (final Exception e) {
            throw new ValidationException(e.getMessage(), e);
        }
    }
}
