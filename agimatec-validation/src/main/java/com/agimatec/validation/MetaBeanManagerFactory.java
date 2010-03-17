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
package com.agimatec.validation;

import com.agimatec.validation.xml.XMLMetaBeanRegistry;

/**
 * Description: API class to hold a singleton of a {@link MetaBeanManager}
 * that implements the finder and registry interfaces for MetaBeans<br/>
 * User: roman.stumm <br/>
 * Date: 05.07.2007 <br/>
 * Time: 16:20:03 <br/>
 * Copyright: Agimatec GmbH 2008
 *
 * @see com.agimatec.validation.model.MetaBean
 * @see MetaBeanManager
 */
public class MetaBeanManagerFactory {
    private static MetaBeanManager manager = new MetaBeanManager();

    /**
     * global meta bean finder.
     * @return
     */
    public static MetaBeanFinder getFinder() {
        return manager;
    }

    /**
     * global meta bean registry
     * @return
     */
    public static XMLMetaBeanRegistry getRegistry() {
        return manager;
    }

    /**
     * global meta bean enricher
     * @return
     */
    public static MetaBeanEnricher getEnricher() {
        return manager;
    }

    /**
     * set global meta bean manager, that is responsible
     * for finding, caching, xml registry and enrichment algorithm.
     * @param finder
     */
    public static void setManager(MetaBeanManager finder) {
        manager = finder;
    }
}
