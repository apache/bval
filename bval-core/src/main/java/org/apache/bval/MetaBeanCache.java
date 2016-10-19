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
package org.apache.bval;

import org.apache.bval.model.MetaBean;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Description: a cache to hold metabeans by id and by class.<br/>
 */
public class MetaBeanCache implements MetaBeanFinder, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Cache keyed by id.
     */
    protected final ConcurrentMap<String, MetaBean> cacheById = new ConcurrentHashMap<String, MetaBean>();
    /**
     * Cache keyed by class.
     */
    protected final ConcurrentMap<Class<?>, MetaBean> cacheByClass = new ConcurrentHashMap<Class<?>, MetaBean>();

    /**
     * Create a new MetaBeanCache instance.
     */
    public MetaBeanCache() {
        super();
    }

    /**
     * Create a new MetaBeanCache instance.
     * @param beans
     */
    public MetaBeanCache(Map<String, MetaBean> beans) {
        this();
        for (MetaBean bean : beans.values()) {
            cache(bean);
        }
    }

    /**
     * Clear the cache.
     */
    public void clear() {
        cacheById.clear();
        cacheByClass.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaBean findForId(String beanInfoId) {
        return cacheById.get(beanInfoId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaBean findForClass(Class<?> clazz) {
        return cacheByClass.get(clazz);
    }

    /**
     * Return all cached MetaBeans by id.
     * @return live map
     */
    public Map<String, MetaBean> findAll() {
        return cacheById;
    }

    /**
     * Cache the specified MetaBean.
     * @param beanInfo
     */
    public void cache(MetaBean beanInfo) {
        cacheById.put(beanInfo.getId(), beanInfo);
        if (beanInfo.getBeanClass() != null && beanInfo.getId().equals(beanInfo.getBeanClass().getName())) {
            cacheByClass.putIfAbsent(beanInfo.getBeanClass(), beanInfo);
        }
    }

    /**
     * Remove a single MetaBean from the cache.
     * @param beanInfo
     */
    public void removeFromCache(MetaBean beanInfo) {
        cacheById.remove(beanInfo.getId());
        if (beanInfo.getBeanClass() != null && beanInfo.getId().equals(beanInfo.getBeanClass().getName())) {
            cacheByClass.remove(beanInfo.getBeanClass());
        }
    }

}
