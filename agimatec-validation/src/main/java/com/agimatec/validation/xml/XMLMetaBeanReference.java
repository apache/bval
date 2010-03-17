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
package com.agimatec.validation.xml;

import static com.agimatec.validation.model.Features.Property.REF_BEAN_ID;
import com.agimatec.validation.model.MetaProperty;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Description: <br/>
 * User: roman.stumm <br/>
 * Date: 05.07.2007 <br/>
 * Time: 14:48:01 <br/>
 * Copyright: Agimatec GmbH 2008
 */
@XStreamAlias("relationship")
public class XMLMetaBeanReference extends XMLMetaElement {
    @XStreamAsAttribute
    private String beanId;

    public XMLMetaBeanReference(String refId) {
        this.beanId = refId;
    }

    public XMLMetaBeanReference() {
    }

    /** id of referenced target bean of the relationship */
    public String getBeanId() {
        return beanId;
    }

    public void setBeanId(String beanId) {
        this.beanId = beanId;
    }

    @Override
    public void mergeInto(MetaProperty prop) throws ClassNotFoundException {
        super.mergeInto(prop);   // call super!
        if (getBeanId() != null) {
            prop.putFeature(REF_BEAN_ID, getBeanId());
        }
    }
}
