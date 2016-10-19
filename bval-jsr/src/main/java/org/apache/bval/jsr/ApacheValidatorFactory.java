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

import org.apache.bval.IntrospectorMetaBeanFactory;
import org.apache.bval.MetaBeanBuilder;
import org.apache.bval.MetaBeanFactory;
import org.apache.bval.MetaBeanFinder;
import org.apache.bval.MetaBeanManager;
import org.apache.bval.jsr.xml.AnnotationIgnores;
import org.apache.bval.jsr.xml.MetaConstraint;
import org.apache.bval.jsr.xml.ValidationMappingParser;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.reflection.Reflection;
import org.apache.bval.xml.XMLMetaBeanBuilder;
import org.apache.bval.xml.XMLMetaBeanFactory;
import org.apache.bval.xml.XMLMetaBeanManager;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.weaver.privilizer.Privileged;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ConfigurationState;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Description: a factory is a complete configurated object that can create
 * validators.<br/>
 * This instance is not thread-safe.<br/>
 */
@Privilizing(@CallTo(Reflection.class))
public class ApacheValidatorFactory implements ValidatorFactory, Cloneable {
    private static volatile ApacheValidatorFactory DEFAULT_FACTORY;
    private static final ConstraintDefaults DEFAULT_CONSTRAINTS = new ConstraintDefaults();

    private MessageInterpolator messageResolver;
    private TraversableResolver traversableResolver;
    private ConstraintValidatorFactory constraintValidatorFactory;
    private ParameterNameProvider parameterNameProvider;
    private final Map<String, String> properties;

    /**
     * information from xml parsing
     */
    private final AnnotationIgnores annotationIgnores = new AnnotationIgnores();
    private final ConstraintCached constraintsCache = new ConstraintCached();
    private final Map<Class<?>, Class<?>[]> defaultSequences;

    /**
     * access strategies for properties with cascade validation @Valid support
     */
    private final ConcurrentMap<Class<?>, List<AccessStrategy>> validAccesses;
    private final ConcurrentMap<Class<?>, List<MetaConstraint<?, ? extends Annotation>>> constraintMap;

    private final Collection<Closeable> toClose = new ArrayList<Closeable>();
    private final MetaBeanFinder defaultMetaBeanFinder;

    /**
     * Create MetaBeanManager that uses factories:
     * <ol>
     * <li>if enabled by
     * {@link ApacheValidatorConfiguration.Properties#ENABLE_INTROSPECTOR}, an
     * {@link IntrospectorMetaBeanFactory}</li>
     * <li>{@link MetaBeanFactory} types (if any) specified by
     * {@link ApacheValidatorConfiguration.Properties#METABEAN_FACTORY_CLASSNAMES}
     * </li>
     * <li>if no {@link JsrMetaBeanFactory} has yet been specified (this
     * allows factory order customization), a {@link JsrMetaBeanFactory}
     * which handles both JSR303-XML and JSR303-Annotations</li>
     * <li>if enabled by
     * {@link ApacheValidatorConfiguration.Properties#ENABLE_METABEANS_XML}, an
     * {@link XMLMetaBeanFactory}</li>
     * </ol>
     *
     * @return a new instance of MetaBeanManager with adequate MetaBeanFactories
     */
    protected MetaBeanFinder buildMetaBeanFinder() {
        final List<MetaBeanFactory> builders = new ArrayList<MetaBeanFactory>();
        if (Boolean.parseBoolean(getProperties().get(
                ApacheValidatorConfiguration.Properties.ENABLE_INTROSPECTOR))) {
            builders.add(new IntrospectorMetaBeanFactory());
        }
        final String[] factoryClassNames =
                StringUtils.split(getProperties().get(
                        ApacheValidatorConfiguration.Properties.METABEAN_FACTORY_CLASSNAMES));
        if (factoryClassNames != null) {
            for (String clsName : factoryClassNames) {
                // cast, relying on #createMetaBeanFactory to throw the exception if incompatible:
                @SuppressWarnings("unchecked")
                final Class<? extends MetaBeanFactory> factoryClass = (Class<? extends MetaBeanFactory>) loadClass(clsName);
                builders.add(createMetaBeanFactory(factoryClass));
            }
        }
        boolean jsrFound = false;
        for (MetaBeanFactory builder : builders) {
            jsrFound |= builder instanceof JsrMetaBeanFactory;
        }
        if (!jsrFound) {
            builders.add(new JsrMetaBeanFactory(this));
        }
        @SuppressWarnings("deprecation")
        final boolean enableMetaBeansXml =
                Boolean.parseBoolean(getProperties().get(
                        ApacheValidatorConfiguration.Properties.ENABLE_METABEANS_XML));
        if (enableMetaBeansXml) {
            XMLMetaBeanManagerCreator.addFactory(builders);
        }
        return createMetaBeanManager(builders);
    }

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
        properties = new HashMap<String, String>(configuration.getProperties());
        defaultSequences = new HashMap<Class<?>, Class<?>[]>();
        validAccesses = new ConcurrentHashMap<Class<?>, List<AccessStrategy>>();
        constraintMap = new ConcurrentHashMap<Class<?>, List<MetaConstraint<?, ? extends Annotation>>>();

