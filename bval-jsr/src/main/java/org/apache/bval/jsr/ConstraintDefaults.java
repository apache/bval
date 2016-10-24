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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintValidator;

import org.apache.bval.util.StringUtils;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Description: Provides access to the default constraints/validator implementation classes built into the framework.
 * These are configured in DefaultConstraints.properties.<br/>
 */
@Privilizing(@CallTo(Reflection.class))
public class ConstraintDefaults {
    private static final Logger log = Logger.getLogger(ConstraintDefaults.class.getName());
    private static final String DEFAULT_CONSTRAINTS = "org/apache/bval/jsr/DefaultConstraints.properties";
    
    /**
     * The default constraint data stored herein.
     */
    private Map<String, Class<? extends ConstraintValidator<?, ?>>[]> defaultConstraints;

    /**
     * Create a new ConstraintDefaults instance.
     */
    public ConstraintDefaults() {
        defaultConstraints = loadDefaultConstraints(DEFAULT_CONSTRAINTS);
    }

    /**
     * Get the default constraint data.
     * @return String-keyed map
     */
    public Map<String, Class<? extends ConstraintValidator<?, ?>>[]> getDefaultConstraints() {
        return defaultConstraints;
    }

    /**
     * Get the default validator implementation types for the specified constraint annotation type. 
     * @param annotationType the annotation type
     * @return array of {@link ConstraintValidator} implementation classes
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> Class<? extends ConstraintValidator<A, ?>>[] getValidatorClasses(
          Class<A> annotationType) {
        return (Class<? extends ConstraintValidator<A, ?>>[]) getDefaultConstraints().get(annotationType.getName());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Class<? extends ConstraintValidator<?, ?>>[]> loadDefaultConstraints(String resource) {
        final Properties constraintProperties = new Properties();
        final ClassLoader classloader = getClassLoader();
        final InputStream stream = classloader.getResourceAsStream(resource);
        if (stream == null) {
            log.log(Level.WARNING, String.format("Cannot find %s", resource));
        } else {
            try {
                constraintProperties.load(stream);
            } catch (IOException e) {
                log.log(Level.SEVERE, String.format("Cannot load %s", resource), e);
            } finally {
                try {
                    stream.close();
                } catch (final IOException e) {
                    // no-op
                }
            }
        }

        final Map<String, Class<? extends ConstraintValidator<?, ?>>[]> loadedConstraints =
            new HashMap<String, Class<? extends ConstraintValidator<?, ?>>[]>();

        for (final Map.Entry<Object, Object> entry : constraintProperties.entrySet()) {
            final List<Class<?>> classes = new LinkedList<Class<?>>();
            for (String className : StringUtils.split((String) entry.getValue(), ',')) {
                try {
                    classes.add(Reflection.toClass(className.trim(), classloader));
                } catch (Exception e) {
                    log.log(Level.SEVERE, String.format("Cannot find class %s", className), e);
                }
            }
            loadedConstraints.put((String) entry.getKey(), classes.toArray(new Class[classes.size()]));
        }
        return loadedConstraints;
    }

    private ClassLoader getClassLoader() {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        return classloader == null ? getClass().getClassLoader() : classloader;
    }
}
