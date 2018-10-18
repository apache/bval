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
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ConfigurationState;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.metadata.MetadataBuilder;
import org.apache.bval.jsr.metadata.MetadataBuilder.ForBean;
import org.apache.bval.jsr.metadata.MetadataSource;
import org.apache.bval.jsr.metadata.ValidatorMappingProvider;
import org.apache.bval.jsr.metadata.XmlBuilder;
import org.apache.bval.jsr.metadata.XmlValidationMappingProvider;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;
import org.xml.sax.InputSource;

/**
 * Uses JAXB to parse constraints.xml based on the validation-mapping XML schema.
 */
@Privilizing(@CallTo(Reflection.class))
public class ValidationMappingParser implements MetadataSource {
    private static final SchemaManager SCHEMA_MANAGER = new SchemaManager.Builder()
        .add(XmlBuilder.Version.v10.getId(), "http://jboss.org/xml/ns/javax/validation/mapping",
            "META-INF/validation-mapping-1.0.xsd")
        .add(XmlBuilder.Version.v11.getId(), "http://jboss.org/xml/ns/javax/validation/mapping",
            "META-INF/validation-mapping-1.1.xsd")
        .add(XmlBuilder.Version.v20.getId(), "http://xmlns.jcp.org/xml/ns/validation/mapping",
            "META-INF/validation-mapping-2.0.xsd")
        .build();

    private ApacheValidatorFactory validatorFactory;

    @Override
    public void initialize(ApacheValidatorFactory validatorFactory) {
        this.validatorFactory = Validate.notNull(validatorFactory);
    }

    @Override
    public void process(ConfigurationState configurationState,
        Consumer<ValidatorMappingProvider> addValidatorMappingProvider, BiConsumer<Class<?>, ForBean<?>> addBuilder) {
        Validate.validState(validatorFactory != null, "validatorFactory unknown");

        if (configurationState.isIgnoreXmlConfiguration()) {
            return;
        }
        final Set<Class<?>> beanTypes = new HashSet<>();
        for (final InputStream xmlStream : configurationState.getMappingStreams()) {
            final ConstraintMappingsType mapping = parseXmlMappings(xmlStream);

            Optional.of(mapping).map(this::toMappingProvider).ifPresent(addValidatorMappingProvider);

            final Map<Class<?>, MetadataBuilder.ForBean<?>> builders =
                new XmlBuilder(validatorFactory, mapping).forBeans();
            if (Collections.disjoint(beanTypes, builders.keySet())) {
                builders.forEach(addBuilder::accept);
                beanTypes.addAll(builders.keySet());
            } else {
                Exceptions.raise(ValidationException::new,
                    builders.keySet().stream().filter(beanTypes::contains).map(Class::getName).collect(Collectors
                        .joining("bean classes specified multiple times for XML validation mapping: [", "; ", "]")));
            }
        }
    }

    /**
     * @param in
     *            XML stream to parse using the validation-mapping-1.0.xsd
     */
    private ConstraintMappingsType parseXmlMappings(final InputStream in) {
        try {
            return SCHEMA_MANAGER.unmarshal(new InputSource(in), ConstraintMappingsType.class);
        } catch (Exception e) {
            throw new ValidationException("Failed to parse XML deployment descriptor file.", e);
        } finally {
            try {
                in.reset(); // can be read several times + we ensured it was
                            // re-readable in addMapping()
            } catch (final IOException e) {
                // no-op
            }
        }
    }

    private ValidatorMappingProvider toMappingProvider(ConstraintMappingsType mapping) {
        if (mapping.getConstraintDefinition().isEmpty()) {
            return null;
        }
        final Map<Class<? extends Annotation>, ValidatedByType> validatorMappings = new HashMap<>();

        for (ConstraintDefinitionType constraintDefinition : mapping.getConstraintDefinition()) {
            final String annotationClassName = constraintDefinition.getAnnotation();

            final Class<?> clazz = loadClass(annotationClassName, mapping.getDefaultPackage());

            Exceptions.raiseUnless(clazz.isAnnotation(), ValidationException::new, "%s is not an annotation",
                annotationClassName);

            final Class<? extends Annotation> annotationClass = clazz.asSubclass(Annotation.class);

            Exceptions.raiseIf(validatorMappings.containsKey(annotationClass), ValidationException::new,
                "XML constraint validator(s) for %s already configured.", annotationClass);

            validatorMappings.put(annotationClass, constraintDefinition.getValidatedBy());
        }
        return new XmlValidationMappingProvider(validatorMappings,
            cn -> toQualifiedClassName(cn, mapping.getDefaultPackage()));
    }

    private Class<?> loadClass(String className, String defaultPackage) {
        return loadClass(toQualifiedClassName(className, defaultPackage));
    }

    private String toQualifiedClassName(String className, String defaultPackage) {
        if (!isQualifiedClass(className)) {
            if (className.startsWith("[L") && className.endsWith(";")) {
                className = "[L" + defaultPackage + '.' + className.substring(2);
            } else {
                className = defaultPackage + '.' + className;
            }
        }
        return className;
    }

    private boolean isQualifiedClass(String clazz) {
        return clazz.indexOf('.') >= 0;
    }

    private Class<?> loadClass(final String className) {
        try {
            return Reflection.toClass(className, Reflection.loaderFromClassOrThread(ValidationMappingParser.class));
        } catch (ClassNotFoundException ex) {
            throw Exceptions.create(ValidationException::new, ex, "Unable to load class: %s", className);
        }
    }
}
