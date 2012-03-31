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
package org.apache.bval.jsr303.xml;


import org.apache.bval.jsr303.ConfigurationImpl;
import org.apache.bval.jsr303.util.IOUtils;
import org.apache.bval.jsr303.util.SecureActions;
import org.apache.bval.util.PrivilegedActions;
import org.xml.sax.SAXException;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.spi.ValidationProvider;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Description: uses jaxb to parse validation.xml<br/>
 */
@SuppressWarnings("restriction")
public class ValidationParser {
    private static final String DEFAULT_VALIDATION_XML_FILE = "META-INF/validation.xml";
    private static final String VALIDATION_CONFIGURATION_XSD =
            "META-INF/validation-configuration-1.0.xsd";
    private static final Logger log = Logger.getLogger(ValidationParser.class.getName());
    protected final String validationXmlFile;

    /**
     * Create a new ValidationParser instance.
     *
     * @param file
     */
    public ValidationParser(String file) {
        if (file == null) {
            validationXmlFile = DEFAULT_VALIDATION_XML_FILE;
        } else {
            validationXmlFile = file;
        }
    }

    /**
     * Process the validation configuration into <code>targetConfig</code>.
     *
     * @param targetConfig
     */
    public void processValidationConfig(ConfigurationImpl targetConfig) {
        ValidationConfigType xmlConfig = parseXmlConfig();
        if (xmlConfig != null) {
            applyConfig(xmlConfig, targetConfig);
        }
    }

