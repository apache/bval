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

import org.apache.bval.model.Validation;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;

import java.util.regex.Pattern;

/**
 * Description: example validation for email addresses using a regular expression<br/>
 */
public class EMailValidation implements Validation {

    private java.util.regex.Pattern pattern = EMailValidationUtils.DEFAULT_EMAIL_PATTERN;

    @Override
    public <T extends ValidationListener> void validate(ValidationContext<T> context) {
        if (context.getPropertyValue() == null)
            return;
        if (!EMailValidationUtils.isValid(context.getPropertyValue(), getPattern())) {
            context.getListener().addError(Reasons.EMAIL_ADDRESS, context);
        }
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

}
