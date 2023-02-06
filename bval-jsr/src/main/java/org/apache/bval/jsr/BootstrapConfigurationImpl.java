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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import jakarta.validation.BootstrapConfiguration;
import jakarta.validation.executable.ExecutableType;

import org.apache.bval.jsr.util.ExecutableTypes;

public class BootstrapConfigurationImpl implements BootstrapConfiguration {
    public static final BootstrapConfigurationImpl DEFAULT = new BootstrapConfigurationImpl(Collections.emptySet(),
        true, EnumSet.of(ExecutableType.IMPLICIT), Collections.emptyMap(), Collections.emptySet());

    private final Set<String> constraintMappingResourcePaths;
    private final boolean executableValidationEnabled;
    private final Set<ExecutableType> defaultValidatedExecutableTypes;
    private final Map<String, String> properties;
    private final Set<String> valueExtractorClassNames;

    private String parameterNameProviderClassName;
    private String traversableResolverClassName;
    private String messageInterpolatorClassName;
    private String constraintValidatorFactoryClassName;
    private String defaultProviderClassName;
    private String clockProviderClassName;

    public BootstrapConfigurationImpl(final String defaultProviderClassName,
        final String constraintValidatorFactoryClassName, final String messageInterpolatorClassName,
        final String traversableResolverClassName, final String parameterNameProviderClassName,
        final Set<String> constraintMappingResourcePaths, final boolean executableValidationEnabled,
        final Set<ExecutableType> defaultValidatedExecutableTypes, final Map<String, String> properties,
        final String clockProviderClassName, final Set<String> valueExtractorClassNames) {

        this(Collections.unmodifiableSet(constraintMappingResourcePaths), executableValidationEnabled,
            defaultValidatedExecutableTypes, Collections.unmodifiableMap(properties),
            Collections.unmodifiableSet(valueExtractorClassNames));

        this.parameterNameProviderClassName = parameterNameProviderClassName;
        this.traversableResolverClassName = traversableResolverClassName;
        this.messageInterpolatorClassName = messageInterpolatorClassName;
        this.constraintValidatorFactoryClassName = constraintValidatorFactoryClassName;
        this.defaultProviderClassName = defaultProviderClassName;
        this.clockProviderClassName = clockProviderClassName;
    }

    private BootstrapConfigurationImpl(final Set<String> constraintMappingResourcePaths,
        final boolean executableValidationEnabled, final Set<ExecutableType> defaultValidatedExecutableTypes,
        final Map<String, String> properties, final Set<String> valueExtractorClassNames) {

        this.constraintMappingResourcePaths = constraintMappingResourcePaths;
        this.executableValidationEnabled = executableValidationEnabled;
        this.defaultValidatedExecutableTypes = ExecutableTypes.interpret(defaultValidatedExecutableTypes);
        this.properties = properties;
        this.valueExtractorClassNames = valueExtractorClassNames;
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
        return Collections.unmodifiableSet(constraintMappingResourcePaths);
    }

    @Override
    public boolean isExecutableValidationEnabled() {
        return executableValidationEnabled;
    }

    @Override
    public Set<ExecutableType> getDefaultValidatedExecutableTypes() {
        return Collections.unmodifiableSet(defaultValidatedExecutableTypes);
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * @since 2.0
     */
    @Override
    public String getClockProviderClassName() {
        return clockProviderClassName;
    }

    /**
     * @since 2.0
     */
    @Override
    public Set<String> getValueExtractorClassNames() {
        return Collections.unmodifiableSet(valueExtractorClassNames);
    }
}
