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

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.bootstrap.ProviderSpecificBootstrap;
import javax.validation.spi.ConfigurationState;

import org.apache.bval.jsr303.util.SecureActions;
import org.apache.bval.jsr303.xml.AnnotationIgnores;
import org.apache.bval.jsr303.xml.MetaConstraint;
import org.apache.bval.jsr303.xml.ValidationMappingParser;
import org.apache.bval.util.AccessStrategy;
import org.apache.commons.lang.ClassUtils;

/**
 * Description: a factory is a complete configurated object that can create
 * validators.<br/>
 * This instance is not thread-safe.<br/>
 */
public class ApacheValidatorFactory implements ValidatorFactory, Cloneable {
    private static volatile ApacheValidatorFactory DEFAULT_FACTORY;
    private static final ConstraintDefaults defaultConstraints =
        new ConstraintDefaults();

    private MessageInterpolator messageResolver;
    private TraversableResolver traversableResolver;
    private ConstraintValidatorFactory constraintValidatorFactory;
    private final Map<String, String> properties;

    /** information from xml parsing */
    private final AnnotationIgnores annotationIgnores = new AnnotationIgnores();
    private final ConstraintCached constraintsCache = new ConstraintCached();
    private final Map<Class<?>, Class<?>[]> defaultSequences;
    /**
     * access strategies for properties with cascade validation @Valid support
     */
    private final Map<Class<?>, List<AccessStrategy>> validAccesses;
    private final Map<Class<?>, List<MetaConstraint<?, ? extends Annotation>>> constraintMap;

