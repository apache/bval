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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.validator.HibernateValidator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.executable.ExecutableValidator;

/**
 * Validation scenarios run against each provider under identical conditions, selected by {@link #provider}. The
 * expected number of violations of every scenario is checked once per trial, so a provider cannot look fast by
 * skipping work.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ScenarioBenchmark {
    private static final int DRIVERS = 128; // power of two, see Cursor

    @Param({ "bval", "hibernate" })
    public String provider;

    private ValidatorFactory factory;
    private Validator validator;
    private ExecutableValidator executableValidator;

    private Driver[] drivers;
    private Object[][] driverArguments;
    private final DriverService driverService = new DriverService();
    private Method createDriver;
    private Method register;

    private Shop shop;
    private Shop cyclicShop;
    private Library library;
    private Node deepGraph;
    private Registration validRegistration;
    private Registration invalidRegistration;
    private Voucher validVoucher;
    private Voucher invalidVoucher;
    private Booking validBooking;
    private Booking invalidBooking;
    private Tag invalidTag;

    /**
     * Round-robin position in the pregenerated drivers, per thread.
     */
    @State(Scope.Thread)
    public static class Cursor {
        private int position;

        int next() {
            position = (position + 1) & (DRIVERS - 1);
            return position;
        }
    }

    @Setup(Level.Trial)
    public void setUp() throws NoSuchMethodException {
        switch (provider) {
        case "bval":
            factory = Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory();
            break;
        case "hibernate":
            factory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
            break;
        default:
            throw new IllegalArgumentException(provider);
        }
        validator = factory.getValidator();
        executableValidator = validator.forExecutables();
        createDriver = DriverService.class.getMethod("createDriver", String.class, int.class, boolean.class);
        register = DriverService.class.getMethod("register", Driver.class);

        final Random random = new Random(42);
        drivers = new Driver[DRIVERS];
        driverArguments = new Object[DRIVERS][];
        for (int i = 0; i < DRIVERS; i++) {
            drivers[i] = Driver.random(random);
            driverArguments[i] =
                new Object[] { drivers[i].name, Integer.valueOf(drivers[i].age), drivers[i].hasDrivingLicense };
        }
        shop = Shop.withArticles(1000, false);
        cyclicShop = Shop.withArticles(200, true);
        library = Library.create();
        deepGraph = Node.chain(8);
        validRegistration = Registration.valid();
        invalidRegistration = Registration.invalid();
        validVoucher = new Voucher("ABC-123");
        invalidVoucher = new Voucher("");
        validBooking = new Booking(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 8));
        invalidBooking = new Booking(LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 1));
        invalidTag = new Tag("abcdef");

        verifyExpectedViolations();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        factory.close();
    }

    private void verifyExpectedViolations() {
        for (int i = 0; i < DRIVERS; i++) {
            final Driver d = drivers[i];
            expect("validate(driver)", d.expectedDefault, validator.validate(d));
            expect("validate(driver, Strict)", d.expectedStrict, validator.validate(d, Strict.class));
            expect("validateProperty", d.age < 18 ? 1 : 0, validator.validateProperty(d, "age"));
            expect("validateValue", d.name == null || d.name.length() < 2 ? 1 : 0,
                validator.validateValue(Driver.class, "name", d.name));
            expect("validateParameters", d.expectedDefault,
                executableValidator.validateParameters(driverService, createDriver, driverArguments[i]));
            expect("validateReturnValue", d.expectedDefault,
                executableValidator.validateReturnValue(driverService, register, d));
        }
        expect("cascadedList", 0, validator.validate(shop));
        expect("cascadedCycles", 0, validator.validate(cyclicShop));
        expect("containerElements", 2, validator.validate(library));
        expect("deepGraph", 0, validator.validate(deepGraph));
        expect("wideBeanValid", 0, validator.validate(validRegistration));
        expect("wideBeanInvalid", 16, validator.validate(invalidRegistration));
        expect("composedValid", 0, validator.validate(validVoucher));
        expect("composedInvalid", 1, validator.validate(invalidVoucher));
        expect("classLevelValid", 0, validator.validate(validBooking));
        expect("classLevelInvalid", 1, validator.validate(invalidBooking));
        expect("elMessage", 1, validator.validate(invalidTag));
        final String message = validator.validate(invalidTag).iterator().next().getMessage();
        if (!"'abcdef' is longer than 3".equals(message)) {
            throw new IllegalStateException(provider + " elMessage: unexpected message " + message);
        }
    }

    private void expect(String scenario, int expected, Set<? extends ConstraintViolation<?>> violations) {
        if (violations.size() != expected) {
            throw new IllegalStateException(
                String.format("%s %s: expected %d violations, got %s", provider, scenario, expected, violations));
        }
    }

    // --- benchmarks ---

    /** Mix of valid and invalid simple beans, as a stream of requests would bring. */
    @Benchmark
    public Set<ConstraintViolation<Driver>> mixedDrivers(Cursor cursor) {
        return validator.validate(drivers[cursor.next()]);
    }

    /** Validation of an explicitly requested, non-default group. */
    @Benchmark
    public Set<ConstraintViolation<Driver>> explicitGroup(Cursor cursor) {
        return validator.validate(drivers[cursor.next()], Strict.class);
    }

    @Benchmark
    public Set<ConstraintViolation<Driver>> validateProperty(Cursor cursor) {
        return validator.validateProperty(drivers[cursor.next()], "age");
    }

    @Benchmark
    public Set<ConstraintViolation<Driver>> validateValue(Cursor cursor) {
        return validator.validateValue(Driver.class, "name", drivers[cursor.next()].name);
    }

    @Benchmark
    public Set<ConstraintViolation<DriverService>> executableParameters(Cursor cursor) {
        return executableValidator.validateParameters(driverService, createDriver, driverArguments[cursor.next()]);
    }

    /** Return value validation cascading into the returned bean. */
    @Benchmark
    public Set<ConstraintViolation<DriverService>> executableReturnValue(Cursor cursor) {
        return executableValidator.validateReturnValue(driverService, register, drivers[cursor.next()]);
    }

    /** Cascading into a list of 1000 valid elements. */
    @Benchmark
    public Set<ConstraintViolation<Shop>> cascadedList() {
        return validator.validate(shop);
    }

    /** Cascading into 200 elements that each refer back to their container. */
    @Benchmark
    public Set<ConstraintViolation<Shop>> cascadedCycles() {
        return validator.validate(cyclicShop);
    }

    /** Constraints on map keys, list elements and an Optional, with cascading through them. */
    @Benchmark
    public Set<ConstraintViolation<Library>> containerElements() {
        return validator.validate(library);
    }

    /** A chain of eight cascaded beans. */
    @Benchmark
    public Set<ConstraintViolation<Node>> deepGraph() {
        return validator.validate(deepGraph);
    }

    /** A bean with many different built-in constraints, all satisfied. */
    @Benchmark
    public Set<ConstraintViolation<Registration>> wideBeanValid() {
        return validator.validate(validRegistration);
    }

    /** The same bean with every constraint violated, so 16 messages are interpolated. */
    @Benchmark
    public Set<ConstraintViolation<Registration>> wideBeanInvalid() {
        return validator.validate(invalidRegistration);
    }

    @Benchmark
    public Set<ConstraintViolation<Voucher>> composedValid() {
        return validator.validate(validVoucher);
    }

    /** A failing composed constraint reported as a single violation. */
    @Benchmark
    public Set<ConstraintViolation<Voucher>> composedInvalid() {
        return validator.validate(invalidVoucher);
    }

    @Benchmark
    public Set<ConstraintViolation<Booking>> classLevelValid() {
        return validator.validate(validBooking);
    }

    /** A failing class-level constraint that reports a custom violation on a property node. */
    @Benchmark
    public Set<ConstraintViolation<Booking>> classLevelInvalid() {
        return validator.validate(invalidBooking);
    }

    /** A failing constraint whose message uses an EL expression. */
    @Benchmark
    public Set<ConstraintViolation<Tag>> elMessage() {
        return validator.validate(invalidTag);
    }

    // --- model ---

    public interface Strict {
    }

    public static class Driver {
        private static final String[] NAMES = { null, "Jacob", "Isabella", "Ethan", "Sophia", "Michael", "Emma", "J" };

        @NotNull
        @Size(min = 2, max = 30)
        private final String name;

        @Min(18)
        private final int age;

        @AssertTrue
        private final boolean hasDrivingLicense;

        @NotBlank(groups = Strict.class)
        @Email(groups = Strict.class)
        private final String email;

        final int expectedDefault;
        final int expectedStrict;

        Driver(String name, int age, boolean hasDrivingLicense, String email) {
            this.name = name;
            this.age = age;
            this.hasDrivingLicense = hasDrivingLicense;
            this.email = email;
            this.expectedDefault =
                (name == null || name.length() < 2 ? 1 : 0) + (age < 18 ? 1 : 0) + (hasDrivingLicense ? 0 : 1);
            this.expectedStrict = email.contains("@") ? 0 : 1;
        }

        static Driver random(Random random) {
            return new Driver(NAMES[random.nextInt(NAMES.length)], random.nextInt(40), random.nextInt(4) != 0,
                random.nextInt(4) == 0 ? "not-an-email" : "driver@example.org");
        }
    }

    public static class DriverService {
        public Driver createDriver(@NotNull @Size(min = 2, max = 30) String name, @Min(18) int age,
            @AssertTrue boolean hasDrivingLicense) {
            return new Driver(name, age, hasDrivingLicense, "driver@example.org");
        }

        @Valid
        public Driver register(Driver driver) {
            return driver;
        }
    }

    public static class Shop {
        @NotNull
        private final Integer id;

        @NotNull
        private final List<@Valid @NotNull Article> articles = new ArrayList<>();

        Shop(Integer id) {
            this.id = id;
        }

        static Shop withArticles(int count, boolean linkBack) {
            final Shop shop = new Shop(1);
            for (int i = 0; i < count; i++) {
                final Article article = new Article(i, "article " + i);
                shop.articles.add(article);
                if (linkBack) {
                    article.shops.add(shop);
                }
            }
            return shop;
        }
    }

    public static class Article {
        @NotNull
        private final Integer id;

        @Size(min = 1, max = 50)
        private final String name;

        private final Set<@Valid Shop> shops = new HashSet<>();

        Article(Integer id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class Library {
        @NotNull
        private final Map<@NotBlank String, @NotNull @Size(max = 50) List<@NotNull @Valid Book>> shelves;

        private final Optional<@Valid Author> curator;

        Library(Map<String, List<Book>> shelves, Optional<Author> curator) {
            this.shelves = shelves;
            this.curator = curator;
        }

        /** 20 shelves of 10 books; one shelf has a blank name and one book has no pages. */
        static Library create() {
            final Map<String, List<Book>> shelves = new LinkedHashMap<>();
            for (int s = 0; s < 20; s++) {
                final List<Book> books = new ArrayList<>();
                for (int b = 0; b < 10; b++) {
                    books.add(new Book("book " + s + '/' + b, s == 3 && b == 7 ? 0 : 100 + b,
                        new Author("author " + b, "author" + b + "@example.org")));
                }
                shelves.put(s == 5 ? " " : "shelf " + s, books);
            }
            return new Library(shelves, Optional.of(new Author("curator", "curator@example.org")));
        }
    }

    public static class Book {
        @NotBlank
        private final String title;

        @Min(1)
        private final int pages;

        @Valid
        private final Author author;

        Book(String title, int pages, Author author) {
            this.title = title;
            this.pages = pages;
            this.author = author;
        }
    }

    public static class Author {
        @NotBlank
        private final String name;

        @Email
        private final String email;

        Author(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }

    public static class Node {
        @NotNull
        private final String label;

        @Min(0)
        private final int weight;

        @Valid
        private final Node child;

        Node(String label, int weight, Node child) {
            this.label = label;
            this.weight = weight;
            this.child = child;
        }

        static Node chain(int depth) {
            Node node = null;
            for (int i = depth; i > 0; i--) {
                node = new Node("node " + i, i, node);
            }
            return node;
        }
    }

    public static class Registration {
        @NotBlank
        @Size(max = 40)
        private String firstName;

        @NotBlank
        @Size(max = 40)
        private String lastName;

        @NotNull
        @Email
        private String email;

        @Pattern(regexp = "\\+?[0-9 ]{6,20}")
        private String phone;

        @Min(0)
        @Max(150)
        private int age;

        @Past
        private LocalDate birthDate;

        @Future
        private LocalDate membershipEnd;

        @Digits(integer = 6, fraction = 2)
        private BigDecimal balance;

        @DecimalMin("0.00")
        private BigDecimal credit;

        @Size(min = 1, max = 5)
        private List<String> tags;

        @NotEmpty
        private String country;

        @Pattern(regexp = "[0-9]{5}")
        private String zip;

        @Positive
        private int loginCount;

        @PositiveOrZero
        private long points;

        @AssertFalse
        private boolean banned;

        @Null
        private String internalNote;

        static Registration valid() {
            final Registration r = new Registration();
            r.firstName = "Ada";
            r.lastName = "Lovelace";
            r.email = "ada@example.org";
            r.phone = "+44 20 7946 0000";
            r.age = 36;
            r.birthDate = LocalDate.now().minusYears(36);
            r.membershipEnd = LocalDate.now().plusYears(1);
            r.balance = new BigDecimal("1234.50");
            r.credit = new BigDecimal("10.00");
            r.tags = Arrays.asList("math", "engines");
            r.country = "GB";
            r.zip = "12345";
            r.loginCount = 3;
            r.points = 0;
            return r;
        }

        /** Violates each of the 16 annotated properties exactly once. */
        static Registration invalid() {
            final Registration r = new Registration();
            r.firstName = "";
            r.lastName = "x".repeat(50);
            r.email = "nope";
            r.phone = "abc";
            r.age = 200;
            r.birthDate = LocalDate.now().plusYears(1);
            r.membershipEnd = LocalDate.now().minusYears(1);
            r.balance = new BigDecimal("1234567.123");
            r.credit = new BigDecimal("-1");
            r.tags = Collections.emptyList();
            r.country = "";
            r.zip = "12";
            r.loginCount = 0;
            r.points = -1;
            r.banned = true;
            r.internalNote = "x";
            return r;
        }
    }

    @Target({ FIELD, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Constraint(validatedBy = {})
    @ReportAsSingleViolation
    @NotNull
    @NotBlank
    @Size(min = 3, max = 16)
    @Pattern(regexp = "[A-Z0-9-]+")
    public @interface ValidCode {
        String message() default "invalid code";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class Voucher {
        @ValidCode
        private final String code;

        Voucher(String code) {
            this.code = code;
        }
    }

    @Target(TYPE)
    @Retention(RUNTIME)
    @Constraint(validatedBy = DateRangeValidator.class)
    public @interface DateRange {
        String message() default "end must not be before start";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class DateRangeValidator implements ConstraintValidator<DateRange, Booking> {
        @Override
        public boolean isValid(Booking booking, ConstraintValidatorContext context) {
            if (booking == null || booking.from == null || booking.to == null || !booking.to.isBefore(booking.from)) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("to").addConstraintViolation();
            return false;
        }
    }

    @DateRange
    public static class Booking {
        @NotNull
        private final LocalDate from;

        @NotNull
        private final LocalDate to;

        Booking(LocalDate from, LocalDate to) {
            this.from = from;
            this.to = to;
        }
    }

    public static class Tag {
        @Size(max = 3, message = "'${validatedValue}' is longer than {max}")
        private final String value;

        Tag(String value) {
            this.value = value;
        }
    }
}
