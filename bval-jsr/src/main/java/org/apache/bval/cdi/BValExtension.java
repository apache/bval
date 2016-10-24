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
package org.apache.bval.cdi;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.validation.BootstrapConfiguration;
import javax.validation.Configuration;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.MethodType;


import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.bval.util.Validate;

/**
 * CDI {@link Extension} for Apache BVal setup.
 */
public class BValExtension implements Extension {
    private static final Logger LOGGER = Logger.getLogger(BValExtension.class.getName());

    private static final AnnotatedTypeFilter DEFAULT_ANNOTATED_TYPE_FILTER = new AnnotatedTypeFilter() {
        
        @Override
        public boolean accept(AnnotatedType<?> annotatedType) {
            return !annotatedType.getJavaClass().getName().startsWith("org.apache.bval.");
        }
    };

    private static AnnotatedTypeFilter annotatedTypeFilter = DEFAULT_ANNOTATED_TYPE_FILTER;

    public static void setAnnotatedTypeFilter(AnnotatedTypeFilter annotatedTypeFilter) {
        BValExtension.annotatedTypeFilter = Validate.notNull(annotatedTypeFilter);
    }

    private boolean validatorFound = Boolean.getBoolean("bval.in-container");
    private boolean validatorFactoryFound = Boolean.getBoolean("bval.in-container");

    private boolean validBean;
    private boolean validConstructors;
    private boolean validBusinessMethods;
    private boolean validGetterMethods;

    private final Configuration<?> config;
    private ValidatorFactory factory;
    private Validator validator;

    private Set<ExecutableType> globalExecutableTypes;
    private boolean isExecutableValidationEnabled;

    public BValExtension() { // read the config, could be done in a quicker way but this let us get defaults without duplicating code
        config = Validation.byDefaultProvider().configure();
        try {
            final BootstrapConfiguration bootstrap = config.getBootstrapConfiguration();
            globalExecutableTypes = Collections.unmodifiableSet(convertToRuntimeTypes(bootstrap.getDefaultValidatedExecutableTypes()));
            isExecutableValidationEnabled = bootstrap.isExecutableValidationEnabled();

            // TODO we never contain IMPLICIT or ALL
            validBean = globalExecutableTypes.contains(ExecutableType.IMPLICIT) || globalExecutableTypes.contains(ExecutableType.ALL);
            validConstructors = validBean || globalExecutableTypes.contains(ExecutableType.CONSTRUCTORS);
            validBusinessMethods = validBean || globalExecutableTypes.contains(ExecutableType.NON_GETTER_METHODS);
            validGetterMethods = globalExecutableTypes.contains(ExecutableType.ALL) || globalExecutableTypes.contains(ExecutableType.GETTER_METHODS);
        } catch (final Exception e) { // custom providers can throw an exception
            LOGGER.log(Level.SEVERE, e.getMessage(), e);

            globalExecutableTypes = Collections.emptySet();
            isExecutableValidationEnabled = false;
        }
    }

    // lazily to get a small luck to have CDI in place
    private void ensureFactoryValidator() {
        if (validator != null) {
            return;
        }
        config.addProperty("bval.before.cdi", "true"); // ignore parts of the config relying on CDI since we didn't start yet
        if (factory == null) {
            factory = config.buildValidatorFactory();
        }
        validator = factory.getValidator();
    }

    private static Set<ExecutableType> convertToRuntimeTypes(final Set<ExecutableType> defaultValidatedExecutableTypes) {
        final Set<ExecutableType> types = EnumSet.noneOf(ExecutableType.class);
        for (final ExecutableType type : defaultValidatedExecutableTypes) {
            if (ExecutableType.NONE == type) {
                continue;
            }
            if (ExecutableType.ALL == type) {
                types.add(ExecutableType.CONSTRUCTORS);
                types.add(ExecutableType.NON_GETTER_METHODS);
                types.add(ExecutableType.GETTER_METHODS);
                break;
            }
            if (ExecutableType.IMPLICIT == type) {
                types.add(ExecutableType.CONSTRUCTORS);
                types.add(ExecutableType.NON_GETTER_METHODS);
            } else {
                types.add(type);
            }
        }
        return types;
    }

    public Set<ExecutableType> getGlobalExecutableTypes() {
        return globalExecutableTypes;
    }

