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

import java.io.Closeable;
import java.io.InputStream;
import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.validation.BootstrapConfiguration;
import javax.validation.ClockProvider;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.ValidatorFactory;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ConfigurationState;
import javax.validation.spi.ValidationProvider;
import javax.validation.valueextraction.ValueExtractor;

import org.apache.bval.jsr.parameter.DefaultParameterNameProvider;
import org.apache.bval.jsr.resolver.DefaultTraversableResolver;
import org.apache.bval.jsr.util.IOs;
import org.apache.bval.jsr.valueextraction.ValueExtractors;
import org.apache.bval.jsr.xml.ValidationParser;
import org.apache.bval.util.CloseableAble;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.commons.weaver.privilizer.Privileged;

/**
 * Description: used to configure apache-validation for jsr.
 * Implementation of Configuration that also implements ConfigurationState,
 * hence this can be passed to buildValidatorFactory(ConfigurationState).
 * <br/>
 */
public class ConfigurationImpl implements ApacheValidatorConfiguration, ConfigurationState, CloseableAble {

    private class LazyParticipant<T> extends Lazy<T> {
        private boolean locked;

        private LazyParticipant(Supplier<T> init) {
            super(init);
        }

        ConfigurationImpl override(T value) {
            if (value != null) {
                synchronized (this) {
                    if (!locked) {
                        try {
                            reset(() -> value);
                        } finally {
                            ConfigurationImpl.this.prepared = false;
                        }
                    }
                }
            }
            return ConfigurationImpl.this;
        }

        synchronized ConfigurationImpl externalOverride(T value) {
            locked = false;
            try {
                return override(value);
            } finally {
                locked = true;
            }
        }
    }

    /**
     * Configured {@link ValidationProvider}
     */
    //couldn't this be parameterized <ApacheValidatorConfiguration> or <? super ApacheValidatorConfiguration>?
    private final ValidationProvider<ApacheValidatorConfiguration> provider;

    /**
     * Configured {@link ValidationProviderResolver}
     */
    private final ValidationProviderResolver providerResolver;

    /**
     * Configured {@link ValidationProvider} class
     */
    private Class<? extends ValidationProvider<?>> providerClass;

    private final MessageInterpolator defaultMessageInterpolator = new DefaultMessageInterpolator();

    private final LazyParticipant<MessageInterpolator> messageInterpolator =
        new LazyParticipant<>(this::getDefaultMessageInterpolator);

    private final ConstraintValidatorFactory defaultConstraintValidatorFactory =
        new DefaultConstraintValidatorFactory();

    private final LazyParticipant<ConstraintValidatorFactory> constraintValidatorFactory =
        new LazyParticipant<>(this::getDefaultConstraintValidatorFactory);

    private final TraversableResolver defaultTraversableResolver = new DefaultTraversableResolver();

    private final LazyParticipant<TraversableResolver> traversableResolver =
        new LazyParticipant<>(this::getDefaultTraversableResolver);

    private final ParameterNameProvider defaultParameterNameProvider = new DefaultParameterNameProvider();

    private final LazyParticipant<ParameterNameProvider> parameterNameProvider =
        new LazyParticipant<>(this::getDefaultParameterNameProvider);

    private final ClockProvider defaultClockProvider = Clock::systemDefaultZone;

    private final LazyParticipant<ClockProvider> clockProvider = new LazyParticipant<>(this::getDefaultClockProvider);

    private final ValueExtractors bootstrapValueExtractors = new ValueExtractors();
    private final ValueExtractors valueExtractors = bootstrapValueExtractors.createChild();

    private final Lazy<BootstrapConfiguration> bootstrapConfiguration = new Lazy<>(this::createBootstrapConfiguration);

    private final Set<InputStream> mappingStreams = new HashSet<>();
    private final Map<String, String> properties = new HashMap<>();

    private boolean beforeCdi = false;
    private ClassLoader loader;

    // BEGIN DEFAULTS
    /**
     * false = dirty flag (to prevent from multiple parsing validation.xml)
     */
    private boolean prepared = false;
    // END DEFAULTS

    private boolean ignoreXmlConfiguration = false;

    private ParticipantFactory participantFactory;

