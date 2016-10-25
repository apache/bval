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

import com.thoughtworks.xstream.XStream;

/**
 * Description: <br/>
 */
public class XMLMapper {
    private static final XMLMapper instance = new XMLMapper();

    private final XStream xStream;

    private XMLMapper() {
        xStream = new XStream();
        xStream.processAnnotations(new Class[] { XMLFeaturesCapable.class, XMLMetaFeature.class, XMLMetaBean.class,
            XMLMetaBeanInfos.class, XMLMetaBeanReference.class, XMLMetaElement.class, XMLMetaProperty.class,
            XMLMetaValidator.class, XMLMetaValidatorReference.class });
        xStream.setMode(XStream.NO_REFERENCES);
    }

    public static XMLMapper getInstance() {
        return instance;
    }

    public XStream getXStream() {
        return xStream;
    }
}
