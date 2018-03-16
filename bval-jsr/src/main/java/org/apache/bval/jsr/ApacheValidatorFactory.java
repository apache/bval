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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.validation.ClockProvider;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ConfigurationState;

import org.apache.bval.jsr.descriptor.DescriptorManager;
import org.apache.bval.jsr.metadata.MetadataBuilders;
import org.apache.bval.jsr.util.AnnotationsManager;
import org.apache.bval.jsr.valueextraction.ValueExtractors;
import org.apache.bval.jsr.xml.ValidationMappingParser;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Description: a factory is a complete configurated object that can create
 * validators.<br/>
 * This instance is not thread-safe.<br/>
 */
@Privilizing(@CallTo(Reflection.class))
public class ApacheValidatorFactory implements ValidatorFactory, Cloneable {

    private static volatile ApacheValidatorFactory DEFAULT_FACTORY;

    private MessageInterpolator messageResolver;
    private TraversableResolver traversableResolver;
    private ConstraintValidatorFactory constraintValidatorFactory;
    private ParameterNameProvider parameterNameProvider;
    private ClockProvider clockProvider;
    private final Map<String, String> properties;
    private final AnnotationsManager annotationsManager;
    private final DescriptorManager descriptorManager = new DescriptorManager(this);
    private final MetadataBuilders metadataBuilders = new MetadataBuilders();
    private final ValueExtractors valueExtractors = new ValueExtractors();
    private final ConstraintCached constraintsCache = new ConstraintCached();
    private final Collection<Closeable> toClose = new ArrayList<>();

    /**
     * Convenience method to retrieve a default global ApacheValidatorFactory
     *
     * @return {@link ApacheValidatorFactory}
     */
    public static ApacheValidatorFactory getDefault() {
        if (DEFAULT_FACTORY == null) {
            synchronized (ApacheValidatorFactory.class) {
                if (DEFAULT_FACTORY == null) {
                    DEFAULT_FACTORY = Validation.byProvider(ApacheValidationProvider.class).configure()
                        .buildValidatorFactory().unwrap(ApacheValidatorFactory.class);
                }
            }
        }
        return DEFAULT_FACTORY;
    }

    /**
     * Set a particular {@link ApacheValidatorFactory} instance as the default.
     *
     * @param aDefaultFactory
     */
    public static void setDefault(ApacheValidatorFactory aDefaultFactory) {
        DEFAULT_FACTORY = aDefaultFactory;
    }

    /**
     * Create a new ApacheValidatorFactory instance.
     */
    public ApacheValidatorFactory(ConfigurationState configuration) {
        properties = new HashMap<>(configuration.getProperties());
        parameterNameProvider = configuration.getParameterNameProvider();
        messageResolver = configuration.getMessageInterpolator();
        traversableResolver = configuration.getTraversableResolver();
        constraintValidatorFactory = configuration.getConstraintValidatorFactory();
        clockProvider = configuration.getClockProvider();

        if (ConfigurationImpl.class.isInstance(configuration)) {
            toClose.add(ConfigurationImpl.class.cast(configuration).getClosable());
        }
        configuration.getValueExtractors().forEach(valueExtractors::add);

        annotationsManager = new AnnotationsManager(this);
        loadAndVerifyUserCustomizations(configuration);
    }

    /**
     * Get the property map of this {@link ApacheValidatorFactory}.
     *
     * @return Map<String, String>
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Shortcut method to create a new Validator instance with factory's settings
     *
     * @return the new validator instance
     */
    @Override
    public Validator getValidator() {
        return usingContext().getValidator();
    }

