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
package org.apache.bval.jsr.util;

import javax.validation.ValidationException;
import java.io.StringWriter;
import java.text.ParsePosition;

/**
 * Defines a path navigation algorithm and a means of interacting with same.
 * 
 * @version $Rev: 1136233 $ $Date: 2011-06-15 17:49:27 -0500 (Wed, 15 Jun 2011) $
 */
public class PathNavigation {

    /**
     * Path traversal callback function interface.
     */
    public interface Callback<T> {
        /**
         * Handle a .-delimited property.
         * 
         * @param name
         */
        void handleProperty(String name);

        /**
         * Handle an index or key embedded in [].
         * 
         * @param value
         */
        void handleIndexOrKey(String value);

        /**
         * Handle contiguous [].
         */
        void handleGenericInIterable();

        /**
         * Return a result. Called after navigation is complete.
         * 
         * @return result
         */
        T result();
    }

    /**
     * Callback "procedure" that always returns null.
     */
    public static abstract class CallbackProcedure implements Callback<Object> {

        /**
         * {@inheritDoc}
         */
        @Override
        public final Object result() {
            complete();
            return null;
        }

        /**
         * Complete this CallbackProcedure. Default implementation is noop.
         */
        protected void complete() {
        }
    }

    /**
     * Create a new PathNavigation instance.
     */
    private PathNavigation() {
    }

    /**
     * Navigate a path using the specified callback, returning its result.
     * 
     * @param <T>
     * @param propertyPath
     *            , null is assumed empty/root
     * @param callback
     * @return T result
     */
    public static <T> T navigateAndReturn(CharSequence propertyPath, Callback<? extends T> callback) {
        try {
            parse(propertyPath == null ? "" : propertyPath, new PathPosition(callback));
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException(String.format("invalid property: %s", propertyPath), ex);
        }
        return callback.result();
    }

    /**
     * Navigate a path using the specified callback.
     * 
     * @param propertyPath
     * @param callback
     */
    public static void navigate(CharSequence propertyPath, Callback<?> callback) {
        navigateAndReturn(propertyPath, callback);
    }

    private static void parse(CharSequence path, PathPosition pos) throws Exception {
        int len = path.length();
        boolean sep = true;
        while (pos.getIndex() < len) {
            int here = pos.getIndex();
            char c = path.charAt(here);
            switch (c) {
            case ']':
                throw new IllegalStateException(String.format("Position %s: unexpected '%s'", here, c));
            case '[':
                handleIndex(path, pos.next());
                break;
            case '.':
                if (sep) {
                    throw new IllegalStateException(String.format("Position %s: expected property, index/key, or end of expression", here));
                }
                sep = true;
                pos.next();
                // fall through:
            default:
                if (!sep) {
                    throw new IllegalStateException(String.format("Position %s: expected property path separator, index/key, or end of expression", here));
                }
                pos.handleProperty(parseProperty(path, pos));
            }
            sep = false;
        }
    }

    private static String parseProperty(CharSequence path, PathPosition pos) throws Exception {
        int len = path.length();
        int start = pos.getIndex();
        loop: while (pos.getIndex() < len) {
            switch (path.charAt(pos.getIndex())) {
            case '[':
            case ']':
            case '.':
                break loop;
            }
            pos.next();
        }
        if (pos.getIndex() > start) {
            return path.subSequence(start, pos.getIndex()).toString();
        }
        throw new IllegalStateException(String.format("Position %s: expected property", start));
    }

    /**
     * Handles an index/key. If the text contained between [] is surrounded by a pair of " or ', it will be treated as a
     * string which may contain Java escape sequences. This function is only available if commons-lang3 is available on the classpath!
     * 
     * @param path
     * @param pos
     * @throws Exception
     */
    private static void handleIndex(CharSequence path, PathPosition pos) throws Exception {
        int len = path.length();
        int start = pos.getIndex();
        if (start < len) {
            char first = path.charAt(pos.getIndex());
            if (first == '"' || first == '\'') {
                String s = parseQuotedString(path, pos);
                if (s != null && path.charAt(pos.getIndex()) == ']') {
                    pos.handleIndexOrKey(s);
                    pos.next();
                    return;
                }
            }
            // no quoted string; match ] greedily
            while (pos.getIndex() < len) {
                int here = pos.getIndex();
                try {
                    if (path.charAt(here) == ']') {
                        if (here == start) {
                            pos.handleGenericInIterable();
                        } else {
                            pos.handleIndexOrKey(path.subSequence(start, here).toString());
                        }
                        return;
                    }
                } finally {
                    pos.next();
                }
            }
        }
        throw new IllegalStateException(String.format("Position %s: unparsable index", start));
    }

    private static String parseQuotedString(CharSequence path, PathPosition pos) throws Exception {
        int len = path.length();
        int start = pos.getIndex();
        if (start < len) {
            char quote = path.charAt(start);
            pos.next();
            StringWriter w = new StringWriter();
            while (pos.getIndex() < len) {
                int here = pos.getIndex();
                // look for matching quote
                if (path.charAt(here) == quote) {
                    pos.next();
                    return w.toString();
                }
                try {
                    int codePoints = org.apache.commons.lang3.StringEscapeUtils.UNESCAPE_JAVA.translate(path, here, w);
                    if (codePoints == 0) {
                        w.write(Character.toChars(Character.codePointAt(path, here)));
                        pos.next();
                    } else {
                        for (int i = 0; i < codePoints; i++) {
                            pos.plus(Character.charCount(Character.codePointAt(path, pos.getIndex())));
                        }
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException("Java escaping in quotes is only supported with Apache commons-lang3 on the classpath!");
                }
            }
            // if reached, reset due to no ending quote found
            pos.setIndex(start);
        }
        return null;
    }

    /**
     * ParsePosition/Callback
     */
    private static class PathPosition extends ParsePosition implements Callback<Object> {
        final Callback<?> delegate;

        /**
         * Create a new {@link PathPosition} instance.
         * 
         * @param delegate
         */
        private PathPosition(Callback<?> delegate) {
            super(0);
            this.delegate = delegate;
        }

        /**
         * Increment and return this.
         * 
         * @return this
         */
        public PathPosition next() {
            return plus(1);
        }

        /**
         * Increase position and return this.
         * 
         * @param addend
         * @return this
         */
        public PathPosition plus(int addend) {
            setIndex(getIndex() + addend);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleProperty(String name) {
            delegate.handleProperty(name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleIndexOrKey(String value) {
            delegate.handleIndexOrKey(value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleGenericInIterable() {
            delegate.handleGenericInIterable();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object result() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        /*
         * Override equals to make findbugs happy;
         * would simply ignore but doesn't seem to be possible at the inner class level
         * without attaching the filter to the containing class.
         */
        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }
        
        /**
         * {@inheritDoc}
         */
        /*
         * Override hashCode to make findbugs happy in the presence of overridden #equals :P
         */
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

}
