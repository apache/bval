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

import org.apache.bval.model.Features;

/**
 * Description: Contains MetaBean feature keys of additional features used in the implementation
 * of JSR303<br/>
 *
 * @see org.apache.bval.model.FeaturesCapable
 * @see org.apache.bval.model.Features
 */
public interface JsrFeatures {
    interface Method extends Features.Property {
        String MethodDescriptor = "MethodDescriptor";
    }

    interface Constructor extends Features.Property {
        String ConstructorDescriptor = "ConstructorDescriptor";
    }

    /**
     * JSR303 Property features
     */
    interface Property extends Features.Property {
        /** INFO: cached PropertyDescriptorImpl of the property */
        String PropertyDescriptor = "PropertyDescriptor";
        /**
         * INFO: Class[] with the groups to validate a REF_CASCADE
         */
        String REF_GROUPS = "refGroups";

        // Collection<Annotation>
        String ANNOTATIONS_TO_PROCESS = "annotationToProcess";
    }

    /**
     * JSR303 bean features
     */
    interface Bean extends Features.Bean {
        /**
         * INFO: List of Group(Class) for {@link javax.validation.GroupSequence#value()}
         * (redefined default group)
         **/
        String GROUP_SEQUENCE = "GroupSequence";

        /**
         * INFO: cached BeanDescriptorImpl of the bean
         */
        String BEAN_DESCRIPTOR = "BeanDescriptor";
    }
}
