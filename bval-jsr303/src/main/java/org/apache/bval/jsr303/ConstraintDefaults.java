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
package org.apache.bval.jsr303;

import org.apache.bval.jsr303.util.SecureActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintValidator;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * Description: Provides access to the default constraints/validator implementation classes built into the framework.
 * These are configured in DefaultConstraints.properties.<br/>
 */
public class ConstraintDefaults {
    private static final Logger log = LoggerFactory.getLogger(ConstraintDefaults.class);
    private static final String DEFAULT_CONSTRAINTS =
          "org/apache/bval/jsr303/DefaultConstraints.properties";
    
    /**
     * The default constraint data stored herein.
     */
    protected Map<String, Class<? extends ConstraintValidator<?, ?>>[]> defaultConstraints;

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
     * @param annotationType
     * @return array of {@link ConstraintValidator} implementation classes
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> Class<? extends ConstraintValidator<A, ?>>[] getValidatorClasses(
          Class<A> annotationType) {
        return (Class<? extends ConstraintValidator<A, ?>>[]) getDefaultConstraints().get(annotationType.getName());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Class<? extends ConstraintValidator<?, ?>>[]> loadDefaultConstraints(String resource) {
        Properties constraintProperties = new Properties();
        final ClassLoader classloader = getClassLoader();
        InputStream stream = classloader.getResourceAsStream(resource);
        if (stream != null) {
            try {
                constraintProperties.load(stream);
            } catch (IOException e) {
                log.error("cannot load " + resource, e);
            }
        } else {
            log.warn("cannot find {}", resource);
        }
        Map<String, Class<? extends ConstraintValidator<?, ?>>[]> loadedConstraints
                = new HashMap<String, Class<? extends ConstraintValidator<?,?>>[]>();
        for (Map.Entry entry : constraintProperties.entrySet()) {

            StringTokenizer tokens = new StringTokenizer((String) entry.getValue(), ", ");
            LinkedList classes = new LinkedList();
            while (tokens.hasMoreTokens()) {
                final String eachClassName = tokens.nextToken();

                Class constraintValidatorClass =
                      SecureActions.run(new PrivilegedAction<Class>() {
                          public Class run() {
                              try {
                                  return Class.forName(eachClassName, true, classloader);
                              } catch (ClassNotFoundException e) {
                                  log.error("Cannot find class " + eachClassName, e);
                                  return null;
                              }
                          }
                      });

                if (constraintValidatorClass != null) classes.add(constraintValidatorClass);

            }
            loadedConstraints
                  .put((String) entry.getKey(),
                        (Class[]) classes.toArray(new Class[classes.size()]));

        }
        return loadedConstraints;
    }

    private ClassLoader getClassLoader() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        if (classloader == null) classloader = getClass().getClassLoader();
        return classloader;
    }
}
