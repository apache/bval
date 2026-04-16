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

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedCallable;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.validation.BootstrapConfiguration;
import jakarta.validation.Configuration;
import jakarta.validation.Constraint;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableType;
import jakarta.validation.executable.ValidateOnExecution;

import org.apache.bval.jsr.ConfigurationImpl;
import org.apache.bval.jsr.util.ExecutableTypes;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.Validate;

/**
 * CDI {@link Extension} for Apache BVal setup.
 */
public class BValExtension implements Extension {
    private static final Logger LOGGER = Logger.getLogger(BValExtension.class.getName());

    // protected so can be accessed in tests
    protected static final AnnotatedTypeFilter DEFAULT_ANNOTATED_TYPE_FILTER =
        annotatedType -> !annotatedType.getJavaClass().getName().startsWith("org.apache.bval.");

    private static AnnotatedTypeFilter annotatedTypeFilter = DEFAULT_ANNOTATED_TYPE_FILTER;

    public static void setAnnotatedTypeFilter(AnnotatedTypeFilter annotatedTypeFilter) {
        BValExtension.annotatedTypeFilter = Validate.notNull(annotatedTypeFilter);
    }

    private boolean validatorFound = Boolean.getBoolean("bval.in-container");
    private boolean validatorFactoryFound = Boolean.getBoolean("bval.in-container");

    private final Configuration<?> config;
    private Lazy<ValidatorFactory> factory;

    private Set<ExecutableType> globalExecutableTypes;
    private boolean isExecutableValidationEnabled;

    private final Collection<Class<?>> potentiallyBValAnnotation = new HashSet<>();
    private final Collection<Class<?>> notBValAnnotation = new HashSet<>();

    public BValExtension() { // read the config, could be done in a quicker way but this let us get defaults without duplicating code
        config = Validation.byDefaultProvider().configure();
        try {
            final BootstrapConfiguration bootstrap = config.getBootstrapConfiguration();
            globalExecutableTypes =
                ExecutableTypes.interpret(bootstrap.getDefaultValidatedExecutableTypes());

            isExecutableValidationEnabled = bootstrap.isExecutableValidationEnabled();

        } catch (final Exception e) { // custom providers can throw an exception
            LOGGER.log(Level.SEVERE, e.getMessage(), e);

            globalExecutableTypes = Collections.emptySet();
            isExecutableValidationEnabled = false;
        }
    }

    public Set<ExecutableType> getGlobalExecutableTypes() {
        return globalExecutableTypes;
    }

