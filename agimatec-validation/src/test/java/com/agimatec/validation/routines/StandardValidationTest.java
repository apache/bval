/**
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
package com.agimatec.validation.routines;

import com.agimatec.validation.BeanValidationContext;
import com.agimatec.validation.model.Features;
import com.agimatec.validation.model.MetaProperty;
import com.agimatec.validation.model.ValidationContext;
import com.agimatec.validation.model.ValidationListener;
import com.agimatec.validation.xml.XMLMetaValue;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * StandardValidation Tester.
 *
 * @author ${USER}
 * @version 1.0
 * @since <pre>07/06/2007</pre>
 *        Copyright: Agimatec GmbH 2008
 */
public class StandardValidationTest extends TestCase implements ValidationListener {
    private StandardValidation validation;
    private BeanValidationContext context;
    private List<String> reasons = new ArrayList<String>();
    private MetaProperty metaProperty;
    private String stringValue;
    private Date dateValue;
    private int intValue;

    public StandardValidationTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        validation = new StandardValidation();
        context = new BeanValidationContext(this);
        metaProperty = new MetaProperty();
        context.setBean(this, null);
        context.setMetaProperty(metaProperty);
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public String getStringValue() {
        return stringValue;
    }

    public void testValidateMandatory() {
        metaProperty.setName("stringValue");

        // test not-null value that is mandatory
        metaProperty.setMandatory(true);
        stringValue = "some value";
        validation.validateMandatory(context);
        assertTrue(reasons.isEmpty());

        // test null value that is mandatory
        context.unknownValue();
        stringValue = null;
        validation.validateMandatory(context);
        assertTrue(reasons.contains(Reasons.MANDATORY));

        // test null value that is NOT mandatory
        context.unknownValue();
        metaProperty.setMandatory(false);
        reasons.clear();
        validation.validateMandatory(context);
        assertTrue(reasons.isEmpty());
    }

    public void testValidateMaxLength() {
        metaProperty.setName("stringValue");
        metaProperty.putFeature(Features.Property.MAX_LENGTH, 5);
        stringValue = "1234";
        validation.validateMaxLength(context);
        assertTrue(reasons.isEmpty());
        context.unknownValue();
        stringValue = "much too long";
        validation.validateMaxLength(context);
        assertTrue(reasons.contains(Reasons.MAX_LENGTH));
    }

    public void testValidateMinLength() {
        metaProperty.setName("stringValue");
        metaProperty.putFeature(Features.Property.MIN_LENGTH, 5);
        stringValue = "123456";
        validation.validateMinLength(context);
        assertTrue(reasons.isEmpty());
        context.unknownValue();
        stringValue = "123";
        validation.validateMinLength(context);
        assertTrue(reasons.contains(Reasons.MIN_LENGTH));
    }

    public void testValidateMaxValue() {
        metaProperty.setName("stringValue");
        metaProperty.putFeature(Features.Property.MAX_VALUE, "9999");
        stringValue = "1111";
        validation.validateMaxValue(context);
        assertTrue(reasons.isEmpty());
        context.unknownValue();
        stringValue = "99999";
        validation.validateMaxValue(context);
        assertTrue(reasons.contains(Reasons.MAX_VALUE));
    }

    public void testValidateMinValue() {
        metaProperty.setName("stringValue");
        metaProperty.putFeature(Features.Property.MIN_VALUE, "5555");
        stringValue = "8888";
        validation.validateMinValue(context);
        assertTrue(reasons.isEmpty());
        context.unknownValue();
        stringValue = "3333";
        validation.validateMinValue(context);
        assertTrue(reasons.contains(Reasons.MIN_VALUE));
    }

    public int getIntValue() {
        return intValue;
    }

    public void testValidateMinValue_MixedNumber() {
        metaProperty.setName("intValue");
        metaProperty.putFeature(Features.Property.MIN_VALUE, new Long(0));
        intValue = 5;
        validation.validateMinValue(context);
        assertTrue(reasons.isEmpty());
        context.unknownValue();
        intValue = -1;
        validation.validateMinValue(context);
        assertTrue(reasons.contains(Reasons.MIN_VALUE));
    }

    public void testValidateMinValue_Date_Timestamp() {
        metaProperty.setName("dateValue");
        Date dt = new Date();
        metaProperty.putFeature(Features.Property.MIN_VALUE, dt);
        dateValue = new Timestamp(dt.getTime()+1000);
        validation.validateMinValue(context);
        assertTrue(reasons.isEmpty());
        context.unknownValue();
        dateValue = new Timestamp(dt.getTime()-1000);
        validation.validateMinValue(context);
        assertTrue(reasons.contains(Reasons.MIN_VALUE));
    }

    public void testValidateMaxValue_AlphabeticString() {
        metaProperty.setName("stringValue");
        metaProperty.putFeature(Features.Property.MAX_VALUE, "BBBB");
        stringValue = "AAAA";
        validation.validateMaxValue(context);
        assertTrue(reasons.isEmpty());
        context.unknownValue();
        stringValue = "BBBC";
        validation.validateMaxValue(context);
        assertTrue(reasons.contains(Reasons.MAX_VALUE));
    }

    public void testValidateRegExp() {
        // regexp for Zip
        String regexp = "[a-zA-Z\\- \\d]*";
        metaProperty.setName("stringValue");
        metaProperty.putFeature(Features.Property.REG_EXP, regexp);
        stringValue = "53773";
        validation.validateRegExp(context);
        assertTrue(reasons.isEmpty());
        context.unknownValue();
        stringValue = "5355/7"; // invalid zip value
        validation.validateRegExp(context);
        assertTrue(reasons.contains(Reasons.REG_EXP));
    }

    public Date getDateValue() {
        return dateValue;
    }

    public void testValidateTimeLag() {
        metaProperty.setName("dateValue");
        metaProperty.putFeature(Features.Property.TIME_LAG, XMLMetaValue.TIMELAG_Past);

        dateValue = new Date(System.currentTimeMillis() - 10000);
        validation.validateTimeLag(context);
        assertTrue(reasons.isEmpty());

        metaProperty.putFeature(Features.Property.TIME_LAG, XMLMetaValue.TIMELAG_Future);
        validation.validateTimeLag(context);
        assertTrue(reasons.contains(Reasons.TIME_LAG));

    }

    public static Test suite() {
        return new TestSuite(StandardValidationTest.class);
    }

    public void addError(String reason, ValidationContext context) {
        reasons.add(reason);
    }

    public void addError(ValidationListener.Error error, ValidationContext context) {
        reasons.add(error.getReason());
    }

}
