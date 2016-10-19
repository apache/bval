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
package org.apache.bval.routines;

import org.apache.bval.model.Features;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.Validation;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;
import org.apache.bval.xml.XMLMetaValue;

import java.util.Collection;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.apache.bval.model.Features.Property.MANDATORY;
import static org.apache.bval.model.Features.Property.MAX_LENGTH;
import static org.apache.bval.model.Features.Property.MAX_VALUE;
import static org.apache.bval.model.Features.Property.MIN_LENGTH;
import static org.apache.bval.model.Features.Property.MIN_VALUE;
import static org.apache.bval.model.Features.Property.REG_EXP;
import static org.apache.bval.model.Features.Property.TIME_LAG;

/**
 * Description: This class implements the standard validations for properties!
 * You can subclass this class and replace the implementation
 * in the beanInfo-xml by providing it a validation "standard"<br/>
 */
public class StandardValidation implements Validation {

    /** key for this validation in the validation list of the beanInfos */
    public String getValidationId() {
        return "standard";
    }

    @Override
    public <T extends ValidationListener> void validate(ValidationContext<T> context) {
        validateMandatory(context);
        validateMaxLength(context);
        validateMinLength(context);
        validateMaxValue(context);
        validateMinValue(context);
        validateRegExp(context);
        validateTimeLag(context);
    }

    protected <T extends ValidationListener> void validateTimeLag(ValidationContext<T> context) {
        String lag = (String) context.getMetaProperty().getFeature(TIME_LAG);
        if (lag == null) return;
        if (context.getPropertyValue() == null) return;
        long date = ((Date) context.getPropertyValue()).getTime();
        long now = System.currentTimeMillis();
        if (XMLMetaValue.TIMELAG_Future.equals(lag)) {
            if (date < now) {
                context.getListener().addError(TIME_LAG, context);
            }
        } else if (XMLMetaValue.TIMELAG_Past.equals(lag)) {
            if (date > now) {
                context.getListener().addError(TIME_LAG, context);
            }
        } else {
            throw new IllegalArgumentException("unknown timelag " + lag + " at " + context);
        }
    }

    private static final String REG_EXP_PATTERN = "cachedRegExpPattern";

    protected <T extends ValidationListener> void validateRegExp(ValidationContext<T> context) {
        final MetaProperty meta = context.getMetaProperty();
        final String regExp = (String) meta.getFeature(REG_EXP);
        if (regExp == null) return;
        if (context.getPropertyValue() == null) return;

        final String value = String.valueOf(context.getPropertyValue());
        try {
            Pattern pattern = (Pattern) meta.getFeature(REG_EXP_PATTERN);
            if (pattern == null) {
                pattern = Pattern.compile(regExp);
                meta.putFeature(REG_EXP_PATTERN, pattern);
            }
            if (!pattern.matcher(value).matches()) {
                context.getListener().addError(REG_EXP, context);
            }
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException(
                  "regular expression malformed. regexp " + regExp + " at " + context, e);
        }
    }

    protected <T extends ValidationListener> void validateMinValue(ValidationContext<T> context) {
        @SuppressWarnings("unchecked")
        Comparable<Object> minValue = (Comparable<Object>) context.getMetaProperty().getFeature(MIN_VALUE);
        if (minValue == null || context.getPropertyValue() == null) return;
        if (compare(context, minValue, context.getPropertyValue()) > 0) {
            context.getListener().addError(MIN_VALUE, context);
        }
    }

    protected <T extends ValidationListener> void validateMaxValue(ValidationContext<T> context) {
        @SuppressWarnings("unchecked")
        Comparable<Object> maxValue = (Comparable<Object>) context.getMetaProperty().getFeature(MAX_VALUE);
        if (maxValue == null || context.getPropertyValue() == null) return;
        if (compare(context, maxValue, context.getPropertyValue()) < 0) {
            context.getListener().addError(MAX_VALUE, context);
        }
    }

    private <T extends ValidationListener> int compare(ValidationContext<T> context, Comparable<Object> constraintValue,
                        Object currentValue) {
        int r;
        if (constraintValue.getClass().isAssignableFrom(currentValue.getClass())) {
            r = constraintValue.compareTo(context.getPropertyValue());
        } else if (currentValue instanceof Number) {
            double dv = ((Number) currentValue).doubleValue();
            double mdv = ((Number) constraintValue).doubleValue();
            r = mdv > dv ? 1 : -1;
        } else {
            r = String.valueOf(constraintValue).compareTo(String.valueOf(currentValue));
        }
        return r;
    }

    protected <T extends ValidationListener> void validateMaxLength(ValidationContext<T> context) {
        Integer maxLength = (Integer) context.getMetaProperty()
              .getFeature(Features.Property.MAX_LENGTH);
        if (maxLength == null) return;
        if (context.getPropertyValue() == null) return;

        final Object value = context.getPropertyValue();
        int length = 0;
        if (value instanceof String) {
            length = ((String) value).length();
        } else if (value instanceof Collection<?>) {
            length = ((Collection<?>) value).size();
        }
        if (length > maxLength) {
            context.getListener().addError(MAX_LENGTH, context);
        }
    }

    protected <T extends ValidationListener> void validateMinLength(ValidationContext<T> context) {
        Integer maxLength = (Integer) context.getMetaProperty()
              .getFeature(Features.Property.MIN_LENGTH);
        if (maxLength == null) return;
        if (context.getPropertyValue() == null) return;

        final Object value = context.getPropertyValue();
        int length = 0;
        if (value instanceof String) {
            length = ((String) value).length();
        } else if (value instanceof Collection<?>) {
            length = ((Collection<?>) value).size();
        }
        if (length < maxLength) {
            context.getListener().addError(MIN_LENGTH, context);
        }
    }

    protected <T extends ValidationListener> void validateMandatory(ValidationContext<T> context) {
        if (context.getMetaProperty().isMandatory()) {
            if (context.getPropertyValue() == null) {
                context.getListener().addError(MANDATORY, context);
            }
        }
    }

    public static StandardValidation getInstance() {
        return new StandardValidation();
    }
}
