/*
* JBoss, Home of Professional Open Source
* Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.bval.arquillian;

import org.jboss.arquillian.test.spi.TestEnricher;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// mock a very very simple EJB container (in fact only local bean @Resource Validator* injections)
public class EJBEnricher implements TestEnricher {
    public void enrich(final Object testCase) {
        for (final Field field : testCase.getClass().getDeclaredFields()) {
            if (field.getAnnotation(EJB.class) != null) {
                try {
                    final Object instance = field.getType().newInstance();
                    for (final Field f : field.getType().getDeclaredFields()) {
                        if (f.getAnnotation(Resource.class) != null) {
                            if (f.getType().equals(Validator.class)) {
                                f.set(instance,
                                    Validation.byDefaultProvider().configure().buildValidatorFactory().getValidator());
                            } else if (f.getType().equals(ValidatorFactory.class)) {
                                f.set(instance, Validation.byDefaultProvider().configure().buildValidatorFactory());
                            }
                        }
                    }
                    field.setAccessible(true);
                    field.set(testCase, instance);
                } catch (final Exception e) {
                    // no-op
                }
            }
        }
    }

    public Object[] resolve(Method method) {
        return new Object[0];
    }
}
