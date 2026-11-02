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
package org.apache.bval.constraints;

import java.lang.annotation.Annotation;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Pattern.Flag;

import org.apache.bval.util.Validate;

public abstract class AbstractPatternValidator<A extends Annotation, T extends CharSequence>
    implements ConstraintValidator<A, T> {

    public interface PatternDescriptor {
        String regexp();

        Flag[] flags();
    }

    private final Function<A, PatternDescriptor> toDescriptor;

    protected Pattern pattern;

    /**
     * Whether {@link #pattern} is {@code .*} without flags, the default of {@code @Email}, which matches exactly the
     * values without a line terminator.
     */
    private boolean matchesAnyLine;

    protected AbstractPatternValidator(Function<A, PatternDescriptor> toDescriptor) {
        super();
        this.toDescriptor = Validate.notNull(toDescriptor);
    }

    @Override
    public void initialize(A constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);

        final PatternDescriptor pd = toDescriptor.apply(constraintAnnotation);

        final Flag flags[] = pd.flags();
        int intFlag = 0;
        for (Flag flag : flags) {
            intFlag = intFlag | flag.getValue();
        }
        try {
            pattern = Pattern.compile(pd.regexp(), intFlag);
            matchesAnyLine = intFlag == 0 && ".*".equals(pd.regexp());
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regular expression.", e);
        }
    }

    @Override
    public boolean isValid(T value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return matchesAnyLine ? !containsLineTerminator(value) : pattern.matcher(value).matches();
    }

    private static boolean containsLineTerminator(CharSequence value) {
        for (int i = 0, n = value.length(); i < n; i++) {
            switch (value.charAt(i)) {
            case '\n':
            case '\r':
            case '\u0085':
            case '\u2028':
            case '\u2029':
                return true;
            default:
                break;
            }
        }
        return false;
    }
}
