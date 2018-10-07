/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bval.util;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// taken from commons-lang3
public final class Escapes {
    private static final CharSequenceTranslator UNESCAPE_JAVA =
            new AggregateTranslator(
                    new OctalUnescaper(),     // .between('\1', '\377'),
                    new UnicodeUnescaper(),
                    new LookupTranslator(new String[][] {
                            {"\\b", "\b"},
                            {"\\n", "\n"},
                            {"\\t", "\t"},
                            {"\\f", "\f"},
                            {"\\r", "\r"}
                    }),
                    new LookupTranslator(
                            new String[][] {
                                    {"\\\\", "\\"},
                                    {"\\\"", "\""},
                                    {"\\'", "'"},
                                    {"\\", ""}
                            })
            );

    private Escapes() {
        // no-op
    }

    public static int unescapeJava(final CharSequence from, final int offset, final Writer output) {
        // return StringEscapeUtils.UNESCAPE_JAVA.translate(path, pos.getIndex(), target);
        return UNESCAPE_JAVA.translate(from, offset, output);
    }

    protected interface CharSequenceTranslator {
        int translate(CharSequence input, int index, Writer out);
    }

    private static class AggregateTranslator implements CharSequenceTranslator {
        private final CharSequenceTranslator[] translators;

        private AggregateTranslator(final CharSequenceTranslator... translators) {
            this.translators = translators;
        }

        @Override
        public int translate(final CharSequence input, final int index, final Writer out) {
            for (final CharSequenceTranslator translator : translators) {
                final int consumed = translator.translate(input, index, out);
                if(consumed != 0) {
                    return consumed;
                }
            }
            return 0;
        }
    }

    private static class OctalUnescaper implements CharSequenceTranslator {
        @Override
        public int translate(final CharSequence input, final int index, final Writer out) {
            final int remaining = input.length() - index - 1;
            final StringBuilder builder = new StringBuilder();
            if (input.charAt(index) == '\\' && remaining > 0 && isOctalDigit(input.charAt(index + 1))) {
                final int next = index + 1;
                final int next2 = index + 2;
                final int next3 = index + 3;

                builder.append(input.charAt(next));

                if (remaining > 1 && isOctalDigit(input.charAt(next2))) {
                    builder.append(input.charAt(next2));
                    if (remaining > 2 && isZeroToThree(input.charAt(next)) && isOctalDigit(input.charAt(next3))) {
                        builder.append(input.charAt(next3));
                    }
                }

                try {
                    out.write(Integer.parseInt(builder.toString(), 8));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return 1 + builder.length();
            }
            return 0;
        }

        private boolean isOctalDigit(final char ch) {
            return ch >= '0' && ch <= '7';
        }

        private boolean isZeroToThree(final char ch) {
            return ch >= '0' && ch <= '3';
        }
    }

    public static class UnicodeUnescaper implements CharSequenceTranslator {
        @Override
        public int translate(final CharSequence input, final int index, final Writer out) {
            if (input.charAt(index) == '\\' && index + 1 < input.length() && input.charAt(index + 1) == 'u') {
                int i = 2;
                while (index + i < input.length() && input.charAt(index + i) == 'u') {
                    i++;
                }

                if (index + i < input.length() && input.charAt(index + i) == '+') {
                    i++;
                }

                if (index + i + 4 <= input.length()) {
                    // Get 4 hex digits
                    final CharSequence unicode = input.subSequence(index + i, index + i + 4);

                    try {
                        final int value = Integer.parseInt(unicode.toString(), 16);
                        out.write((char) value);
                    } catch (final NumberFormatException nfe) {
                        throw new IllegalArgumentException("Unable to parse unicode value: " + unicode, nfe);
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                    return i + 4;
                }
                throw new IllegalArgumentException("Less than 4 hex digits in unicode value: '" + input.subSequence(index, input.length())
                        + "' due to end of CharSequence");
            }
            return 0;
        }
    }

    public static class LookupTranslator implements CharSequenceTranslator {
        private final Map<String, String> lookupMap;
        private final Set<Character> prefixSet;
        private final int shortest;
        private final int longest;

        private LookupTranslator(final String[][] lookup) {
            lookupMap = new HashMap<>();
            prefixSet = new HashSet<>();
            int _shortest = Integer.MAX_VALUE;
            int _longest = 0;
            if (lookup != null) {
                for (final CharSequence[] seq : lookup) {
                    this.lookupMap.put(seq[0].toString(), seq[1].toString());
                    this.prefixSet.add(seq[0].charAt(0));
                    final int sz = seq[0].length();
                    if (sz < _shortest) {
                        _shortest = sz;
                    }
                    if (sz > _longest) {
                        _longest = sz;
                    }
                }
            }
            shortest = _shortest;
            longest = _longest;
        }

        @Override
        public int translate(final CharSequence input, final int index, final Writer out) {
            if (prefixSet.contains(input.charAt(index))) {
                int max = longest;
                if (index + longest > input.length()) {
                    max = input.length() - index;
                }
                for (int i = max; i >= shortest; i--) {
                    final CharSequence subSeq = input.subSequence(index, index + i);
                    final String result = lookupMap.get(subSeq.toString());

                    if (result != null) {
                        try {
                            out.write(result);
                        } catch (final IOException e) {
                            throw new IllegalStateException(e);
                        }
                        return i;
                    }
                }
            }
            return 0;
        }
    }
}
