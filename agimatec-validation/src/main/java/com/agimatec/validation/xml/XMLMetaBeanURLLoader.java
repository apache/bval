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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Description: <br/>
 * User: roman.stumm <br/>
 * Date: 06.07.2007 <br/>
 * Time: 09:17:30 <br/>
 * Copyright: Agimatec GmbH 2008
 */
public class XMLMetaBeanURLLoader implements XMLMetaBeanLoader {
    private final URL url;

    public XMLMetaBeanURLLoader(URL url) {
        if (url == null) throw new NullPointerException("URL required");
        this.url = url;
    }

    public XMLMetaBeanInfos load() throws IOException {
        InputStream stream = url.openStream();
        try {
            XMLMetaBeanInfos beanInfos = (XMLMetaBeanInfos) XMLMapper.getInstance()
                    .getXStream().fromXML(stream);
            beanInfos.setId(url.toExternalForm());
            return beanInfos;
        } finally {
            stream.close();
        }
    }
}
