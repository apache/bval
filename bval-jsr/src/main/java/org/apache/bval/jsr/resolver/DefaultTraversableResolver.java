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
package org.apache.bval.jsr.resolver;

import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

import javax.validation.Path;
import javax.validation.TraversableResolver;

import java.lang.annotation.ElementType;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @see javax.validation.TraversableResolver */
@Privilizing(@CallTo(Reflection.class))
public class DefaultTraversableResolver implements TraversableResolver, CachingRelevant {
    private static final Logger log = Logger.getLogger(DefaultTraversableResolver.class.getName());
    private static final boolean LOG_FINEST = log.isLoggable(Level.FINEST);

    /** Class to load to check whether JPA 2 is on the classpath. */
    private static final String PERSISTENCE_UTIL_CLASSNAME = "javax.persistence.PersistenceUtil";

    /** Class to instantiate in case JPA 2 is on the classpath. */
    private static final String JPA_AWARE_TRAVERSABLE_RESOLVER_CLASSNAME =
        "org.apache.bval.jsr.resolver.JPATraversableResolver";

    private TraversableResolver jpaTR;

    /**
     * Create a new DefaultTraversableResolver instance.
     */
    public DefaultTraversableResolver() {
        initJpa();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReachable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
        Path pathToTraversableObject, ElementType elementType) {
        return jpaTR == null || jpaTR.isReachable(traversableObject, traversableProperty, rootBeanType,
            pathToTraversableObject, elementType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCascadable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
        Path pathToTraversableObject, ElementType elementType) {
        return jpaTR == null || jpaTR.isCascadable(traversableObject, traversableProperty, rootBeanType,
            pathToTraversableObject, elementType);
    }

    /** Tries to load detect and load JPA. */
    @SuppressWarnings("unchecked")
    private void initJpa() {
        final ClassLoader classLoader = Reflection.getClassLoader(DefaultTraversableResolver.class);
        try {
            Reflection.toClass(PERSISTENCE_UTIL_CLASSNAME, classLoader);
            if (LOG_FINEST) {
                log.log(Level.FINEST, String.format("Found %s on classpath.", PERSISTENCE_UTIL_CLASSNAME));
            }
        } catch (final Exception e) {
            log.log(Level.FINEST,
                String.format("Cannot find %s on classpath. All properties will per default be traversable.",
                    PERSISTENCE_UTIL_CLASSNAME));
            return;
        }

        try {
            Class<? extends TraversableResolver> jpaAwareResolverClass =
                (Class<? extends TraversableResolver>) Reflection.toClass(JPA_AWARE_TRAVERSABLE_RESOLVER_CLASSNAME,
                    classLoader);
            jpaTR = jpaAwareResolverClass.newInstance();
            if (LOG_FINEST) {
                log.log(Level.FINEST,
                    String.format("Instantiated an instance of %s.", JPA_AWARE_TRAVERSABLE_RESOLVER_CLASSNAME));
            }
        } catch (final Exception e) {
            log.log(Level.WARNING,
                String.format(
                    "Unable to load or instantiate JPA aware resolver %s. All properties will per default be traversable.",
                    JPA_AWARE_TRAVERSABLE_RESOLVER_CLASSNAME),
                e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean needsCaching() {
        return jpaTR != null && CachingTraversableResolver.needsCaching(jpaTR);
    }
}
