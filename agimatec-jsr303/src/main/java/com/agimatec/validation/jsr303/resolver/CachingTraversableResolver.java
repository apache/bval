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
package com.agimatec.validation.jsr303.resolver;

import javax.validation.Path;
import javax.validation.TraversableResolver;
import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

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
    private Map<CacheEntry, CacheEntry> cache = new HashMap<CacheEntry, CacheEntry>();

    /** @return true when a CachingTraversableResolver is to be used during validation */
    public static boolean needsCaching(TraversableResolver resolver) {
        // caching, if we do not know exactly
        return !(resolver instanceof CachingRelevant) ||
              ((CachingRelevant) resolver).needsCaching();
    }

    public CachingTraversableResolver(TraversableResolver delegate) {
        this.delegate = delegate;
    }

    public static TraversableResolver cacheFor(TraversableResolver traversableResolver) {
        if (needsCaching(traversableResolver)) {
            return new CachingTraversableResolver(traversableResolver);
        } else {
            return traversableResolver;
        }
    }

    public boolean isReachable(Object traversableObject, Path.Node traversableProperty,
                               Class<?> rootBeanType, Path pathToTraversableObject,
                               ElementType elementType) {
        CacheEntry currentLH = new CacheEntry(traversableObject, traversableProperty,
              rootBeanType, pathToTraversableObject, elementType);
        CacheEntry cachedLH = cache.get(currentLH);
        if (cachedLH == null) {
            currentLH.reachable = delegate.isReachable(traversableObject, traversableProperty,
                  rootBeanType, pathToTraversableObject, elementType);
            cache.put(currentLH, currentLH);
            cachedLH = currentLH;
        } else if (cachedLH.reachable == null) {
            cachedLH.reachable = delegate.isReachable(traversableObject, traversableProperty,
                  rootBeanType, pathToTraversableObject, elementType);
        }
        return cachedLH.reachable;
    }

    public boolean isCascadable(Object traversableObject, Path.Node traversableProperty,
                                Class<?> rootBeanType, Path pathToTraversableObject,
                                ElementType elementType) {
        CacheEntry currentLH = new CacheEntry(traversableObject, traversableProperty,
              rootBeanType, pathToTraversableObject, elementType);
        CacheEntry cachedLH = cache.get(currentLH);
        if (cachedLH == null) {
            currentLH.cascadable = delegate.isCascadable(traversableObject,
                  traversableProperty, rootBeanType, pathToTraversableObject, elementType);
            cache.put(currentLH, currentLH);
            cachedLH = currentLH;
        } else if (cachedLH.cascadable == null) {
            cachedLH.cascadable = delegate.isCascadable(traversableObject, traversableProperty,
                  rootBeanType, pathToTraversableObject, elementType);
        }
        return cachedLH.cascadable;
    }

    public boolean needsCaching() {
        return false;  // I am the cache. Do not need cache for cache
    }

    private static class CacheEntry {
        private final Object object;
        private final Path.Node node;
        private final Class<?> type;
        private final Path path;
        private final ElementType elementType;
        private final int hashCode;

        private Boolean reachable;
        private Boolean cascadable;


        private CacheEntry(Object traversableObject, Path.Node traversableProperty,
                           Class<?> rootBeanType, Path pathToTraversableObject,
                           ElementType elementType) {
            this.object = traversableObject;
            this.node = traversableProperty;
            this.type = rootBeanType;
            this.path = pathToTraversableObject;
            this.elementType = elementType;
            this.hashCode = buildHashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CacheEntry that = (CacheEntry) o;

            return elementType == that.elementType && path.equals(that.path) &&
                  type.equals(that.type) &&
                  !(object != null ? !object.equals(that.object) : that.object != null) &&
                  node.equals(that.node);

        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        private int buildHashCode() {
            int result = object != null ? object.hashCode() : 0;
            result = 31 * result + node.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + path.hashCode();
            result = 31 * result + elementType.hashCode();
            return result;
        }
    }
}