    /**
     * Convenience method to retrieve a default global ApacheValidatorFactory
     * 
     * @return {@link ApacheValidatorFactory}
     */
    public static synchronized ApacheValidatorFactory getDefault() {
        if (DEFAULT_FACTORY == null) {
            ProviderSpecificBootstrap<ApacheValidatorConfiguration> provider =
                Validation.byProvider(ApacheValidationProvider.class);
            ApacheValidatorConfiguration configuration = provider.configure();
            DEFAULT_FACTORY =
                (ApacheValidatorFactory) configuration.buildValidatorFactory();
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
    public ApacheValidatorFactory(ConfigurationState configurationState) {
        properties = new HashMap<String, String>();
        defaultSequences = new HashMap<Class<?>, Class<?>[]>();
        validAccesses = new HashMap<Class<?>, List<AccessStrategy>>();
        constraintMap =
            new HashMap<Class<?>, List<MetaConstraint<?, ? extends Annotation>>>();
        configure(configurationState);
    }

    /**
     * Configure this {@link ApacheValidatorFactory} from a
     * {@link ConfigurationState}.
     * 
     * @param configuration
     */
    protected void configure(ConfigurationState configuration) {
        getProperties().putAll(configuration.getProperties());
        setMessageInterpolator(configuration.getMessageInterpolator());
        setTraversableResolver(configuration.getTraversableResolver());
        setConstraintValidatorFactory(configuration
            .getConstraintValidatorFactory());
        ValidationMappingParser parser = new ValidationMappingParser(this);
        parser.processMappingConfig(configuration.getMappingStreams());
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
     * Get the default {@link MessageInterpolator} used by this
     * {@link ApacheValidatorFactory}.
     * 
     * @return {@link MessageInterpolator}
     */
    protected MessageInterpolator getDefaultMessageInterpolator() {
        return messageResolver;
    }

    /**
     * Shortcut method to create a new Validator instance with factory's
     * settings
     * 
     * @return the new validator instance
     */
    public Validator getValidator() {
        return usingContext().getValidator();
    }

    /**
     * {@inheritDoc}
     * 
     * @return the validator factory's context
     */
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
        this.messageResolver = messageResolver;
    }

    /**
     * {@inheritDoc}
     */
    public MessageInterpolator getMessageInterpolator() {
        return ((messageResolver != null) ? messageResolver
            : getDefaultMessageInterpolator());
    }

    /**
     * Set the {@link TraversableResolver} used.
     * 
     * @param traversableResolver
     */
    public final void setTraversableResolver(
        TraversableResolver traversableResolver) {
        this.traversableResolver = traversableResolver;
    }

    /**
     * {@inheritDoc}
     */
    public TraversableResolver getTraversableResolver() {
        return traversableResolver;
    }

    /**
     * Set the {@link ConstraintValidatorFactory} used.
     * 
     * @param constraintValidatorFactory
     */
    public final void setConstraintValidatorFactory(
        ConstraintValidatorFactory constraintValidatorFactory) {
        this.constraintValidatorFactory = constraintValidatorFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return constraintValidatorFactory;
    }

    /**
     * Return an object of the specified type to allow access to the
     * provider-specific API. If the Bean Validation provider implementation
     * does not support the specified class, the ValidationException is thrown.
     * 
     * @param type
     *            the class of the object to be returned.
     * @return an instance of the specified class
     * @throws ValidationException
     *             if the provider does not support the call.
     */
    public <T> T unwrap(Class<T> type) {
        if (type.isInstance(this)) {
            @SuppressWarnings("unchecked")
            final T result = (T) this;
            return result;
        } else if (!(type.isInterface() || Modifier.isAbstract(type
            .getModifiers()))) {
            return SecureActions.newInstance(type);
        } else {
            try {
                Class<?> cls = ClassUtils.getClass(type.getName() + "Impl");
                if (type.isAssignableFrom(cls)) {
                    @SuppressWarnings("unchecked")
                    final Class<? extends T> implClass =
                        (Class<? extends T>) cls;
                    return SecureActions.newInstance(implClass);
                }
            } catch (ClassNotFoundException e) {
            }
            throw new ValidationException("Type " + type + " not supported");
        }
    }

    /**
     * Get the detected {@link ConstraintDefaults}.
     * 
     * @return ConstraintDefaults
     */
    public ConstraintDefaults getDefaultConstraints() {
        return defaultConstraints;
    }

    /**
     * Get the detected {@link AnnotationIgnores}.
     * 
     * @return AnnotationIgnores
     */
    public AnnotationIgnores getAnnotationIgnores() {
        return annotationIgnores;
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
     * Add a meta-constraint to this {@link ApacheValidatorFactory}'s runtime
     * customizations.
     * 
     * @param beanClass
     * @param metaConstraint
     */
    public void addMetaConstraint(Class<?> beanClass,
        MetaConstraint<?, ?> metaConstraint) {
        List<MetaConstraint<?, ? extends Annotation>> slot =
            constraintMap.get(beanClass);
        if (slot != null) {
            slot.add(metaConstraint);
        } else {
            List<MetaConstraint<?, ? extends Annotation>> constraintList =
                new ArrayList<MetaConstraint<?, ? extends Annotation>>();
            constraintList.add(metaConstraint);
            constraintMap.put(beanClass, constraintList);
        }
    }

    /**
     * Mark a property of <code>beanClass</code> for nested validation.
     * 
     * @param beanClass
     * @param accessStrategy
     *            defining the property to validate
     */
    public void addValid(Class<?> beanClass, AccessStrategy accessStrategy) {
        List<AccessStrategy> slot = validAccesses.get(beanClass);
        if (slot != null) {
            slot.add(accessStrategy);
        } else {
            List<AccessStrategy> tmpList = new ArrayList<AccessStrategy>();
            tmpList.add(accessStrategy);
            validAccesses.put(beanClass, tmpList);
        }
    }

    /**
     * Set the default group sequence for a particular bean class.
     * 
     * @param beanClass
     * @param groupSequence
     */
    public void addDefaultSequence(Class<?> beanClass, Class<?>[] groupSequence) {
        defaultSequences.put(beanClass, groupSequence);
    }

    /**
     * Retrieve the runtime constraint configuration for a given class.
     * 
     * @param <T>
     * @param beanClass
     * @return List of {@link MetaConstraint}s applicable to
     *         <code>beanClass</code>
     */
    @SuppressWarnings("unchecked")
    public <T> List<MetaConstraint<T, ? extends Annotation>> getMetaConstraints(
        Class<T> beanClass) {
        List<MetaConstraint<?, ? extends Annotation>> slot =
            constraintMap.get(beanClass);
        if (slot != null) {
            // noinspection RedundantCast
            return (List) slot;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Get the {@link AccessStrategy} {@link List} indicating nested bean
     * validations that must be triggered in the course of validating a
     * <code>beanClass</code> graph.
     * 
     * @param beanClass
     * @return {@link List} of {@link AccessStrategy}
     */
    public List<AccessStrategy> getValidAccesses(Class<?> beanClass) {
        List<AccessStrategy> slot = validAccesses.get(beanClass);
        if (slot != null) {
            return slot;
        } else {
            return Collections.<AccessStrategy> emptyList();
        }
    }

    /**
     * Get the default group sequence configured for <code>beanClass</code>.
     * 
     * @param beanClass
     * @return group Class array
     */
    public Class<?>[] getDefaultSequence(Class<?> beanClass) {
        return defaultSequences.get(beanClass);
    }

}
