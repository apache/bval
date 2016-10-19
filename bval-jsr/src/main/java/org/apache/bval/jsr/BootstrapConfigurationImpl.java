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

import javax.validation.BootstrapConfiguration;
import javax.validation.executable.ExecutableType;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class BootstrapConfigurationImpl implements BootstrapConfiguration {
    private Map<String, String> properties;
    private Set<ExecutableType> defaultValidatedExecutableTypes;
    private boolean executableValidationEnabled;
    private Set<String> constraintMappingResourcePaths;
    private String parameterNameProviderClassName;
    private String traversableResolverClassName;
    private String messageInterpolatorClassName;
    private String constraintValidatorFactoryClassName;
    private String defaultProviderClassName;

    public BootstrapConfigurationImpl(final String defaultProviderClassName,
                                      final String constraintValidatorFactoryClassName,
                                      final String messageInterpolatorClassName,
                                      final String traversableResolverClassName,
                                      final String parameterNameProviderClassName,
                                      final Set<String> constraintMappingResourcePaths,
                                      final boolean executableValidationEnabled,
                                      final Set<ExecutableType> defaultValidatedExecutableTypes,
                                      final Map<String, String> properties) {
        this.properties = Collections.unmodifiableMap(properties);
        this.defaultValidatedExecutableTypes = Collections.unmodifiableSet(defaultValidatedExecutableTypes);
        this.executableValidationEnabled = executableValidationEnabled;
        this.constraintMappingResourcePaths = Collections.unmodifiableSet(constraintMappingResourcePaths);
        this.parameterNameProviderClassName = parameterNameProviderClassName;
        this.traversableResolverClassName = traversableResolverClassName;
        this.messageInterpolatorClassName = messageInterpolatorClassName;
        this.constraintValidatorFactoryClassName = constraintValidatorFactoryClassName;
        this.defaultProviderClassName = defaultProviderClassName;
    }

    @Override
    public String getDefaultProviderClassName() {
        return defaultProviderClassName;
    }

    @Override
    public String getConstraintValidatorFactoryClassName() {
        return constraintValidatorFactoryClassName;
    }

    @Override
    public String getMessageInterpolatorClassName() {
        return messageInterpolatorClassName;
    }

    @Override
    public String getTraversableResolverClassName() {
        return traversableResolverClassName;
    }

    @Override
    public String getParameterNameProviderClassName() {
        return parameterNameProviderClassName;
    }

    @Override
    public Set<String> getConstraintMappingResourcePaths() {
        return constraintMappingResourcePaths;
    }

    @Override
    public boolean isExecutableValidationEnabled() {
        return executableValidationEnabled;
    }

    @Override
    public Set<ExecutableType> getDefaultValidatedExecutableTypes() {
        return defaultValidatedExecutableTypes;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }
}
