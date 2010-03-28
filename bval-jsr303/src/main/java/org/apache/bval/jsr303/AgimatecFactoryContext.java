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

import javax.validation.*;

import org.apache.bval.IntrospectorMetaBeanFactory;
import org.apache.bval.MetaBeanBuilder;
import org.apache.bval.MetaBeanFactory;
import org.apache.bval.MetaBeanFinder;
import org.apache.bval.MetaBeanManager;
import org.apache.bval.xml.XMLMetaBeanFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Description: Represents the context that is used to create <code>ClassValidator</code>
 * instances.<br/>
 * User: roman <br/>
 * Date: 01.10.2009 <br/>
 * Time: 16:35:25 <br/>
 * Copyright: Agimatec GmbH
 */
public class AgimatecFactoryContext implements ValidatorContext {
    private final AgimatecValidatorFactory factory;
    private final MetaBeanFinder metaBeanFinder;

    private MessageInterpolator messageInterpolator;
    private TraversableResolver traversableResolver;
    private ConstraintValidatorFactory constraintValidatorFactory;

    public AgimatecFactoryContext(AgimatecValidatorFactory factory) {
        this.factory = factory;
        this.metaBeanFinder = buildMetaBeanManager();
    }

    protected AgimatecFactoryContext(AgimatecValidatorFactory factory,
                                     MetaBeanFinder metaBeanFinder) {
        this.factory = factory;
        this.metaBeanFinder = metaBeanFinder;
    }

    public AgimatecValidatorFactory getFactory() {
        return factory;
    }

    public final MetaBeanFinder getMetaBeanFinder() {
        return metaBeanFinder;
    }

    public ValidatorContext messageInterpolator(MessageInterpolator messageInterpolator) {
        this.messageInterpolator = messageInterpolator;
        return this;
    }

    public ValidatorContext traversableResolver(TraversableResolver traversableResolver) {
        this.traversableResolver = traversableResolver;
        return this;
    }

    public ValidatorContext constraintValidatorFactory(
          ConstraintValidatorFactory constraintValidatorFactory) {
        this.constraintValidatorFactory = constraintValidatorFactory;
        return this;
    }

    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return constraintValidatorFactory == null ? factory.getConstraintValidatorFactory() :
              constraintValidatorFactory;
    }

    public Validator getValidator() {
        ClassValidator validator = new ClassValidator(this);
        if (Boolean.getBoolean(factory.getProperties().get(
              AgimatecValidatorConfiguration.Properties.TREAT_MAPS_LIKE_BEANS))) {
            validator.setTreatMapsLikeBeans(true);
        }
        return validator;
    }

    public MessageInterpolator getMessageInterpolator() {
        return messageInterpolator == null ? factory.getMessageInterpolator() :
              messageInterpolator;
    }

    public TraversableResolver getTraversableResolver() {
        return traversableResolver == null ? factory.getTraversableResolver() :
              traversableResolver;
    }

    /**
     * Create MetaBeanManager that
     * uses JSR303-XML + JSR303-Annotations
     * to build meta-data from.
     */
    private MetaBeanManager buildMetaBeanManager() {
        // this is relevant: xml before annotations
        // (because ignore-annotations settings in xml)
        List<MetaBeanFactory> builders = new ArrayList(3);
        if (Boolean.parseBoolean(factory.getProperties().get(
              AgimatecValidatorConfiguration.Properties.ENABLE_INTROSPECTOR))) {
            builders.add(new IntrospectorMetaBeanFactory());
        }
        builders.add(new Jsr303MetaBeanFactory(this));

        if (Boolean.parseBoolean(factory.getProperties().get(
              AgimatecValidatorConfiguration.Properties.ENABLE_METABEANS_XML))) {
            builders.add(new XMLMetaBeanFactory());
        }
        return new MetaBeanManager(
              new MetaBeanBuilder(builders.toArray(new MetaBeanFactory[builders.size()])));
    }
}