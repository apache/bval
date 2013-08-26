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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.bval.jsr.ApacheValidationProvider;
import org.apache.bval.jsr.ApacheValidatorConfiguration;
import org.apache.bval.jsr.ConfigurationImpl;
import org.apache.bval.jsr.example.XmlEntitySampleBean;
import org.apache.bval.jsr.resolver.SimpleTraversableResolver;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.util.Set;

/**
 * ValidationParser Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>11/25/2009</pre>
 */
public class ValidationParserTest extends TestCase
      implements ApacheValidatorConfiguration.Properties {
    public ValidationParserTest(String name) {
        super(name);
    }

    public void testGetInputStream() throws IOException {
        assertNotNull(ValidationParser.getInputStream("sample-validation.xml"));

        try {
            ValidationParser.getInputStream("META-INF/MANIFEST.MF"); // this is available in multiple jars hopefully
            fail("exception not thrown");
        } catch(ValidationException vex) {
            assertTrue(vex.getMessage().startsWith("More than "));
        }
    }

    public void testParse() {
        ConfigurationImpl config = new ConfigurationImpl(null, new ApacheValidationProvider());
        ValidationParser.processValidationConfig("sample-validation.xml", config, false);
    }

    public void testConfigureFromXml() {
        ValidatorFactory factory = getFactory();
        assertTrue(factory.getMessageInterpolator() instanceof TestMessageInterpolator);
        assertTrue(factory
              .getConstraintValidatorFactory() instanceof TestConstraintValidatorFactory);
        assertTrue(factory.getTraversableResolver() instanceof SimpleTraversableResolver);
        Validator validator = factory.getValidator();
        assertNotNull(validator);
    }

    private ValidatorFactory getFactory() {
        ApacheValidatorConfiguration config =
              Validation.byProvider(ApacheValidationProvider.class).configure();
        config.addProperty(VALIDATION_XML_PATH, "sample-validation.xml");
        return config.buildValidatorFactory();
    }

    public void testXmlEntitySample() {
        XmlEntitySampleBean bean = new XmlEntitySampleBean();
        bean.setFirstName("tooooooooooooooooooooooooooo long");
        bean.setValueCode("illegal");
        Validator validator = getFactory().getValidator();
        Set<ConstraintViolation<XmlEntitySampleBean>> results = validator.validate(bean);
        assertTrue(!results.isEmpty());
        assertTrue(results.size() == 3);

        bean.setZipCode("123");
        bean.setValueCode("20");
        bean.setFirstName("valid");
        results = validator.validate(bean);
        assertTrue(results.isEmpty());
    }

    public static Test suite() {
        return new TestSuite(ValidationParserTest.class);
    }
}
