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
package org.apache.bval.jsr;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.ConstraintValidator;

import org.apache.bval.jsr.metadata.ClassLoadingValidatorMappingProvider;
import org.apache.bval.jsr.metadata.ValidatorMapping;
import org.apache.bval.util.StringUtils;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Description: Provides access to the default constraints/validator
 * implementation classes built into the framework. These are configured in
 * DefaultConstraints.properties.<br/>
 */
@Privilizing(@CallTo(Reflection.class))
public class ConstraintDefaults extends ClassLoadingValidatorMappingProvider {
    public static final ConstraintDefaults INSTANCE = new ConstraintDefaults();

    private static final Logger log = Logger.getLogger(ConstraintDefaults.class.getName());
    private static final String DEFAULT_CONSTRAINTS = "org/apache/bval/jsr/DefaultConstraints.properties";

    private final Properties properties;

    /**
     * Create a new ConstraintDefaults instance.
     */
    private ConstraintDefaults() {
        this.properties = loadProperties(DEFAULT_CONSTRAINTS);
    }

    private Properties loadProperties(String resource) {
        final Properties result = new Properties();
        final ClassLoader classloader = getClassLoader();
        try (final InputStream stream = classloader.getResourceAsStream(resource)) {
            if (stream == null) {
                if (log.isLoggable(Level.WARNING)) {
                    log.log(Level.WARNING, String.format("Cannot find %s", resource));
                }
            } else {
                result.load(stream);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, String.format("Cannot load %s", resource), e);
        }
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <A extends Annotation> ValidatorMapping<A> doGetValidatorMapping(Class<A> constraintType) {

        final String validators = properties.getProperty(constraintType.getName());

        if (StringUtils.isBlank(validators)) {
            return null;
        }
        return new ValidatorMapping<>("built-in",
            load(Stream.of(StringUtils.split(validators, ',')).map(String::trim),
                (Class<ConstraintValidator<A, ?>>) (Class) ConstraintValidator.class,
                e -> log.log(Level.SEVERE, "exception loading default constraint validators", e))
                    .collect(Collectors.toList()));
    }

    @Override
    protected ClassLoader getClassLoader() {
        return Reflection.loaderFromClassOrThread(ConstraintDefaults.class);
    }
}
