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
package org.apache.bval.routines;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description: holds the regexp to validate an email address<br>
 * User: roman.stumm<br>
 * Date: 17.06.2010<br>
 * Time: 10:40:59<br>
 */
public class EMailValidationUtils {
    private static String ATOM = "[^\\x00-\\x1F\\(\\)\\<\\>\\@\\,\\;\\:\\\\\\\"\\.\\[\\]\\s]";
    private static String DOMAIN = "(" + ATOM + "+(\\." + ATOM + "+)*";
    private static String IP_DOMAIN = "\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\]";
    public static final Pattern DEFAULT_EMAIL_PATTERN;

    static {
        DEFAULT_EMAIL_PATTERN = Pattern.compile("^" + ATOM + "+(\\." + ATOM + "+)*@" + DOMAIN + "|" + IP_DOMAIN + ")$",
            Pattern.CASE_INSENSITIVE);
    }

    /**
     * Learn whether a given object is a valid email address.
     * 
     * @param value
     *            to check
     * @return <code>true</code> if the validation passes
     */
    public static boolean isValid(Object value) {
        if (value == null) {
            return true;
        }
        if (!(value instanceof CharSequence)) {
            return false;
        }
        final CharSequence seq = (CharSequence) value;
        return seq.length() == 0 || matchesDefaultPattern(seq);
    }

    /**
     * Equivalent to {@code DEFAULT_EMAIL_PATTERN.matcher(seq).matches()} without the regular expression engine: one
     * or more dot-separated runs of atom characters, {@code @}, then either such runs or a bracketed dotted quad.
     */
    private static boolean matchesDefaultPattern(CharSequence seq) {
        final int length = seq.length();
        final int at = matchAtomRuns(seq, 0, length);
        if (at < 0 || at >= length || seq.charAt(at) != '@') {
            return false;
        }
        final int domain = at + 1;
        return matchAtomRuns(seq, domain, length) == length || matchIpDomain(seq, domain, length);
    }

    /**
     * Match {@code ATOM+(\.ATOM+)*} starting at {@code start} as far as possible.
     *
     * @return the index after the match, or {@code -1} if there is none
     */
    private static int matchAtomRuns(CharSequence seq, int start, int length) {
        int i = start;
        while (true) {
            final int runStart = i;
            while (i < length && isAtom(seq.charAt(i))) {
                i++;
            }
            if (i == runStart) {
                // an empty run: fail at the start, otherwise back off the dot that introduced it
                return runStart == start ? -1 : runStart - 1;
            }
            if (i < length && seq.charAt(i) == '.') {
                i++;
            } else {
                return i;
            }
        }
    }

    private static boolean isAtom(char c) {
        if (c <= 0x20) {
            // control characters, and the space of \s; the other \s characters are control characters
            return false;
        }
        switch (c) {
        case '(':
        case ')':
        case '<':
        case '>':
        case '@':
        case ',':
        case ';':
        case ':':
        case '\\':
        case '"':
        case '.':
        case '[':
        case ']':
            return false;
        default:
            return true;
        }
    }

    /** Match {@code \[d{1,3}\.d{1,3}\.d{1,3}\.d{1,3}\]} spanning exactly {@code [start, length)}. */
    private static boolean matchIpDomain(CharSequence seq, int start, int length) {
        if (length - start < 9 || seq.charAt(start) != '[' || seq.charAt(length - 1) != ']') {
            return false;
        }
        int i = start + 1;
        for (int part = 0; part < 4; part++) {
            if (part > 0) {
                if (seq.charAt(i) != '.') {
                    return false;
                }
                i++;
            }
            final int digitsStart = i;
            while (i < length - 1 && i - digitsStart < 3 && isDigit(seq.charAt(i))) {
                i++;
            }
            if (i == digitsStart) {
                return false;
            }
        }
        return i == length - 1;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Learn whether a particular value matches a given pattern per
     * {@link Matcher#matches()}.
     * 
     * @param value
     * @param aPattern
     * @return <code>true</code> if <code>value</code> was a <code>String</code>
     *         matching <code>aPattern</code>
     */
    // TODO it would seem to make sense to move or reduce the visibility of this
    // method as it is more general than email.
    public static boolean isValid(Object value, Pattern aPattern) {
        if (value == null) {
            return true;
        }
        if (!(value instanceof CharSequence)) {
            return false;
        }
        CharSequence seq = (CharSequence) value;
        if (seq.length() == 0) {
            return true;
        }
        return aPattern.matcher(seq).matches();
    }

}
