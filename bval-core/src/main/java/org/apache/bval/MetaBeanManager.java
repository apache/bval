/**
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

import static org.apache.bval.model.Features.Property.*;


import java.util.Map;

import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.util.PrivilegedActions;
import org.apache.bval.xml.XMLMetaBeanInfos;
import org.apache.bval.xml.XMLMetaBeanLoader;
import org.apache.bval.xml.XMLMetaBeanRegistry;
import org.apache.bval.xml.XMLMetaBeanURLLoader;

/**
 * Description: Default implementation for the interface to find, register and
 * create MetaBeans. In most situations a single instance of this class is
 * sufficient and you can get this instance from the {@link MetaBeanManagerFactory}.
 * <br/>
 */
public class MetaBeanManager implements MetaBeanFinder, XMLMetaBeanRegistry, MetaBeanEnricher {

    protected final MetaBeanCache cache = new MetaBeanCache();
    protected final MetaBeanBuilder builder;
    private boolean complete = false;

    public MetaBeanManager() {
        builder = new MetaBeanBuilder();
    }

    public MetaBeanManager(MetaBeanBuilder builder) {
        this.builder = builder;
    }

    public void addResourceLoader(String resource) {
        addLoader(new XMLMetaBeanURLLoader(
              PrivilegedActions.getClassLoader(getClass()).getResource(resource)));
    }

    public synchronized void addLoader(XMLMetaBeanLoader loader) {
        builder.addLoader(loader);
        cache.clear(); // clear because new loaders can affect ALL MetaBeans already created!
        complete = false;
    }

    public MetaBeanBuilder getBuilder() {
        return builder;
    }

    public MetaBeanCache getCache() {
        return cache;
    }

    /**
     * @return all MetaBeans for classes that have a xml descriptor:
     *         key = bean.id, value = MetaBean
     */
    public Map<String, MetaBean> findAll() {
        if (!complete) {
            try {
                Map<String, MetaBean> allBuilt = builder.buildAll();
                for (MetaBean meta : allBuilt.values()) {
                    MetaBean cached = cache.findForId(meta.getId());
                    if (cached == null) {
                        cache.cache(meta);
                    }
                }
                Map<String, MetaBean> map = cache.findAll();
                for (Object oentry : map.values()) {
                    MetaBean meta = (MetaBean) oentry;
                    computeRelationships(meta, map);
                }
                complete = true;
                return map;
            } catch (RuntimeException e) {
                throw e; // do not wrap runtime exceptions
            } catch (Exception e) {
                throw new IllegalArgumentException("error creating beanInfos", e);
            }
        } else {
            return cache.findAll();
        }
    }

    /**
     * @param infos - the patches to apply
     * @return all MetaBeans for classes that have a xml descriptor and
     *         additional the MetaBeans loaded by the given loaders.
     *         The given loaders may also return patches for MetaBeans that have
     *         also been returned by other loaders. The beans with patches for
     *         references to patched beans will be copied.
     */
    public Map<String, MetaBean> enrichCopies(XMLMetaBeanInfos... infos) {
        Map<String, MetaBean> cached = findAll();
        try {
            Map<String, MetaBean> patched = builder.enrichCopies(cached, infos);
            for (Object oentry : patched.values()) {
                MetaBean meta = (MetaBean) oentry;
                computeRelationships(meta, patched);
            }
            return patched;
        } catch (RuntimeException e) {
            throw e; // do not wrap runtime exceptions
        } catch (Exception e) {
            throw new IllegalArgumentException("error enriching beanInfos", e);
        }
    }

    public MetaBean findForId(String beanInfoId) {
        MetaBean beanInfo = cache.findForId(beanInfoId);
        if (beanInfo != null) return beanInfo;
        try {
            beanInfo = builder.buildForId(beanInfoId);
            cache.cache(beanInfo);
            computeRelationships(beanInfo);
            return beanInfo;
        } catch (RuntimeException e) {
            throw e; // do not wrap runtime exceptions
        } catch (Exception e) {
            throw new IllegalArgumentException(
                  "error creating beanInfo with id: " + beanInfoId, e);
        }
    }

    public MetaBean findForClass(Class<?> clazz) {
        if (clazz == null) return null;
        MetaBean beanInfo = cache.findForClass(clazz);
        if (beanInfo != null) return beanInfo;
        try {
            beanInfo = builder.buildForClass(clazz);
            cache.cache(beanInfo);
            computeRelationships(beanInfo);
            return beanInfo;
        } catch (RuntimeException e) {
            throw e; // do not wrap runtime exceptions
        } catch (Exception e) {
            throw new IllegalArgumentException("error creating beanInfo for " + clazz, e);
        }
    }

    /**
     * must be called AFTER cache.cache()
     * to avoid endless loop
     */
    protected void computeRelationships(MetaBean beanInfo) {
        for (MetaProperty prop : beanInfo.getProperties()) {
            String beanRef = (String) prop.getFeature(REF_BEAN_ID);
            if (beanRef != null) {
                prop.setMetaBean(findForId(beanRef));
            } else {
                Class<?> beanType = prop.getFeature(REF_BEAN_TYPE);
                if (beanType != null) {
                    prop.setMetaBean(findForClass(beanType));
                } // dynamic type resolution:
                else if (prop.getFeature(REF_CASCADE) != null) {
                    prop.setMetaBean(new DynamicMetaBean(this));
//                            findForClass(prop.getType()));
                }
            }
        }
    }

    private void computeRelationships(MetaBean beanInfo, Map<String, MetaBean> cached) {
        for (MetaProperty prop : beanInfo.getProperties()) {
            String beanRef = (String) prop.getFeature(REF_BEAN_ID);
            if (beanRef != null) {
                prop.setMetaBean(cached.get(beanRef));
            }
        }
    }
}
