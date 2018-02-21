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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

import org.apache.bval.jsr.BootstrapConfigurationImpl;
import org.apache.bval.jsr.ConfigurationImpl;
import org.apache.bval.jsr.metadata.XmlBuilder;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privileged;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;
import org.xml.sax.InputSource;

/**
 * Description: uses jaxb to parse validation.xml<br/>
 */
@Privilizing(@CallTo(Reflection.class))
public class ValidationParser {

    private static final String DEFAULT_VALIDATION_XML_FILE = "META-INF/validation.xml";
    private static final Logger log = Logger.getLogger(ValidationParser.class.getName());

    private static final SchemaManager SCHEMA_MANAGER = new SchemaManager.Builder()
        .add(null, "http://jboss.org/xml/ns/javax/validation/configuration",
            "META-INF/validation-configuration-1.0.xsd")
        .add(XmlBuilder.Version.v11.getId(), "http://jboss.org/xml/ns/javax/validation/configuration",
            "META-INF/validation-configuration-1.1.xsd")
        .add(XmlBuilder.Version.v20.getId(), "http://xmlns.jcp.org/xml/ns/javax/validation/configuration",
            "META-INF/validation-configuration-2.0.xsd")
        .build();

    public static String getValidationXmlFile(String file) {
        if (file == null) {
            return DEFAULT_VALIDATION_XML_FILE;
        }
        return file;
    }

    public static ValidationParser processValidationConfig(final String file, final ConfigurationImpl targetConfig,
        final boolean ignoreXml) {
        final ValidationParser parser = new ValidationParser();

        if (!ignoreXml) {
            parser.xmlConfig = parseXmlConfig(file);
        }

        if (parser.xmlConfig == null) { // default config
            final CopyOnWriteArraySet<ExecutableType> executableTypes = new CopyOnWriteArraySet<>();
            executableTypes.add(ExecutableType.CONSTRUCTORS);
            executableTypes.add(ExecutableType.NON_GETTER_METHODS);

            parser.bootstrap = new BootstrapConfigurationImpl(null, null, null, null, null, Collections.emptySet(),
                true, executableTypes, Collections.emptyMap(), null, Collections.emptySet());

            targetConfig.setExecutableValidation(executableTypes);
        } else {
            if (parser.xmlConfig.getExecutableValidation() == null) {
                final ExecutableValidationType value = new ExecutableValidationType();
                value.setEnabled(true);

                final DefaultValidatedExecutableTypesType defaultValidatedExecutableTypes =
                    new DefaultValidatedExecutableTypesType();
                value.setDefaultValidatedExecutableTypes(defaultValidatedExecutableTypes);
                defaultValidatedExecutableTypes.getExecutableType().add(ExecutableType.CONSTRUCTORS);
                defaultValidatedExecutableTypes.getExecutableType().add(ExecutableType.NON_GETTER_METHODS);

                parser.xmlConfig.setExecutableValidation(value);
            }

            applySimpleConfig(parser.xmlConfig, targetConfig);

            parser.bootstrap = new BootstrapConfigurationImpl(parser.xmlConfig.getDefaultProvider(),
                parser.xmlConfig.getConstraintValidatorFactory(), parser.xmlConfig.getMessageInterpolator(),
                parser.xmlConfig.getTraversableResolver(), parser.xmlConfig.getParameterNameProvider(),
                new HashSet<>(parser.xmlConfig.getConstraintMapping()),
                parser.xmlConfig.getExecutableValidation().getEnabled(),
                new HashSet<>(targetConfig.getExecutableValidation()), toMap(parser.xmlConfig.getProperty()),
                parser.xmlConfig.getClockProvider(), new HashSet<>(parser.xmlConfig.getValueExtractor()));
        }
        return parser;
    }

    private static Map<String, String> toMap(final List<PropertyType> property) {
        final Map<String, String> map = new HashMap<>();
        if (property != null) {
            for (final PropertyType p : property) {
                map.put(p.getName(), p.getValue());
            }
        }
        return map;
    }

    @Privileged
    private static ValidationConfigType parseXmlConfig(final String validationXmlFile) {
        try (InputStream inputStream = getInputStream(getValidationXmlFile(validationXmlFile))) {
            if (inputStream == null) {
                log.log(Level.FINEST,
                    String.format("No %s found. Using annotation based configuration only.", validationXmlFile));
                return null;
            }
            log.log(Level.FINEST, String.format("%s found.", validationXmlFile));

            return SCHEMA_MANAGER.unmarshal(new InputSource(inputStream), ValidationConfigType.class);
        } catch (Exception e) {
            throw new ValidationException("Unable to parse " + validationXmlFile, e);
        }
    }

