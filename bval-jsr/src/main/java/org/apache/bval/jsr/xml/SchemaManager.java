/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bval.jsr.xml;

import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ValidationException;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.StringUtils;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Unmarshals XML converging on latest schema version. Presumes backward compatiblity between schemae.
 */
public class SchemaManager {
    public static class Builder {
        private final SortedMap<Key, Lazy<Schema>> data = new TreeMap<>();

        public Builder add(String version, String ns, String resource) {
            data.put(new Key(version, ns), new Lazy<>(() -> SchemaManager.loadSchema(resource)));
            return this;
        }

        public SchemaManager build() {
            return new SchemaManager(new TreeMap<>(data));
        }
    }

    private static class Key implements Comparable<Key> {
        private static final Comparator<Key> CMP = Comparator.comparing(Key::getVersion).thenComparing(Key::getNs);

        final String version;
        final String ns;

        Key(String version, String ns) {
            super();
            Validate.isTrue(StringUtils.isNotBlank(version), "version cannot be null/empty/blank");
            this.version = version;
            Validate.isTrue(StringUtils.isNotBlank(ns), "ns cannot be null/empty/blank");
            this.ns = ns;
        }

        public String getVersion() {
            return version;
        }

        public String getNs() {
            return ns;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return Optional.ofNullable(obj).filter(SchemaManager.Key.class::isInstance)
                .map(SchemaManager.Key.class::cast)
                .filter(k -> Objects.equals(this.version, k.version) && Objects.equals(this.ns, k.ns)).isPresent();
        }

        @Override
        public int hashCode() {
            return Objects.hash(version, ns);
        }

        @Override
        public String toString() {
            return String.format("%s:%s", version, ns);
        }

        @Override
        public int compareTo(Key o) {
            return CMP.compare(this, o);
        }
    }

    private class DynamicValidatorHandler extends XMLFilterImpl {
        ContentHandler ch;
        SAXParseException e;

        @Override
        public void setContentHandler(ContentHandler handler) {
            super.setContentHandler(handler);
            this.ch = handler;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (getContentHandler() == ch) {
                final String version = Objects.toString(atts.getValue("version"), data.firstKey().getVersion());
                final Key schemaKey = new Key(version, uri);
                Exceptions.raiseUnless(data.containsKey(schemaKey), ValidationException::new,
                    "Unknown validation schema %s", schemaKey);

                final Schema schema = data.get(schemaKey).get();
                final ValidatorHandler vh = schema.newValidatorHandler();
                vh.startDocument();
                vh.setContentHandler(ch);
                super.setContentHandler(vh);
            }
            try {
                super.startElement(uri, localName, qName, atts);
            } catch (SAXParseException e) {
                this.e = e;
            }
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            this.e = e;
            super.error(e);
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            this.e = e;
            super.fatalError(e);
        }

        void validate() throws SAXParseException {
            if (e != null) {
                throw e;
            }
        }
    }

    //@formatter:off
    private enum XmlAttributeType {
        CDATA, ID, IDREF, IDREFS, NMTOKEN, NMTOKENS, ENTITY, ENTITIES, NOTATION;
        //@formatter:on
    }

    private class SchemaRewriter extends XMLFilterImpl {
        private boolean root = true;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            final Key schemaKey =
                new Key(Objects.toString(atts.getValue("version"), data.firstKey().getVersion()), uri);

            if (!target.equals(schemaKey) && data.containsKey(schemaKey)) {
                uri = target.ns;
                if (root) {
                    atts = rewrite(atts);
                    root = false;
                }
            }
            super.startElement(uri, localName, qName, atts);
        }

        private Attributes rewrite(Attributes atts) {
            final AttributesImpl result;
            if (atts instanceof AttributesImpl) {
                result = (AttributesImpl) atts;
            } else {
                result = new AttributesImpl(atts);
            }
            set(result, "", VERSION_ATTRIBUTE, "", XmlAttributeType.CDATA, target.version);
            return result;
        }

