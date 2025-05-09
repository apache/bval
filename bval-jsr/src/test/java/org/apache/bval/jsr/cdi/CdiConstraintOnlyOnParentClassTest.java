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
package org.apache.bval.jsr.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import org.apache.bval.cdi.BValExtension;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;

import java.io.Serializable;

@RunWith(Arquillian.class)
public class CdiConstraintOnlyOnParentClassTest {
    @ClassRule
    public static ExternalResource allowMyServiceImplType = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            BValExtension.setAnnotatedTypeFilter(at -> at.getJavaClass() == LastGreetingService.class);
        }

        @Override
        protected void after() {
            BValExtension.setAnnotatedTypeFilter(BValExtension.DEFAULT_ANNOTATED_TYPE_FILTER);
        }
    };

    @Inject
    private GreetingService service;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(GreetingService.class, GreetingServiceImpl.class, IntermediateGreetingService.class, LastGreetingService.class);
    }

    @Test
    public void validationFail() {
        Assert.assertThrows(ConstraintViolationException.class, () -> service.greet(null));
    }

    public interface GreetingService {
        void greet(@NotNull String name);
    }

    public static class GreetingServiceImpl implements GreetingService {
        @Override
        public void greet(String name) {
        }
    }

    public static class IntermediateGreetingService extends GreetingServiceImpl {
    }

    @ApplicationScoped
    public static class LastGreetingService extends IntermediateGreetingService implements Serializable {
    }
}
