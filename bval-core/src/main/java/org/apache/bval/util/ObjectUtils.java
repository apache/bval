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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;

public final class ObjectUtils {
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

    private ObjectUtils() {
    }

    /**
     * <p>Returns a default value if the object passed is {@code null}.</p>
     *
     * <pre>
     * ObjectUtils.defaultIfNull(null, null)      = null
     * ObjectUtils.defaultIfNull(null, "")        = ""
     * ObjectUtils.defaultIfNull(null, "zz")      = "zz"
     * ObjectUtils.defaultIfNull("abc", *)        = "abc"
     * ObjectUtils.defaultIfNull(Boolean.TRUE, *) = Boolean.TRUE
     * </pre>
     *
     * @param <T> the type of the object
     * @param object  the {@code Object} to test, may be {@code null}
     * @param defaultValue  the default value to return, may be {@code null}
     * @return {@code object} if it is not {@code null}, defaultValue otherwise
     */
    public static <T> T defaultIfNull(final T object, final T defaultValue) {
        return object != null ? object : defaultValue;
    }

    public static <T> boolean isNotEmpty(final T[] array) {
        return !isEmpty(array);
    }

    public static boolean isEmpty(final Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * <p>Checks if the object is in the given array.
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.
     *
     * @param array  the array to search through
     * @param objectToFind  the object to find
     * @return {@code true} if the array contains the object
     */
    public static boolean arrayContains(final Object[] array, final Object objectToFind) {
        if (array == null) {
            return false;
        }
        for (Object o : array) {
            if (o.equals(objectToFind)) {
                return true;
            }
        }
        return false;
    }

    public static <T> T[] arrayAdd(T[] array, T objectToAdd) {
        Class<?> type;
        if (array != null) {
            type = array.getClass().getComponentType();
        } else if (objectToAdd != null) {
            type = objectToAdd.getClass();
        } else {
            throw new IllegalArgumentException("Arguments cannot both be null");
        }
        final int arrayLength = Array.getLength(array);
        T[] newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), arrayLength + 1);
        System.arraycopy(array, 0, newArray, 0, arrayLength);
        newArray[newArray.length - 1] = objectToAdd;

        return newArray;

    }
}
