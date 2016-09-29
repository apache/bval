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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.executable.ExecutableType;
import javax.validation.spi.ValidationProvider;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.bval.jsr.BootstrapConfigurationImpl;
import org.apache.bval.jsr.ConfigurationImpl;
import org.apache.bval.jsr.util.IOs;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privileged;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;
import org.xml.sax.SAXException;

/**
 * Description: uses jaxb to parse validation.xml<br/>
 */
@Privilizing(@CallTo(Reflection.class))
public class ValidationParser {
    private static final String DEFAULT_VALIDATION_XML_FILE = "META-INF/validation.xml";
    private static final String VALIDATION_CONFIGURATION_XSD = "META-INF/validation-configuration-1.1.xsd";
    private static final Logger log = Logger.getLogger(ValidationParser.class.getName());
    private static final ConcurrentMap<String, Schema> SCHEMA_CACHE = new ConcurrentHashMap<String, Schema>(1);

    private ValidationConfigType xmlConfig;
    private BootstrapConfigurationImpl bootstrap;
    private Collection<ValidationException> exceptions = new CopyOnWriteArrayList<ValidationException>();

    private ValidationParser() {
        // no-op
    }

    public void applyConfigWithInstantiation(ConfigurationImpl targetConfig) {
        if (xmlConfig == null) {
            return;
        }

        applyProviderClass(xmlConfig, targetConfig);
        applyMessageInterpolator(xmlConfig, targetConfig);
        applyTraversableResolver(xmlConfig, targetConfig);
        applyConstraintFactory(xmlConfig, targetConfig);
        applyParameterNameProvider(xmlConfig, targetConfig);
        applyMappingStreams(xmlConfig, targetConfig);
        applyProperties(xmlConfig, targetConfig);
    }

    public BootstrapConfigurationImpl getBootstrap() {
        return bootstrap;
    }

    public static String getValidationXmlFile(String file) {
        if (file == null) {
            return DEFAULT_VALIDATION_XML_FILE;
        }
        return file;
    }

    public static ValidationParser processValidationConfig(final String file, final ConfigurationImpl targetConfig, final boolean ignoreXml) {
        final ValidationParser parser = new ValidationParser();

        if (!ignoreXml) {
            parser.xmlConfig = parseXmlConfig(file);
        }

        if (parser.xmlConfig != null) {
            if (parser.xmlConfig.getExecutableValidation() == null) {
                final ExecutableValidationType value = new ExecutableValidationType();
                value.setEnabled(true);

                final DefaultValidatedExecutableTypesType defaultValidatedExecutableTypes = new DefaultValidatedExecutableTypesType();
                value.setDefaultValidatedExecutableTypes(defaultValidatedExecutableTypes);
                defaultValidatedExecutableTypes.getExecutableType().add(ExecutableType.CONSTRUCTORS);
                defaultValidatedExecutableTypes.getExecutableType().add(ExecutableType.NON_GETTER_METHODS);

                parser.xmlConfig.setExecutableValidation(value);
            }

            applySimpleConfig(parser.xmlConfig, targetConfig);

            parser.bootstrap = new BootstrapConfigurationImpl(
                    parser.xmlConfig.getDefaultProvider(),
                    parser.xmlConfig.getConstraintValidatorFactory(),
                    parser.xmlConfig.getMessageInterpolator(),
                    parser.xmlConfig.getTraversableResolver(),
                    parser.xmlConfig.getParameterNameProvider(),
                    new CopyOnWriteArraySet<String>(parser.xmlConfig.getConstraintMapping()),
                    parser.xmlConfig.getExecutableValidation().getEnabled(),
                    new CopyOnWriteArraySet<ExecutableType>(targetConfig.getExecutableValidation()),
                    toMap(parser.xmlConfig.getProperty()));
        } else { // default config
            final CopyOnWriteArraySet<ExecutableType> executableTypes = new CopyOnWriteArraySet<ExecutableType>();
            executableTypes.add(ExecutableType.CONSTRUCTORS);
            executableTypes.add(ExecutableType.NON_GETTER_METHODS);

            parser.bootstrap = new BootstrapConfigurationImpl(
                    null, null, null, null, null,
                    new CopyOnWriteArraySet<String>(),
                    true,
                    executableTypes,
                    new HashMap<String, String>());

            targetConfig.setExecutableValidation(executableTypes);
        }
        return parser;
    }

