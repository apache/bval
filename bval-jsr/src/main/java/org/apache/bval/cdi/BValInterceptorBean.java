/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bval.cdi;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.PassivationCapable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link BValInterceptor} CDI {@link Bean}.
 */
public class BValInterceptorBean implements Bean<BValInterceptor>, PassivationCapable {
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;
    private final Set<InjectionPoint> injectionPoints;
    private final InjectionTarget<BValInterceptor> injectionTarget;

    public BValInterceptorBean(final BeanManager bm) {
        final Set<Type> t = new HashSet<>();
        t.add(BValInterceptor.class);
        t.add(Object.class);
        types = Collections.unmodifiableSet(t);

        final Set<Annotation> q = new HashSet<>();
        q.add(DefaultLiteral.INSTANCE);
        q.add(AnyLiteral.INSTANCE);
        qualifiers = Collections.unmodifiableSet(q);

        injectionTarget = bm.createInjectionTarget(bm.createAnnotatedType(BValInterceptor.class));
        injectionPoints = Collections.singleton(InjectionPoint.class
            .cast(new BValInterceptorInjectionPoint(this, injectionTarget.getInjectionPoints().iterator().next())));
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return injectionPoints;
    }

    @Override
    public Class<?> getBeanClass() {
        return BValInterceptor.class;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public BValInterceptor create(final CreationalContext<BValInterceptor> context) {
        final BValInterceptor produced = injectionTarget.produce(context);
        injectionTarget.inject(produced, context);
        injectionTarget.postConstruct(produced);
        return produced;
    }

    @Override
    public void destroy(final BValInterceptor instance, final CreationalContext<BValInterceptor> context) {
        injectionTarget.preDestroy(instance);
        injectionTarget.dispose(instance);
        context.release();
    }

    @Override
    public String getId() {
        return String.format("%s-%d",BValInterceptor.class.getSimpleName(), hashCode());
    }

    private static class BValInterceptorInjectionPoint implements InjectionPoint {
        private final InjectionPoint delegate;
        private final Bean<?> bean;

        public BValInterceptorInjectionPoint(final Bean<?> bean, final InjectionPoint next) {
            this.bean = bean;
            delegate = next;
        }

        @Override
        public Type getType() {
            return delegate.getType();
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return delegate.getQualifiers();
        }

        @Override
        public Bean<?> getBean() {
            return bean;
        }

        @Override
        public Member getMember() {
            return delegate.getMember();
        }

        @Override
        public Annotated getAnnotated() {
            return delegate.getAnnotated();
        }

        @Override
        public boolean isDelegate() {
            return delegate.isDelegate();
        }

        @Override
        public boolean isTransient() {
            return delegate.isTransient();
        }
    }
}
