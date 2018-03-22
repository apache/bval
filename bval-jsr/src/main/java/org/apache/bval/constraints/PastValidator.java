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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.function.Function;
import java.util.function.IntPredicate;

import javax.validation.ConstraintValidator;
import javax.validation.constraints.Past;

/**
 * Defines built-in {@link ConstraintValidator} implementations for {@link Past}.
 *
 * @param <T>
 *            validated type
 */
public abstract class PastValidator<T extends Comparable<T>> extends TimeValidator<Past, T> {

    public static class ForDate extends PastValidator<Date> {

        public ForDate() {
            super(clock -> Date.from(clock.instant()));
        }
    }

    public static class ForCalendar extends PastValidator<Calendar> {

        public ForCalendar() {
            super(clock -> GregorianCalendar.from(clock.instant().atZone(clock.getZone())));
        }
    }

    public static class ForInstant extends PastValidator<Instant> {

        public ForInstant() {
            super(Instant::now);
        }
    }

    public static class ForChronoLocalDate extends PastValidator<ChronoLocalDate> {

        public ForChronoLocalDate() {
            super(LocalDate::now, CHRONO_LOCAL_DATE_COMPARATOR);
        }
    }

    public static class ForChronoLocalDateTime extends PastValidator<ChronoLocalDateTime<?>> {

        public ForChronoLocalDateTime() {
            super(LocalDateTime::now, CHRONO_LOCAL_DATE_TIME_COMPARATOR);
        }
    }

    public static class ForLocalTime extends PastValidator<LocalTime> {

        public ForLocalTime() {
            super(LocalTime::now);
        }
    }

    public static class ForOffsetDateTime extends PastValidator<OffsetDateTime> {

        public ForOffsetDateTime() {
            super(OffsetDateTime::now);
        }
    }

    public static class ForOffsetTime extends PastValidator<OffsetTime> {

        public ForOffsetTime() {
            super(OffsetTime::now);
        }
    }

    public static class ForChronoZonedDateTime extends PastValidator<ChronoZonedDateTime<?>> {

        public ForChronoZonedDateTime() {
            super(ZonedDateTime::now, PastValidator.CHRONO_ZONED_DATE_TIME_COMPARATOR);
        }
    }

    public static class ForMonthDay extends PastValidator<MonthDay> {

        public ForMonthDay() {
            super(MonthDay::now);
        }
    }

    public static class ForYear extends PastValidator<Year> {

        public ForYear() {
            super(Year::now);
        }
    }

    public static class ForYearMonth extends PastValidator<YearMonth> {

        public ForYearMonth() {
            super(YearMonth::now);
        }
    }

    private static final IntPredicate TEST = n -> n < 0;

    protected PastValidator(Function<Clock, T> now) {
        super(now, TEST);
    }

    protected PastValidator(Function<Clock, T> now, Comparator<? super T> cmp) {
        super(now, cmp, TEST);
    }
}