    private static Map<String, String> toMap(final List<PropertyType> property) {
        final Map<String, String> map = new HashMap<String, String>();
        if (property != null) {
            for (final PropertyType p : property) {
                map.put(p.getName(), p.getValue());
            }
        }
        return map;
    }

    @Privileged
    private static ValidationConfigType parseXmlConfig(final String validationXmlFile) {
        InputStream inputStream = null;
        try {
            inputStream = getInputStream(getValidationXmlFile(validationXmlFile));
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
            IOs.closeQuietly(inputStream);
        }
    }

    protected static InputStream getInputStream(final String path) throws IOException {
        final ClassLoader loader = Reflection.getClassLoader(ValidationParser.class);
        final InputStream inputStream = loader.getResourceAsStream(path);

        if (inputStream != null) {
            // spec says: If more than one META-INF/validation.xml file
            // is found in the classpath, a ValidationException is raised.
            final Enumeration<URL> urls = loader.getResources(path);
            if (urls.hasMoreElements()) {
                final String url = urls.nextElement().toString();
                while (urls.hasMoreElements()) {
                    if (!url.equals(urls.nextElement().toString())) { // complain when first duplicate found
                        throw new ValidationException("More than one " + path + " is found in the classpath");
                    }
                }
            }
        }

        return IOs.convertToMarkableInputStream(inputStream);
    }

    private static Schema getSchema() {
        return getSchema(VALIDATION_CONFIGURATION_XSD);
    }

    static Schema getSchema(final String xsd) {
        final Schema schema = SCHEMA_CACHE.get(xsd);
        if (schema != null) {
            return schema;
        }

        final ClassLoader loader = Reflection.getClassLoader(ValidationParser.class);
        final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final URL schemaUrl = loader.getResource(xsd);
        try {
            Schema s = sf.newSchema(schemaUrl);
            final Schema old = SCHEMA_CACHE.putIfAbsent(xsd, s);
            if (old != null) {
                s = old;
            }
            return s;
        } catch (SAXException e) {
            log.log(Level.WARNING, String.format("Unable to parse schema: %s", xsd), e);
            return null;
        }
    }

    public static void applySimpleConfig(ValidationConfigType xmlConfig, ConfigurationImpl targetConfig) {
        applyExecutableValidation(xmlConfig, targetConfig);
    }

    private static void applyProperties(ValidationConfigType xmlConfig, ConfigurationImpl target) {
        for (final PropertyType property : xmlConfig.getProperty()) {
            target.addProperty(property.getName(), property.getValue());
        }
    }

    private static void applyExecutableValidation(final ValidationConfigType xmlConfig, final ConfigurationImpl targetConfig) {
        final CopyOnWriteArrayList<ExecutableType> executableTypes = new CopyOnWriteArrayList<ExecutableType>();
        if (xmlConfig.getExecutableValidation() != null && xmlConfig.getExecutableValidation().getEnabled()
                && xmlConfig.getExecutableValidation().getDefaultValidatedExecutableTypes() != null) {
            executableTypes.addAll(xmlConfig.getExecutableValidation().getDefaultValidatedExecutableTypes().getExecutableType());
        }

        if (executableTypes.contains(ExecutableType.ALL)) {
            executableTypes.clear();
            executableTypes.add(ExecutableType.CONSTRUCTORS);
            executableTypes.add(ExecutableType.NON_GETTER_METHODS);
            executableTypes.add(ExecutableType.GETTER_METHODS);
        } else if (executableTypes.contains(ExecutableType.NONE)) { // if both are present ALL gains
            executableTypes.clear();
        }

        targetConfig.setExecutableValidation(executableTypes);
    }