        private void set(AttributesImpl attrs, String uri, String localName, String qName, XmlAttributeType type,
            String value) {
            for (int i = 0, sz = attrs.getLength(); i < sz; i++) {
                if (Objects.equals(qName, attrs.getQName(i))
                    || Objects.equals(uri, attrs.getURI(i)) && Objects.equals(localName, attrs.getLocalName(i))) {
                    attrs.setAttribute(i, uri, localName, qName, type.name(), value);
                    return;
                }
            }
            attrs.addAttribute(uri, localName, qName, type.name(), value);
        }
    }

    public static final String VERSION_ATTRIBUTE = "version";

    private static final Logger log = Logger.getLogger(SchemaManager.class.getName());
    private static final SchemaFactory SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    private static final SAXParserFactory SAX_PARSER_FACTORY;

    static {
        SAX_PARSER_FACTORY = SAXParserFactory.newInstance();
        SAX_PARSER_FACTORY.setNamespaceAware(true);
    }

    static Schema loadSchema(String resource) {
        final URL schemaUrl = Reflection.loaderFromThreadOrClass(SchemaManager.class).getResource(resource);
        try {
            return SCHEMA_FACTORY.newSchema(schemaUrl);
        } catch (SAXException e) {
            log.log(Level.WARNING, String.format("Unable to parse schema: %s", resource), e);
            return null;
        }
    }

    private static Class<?> getObjectFactory(Class<?> type) throws ClassNotFoundException {
        final String className = String.format("%s.%s", type.getPackage().getName(), "ObjectFactory");
        return Reflection.toClass(className, type.getClassLoader());
    }

    private final Key target;
    private final SortedMap<Key, Lazy<Schema>> data;
    private final String description;

    private SchemaManager(SortedMap<Key, Lazy<Schema>> data) {
        super();
        this.data = Collections.unmodifiableSortedMap(data);
        this.target = data.lastKey();
        this.description = target.ns.substring(target.ns.lastIndexOf('/') + 1);
    }

    public Optional<Schema> getSchema(String ns, String version) {
        return Optional.of(new Key(version, ns)).map(data::get).map(Lazy::get);
    }

    public Optional<Schema> getSchema(Document document) {
        return Optional.ofNullable(document).map(Document::getDocumentElement)
            .map(e -> getSchema(e.getAttribute(XMLConstants.XMLNS_ATTRIBUTE), e.getAttribute(VERSION_ATTRIBUTE))).get();
    }

    public <E extends Exception> Schema requireSchema(Document document, Function<String, E> exc) throws E {
        return getSchema(document).orElseThrow(() -> Objects.requireNonNull(exc, "exc")
            .apply(String.format("Unknown %s schema", Objects.toString(description, ""))));
    }

    public <T> T unmarshal(InputSource input, Class<T> type) throws Exception {
        final XMLReader xmlReader = SAX_PARSER_FACTORY.newSAXParser().getXMLReader();

        // validate specified schema:
        final DynamicValidatorHandler schemaValidator = new DynamicValidatorHandler();
        xmlReader.setContentHandler(schemaValidator);

        // rewrite to latest schema, if required:
        final SchemaRewriter schemaRewriter = new SchemaRewriter();
        schemaValidator.setContentHandler(schemaRewriter);

        JAXBContext jc = JAXBContext.newInstance(getObjectFactory(type));
        // unmarshal:
        final UnmarshallerHandler unmarshallerHandler = jc.createUnmarshaller().getUnmarshallerHandler();
        schemaRewriter.setContentHandler(unmarshallerHandler);

        xmlReader.parse(input);
        schemaValidator.validate();

        @SuppressWarnings("unchecked")
        final JAXBElement<T> result = (JAXBElement<T>) unmarshallerHandler.getResult();
        return result.getValue();
    }
}
