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
package org.apache.bval.jsr303.resolver;

import org.apache.bval.jsr303.util.SecureActions;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.validation.Path;
import javax.validation.TraversableResolver;
import java.lang.annotation.ElementType;


/** @see javax.validation.TraversableResolver */
public class DefaultTraversableResolver implements TraversableResolver, CachingRelevant {
    private static final Log log = LogFactory.getLog(DefaultTraversableResolver.class);

    /** Class to load to check whether JPA 2 is on the classpath. */
    private static final String PERSISTENCE_UTIL_CLASSNAME =
          "javax.persistence.PersistenceUtil";

    /** Class to instantiate in case JPA 2 is on the classpath. */
    private static final String JPA_AWARE_TRAVERSABLE_RESOLVER_CLASSNAME =
          "org.apache.bval.jsr303.resolver.JPATraversableResolver";


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
    public boolean isReachable(Object traversableObject, Path.Node traversableProperty,
                               Class<?> rootBeanType, Path pathToTraversableObject,
                               ElementType elementType) {
        return jpaTR == null || jpaTR.isReachable(traversableObject, traversableProperty,
              rootBeanType, pathToTraversableObject, elementType);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCascadable(Object traversableObject, Path.Node traversableProperty,
                                Class<?> rootBeanType, Path pathToTraversableObject,
                                ElementType elementType) {
        return jpaTR == null || jpaTR.isCascadable(traversableObject, traversableProperty,
              rootBeanType, pathToTraversableObject, elementType);
    }

    /** Tries to load detect and load JPA. */
    @SuppressWarnings("unchecked")
    private void initJpa() {
        try {
            ClassUtils.getClass(PERSISTENCE_UTIL_CLASSNAME);
            if (log.isDebugEnabled())
                log.debug("Found " + PERSISTENCE_UTIL_CLASSNAME + " on classpath.");
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("Cannot find " + PERSISTENCE_UTIL_CLASSNAME +
                  " on classpath. All properties will per default be traversable.");
            return;
        }

        try {
            Class<? extends TraversableResolver> jpaAwareResolverClass =
                  (Class<? extends TraversableResolver>) ClassUtils
                        .getClass(JPA_AWARE_TRAVERSABLE_RESOLVER_CLASSNAME);
            jpaTR = SecureActions.newInstance(jpaAwareResolverClass);
            if (log.isDebugEnabled())
                log.debug("Instantiated an instance of " +
                    JPA_AWARE_TRAVERSABLE_RESOLVER_CLASSNAME + ".");
        } catch (Exception e) {
            log.warn("Unable to load or instanciate JPA aware resolver " +
                  JPA_AWARE_TRAVERSABLE_RESOLVER_CLASSNAME +
                  ". All properties will per default be traversable.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean needsCaching() {
        return jpaTR != null && CachingTraversableResolver.needsCaching(jpaTR);
    }
}