    private void applyParameterNameProvider(final ValidationConfigType xmlConfig, final ConfigurationImpl targetConfig) {
        final String parameterNameProvider = xmlConfig.getParameterNameProvider();
        if (targetConfig.getParameterNameProvider() == targetConfig.getDefaultParameterNameProvider()) { // ref ==
            if (parameterNameProvider != null) {
                final Class<?> loaded = loadClass(parameterNameProvider);
                if (loaded == null) {
                    log.log(Level.SEVERE, "Can't load " + parameterNameProvider);
                } else {
                    final Class<? extends ParameterNameProvider> clazz = loaded.asSubclass(ParameterNameProvider.class);
                    targetConfig.parameterNameProviderClass(clazz);
                    log.log(Level.INFO, String.format("Using %s as validation provider.", parameterNameProvider));
                }
            }
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
        if (target.getMessageInterpolator() == target.getDefaultMessageInterpolator()) { // ref ==
            if (messageInterpolatorClass != null) {
                Class<MessageInterpolator> clazz = (Class<MessageInterpolator>)
                        loadClass(messageInterpolatorClass);
                target.messageInterpolatorClass(clazz);
                log.log(Level.INFO, String.format("Using %s as message interpolator.", messageInterpolatorClass));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void applyTraversableResolver(ValidationConfigType xmlConfig,
                                          ConfigurationImpl target) {
        String traversableResolverClass = xmlConfig.getTraversableResolver();
        if (target.getTraversableResolver() == target.getDefaultTraversableResolver() && traversableResolverClass != null) {
		    Class<TraversableResolver> clazz = (Class<TraversableResolver>)
		            loadClass(traversableResolverClass);
		    target.traversableResolverClass(clazz);
		    log.log(Level.INFO, String.format("Using %s as traversable resolver.", traversableResolverClass));
		}
    }

    @SuppressWarnings("unchecked")
    private void applyConstraintFactory(ValidationConfigType xmlConfig,
                                        ConfigurationImpl target) {
        String constraintFactoryClass = xmlConfig.getConstraintValidatorFactory();
        if (target.getConstraintValidatorFactory() == target.getDefaultConstraintValidatorFactory() && constraintFactoryClass != null) {
		    Class<ConstraintValidatorFactory> clazz = (Class<ConstraintValidatorFactory>)
		            loadClass(constraintFactoryClass);
		    target.constraintValidatorFactoryClass(clazz);
		    log.log(Level.INFO, String.format("Using %s as constraint factory.", constraintFactoryClass));
		}
    }

    private static void applyMappingStreams(ValidationConfigType xmlConfig,
                                     ConfigurationImpl target) {
        for (String rawMappingFileName : xmlConfig.getConstraintMapping()) {
            String mappingFileName = rawMappingFileName;
            if (mappingFileName.startsWith("/")) {
                // Classloader needs a path without a starting /
                mappingFileName = mappingFileName.substring(1);
            }
            log.log(Level.FINEST, String.format("Trying to open input stream for %s", mappingFileName));
            InputStream in;
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

    private Class<?> loadClass(final String className) {
        final ClassLoader loader = Reflection.getClassLoader(ValidationParser.class);
        try {
            return Class.forName(className, true, loader);
        } catch (final ClassNotFoundException ex) {
            // TCK check BootstrapConfig is present in all cases
            // so throw next exception later
            exceptions.add(new ValidationException("Unable to load class: " + className, ex));
            return null;
        }
    }

    public void ensureValidatorFactoryCanBeBuilt() {
        if (!exceptions.isEmpty()) {
            throw exceptions.iterator().next();
        }
    }
}
