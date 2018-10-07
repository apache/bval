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
package org.apache.bval.jsr.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class Demo {

    @Test
    public void test1() throws Exception {
        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);

        // Set the parent XMLReader on the XMLFilter
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser sp = spf.newSAXParser();
        XMLReader xr = sp.getXMLReader();

        // Set UnmarshallerHandler as ContentHandler on XMLFilter
        
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        
        UnmarshallerHandler unmarshallerHandler = unmarshaller.getUnmarshallerHandler();
        xr.setContentHandler(unmarshallerHandler);

        // Parse the XML
        InputSource xml = new InputSource(getClass().getResourceAsStream("/sample-validation2.xml"));
        xr.parse(xml);
        JAXBElement<ValidationConfigType> result = (JAXBElement<ValidationConfigType>) unmarshallerHandler.getResult();
        System.out.println(result.getValue());
    }
}
