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
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.IntPredicate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public abstract class TimeValidator<A extends Annotation, T> implements ConstraintValidator<A, T> {
    protected static final Comparator<ChronoLocalDate> CHRONO_LOCAL_DATE_COMPARATOR =
        Comparator.nullsFirst((quid, quo) -> quid.isBefore(quo) ? -1 : quid.isAfter(quo) ? 1 : 0);

    protected static final Comparator<ChronoLocalDateTime<?>> CHRONO_LOCAL_DATE_TIME_COMPARATOR =
            Comparator.nullsFirst((quid, quo) -> quid.isBefore(quo) ? -1 : quid.isAfter(quo) ? 1 : 0);

    protected static final Comparator<ChronoZonedDateTime<?>> CHRONO_ZONED_DATE_TIME_COMPARATOR =
            Comparator.nullsFirst((quid, quo) -> quid.isBefore(quo) ? -1 : quid.isAfter(quo) ? 1 : 0);

    private final Function<Clock, T> now;
    private final Comparator<? super T> cmp;
    private final IntPredicate test;

    @SuppressWarnings("unchecked")
    protected TimeValidator(Function<Clock, T> now, IntPredicate test) {
        this(now, (Comparator<T>) Comparator.naturalOrder(), test);
    }

    protected TimeValidator(Function<Clock, T> now, Comparator<? super T> cmp,IntPredicate test) {
        super();
        this.now = now;
        this.cmp = cmp;
        this.test = test;
    }

    @Override
    public final boolean isValid(T value, ConstraintValidatorContext context) {
        return value == null || test.test(cmp.compare(value, now.apply(context.getClockProvider().getClock())));
    }
}
