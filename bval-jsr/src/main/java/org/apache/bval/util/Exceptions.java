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
package org.apache.bval.util;

import java.lang.reflect.InvocationTargetException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Utility class for sundry {@link Exception}-related tasks.
 */
public class Exceptions {
    /**
     * Callback interface that collects format arguments in conditional raise* method variants.
     * @see Exceptions#raiseIf(boolean, Function, String, Consumer)
     * @see Exceptions#raiseIf(boolean, BiFunction, Throwable, String, Consumer)
     * @see Exceptions#raiseUnless(boolean, Function, String, Consumer)
     * @see Exceptions#raiseUnless(boolean, BiFunction, Throwable, String, Consumer)
     */
    @FunctionalInterface
    public interface FormatArgs {
        void args(Object... args);
    }

    public static <E extends Exception> E create(Function<? super String, ? extends E> fn, String format,
        Object... args) {
        return create(fn, () -> String.format(format, args));
    }

    public static <E extends Exception, C extends Throwable> E create(
        BiFunction<? super String, ? super C, ? extends E> fn, C cause, String format, Object... args) {
        return create(fn, cause, () -> String.format(format, args));
    }

    public static <E extends Exception> E create(Function<? super String, ? extends E> fn, Supplier<String> message) {
        return elideStackTrace(fn.apply(message.get()));
    }

    public static <E extends Exception, C extends Throwable> E create(
        BiFunction<? super String, ? super C, ? extends E> fn, C cause, Supplier<String> message) {
        return elideStackTrace(fn.apply(message.get(), cause));
    }

    public static <E extends Exception, R> R raise(Function<? super String, ? extends E> fn, String format,
        Object... args) throws E {
        throw create(fn, format, args);
    }

    public static <E extends Exception> void raiseIf(boolean condition, Function<? super String, ? extends E> fn,
        String format, Object... args) throws E {
        if (condition) {
            raise(fn, format, args);
        }
    }

    public static <E extends Exception> void raiseUnless(boolean condition, Function<? super String, ? extends E> fn,
        String format, Object... args) throws E {
        raiseIf(!condition, fn, format, args);
    }

    public static <E extends Exception, R> R raise(Function<? super String, ? extends E> fn, Supplier<String> message)
        throws E {
        throw create(fn, message);
    }

    public static <E extends Exception> void raiseIf(boolean condition, Function<? super String, ? extends E> fn,
        String format, Consumer<FormatArgs> argsProvider) throws E {
        if (condition) {
            raise(fn, message(format, argsProvider));
        }
    }

    public static <E extends Exception> void raiseIf(boolean condition, Function<? super String, ? extends E> fn,
        Supplier<String> message) throws E {
        if (condition) {
            raise(fn, message);
        }
    }

    public static <E extends Exception> void raiseUnless(boolean condition, Function<? super String, ? extends E> fn,
        String format, Consumer<FormatArgs> argsProvider) throws E {
        raiseIf(!condition, fn, format, argsProvider);
    }

    public static <E extends Exception> void raiseUnless(boolean condition, Function<? super String, ? extends E> fn,
        Supplier<String> message) throws E {
        raiseIf(!condition, fn, message);
    }

    public static <E extends Exception, C extends Throwable, R> R raise(
        BiFunction<? super String, ? super C, ? extends E> fn, C cause, String format, Object... args) throws E {
        throw create(fn, cause, format, args);
    }

    public static <E extends Exception, C extends Throwable> void raiseIf(boolean condition,
        BiFunction<? super String, ? super C, ? extends E> fn, C cause, String format, Object... args) throws E {
        if (condition) {
            raise(fn, cause, format, args);
        }
    }

    public static <E extends Exception, C extends Throwable> void raiseUnless(boolean condition,
        BiFunction<? super String, ? super C, ? extends E> fn, C cause, String format, Object... args) throws E {
        raiseIf(!condition, fn, cause, format, args);
    }

    public static <E extends Exception, C extends Throwable, R> R raise(
        BiFunction<? super String, ? super C, ? extends E> fn, C cause, Supplier<String> message) throws E {
        throw create(fn, cause, message);
    }

    public static <E extends Exception, C extends Throwable> void raiseIf(boolean condition,
        BiFunction<? super String, ? super C, ? extends E> fn, C cause, String format,
        Consumer<FormatArgs> argsProvider) throws E {
        if (condition) {
            raise(fn, cause, message(format, argsProvider));
        }
    }

    public static <E extends Exception, C extends Throwable> void raiseIf(boolean condition,
        BiFunction<? super String, ? super C, ? extends E> fn, C cause, Supplier<String> message) throws E {
        if (condition) {
            raise(fn, cause, message);
        }
    }

    public static <E extends Exception, C extends Throwable> void raiseUnless(boolean condition,
        BiFunction<? super String, ? super C, ? extends E> fn, C cause, String format,
        Consumer<FormatArgs> argsProvider) throws E {
        raiseIf(!condition, fn, cause, message(format, argsProvider));
    }

    public static <E extends Exception, C extends Throwable> void raiseUnless(boolean condition,
        BiFunction<? super String, ? super C, ? extends E> fn, C cause, Supplier<String> message) throws E {
        raiseIf(!condition, fn, cause, message);
    }

    /**
     * Extract cause from {@link InvocationTargetException}s.
     * @param t to unwrap
     * @return first of t, cause hierarchy not instanceof {@link InvocationTargetException}
     */
    public static Throwable causeOf(Throwable t) {
        while (t instanceof InvocationTargetException) {
            t = t.getCause();
        }
        return t;
    }

    private static <T extends Throwable> T elideStackTrace(T t) {
        final StackTraceElement[] stackTrace = t.fillInStackTrace().getStackTrace();
        t.setStackTrace(Stream.of(stackTrace).filter(e -> !Exceptions.class.getName().equals(e.getClassName()))
            .toArray(StackTraceElement[]::new));
        return t;
    }

    private static Supplier<String> message(String format, Consumer<FormatArgs> argsProvider) {
        final ObjectWrapper<Object[]> args = new ObjectWrapper<>();
        argsProvider.accept(args::accept);
        return () -> String.format(format, args.get());
    }

    private Exceptions() {
    }
}
