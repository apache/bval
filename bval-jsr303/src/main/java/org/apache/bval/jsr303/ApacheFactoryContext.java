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

import org.apache.bval.*;
import org.apache.bval.xml.XMLMetaBeanBuilder;
import org.apache.bval.xml.XMLMetaBeanFactory;
import org.apache.bval.xml.XMLMetaBeanManager;

import javax.validation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Description: Represents the context that is used to create
 * <code>ClassValidator</code> instances.<br/>
 */
public class ApacheFactoryContext implements ValidatorContext {
    private final ApacheValidatorFactory factory;
    private final MetaBeanFinder metaBeanFinder;

    private MessageInterpolator messageInterpolator;
    private TraversableResolver traversableResolver;
    private ConstraintValidatorFactory constraintValidatorFactory;

    /**
     * Create a new ApacheFactoryContext instance.
     * 
     * @param factory
     */
    public ApacheFactoryContext(ApacheValidatorFactory factory) {
        this.factory = factory;
        this.metaBeanFinder = buildMetaBeanManager();
    }

    /**
     * Create a new ApacheFactoryContext instance.
     * 
     * @param factory
     * @param metaBeanFinder
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

    /**
     * Create MetaBeanManager that uses JSR303-XML + JSR303-Annotations to build
     * meta-data from.
     * 
     * @return a new instance of MetaBeanManager with adequate MetaBeanFactories
     */
    @SuppressWarnings("deprecation")
    protected MetaBeanManager buildMetaBeanManager() {
        // this is relevant: xml before annotations
        // (because ignore-annotations settings in xml)
        List<MetaBeanFactory> builders = new ArrayList<MetaBeanFactory>(3);
        if (Boolean.parseBoolean(factory.getProperties().get(
            ApacheValidatorConfiguration.Properties.ENABLE_INTROSPECTOR))) {
            builders.add(new IntrospectorMetaBeanFactory());
        }
        builders.add(new Jsr303MetaBeanFactory(this));
        // as long as we support both: jsr303 and xstream-xml metabeans:
        if (Boolean.parseBoolean(factory.getProperties().get(
            ApacheValidatorConfiguration.Properties.ENABLE_METABEANS_XML))) {
            return XMLMetaBeanManagerCreator.createXMLMetaBeanManager(builders);
        } else {
            return createMetaBeanManager(builders);
        }
    }

    /**
     * Create a {@link MetaBeanManager} using the specified builders.
     * 
     * @param builders
     *            {@link MetaBeanFactory} {@link List}
     * @return {@link MetaBeanManager}
     */
    protected MetaBeanManager createMetaBeanManager(List<MetaBeanFactory> builders) {
        return new MetaBeanManager(new MetaBeanBuilder(builders.toArray(new MetaBeanFactory[builders.size()])));
    }

    /**
     * separate class to prevent the classloader to immediately load optional
     * classes: XMLMetaBeanManager, XMLMetaBeanFactory, XMLMetaBeanBuilder that
     * might not be available in the classpath
     */
    private static class XMLMetaBeanManagerCreator {

        /**
         * Create the {@link MetaBeanManager} to process JSR303 XML. Requires
         * bval-xstream at RT.
         * 
         * @param builders
         * @return {@link MetaBeanManager}
         */
        // NOTE - We return MetaBeanManager instead of XMLMetaBeanManager to
        // keep
        // bval-xstream an optional module.
        protected static MetaBeanManager createXMLMetaBeanManager(List<MetaBeanFactory> builders) {
            builders.add(new XMLMetaBeanFactory());
            return new XMLMetaBeanManager(
                new XMLMetaBeanBuilder(builders.toArray(new MetaBeanFactory[builders.size()])));
        }
    }
}