    public void addBvalBinding(final @Observes BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager) {
        beforeBeanDiscovery.addInterceptorBinding(BValBinding.class);
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(BValInterceptor.class),
                             BValExtension.class.getName() + "#" + BValInterceptor.class.getSimpleName());
    }

    // @WithAnnotations(ValidateOnExecution.class) doesn't check interfaces so not enough
    public <A> void processAnnotatedType(final @Observes ProcessAnnotatedType<A> pat, BeanManager beanManager) {
        if (!isExecutableValidationEnabled) {
            return;
        }
        final AnnotatedType<A> annotatedType = pat.getAnnotatedType();

        if (!annotatedTypeFilter.accept(annotatedType)) {
            return;
        }
        final Class<A> javaClass = annotatedType.getJavaClass();
        final int modifiers = javaClass.getModifiers();
        if (!javaClass.isInterface() && !javaClass.isAnonymousClass() && !Modifier.isFinal(modifiers) && !Modifier.isAbstract(modifiers)) {
            try {
                Queue<AnnotatedType<?>> toProcess = new LinkedList<>();
                toProcess.add(annotatedType);

                while (!toProcess.isEmpty()) {
                    AnnotatedType<?> now = toProcess.poll();
                    Set<? extends AnnotatedMethod<?>> methods = now.getMethods();
                    Set<? extends AnnotatedConstructor<?>> constructors = now.getConstructors();

                    if (hasValidation(now)
                            || methods.stream().anyMatch(this::hasValidation)
                            || constructors.stream().anyMatch(this::hasValidation)
                            || methods.stream().anyMatch(this::hasParamsWithValidation)
                            || constructors.stream().anyMatch(this::hasParamsWithValidation)) {
                        pat.setAnnotatedType(new BValAnnotatedType<>(annotatedType));
                        break;
                    }

                    Class<?> superclass = now.getJavaClass().getSuperclass();
                    if (superclass != Object.class && superclass != null) {
                        toProcess.add(beanManager.createAnnotatedType(superclass));
                    }

                    for (Class<?> iface : now.getJavaClass().getInterfaces()) {
                        toProcess.add(beanManager.createAnnotatedType(iface));
                    }
                }
            } catch (final Exception e) {
                if (e instanceof ValidationException) {
                    throw e;
                }
                LOGGER.log(Level.INFO, e.getMessage());
            } catch (final NoClassDefFoundError ncdfe) {
                // skip
            }
        }
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
        if (factory != null && factory.optional().isPresent()) { // cleanup cache used to discover ValidateOnException before factory is recreated
            factory.get().close();
        }
        if (config instanceof ConfigurationImpl) {
            ((ConfigurationImpl) config).releaseDeferredBootstrapOverrides();
        }
        if (!validatorFactoryFound) {
            try { // recreate the factory
                factory = new Lazy<>(config::buildValidatorFactory);
                afterBeanDiscovery.addBean(new ValidatorFactoryBean(factory));
                validatorFactoryFound = true;
            } catch (final ValidationException ve) {
                //throw ve;
            } catch (final Exception e) { // can throw an exception with custom providers
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        if (!validatorFound && validatorFactoryFound) {
            try {
                afterBeanDiscovery.addBean(new ValidatorBean(() -> CDI.current().select(ValidatorFactory.class).get().getValidator()));
                validatorFound = true;
            } catch (final ValidationException ve) {
                throw ve;
            } catch (final Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    public void afterStart(@Observes final AfterDeploymentValidation clearEvent) {
        potentiallyBValAnnotation.clear();
        notBValAnnotation.clear();
    }

    private boolean hasParamsWithValidation(AnnotatedCallable<?> callable) {
        for (AnnotatedParameter<?> param : callable.getParameters()) {
            if (hasValidation(param)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasValidation(final Annotated element) {
        for (Annotation annotation : element.getAnnotations()) {
            final Class<? extends Annotation> type = annotation.annotationType();
            if (type == ValidateOnExecution.class || type == Valid.class) {
                return true;
            }
            if (isSkippedAnnotation(type)) {
                continue;
            }
            if (type.getName().startsWith("jakarta.validation.constraints")) {
                return true;
            }
            if (notBValAnnotation.contains(type)) { // more likely so faster first
                continue;
            }
            if (potentiallyBValAnnotation.contains(type)) {
                return true;
            }
            cacheIsBvalAnnotation(type);
            if (potentiallyBValAnnotation.contains(type)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSkippedAnnotation(final Class<? extends Annotation> type) {
        if (type.getName().startsWith("java.")) {
            return true;
        }
        if (type.getName().startsWith("jakarta.enterprise.")) {
            return true;
        }
        if (type.getName().startsWith("jakarta.inject.")) {
            return true;
        }
        return false;
    }

    private void cacheIsBvalAnnotation(final Class<? extends Annotation> type) {
        if (flattenAnnotations(type, new HashSet<>()).anyMatch(it -> it == Constraint.class)) {
            potentiallyBValAnnotation.add(type);
        } else {
            notBValAnnotation.add(type);
        }
    }

    private Stream<Class<?>> flattenAnnotations(final Class<? extends Annotation> type, final Set<Class<?>> seen) {
        seen.add(type);
        return Stream.of(type)
                     .flatMap(it -> Stream.concat(
                             Stream.of(it),
                             Stream.of(it.getAnnotations())
                                   .map(Annotation::annotationType)
                                   .distinct()
                                   .filter(a -> !isSkippedAnnotation(a))
                                   .filter(seen::add)
                                   .flatMap(a -> flattenAnnotations(a, seen))));
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
            final InjectionTarget<T> it = beanManager.getInjectionTargetFactory(annotatedType).createInjectionTarget(null);
            final CreationalContext<T> context = beanManager.createCreationalContext(null);
            final T instance = it.produce(context);
            it.inject(instance, context);
            it.postConstruct(instance);

            return new Releasable<>(context, it, instance);
        } catch (final Exception | NoClassDefFoundError error) {
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

        private Releasable(final CreationalContext<T> context, final InjectionTarget<T> injectionTarget,
            final T instance) {
            this.context = context;
            this.injectionTarget = injectionTarget;
            this.instance = instance;
        }

        public void release() {
            try {
                injectionTarget.preDestroy(instance);
                injectionTarget.dispose(instance);
                context.release();
            } catch (final Exception | NoClassDefFoundError e) {
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
