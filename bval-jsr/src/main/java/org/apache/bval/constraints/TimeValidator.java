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
import java.time.Clock;
import java.util.function.Function;
import java.util.function.IntPredicate;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public abstract class TimeValidator<A extends Annotation, T extends Comparable<T>> implements ConstraintValidator<A, T> {

    private final Function<Clock, T> now;
    private final IntPredicate test;
    
    protected TimeValidator(Function<Clock, T> now, IntPredicate test) {
        super();
        this.now = now;
        this.test = test;
    }

    @Override
    public final boolean isValid(T value, ConstraintValidatorContext context) {
        return value == null || test.test(value.compareTo(now.apply(context.getClockProvider().getClock())));
    }
}
