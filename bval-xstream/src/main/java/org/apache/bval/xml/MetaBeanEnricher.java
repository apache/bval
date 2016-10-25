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
package org.apache.bval.xml;

import org.apache.bval.model.MetaBean;

import java.util.Map;

/**
 * Description: Interface to merge meta beans<br/>
 */
public interface MetaBeanEnricher {

    /**
     * @param infos - the patches to apply
     * @return all MetaBeans for classes that have a xml descriptor and
     *         additional the MetaBeans loaded by the given loaders.
     *         The given loaders may also return patches for MetaBeans that have
     *         also been returned by other loaders. The beans with patches for
     *         references to patched beans will be copied.
     */
    Map<String, MetaBean> enrichCopies(XMLMetaBeanInfos... infos);
}
