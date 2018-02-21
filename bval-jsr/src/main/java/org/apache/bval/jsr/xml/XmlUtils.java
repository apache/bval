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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.bval.util.reflection.Reflection;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

class XmlUtils {
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final Logger log = Logger.getLogger(XmlUtils.class.getName());
    private static final SchemaFactory SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    static Schema loadSchema(String resource) {
        final URL schemaUrl = Reflection.getClassLoader(XmlUtils.class).getResource(resource);
        try {
            return SCHEMA_FACTORY.newSchema(schemaUrl);
        } catch (SAXException e) {
            log.log(Level.WARNING, String.format("Unable to parse schema: %s", resource), e);
            return null;
        }
    }

    static Document parse(InputStream in) throws SAXException, IOException, ParserConfigurationException {
        return DOCUMENT_BUILDER_FACTORY.newDocumentBuilder().parse(in);
    }

    static <T> T unmarshal(Document document, Schema schema, Class<T> type) throws JAXBException {
        final JAXBContext jc = JAXBContext.newInstance(type);
        final Unmarshaller unmarshaller = jc.createUnmarshaller();
        unmarshaller.setSchema(schema);
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());
        final JAXBElement<T> root = unmarshaller.unmarshal(document, type);
        return root.getValue();
    }
}