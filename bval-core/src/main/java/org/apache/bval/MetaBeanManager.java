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
import org.apache.bval.model.MetaProperty;

import static org.apache.bval.model.Features.Property.REF_BEAN_ID;
import static org.apache.bval.model.Features.Property.REF_BEAN_TYPE;
import static org.apache.bval.model.Features.Property.REF_CASCADE;

/**
 * Description: Default implementation for the interface to find, register and
 * create MetaBeans. In most situations a single instance of this class is
 * sufficient and you can get this instance from the
 * {@link MetaBeanManagerFactory}. <br/>
 */
public class MetaBeanManager implements MetaBeanFinder {

    /** MetaBean cache */
    protected final MetaBeanCache cache = new MetaBeanCache();
    /** MetaBean builder */
    protected final MetaBeanBuilder builder;
    /** Complete flag */
    protected boolean complete = false;

    /**
     * Create a new MetaBeanManager instance.
     */
    public MetaBeanManager() {
        builder = new MetaBeanBuilder();
    }

    /**
     * Create a new MetaBeanManager instance.
     * 
     * @param builder meta bean builder
     */
    public MetaBeanManager(MetaBeanBuilder builder) {
        this.builder = builder;
    }

    /**
     * Get the builder used.
     * 
     * @return {@link MetaBeanBuilder}
     */
    public MetaBeanBuilder getBuilder() {
        return builder;
    }

    /**
     * Get the cache used.
     * 
     * @return {@link MetaBeanCache}
     */
    public MetaBeanCache getCache() {
        return cache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaBean findForId(final String beanInfoId) {
        MetaBean beanInfo = cache.findForId(beanInfoId);
        if (beanInfo != null) {
            return beanInfo;
        }

        try {
            beanInfo = builder.buildForId(beanInfoId);
            cache.cache(beanInfo);
            computeRelationships(beanInfo);
            return beanInfo;
        } catch (final RuntimeException e) {
            throw e; // do not wrap runtime exceptions
        } catch (final Exception e) {
            throw new IllegalArgumentException("error creating beanInfo with id: " + beanInfoId, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaBean findForClass(final Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        MetaBean beanInfo = cache.findForClass(clazz);
        if (beanInfo != null) {
            return beanInfo;
        }

        try {
            beanInfo = builder.buildForClass(clazz);
            cache.cache(beanInfo);
            computeRelationships(beanInfo);
            return beanInfo;
        } catch (final RuntimeException e) {
            throw e; // do not wrap runtime exceptions
        } catch (final Exception e) {
            throw new IllegalArgumentException("error creating beanInfo for " + clazz, e);
        }
    }

    /**
     * Compute all known relationships for <code>beanInfo</code>. must be called
     * AFTER cache.cache() to avoid endless loop
     * 
     * @param beanInfo
     *            - the bean for which to compute relationships
     */
    protected void computeRelationships(MetaBean beanInfo) {
        for (final MetaProperty prop : beanInfo.getProperties()) {
            final String beanRef = prop.getFeature(REF_BEAN_ID);
            computeRelatedMetaBean(prop, beanRef);
        }
    }

    /**
     * Compute a single related {@link MetaBean}.
     * 
     * @param prop meta property
     * @param beanRef bean reference
     */
    protected void computeRelatedMetaBean(MetaProperty prop, String beanRef) {
        Class<?> beanType = prop.getFeature(REF_BEAN_TYPE);
        if (beanType == null) {
            if (prop.getFeature(REF_CASCADE) != null) { // dynamic type resolution:
                prop.setMetaBean(new DynamicMetaBean(this));
            }
        } else {
            prop.setMetaBean(findForClass(beanType));
        }
    }

}
