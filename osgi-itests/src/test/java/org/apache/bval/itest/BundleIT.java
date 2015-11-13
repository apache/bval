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

package org.apache.bval.itest;

import org.apache.bval.jsr.ApacheValidationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import javax.validation.Validation;
import javax.validation.ValidationProviderResolver;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ValidationProvider;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BundleIT {
    @Configuration
    public Option[] config() {
        return options(
                mavenBundle("org.apache.bval", "org.apache.bval.bundle").versionAsInProject(),
                mavenBundle("javax.validation", "validation-api").versionAsInProject(),
                mavenBundle("javax.el", "javax.el-api").versionAsInProject(),
                mavenBundle("org.glassfish.web", "javax.el").versionAsInProject(),
                mavenBundle("commons-validator", "commons-validator").versionAsInProject(),
                mavenBundle("commons-beanutils", "commons-beanutils").versionAsInProject(),
                mavenBundle("commons-digester", "commons-digester").versionAsInProject(),
                mavenBundle("commons-logging", "commons-logging").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-weaver-privilizer-api").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                mavenBundle("commons-collections", "commons-collections").versionAsInProject(),
                junitBundles(),
                systemPackages("javax.annotation"),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO")
        );
    }

    public final class OSGIValidationFactory {
        private OSGIValidationFactory() {
            //
        }

        class OSGIServiceDiscoverer implements ValidationProviderResolver {

            @Override
            public List<ValidationProvider<?>> getValidationProviders() {
                List<ValidationProvider<?>> providers = new ArrayList<ValidationProvider<?>>();
                providers.add(new ApacheValidationProvider());
                return providers;
            }
        }

        public ValidatorFactory newValidatorFactory() {
            javax.validation.Configuration<?> config = Validation.byDefaultProvider()
                    .providerResolver(new OSGIServiceDiscoverer())
                    .configure();

            return config.buildValidatorFactory();
        }
    }

    @Test
    public void validateSomething() throws Exception {
        Validator validator = new OSGIValidationFactory().newValidatorFactory().getValidator();

        Customer customer = new Customer();
        customer.setCustomerId("id-1");
        customer.setFirstName("Mary");
        customer.setLastName("Do");

        assertEquals(0, validator.validate(customer).size());

        customer.setEmailAddress("some@invalid@address");
        assertEquals(1, validator.validate(customer).size());

        customer.setEmailAddress("some.valid-012345@address_at-test.org");
        assertEquals(0, validator.validate(customer).size());
    }
}
