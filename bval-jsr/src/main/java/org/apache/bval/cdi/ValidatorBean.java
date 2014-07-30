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
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ValidatorBean implements Bean<Validator> , PassivationCapable{
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;
    private final ValidatorFactory factory;
    private volatile Validator instance;

    public ValidatorBean(final ValidatorFactory factory, final Validator validator) {
        this.factory = factory;
        this.instance = validator;

        types = new HashSet<Type>();
        types.add(Validator.class);
        types.add(Object.class);

        qualifiers = new HashSet<Annotation>();
        qualifiers.add(DefaultLiteral.INSTANCE);
        qualifiers.add(AnyLiteral.INSTANCE);
    }

    public Set<Type> getTypes() {
        return types;
    }

    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    public String getName() {
        return null;
    }

    public boolean isNullable() {
        return false;
    }

    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    public Class<?> getBeanClass() {
        return Validator.class;
    }

    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    public boolean isAlternative() {
        return false;
    }

    public Validator create(final CreationalContext<Validator> context) {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = factory.getValidator();
                }
            }
        }
        return instance;
    }

    public void destroy(final Validator instance, final CreationalContext<Validator> context) {
        // no-op
    }

    public String getId() {
        return "BValValidator-" + hashCode();
    }
}
