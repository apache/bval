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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.validation.BootstrapConfiguration;
import jakarta.validation.ValidationException;
import jakarta.validation.executable.ExecutableType;

import org.apache.bval.jsr.BootstrapConfigurationImpl;
import org.apache.bval.jsr.ConfigurationImpl;
import org.apache.bval.jsr.metadata.XmlBuilder;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;
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

    /*
    The order is very important because the last entry is used to override all schema location before parsing
     */
    private static final SchemaManager SCHEMA_MANAGER = new SchemaManager.Builder()
        .add(XmlBuilder.Version.v10.getId(), "http://jboss.org/xml/ns/javax/validation/configuration",
            "META-INF/validation-configuration-1.0.xsd")
        .add(XmlBuilder.Version.v11.getId(), "http://jboss.org/xml/ns/javax/validation/configuration",
            "META-INF/validation-configuration-1.1.xsd")
        .add(XmlBuilder.Version.v20.getId(), "http://xmlns.jcp.org/xml/ns/validation/configuration",
            "META-INF/validation-configuration-2.0.xsd")
        .add(XmlBuilder.Version.v30.getId(), "https://jakarta.ee/xml/ns/validation/configuration",
                "META-INF/validation-configuration-3.0.xsd")
        .build();

    private static String getValidationXmlFile(String file) {
        return file == null ? DEFAULT_VALIDATION_XML_FILE : file;
    }

    private static Map<String, String> toMap(final List<PropertyType> property) {
        return property == null || property.isEmpty() ? Collections.emptyMap()
            : property.stream().collect(Collectors.toMap(PropertyType::getName, PropertyType::getValue));
    }

    private final ClassLoader loader;

    public ValidationParser(ClassLoader loader) {
        this.loader = Validate.notNull(loader, null);
    }

    public BootstrapConfiguration processValidationConfig(final String file,
        final ConfigurationImpl targetConfig) {
        final ValidationConfigType xmlConfig = parseXmlConfig(file);
        if (xmlConfig == null) {
            return null;
        }
        final boolean executableValidationEnabled;
        final Set<ExecutableType> defaultValidatedExecutableTypes;

        if (xmlConfig.getExecutableValidation() == null) {
            defaultValidatedExecutableTypes = EnumSet.of(ExecutableType.IMPLICIT);
            executableValidationEnabled = true;
        } else {
            final Optional<ExecutableValidationType> executableValidation =
                Optional.of(xmlConfig).map(ValidationConfigType::getExecutableValidation);
            executableValidationEnabled = executableValidation.map(ExecutableValidationType::getEnabled)
                .filter(Predicate.isEqual(Boolean.TRUE)).isPresent();

            defaultValidatedExecutableTypes = executableValidation.filter(x -> executableValidationEnabled)
                .map(ExecutableValidationType::getDefaultValidatedExecutableTypes)
                .map(DefaultValidatedExecutableTypesType::getExecutableType).map(EnumSet::copyOf)
                .orElse(EnumSet.noneOf(ExecutableType.class));
        }
        return new BootstrapConfigurationImpl(xmlConfig.getDefaultProvider(), xmlConfig.getConstraintValidatorFactory(),
            xmlConfig.getMessageInterpolator(), xmlConfig.getTraversableResolver(),
            xmlConfig.getParameterNameProvider(), new HashSet<>(xmlConfig.getConstraintMapping()),
            executableValidationEnabled, defaultValidatedExecutableTypes, toMap(xmlConfig.getProperty()),
            xmlConfig.getClockProvider(), new HashSet<>(xmlConfig.getValueExtractor()));
    }

    public InputStream open(String mappingFileName) {
        if (mappingFileName.charAt(0) == '/') {
            // Classloader needs a path without a starting /
            mappingFileName = mappingFileName.substring(1);
        }
        mappingFileName = mappingFileName.trim();
        try {
            final InputStream in = getInputStream(mappingFileName);
            Exceptions.raiseIf(in == null, ValidationException::new,
                    "Unable to open input stream for mapping file %s", mappingFileName);
            return(in);
        } catch (IOException e) {
            throw Exceptions.create(ValidationException::new, e, "Unable to open input stream for mapping file %s",
                mappingFileName);
        }
    }

    InputStream getInputStream(final String path) throws IOException {
        final List<URL> urls = Collections.list(loader.getResources(path));
        Exceptions.raiseIf(urls.stream().distinct().count() > 1, ValidationException::new,
            "More than one %s is found in the classpath", path);
        return urls.isEmpty() ? null : urls.get(0).openStream();
    }

    @Privileged
    private ValidationConfigType parseXmlConfig(final String validationXmlFile) {
        try (InputStream inputStream = getInputStream(getValidationXmlFile(validationXmlFile))) {
            if (inputStream == null) {
                log.log(Level.FINEST,
                    String.format("No %s found. Using annotation based configuration only.", validationXmlFile));
                return null;
            }
            log.log(Level.FINEST, String.format("%s found.", validationXmlFile));

            return SCHEMA_MANAGER.unmarshal(new InputSource(inputStream), ValidationConfigType.class);
        } catch (Exception e) {
            throw Exceptions.create(ValidationException::new, e, "Unable to parse %s", validationXmlFile);
        }
    }
}