        parameterNameProvider = configuration.getParameterNameProvider();
        messageResolver = configuration.getMessageInterpolator();
        traversableResolver = configuration.getTraversableResolver();
        constraintValidatorFactory = configuration.getConstraintValidatorFactory();

        if (ConfigurationImpl.class.isInstance(configuration)) {
            final ConfigurationImpl impl = ConfigurationImpl.class.cast(configuration);
            toClose.add(impl.getClosable());
        }

        new ValidationMappingParser(this).processMappingConfig(configuration.getMappingStreams());

        defaultMetaBeanFinder = buildMetaBeanFinder();
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
     * Shortcut method to create a new Validator instance with factory's
     * settings
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
        return new ApacheFactoryContext(this, defaultMetaBeanFinder);
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
    public final void setTraversableResolver(
            TraversableResolver traversableResolver) {
        if (traversableResolver != null) {
            this.traversableResolver = traversableResolver;
        }
    }

    public void setParameterNameProvider(final ParameterNameProvider parameterNameProvider) {
        if (parameterNameProvider != null) {
            this.parameterNameProvider = parameterNameProvider;
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
    public final void setConstraintValidatorFactory(
            ConstraintValidatorFactory constraintValidatorFactory) {
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
     * Return an object of the specified type to allow access to the
     * provider-specific API. If the Bean Validation provider implementation
     * does not support the specified class, the ValidationException is thrown.
     *
     * @param type the class of the object to be returned.
     * @return an instance of the specified class
     * @throws ValidationException if the provider does not support the call.
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
        
        if (!(type.isInterface() || Modifier.isAbstract(type
                .getModifiers()))) {
            return newInstance(type);
        }
        try {
            final Class<?> cls = ClassUtils.getClass(type.getName() + "Impl");
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
     * Get the detected {@link ConstraintDefaults}.
     *
     * @return ConstraintDefaults
     */
    public ConstraintDefaults getDefaultConstraints() {
        return DEFAULT_CONSTRAINTS;
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
    public void addMetaConstraint(final Class<?> beanClass,
                                  final MetaConstraint<?, ?> metaConstraint) {
        List<MetaConstraint<?, ? extends Annotation>> slot = constraintMap.get(beanClass);
        if (slot == null) {
            slot = new ArrayList<MetaConstraint<?, ? extends Annotation>>();
            final List<MetaConstraint<?, ? extends Annotation>> old = constraintMap.putIfAbsent(beanClass, slot);
            if (old != null) {
                slot = old;
            }
        }
        slot.add(metaConstraint);
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
        if (slot == null) {
            slot = new ArrayList<AccessStrategy>();
            final List<AccessStrategy> old = validAccesses.putIfAbsent(beanClass, slot);
            if (old != null) {
                slot = old;
            }
        }
        slot.add(accessStrategy);
    }

    /**
     * Set the default group sequence for a particular bean class.
     *
     * @param beanClass
     * @param groupSequence
     */
    public void addDefaultSequence(Class<?> beanClass, Class<?>... groupSequence) {
        defaultSequences.put(beanClass, safeArray(groupSequence));
    }

    /**
     * Retrieve the runtime constraint configuration for a given class.
     *
     * @param <T>
     * @param beanClass
     * @return List of {@link MetaConstraint}s applicable to
     *         <code>beanClass</code>
     */
    public <T> List<MetaConstraint<T, ? extends Annotation>> getMetaConstraints(
            Class<T> beanClass) {
        final List<MetaConstraint<?, ? extends Annotation>> slot = constraintMap.get(beanClass);
        if (slot == null) {
            return Collections.emptyList();
        }
        // noinspection RedundantCast
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final List<MetaConstraint<T, ? extends Annotation>> result = (List) slot;
        return Collections.unmodifiableList(result);
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
        final List<AccessStrategy> slot = validAccesses.get(beanClass);
        return slot == null ? Collections.<AccessStrategy> emptyList() : Collections.unmodifiableList(slot);
    }

    /**
     * Get the default group sequence configured for <code>beanClass</code>.
     *
     * @param beanClass
     * @return group Class array
     */
    public Class<?>[] getDefaultSequence(Class<?> beanClass) {
        return safeArray(defaultSequences.get(beanClass));
    }

    private static Class<?>[] safeArray(Class<?>... array) {
        return ArrayUtils.isEmpty(array) ? ArrayUtils.EMPTY_CLASS_ARRAY : ArrayUtils.clone(array);
    }

    /**
     * Create a {@link MetaBeanManager} using the specified builders.
     *
     * @param builders
     *            {@link MetaBeanFactory} {@link List}
     * @return {@link MetaBeanManager}
     */
    @SuppressWarnings("deprecation")
    protected MetaBeanFinder createMetaBeanManager(List<MetaBeanFactory> builders) {
        // as long as we support both: jsr (in the builders list) and xstream-xml metabeans:
        if (Boolean.parseBoolean(getProperties().get(
                ApacheValidatorConfiguration.Properties.ENABLE_METABEANS_XML))) {
            return XMLMetaBeanManagerCreator.createXMLMetaBeanManager(builders);
        }
        return new MetaBeanManager(new MetaBeanBuilder(builders.toArray(new MetaBeanFactory[builders.size()])));
    }

    @Privileged
    private <F extends MetaBeanFactory> F createMetaBeanFactory(final Class<F> cls) {
        try {
            Constructor<F> c = ConstructorUtils.getMatchingAccessibleConstructor(cls, ApacheValidatorFactory.this.getClass());
            if (c != null) {
                return c.newInstance(this);
            }
            c = ConstructorUtils.getMatchingAccessibleConstructor(cls, getClass());
            if (c != null) {
                return c.newInstance(this);
            }
            return cls.newInstance();
        } catch (Exception e) {
            throw new ValidationException(e);
        }
    }

    /**
     * separate class to prevent the classloader to immediately load optional
     * classes: XMLMetaBeanManager, XMLMetaBeanFactory, XMLMetaBeanBuilder that
     * might not be available in the classpath
     */
    private static class XMLMetaBeanManagerCreator {

        static void addFactory(List<MetaBeanFactory> builders) {
            builders.add(new XMLMetaBeanFactory());
        }

        /**
         * Create the {@link MetaBeanManager} to process JSR303 XML. Requires
         * bval-xstream at RT.
         *
         * @param builders meta bean builders
         * @return {@link MetaBeanManager}
         */
        // NOTE - We return MetaBeanManager instead of XMLMetaBeanManager to
        // keep
        // bval-xstream an optional module.
        protected static MetaBeanManager createXMLMetaBeanManager(List<MetaBeanFactory> builders) {
            return new XMLMetaBeanManager(
                    new XMLMetaBeanBuilder(builders.toArray(new MetaBeanFactory[builders.size()])));
        }
    }

    private Class<?> loadClass(final String className) {
        try {
            return Class.forName(className, true, Reflection.getClassLoader(ApacheValidatorFactory.class));
        } catch (ClassNotFoundException ex) {
            throw new ValidationException("Unable to load class: " + className, ex);
        }
    }
}
