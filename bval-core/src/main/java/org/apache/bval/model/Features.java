/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval.model;

/**
 * Description: Contains key of common feature keys used by standard validators etc.
 * This DOES NOT MEAN that the list of property- or bean-features is closed. You can
 * put anything into the metabean as a feature and use it in your custom validators
 * and other classes that access your metabeans.<br/>
 *
 * @see FeaturesCapable
 */
public interface Features {
    /** Features of {@link MetaBean} */
    public interface Bean {
        /** INFO: String, name of the Property, that is the Primary Key */
        String MAIN_KEY = "mainKey";
        /** INFO: category/domain to which the metaBean belongs to */
        String DOMAIN = "domain";

        //        String DISPLAY_NAME = "displayName";
        String UNIQUE_KEY = "uniqueKey";
    }

    /** Features of {@link MetaProperty} */
    public interface Property {
        /** INFO: possible Enum values */
        String ENUM = "enum";
        /** INFO: Boolean, TRUE if Property is a Unique Key */
        String UNIQUE_KEY = "uniqueKey";
        /** VALIDATION: Boolean, mandatory field? */
        String MANDATORY = "mandatory";
        /** VALIDATION: Integer, max. number of chars/digits / max. cardinality of a to-many relationship */
        String MAX_LENGTH = "maxLen";
        /** VALIDATION: Comparable (e.g. a subclass of Number), max value */
        String MAX_VALUE = "maxValue";
        /** VALIDATION: Integer, min. number of chars/digits / min. cardinality of a to-many relationship */
        String MIN_LENGTH = "minLen";
        /** VALIDATION: Comparable (e.g. a subclass of Number), min value */
        String MIN_VALUE = "minValue";
        /** INFO: String-representation of a default value */
        String DEFAULT_VALUE = "defValue";
        /** SECURITY, INFO: Boolean, is value or relationship unmodifiable */
        String READONLY = "readonly";
        /**
         * SECURITY, INFO: Boolean, Field accessible?
         * If false, the field must not be displayed, queried, changed.
         */
        String DENIED = "denied";
        /** VALIDATION: String, regular expression to validate the format of input data */
        String REG_EXP = "regExp";
        /**
         * VALIDATION: String, Constraint for time-information of a Date-field:
         * {@link org.apache.bval.xml.XMLMetaValue#TIMELAG_Past}
         * or
         * {@link org.apache.bval.xml.XMLMetaValue#TIMELAG_Future}
         */
        String TIME_LAG = "timeLag";

        /**
         * INFO: Boolean, Field visible?
         *
         * @see java.beans.PropertyDescriptor#isHidden()
         */
        String HIDDEN = "hidden";
        /**
         * INFO: Boolean
         *
         * @see java.beans.PropertyDescriptor#isPreferred()
         */
        String PREFERRED = "preferred";

        /** INFO: relationship's target metaBean.id * */
        String REF_BEAN_ID = "refBeanId";

        /**
         * INFO: Class<br>
         * Relationship's target metaBean.beanClass.
         * In case of to-many relationships, this feature
         * hold the Bean-type not the Collection-type.
         */
        String REF_BEAN_TYPE = "refBeanType";

        /**
         * INFO: AccessStrategy[]<br>
         * an array of accessStrategies
         * how validation should cascade into relationship target beans<br>
         * null when validation should NOT cascade into relationship target
         * beans<br>
         * <p/>
         * Default: {PropertyAccess(metaProperty.name)},
         * when MetaProperty.metaBean is != null
         */
        String REF_CASCADE = "refCascade";

        /** INFO: an array with the string names of custom java script validation functions */
        @Deprecated // TODO RSt - I suggest to remove this and all related code
        String JAVASCRIPT_VALIDATION_FUNCTIONS = "jsFunctions";
    }
}
