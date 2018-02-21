/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bval.util;

import java.util.function.Function;

/**
 * Some used validations from commons.
 */
public final class Validate {
    private Validate() {
    }

    public static <T> T notNull(final T object) {
        return notNull(object, "The validated object is null");
    }

    public static <T> T notNull(final T object, final String message, final Object... values) {
        return notNull(object, NullPointerException::new, message, values);
    }

    public static <E extends Exception, T> T notNull(final T object, Function<? super String, ? extends E> fn,
        final String message, final Object... values) throws E {
        Exceptions.raiseIf(object == null, fn, message, values);
        return object;
    }

    public static void isTrue(final boolean expression, final String message, final Object... values) {
        Exceptions.raiseUnless(expression, IllegalArgumentException::new, message, values);
    }

    public static <T> T[] noNullElements(final T[] array, final String message, final Object... values) {
        Validate.notNull(array);

        for (int i = 0; i < array.length; i++) {
            Exceptions.raiseIf(array[i] == null, IllegalArgumentException::new, message,
                ObjectUtils.arrayAdd(values, Integer.valueOf(i)));
        }
        return array;
    }

    public static void validState(final boolean expression, final String message, final Object... values) {
        Exceptions.raiseUnless(expression, IllegalStateException::new, message, values);
    }
}
