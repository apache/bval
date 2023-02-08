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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.validation.Validator;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * {@link Validator} CDI {@link Bean}.
 */
public class ValidatorBean implements Bean<Validator>, PassivationCapable {
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;
    private final Supplier<Validator> instance;

    public ValidatorBean(final Supplier<Validator> validator) {
        this.instance = validator;

        final Set<Type> t = new HashSet<>();
        t.add(Validator.class);
        t.add(Object.class);
        types = Collections.unmodifiableSet(t);

        final Set<Annotation> q = new HashSet<>();
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
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public Class<?> getBeanClass() {
        return Validator.class;
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
    public Validator create(final CreationalContext<Validator> context) {
        return instance == null ? null : instance.get();
    }

    @Override
    public void destroy(final Validator instance, final CreationalContext<Validator> context) {
        // no-op
    }

    @Override
    public String getId() {
        return String.format("BVal%s-%d", Validator.class.getSimpleName(), hashCode());
    }
}
