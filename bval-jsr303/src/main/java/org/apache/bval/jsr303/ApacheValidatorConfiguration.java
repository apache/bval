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
package org.apache.bval.jsr303;

import javax.validation.Configuration;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ConfigurationState;

/**
 * Description: Uniquely identify Apache Bean Validation in the Bean Validation bootstrap
 * strategy. Also contains Apache Bean Validation specific configurations<br/>
 */
public interface ApacheValidatorConfiguration
      extends Configuration<ApacheValidatorConfiguration> {

    /**
     * Proprietary property keys for {@link ConfigurationImpl}  
     */
    public interface Properties {
        /**
         * the location where to look for the validation.xml file.
         * default: "META-INF/validation.xml"
         */
        String VALIDATION_XML_PATH = "apache.bval.validation-xml-path";

        /**
         * true/false. use Introspector (java beans) metadata additionally
         * to build metadata with JSR303.<br>
         * This means that all properties exist that are java-bean properties and
         * and that some features (Hidden, Readonly) are taken from Introspector
         * to create the meta data.<br>
         * default: false
         */
        String ENABLE_INTROSPECTOR = "apache.bval.enable-introspector";

        /**
         * true/false. use Apache metaBeans xml format additionally to
         * build metadata with JSR303.
         * default: false
         *
         * @deprecated we could decide to drop this feature in the future.
         * we keep it as long as we support both: jsr303 and xstream-xml meta data at
         * the same time (and potentially for the same domain classes)
         */
        String ENABLE_METABEANS_XML = "apache.bval.enable-metabeans-xml";

        /**
         * BeanValidator.treatMapsLikeBeans.
         * default: false
         */
        String TREAT_MAPS_LIKE_BEANS = "apache.bval.treat-maps-like-beans";

        /**
         * Specifies the classname of the {@link ValidatorFactory} to use: this
         * class is presumed have a constructor that accepts a single
         * {@link ConfigurationState} argument.
         */
         String VALIDATOR_FACTORY_CLASSNAME = "apache.bval.validator-factory-classname";
    }
}
