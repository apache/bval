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
package org.apache.bval.jsr;

import javax.validation.Configuration;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ConfigurationState;

/**
 * Description: Uniquely identify Apache BVal in the Bean Validation bootstrap
 * strategy. Also contains Apache BVal specific configurations<br/>
 */
public interface ApacheValidatorConfiguration extends Configuration<ApacheValidatorConfiguration> {

    /**
     * Proprietary property keys for {@link ConfigurationImpl}  
     */
    interface Properties {
        /**
         * the location where to look for the validation.xml file.
         * default: "META-INF/validation.xml"
         */
        String VALIDATION_XML_PATH = "apache.bval.validation-xml-path";

        /**
         * Specifies the classname of the {@link ValidatorFactory} to use: this
         * class is presumed have a constructor that accepts a single
         * {@link ConfigurationState} argument.
         */
        String VALIDATOR_FACTORY_CLASSNAME = "apache.bval.validator-factory-classname";

        /**
         * Specifies whether EL evaluation is permitted in non-default message
         * templates. By default this feature is disabled; if you enable it you
         * should ensure that no constraint validator builds violations using
         * message templates containing unchecked text (e.g. the validated
         * value). To do otherwise is to expose your system to potential
         * injection attacks.
         */
        String CUSTOM_TEMPLATE_EXPRESSION_EVALUATION = "apache.bval.custom-template-expression-evaluation";
    }
}
