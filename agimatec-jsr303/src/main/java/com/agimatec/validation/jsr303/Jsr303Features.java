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
package com.agimatec.validation.jsr303;

import com.agimatec.validation.model.Features;

/**
 * Description: Contains MetaBean feature keys of additional features used in the implementation
 * of JSR303<br/>
 * User: roman.stumm <br/>
 * Date: 02.04.2008 <br/>
 * Time: 15:22:49 <br/>
 * Copyright: Agimatec GmbH 2008
 *
 * @see com.agimatec.validation.model.FeaturesCapable
 * @see com.agimatec.validation.model.Features
 */
public interface Jsr303Features {
    interface Property extends Features.Property {
        /** INFO: cached PropertyDescriptorImpl of the property */
        String PropertyDescriptor = "PropertyDescriptor";
    }

    interface Bean extends Features.Bean {
        /**
         * INFO: List of Group(Class) for {@link javax.validation.GroupSequence#value()}
         * (redefined default group)
         **/
        String GROUP_SEQUENCE = "GroupSequence";

        /** INFO: cached sortied Array with ValidationEntries */
        String VALIDATION_SEQUENCE = "ValidationSequence";

        /**
         * INFO: cached BeanDescriptorImpl of the bean
         */
        String BEAN_DESCRIPTOR = "BeanDescriptor";
    }
}
