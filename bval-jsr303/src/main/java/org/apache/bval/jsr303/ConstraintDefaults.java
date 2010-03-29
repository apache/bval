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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.validation.ConstraintValidator;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * Description: hold default constraints that are build in the framework.
 * The default constraints are configured in DefaultConstraints.properties.<br/>
 * User: roman <br/>
 * Date: 26.11.2009 <br/>
 * Time: 15:12:23 <br/>
 * Copyright: Agimatec GmbH
 */
public class ConstraintDefaults {
    private static final Log log = LogFactory.getLog(ConstraintDefaults.class);
    private static final String DEFAULT_CONSTAINTS =
          "org/apache/bval/jsr303/DefaultConstraints.properties";
    protected Map<String, Class[]> defaultConstraints;

    public ConstraintDefaults() {
        defaultConstraints = loadDefaultConstraints(DEFAULT_CONSTAINTS);
    }

    public Map<String, Class[]> getDefaultConstraints() {
        return defaultConstraints;
    }

    public Class<? extends ConstraintValidator<?, ?>>[] getValidatorClasses(
          Class<? extends java.lang.annotation.Annotation> annotationType) {
        return getDefaultConstraints().get(annotationType.getName());
    }
    
    private Map<String, Class[]> loadDefaultConstraints(String resource) {
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
            log.warn("cannot find " + resource);
        }
        Map<String, Class[]> loadedConstraints = new HashMap();
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
