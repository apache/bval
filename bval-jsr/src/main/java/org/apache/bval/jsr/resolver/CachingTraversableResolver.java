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

import javax.validation.Path;
import javax.validation.TraversableResolver;
import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Cache results of a delegated traversable resovler to optimize calls
 * It works only for a single validate* call and should not be used if
 * the TraversableResolver is accessed concurrently
 * <p/>
 * Date: 25.11.2009 <br/>
 * Time: 13:56:18 <br/>
 *
 * @author Roman Stumm (based on the code of Emmanuel Bernard)
 */
public class CachingTraversableResolver implements TraversableResolver, CachingRelevant {
    private TraversableResolver delegate;
    private Map<CacheEntry, CacheEntry> cache = new HashMap<>();

    /**
     * Convenience method to check whether caching is necessary on a given {@link TraversableResolver}.
     * @param resolver to check
     * @return true when a CachingTraversableResolver is to be used during validation
     */
    public static boolean needsCaching(TraversableResolver resolver) {
        // caching, if we do not know exactly
        return !(resolver instanceof CachingRelevant) || ((CachingRelevant) resolver).needsCaching();
    }

    /**
     * Create a new CachingTraversableResolver instance.
     * @param delegate
     */
    public CachingTraversableResolver(TraversableResolver delegate) {
        this.delegate = delegate;
    }

    /**
     * If necessary, return a caching wrapper for the specified {@link TraversableResolver}.
     * @param traversableResolver
     * @return {@link TraversableResolver}
     * @see #needsCaching(TraversableResolver)
     */
    public static TraversableResolver cacheFor(TraversableResolver traversableResolver) {
        return needsCaching(traversableResolver) ? new CachingTraversableResolver(traversableResolver)
            : traversableResolver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReachable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
        Path pathToTraversableObject, ElementType elementType) {
        CacheEntry currentLH =
            new CacheEntry(traversableObject, traversableProperty, rootBeanType, pathToTraversableObject, elementType);
        CacheEntry cachedLH = cache.get(currentLH);
        if (cachedLH == null) {
            currentLH.reachable = delegate.isReachable(traversableObject, traversableProperty, rootBeanType,
                pathToTraversableObject, elementType);
            cache.put(currentLH, currentLH);
            cachedLH = currentLH;
        } else if (cachedLH.reachable == null) {
            cachedLH.reachable = delegate.isReachable(traversableObject, traversableProperty, rootBeanType,
                pathToTraversableObject, elementType);
        }
        return cachedLH.reachable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCascadable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
        Path pathToTraversableObject, ElementType elementType) {
        CacheEntry currentLH =
            new CacheEntry(traversableObject, traversableProperty, rootBeanType, pathToTraversableObject, elementType);
        CacheEntry cachedLH = cache.get(currentLH);
        if (cachedLH == null) {
            currentLH.cascadable = delegate.isCascadable(traversableObject, traversableProperty, rootBeanType,
                pathToTraversableObject, elementType);
            cache.put(currentLH, currentLH);
            cachedLH = currentLH;
        } else if (cachedLH.cascadable == null) {
            cachedLH.cascadable = delegate.isCascadable(traversableObject, traversableProperty, rootBeanType,
                pathToTraversableObject, elementType);
        }
        return cachedLH.cascadable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean needsCaching() {
        return false; // I am the cache. Do not need cache for cache
    }

    /**
     * Entry in the cache.
     */
    private static class CacheEntry {
        private final Object object;
        private final Path.Node node;
        private final Class<?> type;
        private final Path path;
        private final ElementType elementType;
        private final int hashCode;

        private Boolean reachable;
        private Boolean cascadable;

        /**
         * Create a new CacheEntry instance.
         * @param traversableObject
         * @param traversableProperty
         * @param rootBeanType
         * @param pathToTraversableObject
         * @param elementType
         */
        private CacheEntry(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
            Path pathToTraversableObject, ElementType elementType) {
            this.object = traversableObject;
            this.node = traversableProperty;
            this.type = rootBeanType;
            this.path = pathToTraversableObject;
            this.elementType = elementType;
            this.hashCode = buildHashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || !getClass().equals(o.getClass())) {
                return false;
            }

            CacheEntry that = (CacheEntry) o;

            return elementType == that.elementType && Objects.equals(path, that.path) && Objects.equals(type, that.type)
                && Objects.equals(object, that.object) && Objects.equals(node, that.node);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return hashCode;
        }

        private int buildHashCode() {
            return Objects.hash(object, node, type, path, elementType);
        }
    }
}
