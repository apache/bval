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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.validation.ValidatorFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link ValidatorFactory} CDI {@link Bean}.
 */
public class ValidatorFactoryBean implements Bean<ValidatorFactory> , PassivationCapable{
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;
    private final ValidatorFactory instance;

    public ValidatorFactoryBean(final ValidatorFactory validatorFactory) {
        this.instance = validatorFactory;

        final Set<Type> t = new HashSet<Type>();
        t.add(ValidatorFactory.class);
        t.add(Object.class);
        types = Collections.unmodifiableSet(t);

        final Set<Annotation> q = new HashSet<Annotation>();
        q.add(DefaultLiteral.INSTANCE);
        q.add(AnyLiteral.INSTANCE);
        qualifiers = Collections.unmodifiableSet(q);
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
        return ApplicationScoped.class;
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
        return Collections.emptySet();
    }

    @Override
    public Class<?> getBeanClass() {
        return ValidatorFactory.class;
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
    public ValidatorFactory create(final CreationalContext<ValidatorFactory> context) {
        return instance;
    }

    @Override
    public void destroy(final ValidatorFactory instance, final CreationalContext<ValidatorFactory> context) {
        instance.close();
    }

    @Override
    public String getId() {
        return "BValValidatorFactory-" + hashCode();
    }
}