    protected static InputStream getInputStream(final String path) throws IOException {
        final ClassLoader loader = Reflection.getClassLoader(ValidationParser.class);
        final List<URL> urls = Collections.list(loader.getResources(path));
        Exceptions.raiseIf(urls.stream().distinct().count() > 1, ValidationException::new,
            "More than one %s is found in the classpath", path);
        return urls.isEmpty() ? null : urls.get(0).openStream();
    }

    public static void applySimpleConfig(ValidationConfigType xmlConfig, ConfigurationImpl targetConfig) {
        applyExecutableValidation(xmlConfig, targetConfig);
    }

    private static void applyProperties(ValidationConfigType xmlConfig, ConfigurationImpl target) {
        for (final PropertyType property : xmlConfig.getProperty()) {
            target.addProperty(property.getName(), property.getValue());
        }
    }

    private static void applyExecutableValidation(final ValidationConfigType xmlConfig,
        final ConfigurationImpl targetConfig) {

        final Set<ExecutableType> executableTypes = Optional.of(xmlConfig)
            .map(ValidationConfigType::getExecutableValidation).filter(vc -> Boolean.TRUE.equals(vc.getEnabled()))
            .map(ExecutableValidationType::getDefaultValidatedExecutableTypes)
            .map(DefaultValidatedExecutableTypesType::getExecutableType).map(EnumSet::copyOf)
            .orElseGet(() -> EnumSet.noneOf(ExecutableType.class));

        if (executableTypes.contains(ExecutableType.ALL)) {
            executableTypes.clear();
            executableTypes.add(ExecutableType.CONSTRUCTORS);
            executableTypes.add(ExecutableType.NON_GETTER_METHODS);
            executableTypes.add(ExecutableType.GETTER_METHODS);
        } else if (executableTypes.contains(ExecutableType.NONE)) { // if both are present ALL trumps NONE
            executableTypes.clear();
        }
        targetConfig.setExecutableValidation(Collections.unmodifiableSet(executableTypes));
    }

    private static void applyMappingStreams(ValidationConfigType xmlConfig, ConfigurationImpl target) {
        for (String rawMappingFileName : xmlConfig.getConstraintMapping()) {
            String mappingFileName = rawMappingFileName;
            if (mappingFileName.charAt(0) == '/') {
                // Classloader needs a path without a starting /
                mappingFileName = mappingFileName.substring(1);
            }
            log.log(Level.FINEST, String.format("Trying to open input stream for %s", mappingFileName));
            try {
                final InputStream in = getInputStream(mappingFileName);
                Exceptions.raiseIf(in == null, ValidationException::new,
                    "Unable to open input stream for mapping file %s", mappingFileName);
                target.addMapping(in);
            } catch (IOException e) {
                Exceptions.raise(ValidationException::new, e, "Unable to open input stream for mapping file %s",
                    mappingFileName);
            }
        }
    }

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

    private void applyParameterNameProvider(final ValidationConfigType xmlConfig,
        final ConfigurationImpl targetConfig) {
        final String parameterNameProvider = xmlConfig.getParameterNameProvider();
        if (targetConfig.getParameterNameProvider() == targetConfig.getDefaultParameterNameProvider()
            && parameterNameProvider != null) {
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
    private void applyMessageInterpolator(ValidationConfigType xmlConfig, ConfigurationImpl target) {
        String messageInterpolatorClass = xmlConfig.getMessageInterpolator();
        if (target.getMessageInterpolator() == target.getDefaultMessageInterpolator()
            && messageInterpolatorClass != null) {
            Class<MessageInterpolator> clazz = (Class<MessageInterpolator>) loadClass(messageInterpolatorClass);
            target.messageInterpolatorClass(clazz);
            log.log(Level.INFO, String.format("Using %s as message interpolator.", messageInterpolatorClass));
        }
    }

    @SuppressWarnings("unchecked")
    private void applyTraversableResolver(ValidationConfigType xmlConfig, ConfigurationImpl target) {
        String traversableResolverClass = xmlConfig.getTraversableResolver();
        if (target.getTraversableResolver() == target.getDefaultTraversableResolver()
            && traversableResolverClass != null) {
            Class<TraversableResolver> clazz = (Class<TraversableResolver>) loadClass(traversableResolverClass);
            target.traversableResolverClass(clazz);
            log.log(Level.INFO, String.format("Using %s as traversable resolver.", traversableResolverClass));
        }
    }

    @SuppressWarnings("unchecked")
    private void applyConstraintFactory(ValidationConfigType xmlConfig, ConfigurationImpl target) {
        String constraintFactoryClass = xmlConfig.getConstraintValidatorFactory();
        if (target.getConstraintValidatorFactory() == target.getDefaultConstraintValidatorFactory()
            && constraintFactoryClass != null) {
            Class<ConstraintValidatorFactory> clazz =
                (Class<ConstraintValidatorFactory>) loadClass(constraintFactoryClass);
            target.constraintValidatorFactoryClass(clazz);
            log.log(Level.INFO, String.format("Using %s as constraint factory.", constraintFactoryClass));
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
