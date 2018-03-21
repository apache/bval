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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class StringUtils {
    private StringUtils() {
    }

    /**
     * Taken from commons-lang3.
     * <p>
     * <p>Capitalizes a String changing the first character to title case as
     * per {@link Character#toTitleCase(char)}. No other characters are changed.</p>
     * <p>
     * <p>For a word based algorithm, see {@link org.apache.commons.lang3.text.WordUtils#capitalize(String)}.
     * A {@code null} input String returns {@code null}.</p>
     * <p>
     * <pre>
     * StringUtils.capitalize(null)  = null
     * StringUtils.capitalize("")    = ""
     * StringUtils.capitalize("cat") = "Cat"
     * StringUtils.capitalize("cAt") = "CAt"
     * StringUtils.capitalize("'cat'") = "'cat'"
     * </pre>
     *
     * @param str the String to capitalize, may be null
     * @return the capitalized String, {@code null} if null String input
     * @see org.apache.commons.lang3.text.WordUtils#capitalize(String)
     */
    public static String capitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }

        final char firstChar = str.charAt(0);
        final char newChar = Character.toTitleCase(firstChar);
        if (firstChar == newChar) {
            // already capitalized
            return str;
        }

        char[] newChars = new char[strLen];
        newChars[0] = newChar;
        str.getChars(1, strLen, newChars, 1);
        return String.valueOf(newChars);
    }

    /**
     * Taken from commons-lang3.
     * <p>Checks if a CharSequence is whitespace, empty ("") or null.</p>
     * <p>
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     */
    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (Character.isWhitespace(cs.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * Taken from commons-lang3.
     * <p>Checks if a CharSequence is not empty (""), not null and not whitespace only.</p>
     *
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("bob")     = true
     * StringUtils.isNotBlank("  bob  ") = true
     * </pre>
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is
     *  not empty and not null and not whitespace
     */
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * <p>Splits the provided text into an array, separator is whitespace.
     * @param str
     * @return {@link String}[]
     */
    public static String[] split(String str) {
        return split(str, null);
    }

    /**
     * <p>Splits the provided text into an array, separator is whitespace.
     * @param str
     * @param token
     * @return {@link String}[]
     */
    public static String[] split(String str, Character token) {
        if (str == null || str.isEmpty()) {
            return ObjectUtils.EMPTY_STRING_ARRAY;
        }
        // split on token
        List<String> ret = new ArrayList<>();
        StringBuilder sb = new StringBuilder(str.length());
        for (int pos = 0; pos < str.length(); pos++) {
            char c = str.charAt(pos);
            if ((token == null && Character.isWhitespace(c)) || (token != null && token.equals(c))) {
                if (sb.length() > 0) {
                    ret.add(sb.toString());
                    sb.setLength(0); // reset the string
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            ret.add(sb.toString());
        }
        return ret.toArray(new String[ret.size()]);
    }

    /**
     * Return a {@link String} representation of {@code o}, accounting for array types.
     * @param o
     * @return {@link String}
     * @see Arrays
     * @see String#valueOf(Object)
     */
    public static String valueOf(Object o) {
        if (o instanceof Object[]) {
            return Arrays.toString((Object[]) o);
        }
        if (o instanceof byte[]) {
            return Arrays.toString((byte[]) o);
        }
        if (o instanceof short[]) {
            return Arrays.toString((short[]) o);
        }
        if (o instanceof int[]) {
            return Arrays.toString((int[]) o);
        }
        if (o instanceof char[]) {
            return Arrays.toString((char[]) o);
        }
        if (o instanceof long[]) {
            return Arrays.toString((long[]) o);
        }
        if (o instanceof float[]) {
            return Arrays.toString((float[]) o);
        }
        if (o instanceof double[]) {
            return Arrays.toString((double[]) o);
        }
        if (o instanceof boolean[]) {
            return Arrays.toString((boolean[]) o);
        }
        return String.valueOf(o);
    }
}