    private ValidationConfigType parseXmlConfig() {
        InputStream inputStream = null;
        try {
            inputStream = getInputStream(validationXmlFile);
            if (inputStream == null) {
            	log.log(Level.FINEST, String.format("No %s found. Using annotation based configuration only.", validationXmlFile));
                return null;
            }

            log.log(Level.FINEST, String.format("%s found.", validationXmlFile));

            Schema schema = getSchema();
            JAXBContext jc = JAXBContext.newInstance(ValidationConfigType.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshaller.setSchema(schema);
            StreamSource stream = new StreamSource(inputStream);
            JAXBElement<ValidationConfigType> root =
                    unmarshaller.unmarshal(stream, ValidationConfigType.class);
            return root.getValue();
        } catch (JAXBException e) {
            throw new ValidationException("Unable to parse " + validationXmlFile, e);
        } catch (IOException e) {
            throw new ValidationException("Unable to parse " + validationXmlFile, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    protected InputStream getInputStream(String path) throws IOException {
        ClassLoader loader = PrivilegedActions.getClassLoader(getClass());
        InputStream inputStream = loader.getResourceAsStream(path);

        if (inputStream != null) {
            // spec says: If more than one META-INF/validation.xml file
            // is found in the classpath, a ValidationException is raised.
            Enumeration<URL> urls = loader.getResources(path);
            if (urls.hasMoreElements()) {
                String url = urls.nextElement().toString();
                while (urls.hasMoreElements()) {
                    if (!url.equals(urls.nextElement().toString())) { // complain when first duplicate found
                        throw new ValidationException("More than one " + path + " is found in the classpath");
                    }
                }
            }
        }

        return inputStream;
    }

    private Schema getSchema() {
        return getSchema(VALIDATION_CONFIGURATION_XSD);
    }

    /**
     * Get a Schema object from the specified resource name.
     *
     * @param xsd
     * @return {@link Schema}
     */
    static Schema getSchema(String xsd) {
        ClassLoader loader = PrivilegedActions.getClassLoader(ValidationParser.class);
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL schemaUrl = loader.getResource(xsd);
        try {
            return sf.newSchema(schemaUrl);
        } catch (SAXException e) {
            log.log(Level.WARNING, String.format("Unable to parse schema: %s", xsd), e);
            return null;
        }
    }

    private void applyConfig(ValidationConfigType xmlConfig, ConfigurationImpl targetConfig) {
        applyProviderClass(xmlConfig, targetConfig);
        applyMessageInterpolator(xmlConfig, targetConfig);
        applyTraversableResolver(xmlConfig, targetConfig);
        applyConstraintFactory(xmlConfig, targetConfig);
        applyMappingStreams(xmlConfig, targetConfig);
        applyProperties(xmlConfig, targetConfig);
    }

    private void applyProperties(ValidationConfigType xmlConfig, ConfigurationImpl target) {
        for (PropertyType property : xmlConfig.getProperty()) {
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, String.format("Found property '%s' with value '%s' in %s", property.getName(), property.getValue(), validationXmlFile));
            }
            target.addProperty(property.getName(), property.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private void applyProviderClass(ValidationConfigType xmlConfig, ConfigurationImpl target) {
        String providerClassName = xmlConfig.getDefaultProvider();
        if (providerClassName != null) {
            Class<? extends ValidationProvider<?>> clazz =
                    (Class<? extends ValidationProvider<?>>) loadClass(providerClassName);
            target.setProviderClass(clazz);
            log.log(Level.INFO, String.format("Using %s as validation provider.", providerClassName));
        }
    }

    @SuppressWarnings("unchecked")
    private void applyMessageInterpolator(ValidationConfigType xmlConfig,
                                          ConfigurationImpl target) {
        String messageInterpolatorClass = xmlConfig.getMessageInterpolator();
        if (target.getMessageInterpolator() == null) {
            if (messageInterpolatorClass != null) {
                Class<MessageInterpolator> clazz = (Class<MessageInterpolator>)
                        loadClass(messageInterpolatorClass);
                target.messageInterpolator(newInstance(clazz));
                log.log(Level.INFO, String.format("Using %s as message interpolator.", messageInterpolatorClass));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void applyTraversableResolver(ValidationConfigType xmlConfig,
                                          ConfigurationImpl target) {
        String traversableResolverClass = xmlConfig.getTraversableResolver();
        if (target.getTraversableResolver() == null) {
            if (traversableResolverClass != null) {
                Class<TraversableResolver> clazz = (Class<TraversableResolver>)
                        loadClass(traversableResolverClass);
                target.traversableResolver(newInstance(clazz));
                log.log(Level.INFO, String.format("Using %s as traversable resolver.", traversableResolverClass));
            }
        }
    }

    private <T> T newInstance(final Class<T> cls) {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            public T run() {
                try {
                    return cls.newInstance();
                } catch (final Exception ex) {
                    throw new ValidationException("Cannot instantiate : " + cls, ex);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void applyConstraintFactory(ValidationConfigType xmlConfig,
                                        ConfigurationImpl target) {
        String constraintFactoryClass = xmlConfig.getConstraintValidatorFactory();
        if (target.getConstraintValidatorFactory() == null) {
            if (constraintFactoryClass != null) {
                Class<ConstraintValidatorFactory> clazz = (Class<ConstraintValidatorFactory>)
                        loadClass(constraintFactoryClass);
                target.constraintValidatorFactory(newInstance(clazz));
                log.log(Level.INFO, String.format("Using %s as constraint factory.", constraintFactoryClass));
            }
        }
    }

    private void applyMappingStreams(ValidationConfigType xmlConfig,
                                     ConfigurationImpl target) {
        for (JAXBElement<String> mappingFileNameElement : xmlConfig.getConstraintMapping()) {
            String mappingFileName = mappingFileNameElement.getValue();
            if (mappingFileName.startsWith("/")) {
                // Classloader needs a path without a starting /
                mappingFileName = mappingFileName.substring(1);
            }
            log.log(Level.FINEST, String.format("Trying to open input stream for %s", mappingFileName));
            InputStream in = null;
            try {
                in = getInputStream(mappingFileName);
                if (in == null) {
                    throw new ValidationException(
                            "Unable to open input stream for mapping file " +
                                    mappingFileName);
                }
            } catch (IOException e) {
                throw new ValidationException("Unable to open input stream for mapping file " +
                        mappingFileName, e);
            }
            target.addMapping(in);
        }
    }


    private static <T> T doPrivileged(final PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
    }

    private Class<?> loadClass(final String className) {
        ClassLoader loader = doPrivileged(SecureActions.getContextClassLoader());
        if (loader == null)
            loader = getClass().getClassLoader();

        try {
            return Class.forName(className, true, loader);
        } catch (ClassNotFoundException ex) {
            throw new ValidationException("Unable to load class: " + className, ex);
        }
    }

}
