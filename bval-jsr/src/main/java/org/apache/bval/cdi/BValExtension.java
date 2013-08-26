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
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.validation.BootstrapConfiguration;
import javax.validation.Configuration;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableType;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

// mainly copied from deltaspike to not force users to use deltaspike
// which would be a pain in app servers
// TODO: get rid of beans.xml adding interceptor automatically
public class BValExtension implements Extension {
    private static final Logger LOGGER = Logger.getLogger(BValExtension.class.getName());

    private static BValExtension bmpSingleton = null;
    private volatile Map<ClassLoader, BeanManagerInfo> bmInfos = new ConcurrentHashMap<ClassLoader, BeanManagerInfo>();

    private boolean validatorFound = false;
    private boolean validatorFactoryFound = false;

    private final Configuration<?> config;

    private Set<ExecutableType> globalExecutableTypes;
    private boolean isExecutableValidationEnabled;

    public BValExtension() { // read the config, could be done in a quicker way but this let us get defaults without duplicating code
        config = Validation.byDefaultProvider().configure();
        try {
            final BootstrapConfiguration bootstrap = config.getBootstrapConfiguration();
            globalExecutableTypes = convertToRuntimeTypes(bootstrap.getDefaultValidatedExecutableTypes());
            isExecutableValidationEnabled = bootstrap.isExecutableValidationEnabled();
        } catch (final Exception e) { // custom providers can throw an exception
            LOGGER.log(Level.SEVERE, e.getMessage(), e);

            globalExecutableTypes = Collections.emptySet();
            isExecutableValidationEnabled = false;
        }
    }

    private static Set<ExecutableType> convertToRuntimeTypes(final Set<ExecutableType> defaultValidatedExecutableTypes) {
        final Set<ExecutableType> types = new CopyOnWriteArraySet<ExecutableType>();
        for (final ExecutableType type : defaultValidatedExecutableTypes) {
            if (ExecutableType.IMPLICIT.equals(type)) {
                types.add(ExecutableType.CONSTRUCTORS);
                types.add(ExecutableType.NON_GETTER_METHODS);
            } else if (ExecutableType.ALL.equals(type)) {
                types.add(ExecutableType.CONSTRUCTORS);
                types.add(ExecutableType.NON_GETTER_METHODS);
                types.add(ExecutableType.GETTER_METHODS);
                break;
            } else if (!ExecutableType.NONE.equals(type)) {
                types.add(type);
            }
        }
        return types;
    }

    public Set<ExecutableType> getGlobalExecutableTypes() {
        return globalExecutableTypes;
    }

    public static BValExtension getInstance() {
        return bmpSingleton;
    }

    public void addBvalBinding(final @Observes BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager) {
        if (!isExecutableValidationEnabled) {
            return;
        }

        beforeBeanDiscovery.addInterceptorBinding(BValBinding.class);
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(BValInterceptor.class));
    }

    public <A> void processAnnotatedType(final @Observes ProcessAnnotatedType<A> pat) {
        if (!isExecutableValidationEnabled) {
            return;
        }

        final Class<A> javaClass = pat.getAnnotatedType().getJavaClass();
        final int modifiers = javaClass.getModifiers();
        if (!javaClass.getName().startsWith("javax.") && !javaClass.getName().startsWith("org.apache.bval")
                && !javaClass.isInterface() && !Modifier.isFinal(modifiers) && !Modifier.isAbstract(modifiers)) {
            pat.setAnnotatedType(new BValAnnotatedType<A>(pat.getAnnotatedType()));
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
        captureBeanManager(beanManager);
        cdiIntegration(afterBeanDiscovery, beanManager);
    }

    private void captureBeanManager(final BeanManager beanManager) {
        // bean manager holder
        if (bmpSingleton == null) {
            bmpSingleton = this;
        }
        final BeanManagerInfo bmi = getBeanManagerInfo(loader());
        bmi.loadTimeBm = beanManager;
    }

    private void cdiIntegration(final AfterBeanDiscovery afterBeanDiscovery, final BeanManager beanManager) {
        // add validator and validatorFactory if needed
        ValidatorFactory factory = null;
        if (!validatorFactoryFound) {
            try {
                factory = config.buildValidatorFactory();
                afterBeanDiscovery.addBean(new ValidatorFactoryBean(factory));
            } catch (final Exception e) { // can throw an exception with custom providers
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        if (!validatorFound) {
            try {
                if (factory == null) {
                    factory = config.buildValidatorFactory();
                }
                afterBeanDiscovery.addBean(new ValidatorBean(factory.getValidator()));
                validatorFound = true;
            } catch (final Exception e) { // getValidator can throw an exception with custom providers
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        // add our interceptor, after having added validator if needed since it is injected in the interceptor
        if (validatorFound) {
            afterBeanDiscovery.addBean(new BValInterceptorBean(beanManager));
        } // else we couldn't resolve the interceptor injection point
    }

    private static ClassLoader loader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public BeanManager getBeanManager() {
        final BeanManagerInfo bmi = getBeanManagerInfo(loader());

        BeanManager result = bmi.finalBm;
        if (result == null && bmi.cdi == null) {
            synchronized (this) {
                result = resolveBeanManagerViaJndi();
                if (result == null) {
                    result = bmi.loadTimeBm;
                }
                if (result == null) {
                    bmi.cdi = false;
                    return null;
                }
                bmi.cdi = true;
                bmi.finalBm = result;
            }
        }

        return result;
    }

    public void cleanupFinalBeanManagers(final @Observes AfterDeploymentValidation adv) {
        for (final BeanManagerInfo bmi : bmpSingleton.bmInfos.values()) {
            bmi.finalBm = null;
        }
    }

    public void cleanupStoredBeanManagerOnShutdown(final @Observes BeforeShutdown beforeShutdown) {
        bmpSingleton.bmInfos.remove(loader());
    }

    private static BeanManager resolveBeanManagerViaJndi() {
        try {
            return (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
        } catch (final NamingException e) {
            return null;
        }
    }

    private BeanManagerInfo getBeanManagerInfo(final ClassLoader cl) {
        BeanManagerInfo bmi = bmpSingleton.bmInfos.get(cl);
        if (bmi == null) {
            synchronized (this) {
                bmi = bmpSingleton.bmInfos.get(cl);
                if (bmi == null) {
                    bmi = new BeanManagerInfo();
                    bmpSingleton.bmInfos.put(cl, bmi);
                }
            }
        }
        return bmi;
    }

    public static <T> Releasable<T> inject(final Class<T> clazz) {
        try {
            final BeanManager beanManager = getInstance().getBeanManager();
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

    private static class BeanManagerInfo {
        private BeanManager loadTimeBm = null;
        private BeanManager finalBm = null;
        private Boolean cdi = null;
    }

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
}
