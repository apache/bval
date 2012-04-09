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

// TODO: Reduce visibility

/**
 * Description: Dynamic {@link MetaBean} subclass.<br/>
 */
public final class DynamicMetaBean extends MetaBean {
    private static final long serialVersionUID = 1L;

    private final MetaBeanFinder finder;

    /**
     * Create a new DynamicMetaBean instance.
     * @param finder
     */
    public DynamicMetaBean(MetaBeanFinder finder) {
        this.finder = finder;
    }

    /**
     * {@inheritDoc}
     * different strategies with hints to find MetaBean of associated object can
     * be implemented here.
     */
    @Override
    public MetaBean resolveMetaBean(Object bean) {
        return bean instanceof Class<?> ?
                finder.findForClass((Class<?>) bean) : finder.findForClass(bean.getClass());
    }
}
