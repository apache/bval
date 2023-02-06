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
package org.apache.bval.jsr.xml;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import jakarta.validation.BootstrapConfiguration;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.apache.bval.jsr.ApacheValidationProvider;
import org.apache.bval.jsr.ApacheValidatorConfiguration;
import org.apache.bval.jsr.ConfigurationImpl;
import org.apache.bval.jsr.example.XmlEntitySampleBean;
import org.apache.bval.jsr.resolver.SimpleTraversableResolver;
import org.apache.bval.util.reflection.Reflection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * ValidationParser Tester.
 */
public class ValidationParserTest implements ApacheValidatorConfiguration.Properties {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ValidationParser validationParser;

    @Before
    public void setup() {
        validationParser = new ValidationParser(Reflection.loaderFromThreadOrClass(ValidationParserTest.class));
    }

    @Test
    public void testGetInputStream() throws IOException {
        assertNotNull(validationParser.getInputStream("sample-validation.xml"));

        // make sure there are duplicate resources on the classpath before the next checks:
        final Enumeration<URL> resources =
            Reflection.loaderFromClassOrThread(ValidationParser.class).getResources("META-INF/MANIFEST.MF");

        assumeTrue(resources.hasMoreElements());
        resources.nextElement();
        assumeTrue(resources.hasMoreElements());
    }

    @Test
    public void testGetNonUniqueInputStream() throws IOException {
        thrown.expect(ValidationException.class);
        thrown.expectMessage("More than ");
        validationParser.getInputStream("META-INF/MANIFEST.MF"); // this is available in multiple jars hopefully
    }

    @Test
    public void testParse() {
        ConfigurationImpl config = new ConfigurationImpl(null, new ApacheValidationProvider());
        final BootstrapConfiguration configuration =
            validationParser.processValidationConfig("sample-validation.xml", config);
        assertEquals("org.apache.bval.jsr.xml.TestMessageInterpolator", configuration.getMessageInterpolatorClassName());
    }

    @Test
    public void testParseV11() {
        ConfigurationImpl config = new ConfigurationImpl(null, new ApacheValidationProvider());
        final BootstrapConfiguration configuration =
            validationParser.processValidationConfig("sample-validation11.xml", config);
        assertEquals("org.apache.bval.jsr.xml.TestMessageInterpolator", configuration.getMessageInterpolatorClassName());

    }

    @Test
    public void testParseV20() {
        ConfigurationImpl config = new ConfigurationImpl(null, new ApacheValidationProvider());
        final BootstrapConfiguration configuration =
            validationParser.processValidationConfig("sample-validation2.xml", config);
        assertEquals("org.apache.bval.jsr.xml.TestMessageInterpolator", configuration.getMessageInterpolatorClassName());
    }

    @Test
    public void testParseV30() {
        ConfigurationImpl config = new ConfigurationImpl(null, new ApacheValidationProvider());
        final BootstrapConfiguration configuration =
            validationParser.processValidationConfig("sample-validation3.xml", config);
        assertEquals("org.apache.bval.jsr.xml.TestMessageInterpolator", configuration.getMessageInterpolatorClassName());
    }

    @Test
    public void testConfigureFromXml() {
        ValidatorFactory factory = getFactory();
        assertThat(factory.getMessageInterpolator(), instanceOf(TestMessageInterpolator.class));
        assertThat(factory.getConstraintValidatorFactory(), instanceOf(TestConstraintValidatorFactory.class));
        assertThat(factory.getTraversableResolver(), instanceOf(SimpleTraversableResolver.class));
        assertNotNull(factory.getValidator());
    }

    private ValidatorFactory getFactory() {
        ApacheValidatorConfiguration config = Validation.byProvider(ApacheValidationProvider.class).configure();
        config.addProperty(VALIDATION_XML_PATH, "sample-validation.xml");
        return config.buildValidatorFactory();
    }

    @Test
    public void testXmlEntitySample() {
        XmlEntitySampleBean bean = new XmlEntitySampleBean();
        bean.setFirstName("tooooooooooooooooooooooooooo long");
        bean.setValueCode("illegal");
        Validator validator = getFactory().getValidator();
        Set<ConstraintViolation<XmlEntitySampleBean>> results = validator.validate(bean);
        assertFalse(results.isEmpty());
        assertEquals(3, results.size());

        bean.setZipCode("123");
        bean.setValueCode("20");
        bean.setFirstName("valid");
        results = validator.validate(bean);
        assertTrue(results.isEmpty());
    }

}
