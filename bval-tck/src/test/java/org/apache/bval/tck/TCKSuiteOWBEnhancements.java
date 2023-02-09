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
package org.apache.bval.tck;

import org.apache.webbeans.arquillian.standalone.OwbArquillianSingletonService;
import org.apache.webbeans.arquillian.standalone.OwbStandaloneConfiguration;
import org.apache.webbeans.arquillian.standalone.OwbStandaloneContainer;
import org.apache.webbeans.config.WebBeansFinder;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.core.spi.LoadableExtension;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;

public class TCKSuiteOWBEnhancements implements LoadableExtension {
    @Override
    public void register(final ExtensionBuilder extensionBuilder) {
        extensionBuilder.override(DeployableContainer.class, OwbStandaloneContainer.class, ExtendedOwbStandaloneContainer.class);
    }

    // testng suites handling looks buggy since it does not shutdown previous container when running another suite so this does not work with owb
    public static class ExtendedOwbStandaloneContainer extends OwbStandaloneContainer {
        @Override
        public void setup(final OwbStandaloneConfiguration owbStandaloneConfiguration) {
            final var singletonService = WebBeansFinder.getSingletonService();
            if (singletonService instanceof OwbArquillianSingletonService) {
                setParent("singletonService", (OwbArquillianSingletonService) singletonService, OwbArquillianSingletonService.class);
                setParent("useOnlyArchiveResources", true, boolean.class);
                setParent("useOnlyArchiveResourcesExcludes", List.of("META-INF/services/jakarta.validation.spi.ValidationProvider"), Collection.class);
                return;
            }
            super.setup(owbStandaloneConfiguration);
        }

        private <T> void setParent(final String field, final T value, final Class<T> type) {
            try {
                MethodHandles.privateLookupIn(OwbStandaloneContainer.class, MethodHandles.lookup())
                        .findSetter(OwbStandaloneContainer.class, field, type)
                        .bindTo(this)
                        .invoke(value);
            } catch (final Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
