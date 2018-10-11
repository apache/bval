/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.bval.jsr.util;

import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.bval.util.Validate;

/**
 * Utility class to manage regular expressions that require simulated infinite
 * lookbehind, e.g. to determine whether a sequence of escape characters is
 * complete unto itself.
 */
public class LookBehindRegexHolder {
    public static final int DEFAULT_INITIAL_MAXIMUM_LENGTH = 256;
    public static final int DEFAULT_EXPANSION_BLOCK_SIZE = 128;

    private final String regex;
    private final int expansionBlockSize;
    private final IntUnaryOperator computeInjectedRepetition;

    private volatile int maximumLength;
    private Pattern pattern;

    /**
     * Create a new {@link LookBehindRegexHolder} instance with the default
     * initial maximum length and expansion block size.
     * 
     * @param regex
     *            assumed to contain a {@code %d} Java format sequence
     * @param computeInjectedRepetition
     *            function to compute the injected number of repetitions to
     *            inject at {@code %d} in {@code regex}
     */
    public LookBehindRegexHolder(String regex, IntUnaryOperator computeInjectedRepetition) {
        this(regex, DEFAULT_INITIAL_MAXIMUM_LENGTH, DEFAULT_EXPANSION_BLOCK_SIZE, computeInjectedRepetition);
    }

    /**
     * Create a new {@link LookBehindRegexHolder} instance.
     * 
     * @param regex
     *            assumed to contain a {@code %d} Java format sequence
     * @param initialMaximumLength
     *            initial guess
     * @param expansionBlockSize
     *            number of bytes by which to increase the maximum length when a
     *            {@link Matcher} is requested for a larger message size
     * @param computeInjectedRepetition
     *            function to compute the injected number of repetitions to
     *            inject at {@code %d} in {@code regex}
     */
    public LookBehindRegexHolder(String regex, int initialMaximumLength, int expansionBlockSize,
        IntUnaryOperator computeInjectedRepetition) {
        super();
        Validate.isTrue(regex != null && !regex.trim().isEmpty(), "empty regex");
        Validate.isTrue(initialMaximumLength > 0, "invalid initial maximum length %d", initialMaximumLength);
        Validate.isTrue(expansionBlockSize > 0, "Invalid expansion block size %d", expansionBlockSize);
        Validate.notNull(computeInjectedRepetition, "missing %s to compute injected repetition",
            IntUnaryOperator.class.getSimpleName());
        this.regex = regex;
        this.expansionBlockSize = expansionBlockSize;
        this.computeInjectedRepetition = computeInjectedRepetition;
        accommodate(initialMaximumLength);
    }

    /**
     * Get a {@link Matcher} against the specified {@link CharSequence}.
     * 
     * @param s
     * @return {@link Matcher}
     */
    public Matcher matcher(CharSequence s) {
        if (s.length() > maximumLength) {
            accommodate(s.length());
        }
        return pattern.matcher(s);
    }

    int getMaximumLength() {
        return this.maximumLength;
    }

    String getPattern() {
        return pattern.pattern();
    }

    private synchronized void accommodate(int maximumLength) {
        if (this.maximumLength < maximumLength) {
            if (this.maximumLength == 0) {
                this.maximumLength = maximumLength;
            } else {
                int difference = maximumLength - this.maximumLength;
                int addBlocks = difference / expansionBlockSize + 1;
                this.maximumLength += addBlocks * expansionBlockSize;
            }
            this.pattern =
                Pattern.compile(String.format(regex, computeInjectedRepetition.applyAsInt(this.maximumLength)));
        }
    }
}
