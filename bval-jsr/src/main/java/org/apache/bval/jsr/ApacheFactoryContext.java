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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorContext;

import org.apache.bval.IntrospectorMetaBeanFactory;
import org.apache.bval.MetaBeanBuilder;
import org.apache.bval.MetaBeanFactory;
import org.apache.bval.MetaBeanFinder;
import org.apache.bval.MetaBeanManager;
import org.apache.bval.util.reflection.Reflection;
import org.apache.bval.xml.XMLMetaBeanBuilder;
import org.apache.bval.xml.XMLMetaBeanFactory;
import org.apache.bval.xml.XMLMetaBeanManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.weaver.privilizer.Privileged;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Description: Represents the context that is used to create
 * <code>ClassValidator</code> instances.<br/>
 */
@Privilizing(@CallTo(Reflection.class))
public class ApacheFactoryContext implements ValidatorContext {
    private final ApacheValidatorFactory factory;
    private final MetaBeanFinder metaBeanFinder;

    private MessageInterpolator messageInterpolator;
    private TraversableResolver traversableResolver;
    private ParameterNameProvider parameterNameProvider;
    private ConstraintValidatorFactory constraintValidatorFactory;

    /**
     * Create a new ApacheFactoryContext instance.
     * 
     * @param factory validator factory
     */
    public ApacheFactoryContext(ApacheValidatorFactory factory) {
        this.factory = factory;
        this.metaBeanFinder = buildMetaBeanFinder();
    }

    /**
     * Create a new ApacheFactoryContext instance.
     * 
     * @param factory validator factory
     * @param metaBeanFinder meta finder
     */
    protected ApacheFactoryContext(ApacheValidatorFactory factory, MetaBeanFinder metaBeanFinder) {
        this.factory = factory;
        this.metaBeanFinder = metaBeanFinder;
    }

    /**
     * Get the {@link ApacheValidatorFactory} used by this
     * {@link ApacheFactoryContext}.
     * 
     * @return {@link ApacheValidatorFactory}
     */
    public ApacheValidatorFactory getFactory() {
        return factory;
    }

    /**
     * Get the metaBeanFinder.
     * 
     * @return {@link MetaBeanFinder}
     */
    public final MetaBeanFinder getMetaBeanFinder() {
        return metaBeanFinder;
    }

    /**
     * {@inheritDoc}
     */
    public ValidatorContext messageInterpolator(MessageInterpolator messageInterpolator) {
        this.messageInterpolator = messageInterpolator;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ValidatorContext traversableResolver(TraversableResolver traversableResolver) {
        this.traversableResolver = traversableResolver;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ValidatorContext constraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
        this.constraintValidatorFactory = constraintValidatorFactory;
        return this;
    }

    public ValidatorContext parameterNameProvider(ParameterNameProvider parameterNameProvider) {
        this.parameterNameProvider = parameterNameProvider;
        return this;
    }

    /**
     * Get the {@link ConstraintValidatorFactory}.
     * 
     * @return {@link ConstraintValidatorFactory}
     */
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return constraintValidatorFactory == null ? factory.getConstraintValidatorFactory()
            : constraintValidatorFactory;
    }

    /**
     * {@inheritDoc}
     */
    public Validator getValidator() {
        ClassValidator validator = new ClassValidator(this);
        if (Boolean.parseBoolean(factory.getProperties().get(
            ApacheValidatorConfiguration.Properties.TREAT_MAPS_LIKE_BEANS))) {
            validator.setTreatMapsLikeBeans(true);
        }
        return validator;
    }

    /**
     * Get the {@link MessageInterpolator}.
     * 
     * @return {@link MessageInterpolator}
     */
    public MessageInterpolator getMessageInterpolator() {
        return messageInterpolator == null ? factory.getMessageInterpolator() : messageInterpolator;
    }

    /**
     * Get the {@link TraversableResolver}.
     * 
     * @return {@link TraversableResolver}
     */
    public TraversableResolver getTraversableResolver() {
        return traversableResolver == null ? factory.getTraversableResolver() : traversableResolver;
    }

    public ParameterNameProvider getParameterNameProvider() {
        return parameterNameProvider == null ? factory.getParameterNameProvider() : parameterNameProvider;
    }

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
        if (Boolean.parseBoolean(factory.getProperties().get(
            ApacheValidatorConfiguration.Properties.ENABLE_INTROSPECTOR))) {
            builders.add(new IntrospectorMetaBeanFactory());
        }
        final String[] factoryClassNames =
            StringUtils.split(factory.getProperties().get(
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
            Boolean.parseBoolean(factory.getProperties().get(
                ApacheValidatorConfiguration.Properties.ENABLE_METABEANS_XML));
        if (enableMetaBeansXml) {
            XMLMetaBeanManagerCreator.addFactory(builders);
        }
        return createMetaBeanManager(builders);
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
        if (Boolean.parseBoolean(factory.getProperties().get(
            ApacheValidatorConfiguration.Properties.ENABLE_METABEANS_XML))) {
            return XMLMetaBeanManagerCreator.createXMLMetaBeanManager(builders);
        }
        return new MetaBeanManager(new MetaBeanBuilder(builders.toArray(new MetaBeanFactory[builders.size()])));
    }

    @Privileged
    private <F extends MetaBeanFactory> F createMetaBeanFactory(final Class<F> cls) {
        try {
            Constructor<F> c = ConstructorUtils.getMatchingAccessibleConstructor(cls, ApacheFactoryContext.this.getClass());
            if (c != null) {
                return c.newInstance(ApacheFactoryContext.this);
            }
            c = ConstructorUtils.getMatchingAccessibleConstructor(cls, getFactory().getClass());
            if (c != null) {
                return c.newInstance(getFactory());
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
            return Class.forName(className, true, Reflection.getClassLoader(ApacheFactoryContext.class));
        } catch (ClassNotFoundException ex) {
            throw new ValidationException("Unable to load class: " + className, ex);
        }
    }
}
