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


import org.apache.bval.jsr303.util.SecureActions;
import org.apache.bval.jsr303.xml.AnnotationIgnores;
import org.apache.bval.jsr303.xml.MetaConstraint;
import org.apache.bval.jsr303.xml.ValidationMappingParser;
import org.apache.bval.util.AccessStrategy;
import org.apache.commons.lang.ClassUtils;

import javax.validation.*;
import javax.validation.bootstrap.ProviderSpecificBootstrap;
import javax.validation.spi.ConfigurationState;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Description: a factory is a complete configurated object that can create validators<br/>
 * this instance is not thread-safe<br/>
 */
public class ApacheValidatorFactory implements ValidatorFactory, Cloneable {
    private static ApacheValidatorFactory DEFAULT_FACTORY;
    private static final ConstraintDefaults defaultConstraints = new ConstraintDefaults();

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

  /** convenience to retrieve a default global ApacheValidatorFactory */
    public static ApacheValidatorFactory getDefault() {
        if (DEFAULT_FACTORY == null) {
            ProviderSpecificBootstrap<ApacheValidatorConfiguration> provider =
                  Validation.byProvider(ApacheValidationProvider.class);
            ApacheValidatorConfiguration configuration = provider.configure();
            DEFAULT_FACTORY = (ApacheValidatorFactory) configuration
                  .buildValidatorFactory();
        }
        return DEFAULT_FACTORY;
    }

    public static void setDefault(ApacheValidatorFactory aDefaultFactory) {
     DEFAULT_FACTORY = aDefaultFactory;
    }

  public ApacheValidatorFactory() {
        properties = new HashMap<String, String>();
        defaultSequences = new HashMap<Class<?>, Class<?>[]>();
        validAccesses = new HashMap<Class<?>, List<AccessStrategy>>();
        constraintMap = new HashMap<Class<?>, List<MetaConstraint<?, ? extends Annotation>>>();
    }

    public void configure(ConfigurationState configuration) {
        getProperties().putAll(configuration.getProperties());
        setMessageInterpolator(configuration.getMessageInterpolator());
        setTraversableResolver(configuration.getTraversableResolver());
        setConstraintValidatorFactory(configuration.getConstraintValidatorFactory());
        ValidationMappingParser parser = new ValidationMappingParser(this);
        parser.processMappingConfig(configuration.getMappingStreams());
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    protected MessageInterpolator getDefaultMessageInterpolator() {
        return messageResolver;
    }

    /**
     * shortcut method to create a new Validator instance with factory's settings
     *
     * @return the new validator instance
     */
    public Validator getValidator() {
        return usingContext().getValidator();
    }

    /** @return the validator factory's context */
    public ApacheFactoryContext usingContext() {
        return new ApacheFactoryContext(this);
    }

    @Override
    public synchronized ApacheValidatorFactory clone() {
        try {
            return (ApacheValidatorFactory) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(); // VM bug.
        }
    }

    public final void setMessageInterpolator(MessageInterpolator messageResolver) {
        this.messageResolver = messageResolver;
    }

    public MessageInterpolator getMessageInterpolator() {
        return ((messageResolver != null) ? messageResolver : getDefaultMessageInterpolator());
    }

    public final void setTraversableResolver(TraversableResolver traversableResolver) {
        this.traversableResolver = traversableResolver;
    }

    public TraversableResolver getTraversableResolver() {
        return traversableResolver;
    }

    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return constraintValidatorFactory;
    }

    public final void setConstraintValidatorFactory(
          ConstraintValidatorFactory constraintValidatorFactory) {
        this.constraintValidatorFactory = constraintValidatorFactory;
    }

    /**
     * Return an object of the specified type to allow access to the
     * provider-specific API.  If the Bean Validation provider
     * implementation does not support the specified class, the
     * ValidationException is thrown.
     *
     * @param type the class of the object to be returned.
     * @return an instance of the specified class
     * @throws ValidationException if the provider does not
     *                             support the call.
     */
    public <T> T unwrap(Class<T> type) {
        if (type.isAssignableFrom(getClass())) {
            return (T) this;
        } else if (!type.isInterface()) {
            return SecureActions.newInstance(type);
        } else {
            try {
                Class<T> cls = ClassUtils.getClass(type.getName() + "Impl");
                return SecureActions.newInstance(cls);
            } catch (ClassNotFoundException e) {
                throw new ValidationException("Type " + type + " not supported");
            }
        }
    }

    public ConstraintDefaults getDefaultConstraints() {
        return defaultConstraints;
    }

    public AnnotationIgnores getAnnotationIgnores() {
        return annotationIgnores;
    }

    public ConstraintCached getConstraintsCache() {
        return constraintsCache;
    }

    public void addMetaConstraint(Class<?> beanClass, MetaConstraint<?, ?> metaConstraint) {
        List<MetaConstraint<?,? extends Annotation>> slot = constraintMap.get(beanClass);
        if (slot != null) {
            slot.add(metaConstraint);
        } else {
            List<MetaConstraint<?, ? extends Annotation>> constraintList =
                  new ArrayList<MetaConstraint<?, ? extends Annotation>>();
            constraintList.add(metaConstraint);
            constraintMap.put(beanClass, constraintList);
        }
    }

    public void addValid(Class<?> beanClass, AccessStrategy accessStategy) {
        List<AccessStrategy> slot = validAccesses.get(beanClass);
        if (slot != null) {
            slot.add(accessStategy);
        } else {
            List<AccessStrategy> tmpList = new ArrayList<AccessStrategy>();
            tmpList.add(accessStategy);
            validAccesses.put(beanClass, tmpList);
        }
    }

    public void addDefaultSequence(Class<?> beanClass, Class<?>[] groupSequence) {
        defaultSequences.put(beanClass, groupSequence);
    }

    public <T> List<MetaConstraint<T, ? extends Annotation>> getMetaConstraints(
          Class<T> beanClass) {
        List<MetaConstraint<?,? extends Annotation>> slot = constraintMap.get(beanClass);
        if (slot != null) {
            //noinspection RedundantCast
            return (List) slot;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public List<AccessStrategy> getValidAccesses(Class<?> beanClass) {
        List<AccessStrategy> slot = validAccesses.get(beanClass);
        if (slot != null) {
            return slot;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public Class<?>[] getDefaultSequence(Class<?> beanClass) {
        return defaultSequences.get(beanClass);
    }

}
