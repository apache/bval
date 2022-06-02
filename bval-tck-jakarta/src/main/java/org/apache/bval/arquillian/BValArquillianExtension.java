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
package org.apache.bval.arquillian;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

public class BValArquillianExtension implements LoadableExtension {
    public void register(final ExtensionBuilder builder) {
        builder.service(TestEnricher.class, EJBEnricher.class).observer(TestLogger.class);
    }

    public static class TestLogger {

        private static final Logger LOGGER = Logger.getLogger(TestLogger.class.getName());
        private static final AtomicInteger COUNTER = new AtomicInteger(1);

        public void before(@Observes final BeforeClass beforeClass) {
            LOGGER.info(() -> COUNTER.getAndIncrement() + "/Launching " + toName(beforeClass.getTestClass()));
        }

        public void after(@Observes final AfterClass beforeClass) {
            LOGGER.info(() -> "Executed " + toName(beforeClass.getTestClass()));
        }

        private String toName(final TestClass testClass) {
            return testClass.getJavaClass()
                            .getName()
                            .replace("org.hibernate.beanvalidation.tck.tests.", "o.h.b.t.t.");
        }
    }
}
