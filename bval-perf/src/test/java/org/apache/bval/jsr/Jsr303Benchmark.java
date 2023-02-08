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

import org.apache.bval.jsr.example.Author;
import org.apache.bval.jsr.example.Book;
import org.apache.bval.jsr.example.First;
import org.apache.bval.jsr.example.Second;
import org.hibernate.validator.HibernateValidator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@State(Scope.Benchmark)
public class Jsr303Benchmark {

    private final ValidatorFactory bvalFactory =
        Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory();
    private final Validator bvalValidator = bvalFactory.getValidator();
    private final ValidatorFactory hibernateFactory =
        Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
    private final Validator hibernateValidator = hibernateFactory.getValidator();

    public static void main(String[] args) throws RunnerException {
        final Options opt = new OptionsBuilder()
            .include(Jsr303Benchmark.class.getSimpleName())

            .forks(1)
            .threads(10)

            .measurementIterations(1)
            .measurementTime(TimeValue.seconds(30))

            .warmupIterations(2)
            .warmupTime(TimeValue.seconds(10))

            .addProfiler(JavaFlightRecorderProfiler.class)

            .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void bvalNoConstraints() {
        final Set<ConstraintViolation<BookNoConstraints>> constraintViolations =
            bvalFactory.getValidator().validate(new BookNoConstraints());
        assertFalse(constraintViolations.iterator().hasNext());
    }

    @Benchmark
    public void bvalNoConstraintsReuseValidator() {
        final Set<ConstraintViolation<BookNoConstraints>> constraintViolations =
            bvalValidator.validate(new BookNoConstraints());
        assertFalse(constraintViolations.iterator().hasNext());
    }

    @Benchmark
    public void bvalConstraintsSuccess() {
        final Set<ConstraintViolation<BookSimple>> constraintViolations = bvalFactory.getValidator()
                                                                                     .validate(new BookSimple("Hello",
                                                                                                              "Awesome validation",
                                                                                                              3,
                                                                                                              76));
        assertFalse(constraintViolations.iterator().hasNext());
    }

    @Benchmark
    public void bvalConstraintsSuccessReuseValidator() {
        final Set<ConstraintViolation<BookSimple>> constraintViolations = bvalValidator
                                                                                     .validate(new BookSimple("Hello",
                                                                                                              "Awesome validation",
                                                                                                              3,
                                                                                                              76));
        assertFalse(constraintViolations.iterator().hasNext());
    }

    @Benchmark
    public void bvalConstraintsFailure() {
        final Set<ConstraintViolation<Book>> constraintViolations = bvalFactory.getValidator().validate(new Book());
        assertTrue(constraintViolations.iterator().hasNext());
    }

    @Benchmark
    public void bvalConstraintsFailureReuseValidator() {
        final Set<ConstraintViolation<Book>> constraintViolations = bvalValidator.validate(new Book());
        assertTrue(constraintViolations.iterator().hasNext());
    }

    @Benchmark
    public void hibernateNoConstraints() {
        final Set<ConstraintViolation<BookNoConstraints>> constraintViolations =
            hibernateFactory.getValidator().validate(new BookNoConstraints());
        assertFalse(constraintViolations.iterator().hasNext());
    }

    @Benchmark
    public void hibernateNoConstraintsReuseValidator() {
        final Set<ConstraintViolation<BookNoConstraints>> constraintViolations =
            hibernateValidator.validate(new BookNoConstraints());
        assertFalse(constraintViolations.iterator().hasNext());
    }

    @Benchmark
    public void hibernateConstraintsSuccess() {
        final Set<ConstraintViolation<BookSimple>> constraintViolations = hibernateFactory.getValidator()
                                                                                          .validate(
                                                                                              new BookSimple("Hello",
                                                                                                             "Awesome" +
                                                                                                             " validation",
                                                                                                             3,
                                                                                                             76));
        assertFalse(constraintViolations.iterator().hasNext());
    }

    @Benchmark
    public void hibernateConstraintsSuccessReuseValidator() {
        final Set<ConstraintViolation<BookSimple>> constraintViolations = hibernateValidator
                                                                                          .validate(
                                                                                              new BookSimple("Hello",
                                                                                                             "Awesome" +
                                                                                                             " validation",
                                                                                                             3,
                                                                                                             76));
        assertFalse(constraintViolations.iterator().hasNext());
    }

    @Benchmark
    public void hibernateConstraintsFailure() {
        final Set<ConstraintViolation<Book>> constraintViolations =
            hibernateFactory.getValidator().validate(new Book());
        assertTrue(constraintViolations.iterator().hasNext());
    }

    @Benchmark
    public void hibernateConstraintsFailureReuseValidator() {
        final Set<ConstraintViolation<Book>> constraintViolations =
            hibernateValidator.validate(new Book());
        assertTrue(constraintViolations.iterator().hasNext());
    }


    public static class BookNoConstraints {
        private String title;
        private String subtitle;
        private Author author;
        private int uselessField;
        private int unconstraintField;

        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title = title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(final String subtitle) {
            this.subtitle = subtitle;
        }

        public Author getAuthor() {
            return author;
        }

        public void setAuthor(final Author author) {
            this.author = author;
        }

        public int getUselessField() {
            return uselessField;
        }

        public void setUselessField(final int uselessField) {
            this.uselessField = uselessField;
        }

        public int getUnconstraintField() {
            return unconstraintField;
        }

        public void setUnconstraintField(final int unconstraintField) {
            this.unconstraintField = unconstraintField;
        }
    }

    public static class BookSimple {

        @NotEmpty(groups = First.class)
        private String title;

        @Size(max = 30, groups = Second.class)
        private String subtitle;

        @NotNull
        private int uselessField;

        private int unconstraintField;

        public BookSimple(
            final String title,
            final String subtitle,
            final int uselessField,
            final int unconstraintField) {

            this.title = title;
            this.subtitle = subtitle;
            this.uselessField = uselessField;
            this.unconstraintField = unconstraintField;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title = title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(final String subtitle) {
            this.subtitle = subtitle;
        }

        public int getUselessField() {
            return uselessField;
        }

        public void setUselessField(final int uselessField) {
            this.uselessField = uselessField;
        }

        public int getUnconstraintField() {
            return unconstraintField;
        }

        public void setUnconstraintField(final int unconstraintField) {
            this.unconstraintField = unconstraintField;
        }
    }

}