    /**
     * {@inheritDoc}
     *
     * @return the validator factory's context
     */
    @Override
    public ApacheFactoryContext usingContext() {
        return new ApacheFactoryContext(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized ApacheValidatorFactory clone() {
        try {
            return (ApacheValidatorFactory) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(); // VM bug.
        }
    }

    /**
     * Set the {@link MessageInterpolator} used.
     *
     * @param messageResolver
     */
    public final void setMessageInterpolator(MessageInterpolator messageResolver) {
        if (messageResolver != null) {
            this.messageResolver = messageResolver;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageInterpolator getMessageInterpolator() {
        return messageResolver;
    }

    /**
     * Set the {@link TraversableResolver} used.
     *
     * @param traversableResolver
     */
    public final void setTraversableResolver(TraversableResolver traversableResolver) {
        if (traversableResolver != null) {
            this.traversableResolver = traversableResolver;
        }
    }

    public void setParameterNameProvider(final ParameterNameProvider parameterNameProvider) {
        if (parameterNameProvider != null) {
            this.parameterNameProvider = parameterNameProvider;
        }
    }

    public void setClockProvider(final ClockProvider clockProvider) {
        if (clockProvider != null) {
            this.clockProvider = clockProvider;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TraversableResolver getTraversableResolver() {
        return traversableResolver;
    }

    /**
     * Set the {@link ConstraintValidatorFactory} used.
     *
     * @param constraintValidatorFactory
     */
    public final void setConstraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
        if (constraintValidatorFactory != null) {
            this.constraintValidatorFactory = constraintValidatorFactory;
            if (DefaultConstraintValidatorFactory.class.isInstance(constraintValidatorFactory)) {
                toClose.add(Closeable.class.cast(constraintValidatorFactory));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return constraintValidatorFactory;
    }

    @Override
    public ParameterNameProvider getParameterNameProvider() {
        return parameterNameProvider;
    }

    @Override
    public ClockProvider getClockProvider() {
        return clockProvider;
    }

    @Override
    public void close() {
        try {
            for (final Closeable c : toClose) {
                c.close();
            }
            toClose.clear();
        } catch (final Exception e) {
            // no-op
        }
    }

    /**
     * Return an object of the specified type to allow access to the provider-specific API. If the Bean Validation
     * provider implementation does not support the specified class, the ValidationException is thrown.
     *
     * @param type
     *            the class of the object to be returned.
     * @return an instance of the specified class
     * @throws ValidationException
     *             if the provider does not support the call.
     */
    @Override
    public <T> T unwrap(final Class<T> type) {
        if (type.isInstance(this)) {
            @SuppressWarnings("unchecked")
            final T result = (T) this;
            return result;
        }

        // FIXME 2011-03-27 jw:
        // This code is unsecure.
        // It should allow only a fixed set of classes.
        // Can't fix this because don't know which classes this method should support.

        if (!(type.isInterface() || Modifier.isAbstract(type.getModifiers()))) {
            return newInstance(type);
        }
        try {
            final Class<?> cls = Reflection.toClass(type.getName() + "Impl");
            if (type.isAssignableFrom(cls)) {
                @SuppressWarnings("unchecked")
                T result = (T) newInstance(cls);
                return result;
            }
        } catch (ClassNotFoundException e) {
            // do nothing
        }
        throw new ValidationException("Type " + type + " not supported");
    }

    private <T> T newInstance(final Class<T> cls) {
        try {
            return Reflection.newInstance(cls);
        } catch (final RuntimeException e) {
            throw new ValidationException(e.getCause());
        }
    }

    /**
     * Get the constraint cache used.
     *
     * @return {@link ConstraintCached}
     */
    public ConstraintCached getConstraintsCache() {
        return constraintsCache;
    }

    /**
     * Get the {@link AnnotationsManager}.
     * 
     * @return {@link AnnotationsManager}
     */
    public AnnotationsManager getAnnotationsManager() {
        return annotationsManager;
    }

    /**
     * Get the {@link DescriptorManager}.
     * 
     * @return {@link DescriptorManager}
     */
    public DescriptorManager getDescriptorManager() {
        return descriptorManager;
    }

    /**
     * Get the {@link ValueExtractors}.
     * 
     * @return {@link ValueExtractors}
     */
    public ValueExtractors getValueExtractors() {
        return valueExtractors;
    }

    public MetadataBuilders getMetadataBuilders() {
        return metadataBuilders;
    }

    private void loadAndVerifyUserCustomizations(ConfigurationState configuration) {
        //TODO introduce service interface
        new ValidationMappingParser(this).processMappingConfig(configuration.getMappingStreams());

        getMetadataBuilders().getCustomizedTypes().forEach(getDescriptorManager()::getBeanDescriptor);
    }
}
