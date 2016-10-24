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
import org.apache.bval.util.reflection.Reflection;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Description: internal implementation class to construct metabeans with
 * factories<br/>
 */
public class MetaBeanBuilder {

    private static final Logger log =  Logger.getLogger(MetaBeanBuilder.class.getName());

    /**
     * here you can install different kinds of factories to create MetaBeans
     * from
     */
    private MetaBeanFactory[] factories;

    /**
     * Create a new MetaBeanBuilder instance.
     */
    public MetaBeanBuilder() {
        this(new MetaBeanFactory[] { new IntrospectorMetaBeanFactory() });
    }

    /**
     * Create a new MetaBeanBuilder instance.
     * 
     * @param factories
     */
    public MetaBeanBuilder(MetaBeanFactory[] factories) {
        setFactories(factories);
    }

    /**
     * Get the configured set of {@link MetaBeanFactory} objects.
     * 
     * @return {@link MetaBeanFactory} array
     */
    public MetaBeanFactory[] getFactories() {
        return factories != null ? factories.clone() : null;
    }

    /**
     * Set the array of {@link MetaBeanFactory} instances with which to enrich
     * {@link MetaBean}s.
     * 
     * @param factories
     */
    public void setFactories(MetaBeanFactory[] factories) {
        this.factories = factories != null ? factories.clone() : null;
    }

    /**
     * Build a {@link MetaBean} for a given id.
     * 
     * @param beanInfoId
     * @return MetaBean
     * @throws Exception
     *             if unable to build
     */
    public MetaBean buildForId(String beanInfoId) throws Exception {
        throw new IllegalArgumentException("MetaBean " + beanInfoId + " not found");
    }

    /**
     * Build beans for all known ids. Default implementation returns an empty
     * map.
     * 
     * @return Map of String : MetaBean
     */
    public Map<String, MetaBean> buildAll() throws Exception {
        return new HashMap<String, MetaBean>();
    }

    /**
     * Find the named class.
     * 
     * @param className
     * @return Class found or null
     */
    protected Class<?> findLocalClass(String className) {
        if (className != null) {
            try {
                return Reflection.toClass(className);
            } catch (ClassNotFoundException e) {
                log.log(Level.FINE, String.format("Class not found: %s", className), e);
            }
        }
        return null;
    }

    /**
     * Build a MetaBean for the specified class.
     * 
     * @param clazz
     * @return MetaBean
     * @throws Exception
     */
    public MetaBean buildForClass(Class<?> clazz) throws Exception {
        MetaBean meta = new MetaBean();
        if (clazz != null) { // local class here?
            meta.setBeanClass(clazz);
            meta.setId(clazz.getName()); // default id = full class name!
        }
        for (MetaBeanFactory factory : factories) {
            factory.buildMetaBean(meta);
        }
        return meta;
    }

}
