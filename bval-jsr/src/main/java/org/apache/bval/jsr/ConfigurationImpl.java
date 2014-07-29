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

import org.apache.bval.cdi.BValExtension;
import org.apache.bval.jsr.parameter.DefaultParameterNameProvider;
import org.apache.bval.jsr.resolver.DefaultTraversableResolver;
import org.apache.bval.jsr.util.IOs;
import org.apache.bval.jsr.xml.ValidationParser;

import javax.validation.BootstrapConfiguration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableType;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ConfigurationState;
import javax.validation.spi.ValidationProvider;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Description: used to configure apache-validation for jsr.
 * Implementation of Configuration that also implements ConfigurationState,
 * hence this can be passed to buildValidatorFactory(ConfigurationState).
 * <br/>
 */
public class ConfigurationImpl implements ApacheValidatorConfiguration, ConfigurationState {
    /**
     * Configured {@link ValidationProvider}
     */
    //couldn't this be parameterized <ApacheValidatorConfiguration> or <? super ApacheValidatorConfiguration>?
    protected final ValidationProvider<?> provider;

    /**
     * Configured {@link ValidationProviderResolver}
     */
    protected final ValidationProviderResolver providerResolver;

    /**
     * Configured {@link ValidationProvider} class
     */
    protected Class<? extends ValidationProvider<?>> providerClass;

    /**
     * Configured {@link MessageInterpolator}
     */
    protected MessageInterpolator defaultMessageInterpolator = new DefaultMessageInterpolator();
    protected MessageInterpolator messageInterpolator = defaultMessageInterpolator;
    protected Class<? extends MessageInterpolator> messageInterpolatorClass = null;

    /**
     * Configured {@link ConstraintValidatorFactory}
     */
    protected ConstraintValidatorFactory defaultConstraintValidatorFactory = new DefaultConstraintValidatorFactory();
    protected ConstraintValidatorFactory constraintValidatorFactory = defaultConstraintValidatorFactory;
    protected Class<? extends ConstraintValidatorFactory> constraintValidatorFactoryClass = null;

    protected TraversableResolver defaultTraversableResolver = new DefaultTraversableResolver();
    protected TraversableResolver traversableResolver = defaultTraversableResolver;
    protected Class<? extends TraversableResolver> traversableResolverClass = null;

    protected ParameterNameProvider defaultParameterNameProvider = new DefaultParameterNameProvider();
    protected ParameterNameProvider parameterNameProvider = defaultParameterNameProvider;
    protected Class<? extends ParameterNameProvider> parameterNameProviderClass = null;

    protected BootstrapConfiguration  bootstrapConfiguration;

    protected Collection<ExecutableType> executableValidation;

    private Collection<BValExtension.Releasable> releasables = new CopyOnWriteArrayList<BValExtension.Releasable>();

    private boolean beforeCdi = false;

    // BEGIN DEFAULTS
    /**
     * false = dirty flag (to prevent from multiple parsing validation.xml)
     */
    private boolean prepared = false;
    // END DEFAULTS

    private Set<InputStream> mappingStreams = new HashSet<InputStream>();
    private Map<String, String> properties = new HashMap<String, String>();
    private boolean ignoreXmlConfiguration = false;

    private volatile ValidationParser parser;

    /**
     * Create a new ConfigurationImpl instance.
     * @param aState bootstrap state
     * @param aProvider provider
     */
    public ConfigurationImpl(BootstrapState aState, ValidationProvider<?> aProvider) {
        if (aProvider != null) {
            this.provider = aProvider;
            this.providerResolver = null;
        } else if (aState != null) {
            this.provider = null;
            if (aState.getValidationProviderResolver() == null) {
                providerResolver = aState.getDefaultValidationProviderResolver();
            } else {
                providerResolver = aState.getValidationProviderResolver();
            }
        } else {
            throw new ValidationException("either provider or state are required");
        }
    }

    /**
     * {@inheritDoc}
     */
    public ApacheValidatorConfiguration traversableResolver(TraversableResolver resolver) {
        if (resolver == null) {
            return this;
        }

        this.traversableResolverClass = null;
        this.traversableResolver = resolver;
        this.prepared = false;
        return this;
    }