    /**
     * Create a new ConfigurationImpl instance.
     * @param aState bootstrap state
     * @param aProvider provider
     */
    public ConfigurationImpl(BootstrapState aState, ValidationProvider<ApacheValidatorConfiguration> aProvider) {
        Exceptions.raiseIf(aProvider == null && aState == null, ValidationException::new,
            "one of provider or state is required");

        if (aProvider == null) {
            this.provider = null;
            if (aState.getValidationProviderResolver() == null) {
                providerResolver = aState.getDefaultValidationProviderResolver();
            } else {
                providerResolver = aState.getValidationProviderResolver();
            }
        } else {
            this.provider = aProvider;
            this.providerResolver = null;
        }
        initializePropertyDefaults();
    }

    /**
     * {@inheritDoc}
     * Ignore data from the <i>META-INF/validation.xml</i> file if this
     * method is called.
     *
     * @return this
     */
    @Override
    public ApacheValidatorConfiguration ignoreXmlConfiguration() {
        ignoreXmlConfiguration = true;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationImpl messageInterpolator(MessageInterpolator resolver) {
        return messageInterpolator.externalOverride(resolver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApacheValidatorConfiguration traversableResolver(TraversableResolver resolver) {
        return traversableResolver.externalOverride(resolver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationImpl constraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
        return this.constraintValidatorFactory.externalOverride(constraintValidatorFactory);
    }

    @Override
    public ApacheValidatorConfiguration parameterNameProvider(ParameterNameProvider parameterNameProvider) {
        return this.parameterNameProvider.externalOverride(parameterNameProvider);
    }

    @Override
    public ApacheValidatorConfiguration clockProvider(ClockProvider clockProvider) {
        return this.clockProvider.externalOverride(clockProvider);
    }

    /**
     * {@inheritDoc}
     * Add a stream describing constraint mapping in the Bean Validation
     * XML format.
     *
     * @return this
     */
    @Override
    public ApacheValidatorConfiguration addMapping(InputStream stream) {
        if (stream != null) {
            mappingStreams.add(IOs.convertToMarkableInputStream(stream));
        }
        return this;
    }

    /**
     * {@inheritDoc}
     * Add a provider specific property. This property is equivalent to
     * XML configuration properties.
     * If we do not know how to handle the property, we silently ignore it.
     *
     * @return this
     */
    @Override
    public ApacheValidatorConfiguration addProperty(String name, String value) {
        properties.put(name, value);
        return this;
    }

    @Override
    public MessageInterpolator getDefaultMessageInterpolator() {
        return defaultMessageInterpolator;
    }

    @Override
    public TraversableResolver getDefaultTraversableResolver() {
        return defaultTraversableResolver;
    }

    @Override
    public ConstraintValidatorFactory getDefaultConstraintValidatorFactory() {
        return defaultConstraintValidatorFactory;
    }

    @Override
    public ParameterNameProvider getDefaultParameterNameProvider() {
        return defaultParameterNameProvider;
    }

    @Override
    public ClockProvider getDefaultClockProvider() {
        return defaultClockProvider;
    }

    /**
     * {@inheritDoc}
     * Return a map of non type-safe custom properties.
     *
     * @return null
     */
    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * {@inheritDoc}
     * Returns true if Configuration.ignoreXMLConfiguration() has been called.
     * In this case, we ignore META-INF/validation.xml
     *
     * @return true
     */
    @Override
    public boolean isIgnoreXmlConfiguration() {
        return ignoreXmlConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<InputStream> getMappingStreams() {
        return mappingStreams;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageInterpolator getMessageInterpolator() {
        return messageInterpolator.get();
    }

    @Override
    public BootstrapConfiguration getBootstrapConfiguration() {
        return bootstrapConfiguration.get();
    }

    /**
     * {@inheritDoc}
     * main factory method to build a ValidatorFactory
     *
     * @throws ValidationException if the ValidatorFactory cannot be built
     */
    @Override
    public ValidatorFactory buildValidatorFactory() {
        return doBuildValidatorFactory();
    }

    /**
     * {@inheritDoc}
     * @return the constraint validator factory of this configuration.
     */
    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return constraintValidatorFactory.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TraversableResolver getTraversableResolver() {
        return traversableResolver.get();
    }

    @Override
    public ParameterNameProvider getParameterNameProvider() {
        return parameterNameProvider.get();
    }

    @Override
    public ClockProvider getClockProvider() {
        return clockProvider.get();
    }

    @Override
    public ApacheValidatorConfiguration addValueExtractor(ValueExtractor<?> extractor) {
        valueExtractors.add(extractor);
        return this;
    }

    @Override
    public Set<ValueExtractor<?>> getValueExtractors() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(valueExtractors.getLocalValueExtractors().values()));
    }

    public void deferBootstrapOverrides() {
        beforeCdi = true;
    }

    public void releaseDeferredBootstrapOverrides() {
        if (beforeCdi) {
            beforeCdi = false;
            performBootstrapOverrides();
        }
    }

    @Override
    public Closeable getCloseable() {
        if (participantFactory == null) {
            return () -> {
            };
        }
        return participantFactory;
    }

    @Privileged
    private ValidatorFactory doBuildValidatorFactory() {
        prepare();
        return Optional.<ValidationProvider<?>> ofNullable(provider).orElseGet(this::findProvider)
            .buildValidatorFactory(this);
    }

    private void prepare() {
        if (!prepared) {
            applyBootstrapConfiguration();
            prepared = true;
        }
    }

    private BootstrapConfiguration createBootstrapConfiguration() {
        try {
            if (!ignoreXmlConfiguration) {
                loader = ValidationParser.class.getClassLoader();
                final BootstrapConfiguration xmlBootstrap =
                    ValidationParser.processValidationConfig(getProperties().get(Properties.VALIDATION_XML_PATH), this);
                if (xmlBootstrap != null) {
                    return xmlBootstrap;
                }
            }
            loader = ApacheValidatorFactory.class.getClassLoader();
            return BootstrapConfigurationImpl.DEFAULT;
        } finally {
            participantFactory = new ParticipantFactory(loader);
        }
    }

    private void applyBootstrapConfiguration() {
        final BootstrapConfiguration bootstrapConfig = bootstrapConfiguration.get();

        if (bootstrapConfig.getDefaultProviderClassName() != null) {
            this.providerClass = loadClass(bootstrapConfig.getDefaultProviderClassName());
        }
        bootstrapConfig.getProperties().forEach(this::addProperty);
        bootstrapConfig.getConstraintMappingResourcePaths().stream().map(ValidationParser::open)
            .forEach(this::addMapping);

        if (!beforeCdi) {
            performBootstrapOverrides();
        }
    }

    private void performBootstrapOverrides() {
        final BootstrapConfiguration bootstrapConfig = bootstrapConfiguration.get();
        override(messageInterpolator, bootstrapConfig::getMessageInterpolatorClassName);
        override(traversableResolver, bootstrapConfig::getTraversableResolverClassName);
        override(constraintValidatorFactory, bootstrapConfig::getConstraintValidatorFactoryClassName);
        override(parameterNameProvider, bootstrapConfig::getParameterNameProviderClassName);
        override(clockProvider, bootstrapConfig::getClockProviderClassName);

        bootstrapConfig.getValueExtractorClassNames().stream().<ValueExtractor<?>> map(participantFactory::create)
            .forEach(bootstrapValueExtractors::add);
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> loadClass(final String className) {
        try {
            return (Class<T>) Class.forName(className, true, loader);
        } catch (final ClassNotFoundException ex) {
            throw new ValidationException(ex);
        }
    }

    private void initializePropertyDefaults() {
        properties.put(Properties.CONSTRAINTS_CACHE_SIZE, Integer.toString(50));
    }

    private ValidationProvider<?> findProvider() {
        if (providerClass == null) {
            return providerResolver.getValidationProviders().get(0);
        }
        final Optional<ValidationProvider<?>> knownProvider =
            providerResolver.getValidationProviders().stream().filter(providerClass::isInstance).findFirst();
        if (knownProvider.isPresent()) {
            return knownProvider.get();
        }
        try {
            return providerClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw Exceptions.create(ValidationException::new, "Unable to find/create %s of type %s",
                ValidationProvider.class.getSimpleName(), providerClass);
        }
    }

    private <T> void override(LazyParticipant<T> participant, Supplier<String> getClassName) {
        Optional.ofNullable(getClassName.get()).<T> map(participantFactory::create).ifPresent(participant::override);
    }
}
