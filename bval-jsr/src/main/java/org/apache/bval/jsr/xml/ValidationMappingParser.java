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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ValidationException;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.metadata.XmlBuilder;
import org.apache.bval.jsr.metadata.XmlValidationMappingProvider;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;
import org.xml.sax.InputSource;

/**
 * Uses JAXB to parse constraints.xml based on the validation-mapping XML
 * schema.
 */
@Privilizing(@CallTo(Reflection.class))
public class ValidationMappingParser {
    private static final SchemaManager SCHEMA_MANAGER = new SchemaManager.Builder()
        .add(null, "http://jboss.org/xml/ns/javax/validation/mapping", "META-INF/validation-mapping-1.0.xsd")
        .add(XmlBuilder.Version.v11.getId(), "http://jboss.org/xml/ns/javax/validation/mapping",
            "META-INF/validation-mapping-1.1.xsd")
        .add(XmlBuilder.Version.v20.getId(), "http://xmlns.jcp.org/xml/ns/javax/validation/mapping",
            "META-INF/validation-mapping-2.0.xsd")
        .build();

    private final ApacheValidatorFactory factory;

    public ValidationMappingParser(ApacheValidatorFactory factory) {
        this.factory = Validate.notNull(factory, "factory");
    }

    /**
     * Parse files with constraint mappings and collect information in the
     * factory.
     * 
     * @param xmlStreams
     *            - one or more contraints.xml file streams to parse
     */
    public void processMappingConfig(Set<InputStream> xmlStreams) throws ValidationException {
        for (final InputStream xmlStream : xmlStreams) {
            final ConstraintMappingsType mapping = parseXmlMappings(xmlStream);
            processConstraintDefinitions(mapping.getConstraintDefinition(), mapping.getDefaultPackage());
            new XmlBuilder(mapping).forBeans().forEach(factory.getMetadataBuilders()::registerCustomBuilder);
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

    private void processConstraintDefinitions(List<ConstraintDefinitionType> constraintDefinitionList,
        String defaultPackage) {

        final Map<Class<? extends Annotation>, ValidatedByType> validatorMappings = new HashMap<>();

        for (ConstraintDefinitionType constraintDefinition : constraintDefinitionList) {
            final String annotationClassName = constraintDefinition.getAnnotation();

            final Class<?> clazz = loadClass(annotationClassName, defaultPackage);

            Exceptions.raiseUnless(clazz.isAnnotation(), ValidationException::new, "%s is not an annotation",
                annotationClassName);

            final Class<? extends Annotation> annotationClass = clazz.asSubclass(Annotation.class);

            Exceptions.raiseIf(validatorMappings.containsKey(annotationClass), ValidationException::new,
                "Constraint validator for %s already configured.", annotationClass);

            validatorMappings.put(annotationClass, constraintDefinition.getValidatedBy());
        }
        factory.getConstraintsCache()
            .add(new XmlValidationMappingProvider(validatorMappings, cn -> toQualifiedClassName(cn, defaultPackage)));
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
        ClassLoader loader = Reflection.getClassLoader(ValidationMappingParser.class);
        if (loader == null) {
            loader = getClass().getClassLoader();
        }
        try {
            return Reflection.toClass(className, loader);
        } catch (ClassNotFoundException ex) {
            throw Exceptions.create(ValidationException::new, ex, "Unable to load class: %s", className);
        }
    }
}