    /**
     * {@inheritDoc}
     * Ignore data from the <i>META-INF/validation.xml</i> file if this
     * method is called.
     *
     * @return this
     */
    public ApacheValidatorConfiguration ignoreXmlConfiguration() {
        ignoreXmlConfiguration = true;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurationImpl messageInterpolator(MessageInterpolator resolver) {
        if (resolver == null) {
            return this;
        }

        this.messageInterpolatorClass = null;
        this.messageInterpolator = resolver;
        this.prepared = false;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurationImpl constraintValidatorFactory(
          ConstraintValidatorFactory constraintFactory) {
        if (constraintFactory == null) {
            return this;
        }

        this.constraintValidatorFactoryClass = null;
        this.constraintValidatorFactory = constraintFactory;
        this.prepared = false;
        return this;
    }

    public ApacheValidatorConfiguration parameterNameProvider(ParameterNameProvider parameterNameProvider) {
        if (parameterNameProvider == null) {
            return this;
        }
        this.parameterNameProviderClass = null;
        this.parameterNameProvider = parameterNameProvider;
        return this;
    }

    /**
     * {@inheritDoc}
     * Add a stream describing constraint mapping in the Bean Validation
     * XML format.
     *
     * @return this
     */
    public ApacheValidatorConfiguration addMapping(InputStream stream) {
        if (stream == null) {
            return this;
        }
        mappingStreams.add(IOs.convertToMarkableInputStream(stream));
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
    public ApacheValidatorConfiguration addProperty(String name, String value) {
        if ("bval.before.cdi".equals(name)) {
            beforeCdi = Boolean.parseBoolean(value);
        } else {
            properties.put(name, value);
        }
        return this;
    }

    public MessageInterpolator getDefaultMessageInterpolator() {
        return defaultMessageInterpolator;
    }

    public TraversableResolver getDefaultTraversableResolver() {
        return defaultTraversableResolver;
    }

    public ConstraintValidatorFactory getDefaultConstraintValidatorFactory() {
        return defaultConstraintValidatorFactory;
    }

    public ParameterNameProvider getDefaultParameterNameProvider() {
        return defaultParameterNameProvider;
    }

    /**
     * {@inheritDoc}
     * Return a map of non type-safe custom properties.
     *
     * @return null
     */
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
    public boolean isIgnoreXmlConfiguration() {
        return ignoreXmlConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    public Set<InputStream> getMappingStreams() {
        return mappingStreams;
    }

    /**
     * {@inheritDoc}
     */
    public MessageInterpolator getMessageInterpolator() {
        if (beforeCdi) {
            return defaultMessageInterpolator;
        }

        if (messageInterpolator == defaultMessageInterpolator && messageInterpolatorClass != null) {
            synchronized (this) {
                if (messageInterpolator == defaultMessageInterpolator && messageInterpolatorClass != null) {
                    messageInterpolator = newInstance(messageInterpolatorClass);
                }
            }
        }
        return messageInterpolator;
    }

    public BootstrapConfiguration getBootstrapConfiguration() {
        return createBootstrapConfiguration();
    }

    /**
     * {@inheritDoc}
     * main factory method to build a ValidatorFactory
     *
     * @throws ValidationException if the ValidatorFactory cannot be built
     */
    public ValidatorFactory buildValidatorFactory() {
        if (System.getSecurityManager() == null) {
            return doPrivBuildValidatorFactory(this);
        }
        return AccessController.doPrivileged(new PrivilegedAction<ValidatorFactory>() {
            public ValidatorFactory run() {
                return doPrivBuildValidatorFactory(ConfigurationImpl.this);
            }
        });
    }

    public ValidatorFactory doPrivBuildValidatorFactory(final ConfigurationImpl impl) {
        prepare();
        parser.ensureValidatorFactoryCanBeBuilt();
        if (provider != null) {
            return provider.buildValidatorFactory(impl);
        } else {
            return findProvider().buildValidatorFactory(impl);
        }
    }

    public ConfigurationImpl prepare() {
        if (prepared) {
            return this;
        }

        createBootstrapConfiguration();
        parser.applyConfigWithInstantiation(this); // instantiate the config if needed

        prepared = true;
        return this;
    }

    private BootstrapConfiguration createBootstrapConfiguration() {
        if (parser == null) {
            parser = parseValidationXml(); // already done if BootstrapConfiguration already looked up
            bootstrapConfiguration = parser.getBootstrap();
        }
        return bootstrapConfiguration;
    }

    /** Check whether a validation.xml file exists and parses it with JAXB */
    private ValidationParser parseValidationXml() {
        return ValidationParser.processValidationConfig(getProperties().get(Properties.VALIDATION_XML_PATH), this, ignoreXmlConfiguration);
    }

    /**
     * {@inheritDoc}
     * @return the constraint validator factory of this configuration.
     */
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        if (beforeCdi) {
            return constraintValidatorFactory;
        }

        if (constraintValidatorFactory == defaultConstraintValidatorFactory && constraintValidatorFactoryClass != null) {
            synchronized (this) {
                if (constraintValidatorFactory == defaultConstraintValidatorFactory && constraintValidatorFactoryClass != null) {
                    constraintValidatorFactory = newInstance(constraintValidatorFactoryClass);
                }
            }
        }
        return constraintValidatorFactory;
    }

    /**
     * {@inheritDoc}
     */
    public TraversableResolver getTraversableResolver() {
        if (beforeCdi) {
            return defaultTraversableResolver;
        }

        if (traversableResolver == defaultTraversableResolver && traversableResolverClass != null) {
            synchronized (this) {
                if (traversableResolver == defaultTraversableResolver && traversableResolverClass != null) {
                    traversableResolver = newInstance(traversableResolverClass);
                }
            }
        }
        return traversableResolver;
    }

    public ParameterNameProvider getParameterNameProvider() {
        if (beforeCdi) {
            return defaultParameterNameProvider;
        }

        if (parameterNameProvider == defaultParameterNameProvider && parameterNameProviderClass != null) {
            synchronized (this) {
                if (parameterNameProvider == defaultParameterNameProvider && parameterNameProviderClass != null) {
                    parameterNameProvider = newInstance(parameterNameProviderClass);
                }
            }
        }
        return parameterNameProvider;
    }

    /**
     * Get the configured {@link ValidationProvider}.
     * @return {@link ValidationProvider}
     */
    public ValidationProvider<?> getProvider() {
        return provider;
    }

    private ValidationProvider<?> findProvider() {
        if (providerClass != null) {
            for (ValidationProvider<?> provider : providerResolver
                  .getValidationProviders()) {
                if (providerClass.isAssignableFrom(provider.getClass())) {
                    return provider;
                }
            }
            throw new ValidationException(
                  "Unable to find suitable provider: " + providerClass);
        } else {
            List<ValidationProvider<?>> providers = providerResolver.getValidationProviders();
            return providers.get(0);
        }
    }

    /**
     * Set {@link ValidationProvider} class.
     * @param providerClass the provider type
     */
    public void setProviderClass(Class<? extends ValidationProvider<?>> providerClass) {
        this.providerClass = providerClass;
    }

    public void setExecutableValidation(final Collection<ExecutableType> executableValidation) {
        this.executableValidation = executableValidation;
    }

    public Collection<ExecutableType> getExecutableValidation() {
        return executableValidation;
    }

    private String executableValidationTypesAsString() {
        if (executableValidation == null || executableValidation.isEmpty()) {
            return "";
        }

        final StringBuilder builder = new StringBuilder();
        for (final ExecutableType type : executableValidation) {
            builder.append(type.name()).append(",");
        }
        final String s = builder.toString();
        return s.substring(0, s.length() - 1);
    }

    public Closeable getClosable() {
        return new Closeable() {
            public void close() throws IOException {
                for (final BValExtension.Releasable<?> releasable : releasables) {
                    releasable.release();
                }
                releasables.clear();
            }
        };
    }

    private <T> T newInstance(final Class<T> cls) {
        if (System.getSecurityManager() == null) {
            return createInstance(cls);
        }
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            public T run() {
                return createInstance(cls);
            }
        });
    }

    private <T> T createInstance(final Class<T> cls) {
        try {
            final BValExtension.Releasable<T> releasable = BValExtension.inject(cls);
            releasables.add(releasable);
            return releasable.getInstance();
        } catch (final Exception e) {
            try {
                return cls.newInstance();
            } catch (final InstantiationException e1) {
                throw new ValidationException(e1.getMessage(), e1);
            } catch (final IllegalAccessException e1) {
                throw new ValidationException(e1.getMessage(), e1);
            }
        } catch (final NoClassDefFoundError error) {
            try {
                return cls.newInstance();
            } catch (final InstantiationException e1) {
                throw new ValidationException(e1.getMessage(), e1);
            } catch (final IllegalAccessException e1) {
                throw new ValidationException(e1.getMessage(), e1);
            }
        }
    }

    public void traversableResolverClass(final Class<TraversableResolver> clazz) {
        traversableResolverClass = clazz;
    }

    public void constraintValidatorFactoryClass(final Class<ConstraintValidatorFactory> clazz) {
        constraintValidatorFactoryClass = clazz;
    }

    public void messageInterpolatorClass(final Class<MessageInterpolator> clazz) {
        messageInterpolatorClass = clazz;
    }

    public void parameterNameProviderClass(final Class<? extends ParameterNameProvider> clazz) {
        parameterNameProviderClass = clazz;
    }
}