    public void addBvalBinding(final @Observes BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager) {
        beforeBeanDiscovery.addInterceptorBinding(BValBinding.class);
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(BValInterceptor.class));
    }

    // @WithAnnotations(ValidateOnExecution.class) doesn't check interfaces so not enough
    public <A> void processAnnotatedType(final @Observes ProcessAnnotatedType<A> pat) {
        if (!isExecutableValidationEnabled) {
            return;
        }

        final AnnotatedType<A> annotatedType = pat.getAnnotatedType();

        if (!annotatedTypeFilter.accept(annotatedType)) {
            return;
        }

        final Class<A> javaClass = annotatedType.getJavaClass();
        final int modifiers = javaClass.getModifiers();
        if (!javaClass.isInterface() && !Modifier.isFinal(modifiers) && !Modifier.isAbstract(modifiers)) {
            try {
                ensureFactoryValidator();
                try {
                    final BeanDescriptor classConstraints = validator.getConstraintsForClass(javaClass);
                    if (annotatedType.isAnnotationPresent(ValidateOnExecution.class)
                            || hasValidationAnnotation(annotatedType.getMethods())
                            || hasValidationAnnotation(annotatedType.getConstructors())
                            || classConstraints != null
                            && (validBean && classConstraints.isBeanConstrained()
                                || validConstructors && !classConstraints.getConstrainedConstructors().isEmpty()
                                || validBusinessMethods && !classConstraints.getConstrainedMethods(MethodType.NON_GETTER).isEmpty()
                                || validGetterMethods && !classConstraints.getConstrainedMethods(MethodType.GETTER).isEmpty())
                            ) {
                        final BValAnnotatedType<A> bValAnnotatedType = new BValAnnotatedType<A>(annotatedType);
                        pat.setAnnotatedType(bValAnnotatedType);
                    }
                } catch (final NoClassDefFoundError ncdfe) {
                    // skip
                }
            } catch (final ValidationException ve) {
                LOGGER.log(Level.FINEST, ve.getMessage(), ve);
            } catch (final Exception e) { // just info
                LOGGER.log(Level.INFO, e.getMessage());
            }
        }
    }

    private static <A> boolean hasValidationAnnotation(final Collection<? extends AnnotatedCallable<? super A>> methods) {
        for (final AnnotatedCallable<? super A> m : methods) {
            if (m.isAnnotationPresent(ValidateOnExecution.class)) {
                return true;
            }
        }
        return false;
    }

    public <A> void processBean(final @Observes ProcessBean<A> processBeanEvent) {
        if (validatorFound && validatorFactoryFound) {
            return;
        }

        final Bean<A> bean = processBeanEvent.getBean();
        if (ValidatorBean.class.isInstance(bean) || ValidatorFactoryBean.class.isInstance(bean)) {
            return;
        }

        final Set<Type> types = bean.getTypes();
        if (!validatorFound) {
            validatorFound = types.contains(Validator.class);
        }
        if (!validatorFactoryFound) {
            validatorFactoryFound = types.contains(ValidatorFactory.class);
        }
    }

    public void addBValBeans(final @Observes AfterBeanDiscovery afterBeanDiscovery, final BeanManager beanManager) {
        if (factory != null) { // cleanup cache used to discover ValidateOnException before factory is recreated
            factory.close();
        }

        cdiIntegration(afterBeanDiscovery, beanManager);
    }

    private void cdiIntegration(final AfterBeanDiscovery afterBeanDiscovery, final BeanManager beanManager) {
        try {
            config.addProperty("bval.before.cdi", "false"); // now take into account all the config
        } catch (final Exception e) {
            // no-op: sadly tck does it
        }

        if (!validatorFactoryFound) {
            try { // recreate the factory
                afterBeanDiscovery.addBean(new ValidatorFactoryBean(factory = config.buildValidatorFactory()));
            } catch (final Exception e) { // can throw an exception with custom providers
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        if (!validatorFound) {
            try {
                if (validatorFactoryFound) {
                    factory = config.buildValidatorFactory();
                } // else fresh factory already created in previous if
                afterBeanDiscovery.addBean(new ValidatorBean(factory, factory.getValidator()));
                validatorFound = true;
            } catch (final Exception e) { // getValidator can throw an exception with custom providers
                afterBeanDiscovery.addBean(new ValidatorBean(factory, null));
                validatorFound = true;
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    /**
     * Request that an instance of the specified type be provided by the container.
     * @param clazz
     * @return the requested instance wrapped in a {@link Releasable}.
     */
    public static <T> Releasable<T> inject(final Class<T> clazz) {
        try {
            final BeanManager beanManager = CDI.current().getBeanManager();
            if (beanManager == null) {
                return null;
            }
            final AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(clazz);
            final InjectionTarget<T> it = beanManager.createInjectionTarget(annotatedType);
            final CreationalContext<T> context = beanManager.createCreationalContext(null);
            final T instance = it.produce(context);
            it.inject(instance, context);
            it.postConstruct(instance);

            return new Releasable<T>(context, it, instance);
        } catch (final Exception e) {
            // no-op
        } catch (final NoClassDefFoundError error) {
            // no-op
        }
        return null;
    }

    public static BeanManager getBeanManager() {
        return CDI.current().getBeanManager();
    }

    /**
     * Represents an item that can be released from a {@link CreationalContext} at some point in the future.
     * @param <T>
     */
    public static class Releasable<T> {
        private final CreationalContext<T> context;
        private final InjectionTarget<T> injectionTarget;
        private final T instance;

        private Releasable(final CreationalContext<T> context, final InjectionTarget<T> injectionTarget, final T instance) {
            this.context = context;
            this.injectionTarget = injectionTarget;
            this.instance = instance;
        }

        public void release() {
            try {
                injectionTarget.preDestroy(instance);
                injectionTarget.dispose(instance);
                context.release();
            } catch (final Exception e) {
                // no-op
            } catch (final NoClassDefFoundError e) {
                // no-op
            }
        }

        public T getInstance() {
            return instance;
        }
    }

    /**
     * Defines an item that can determine whether a given {@link AnnotatedType} will be processed
     * by the {@link BValExtension} for executable validation. May be statically applied before
     * container startup.
     * @see BValExtension#setAnnotatedTypeFilter(AnnotatedTypeFilter)
     */
    public interface AnnotatedTypeFilter {
        boolean accept(AnnotatedType<?> annotatedType);
    }
}
