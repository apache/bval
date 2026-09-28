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
package org.apache.bval.jsr.descriptor;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.apache.bval.jsr.groups.GroupStrategy;

/**
 * Thread-safe cache of values computed per {@link GroupStrategy}. Validation passes the same few strategy instances
 * over and over, so lookups first scan a small array of recently seen instances by identity and only then fall back
 * to an equality-keyed map.
 *
 * @param <V> value type
 */
final class GroupStrategyMap<V> {
    private static final int MAX_IDENTITY_ENTRIES = 8;
    private static final Object[] NO_ENTRIES = {};

    private final ConcurrentMap<GroupStrategy, V> byEquality = new ConcurrentHashMap<>();
    /** Alternating key, value pairs; replaced wholesale, so readers always see a consistent array. */
    private volatile Object[] byIdentity = NO_ENTRIES;

    @SuppressWarnings("unchecked")
    V computeIfAbsent(GroupStrategy key, Function<? super GroupStrategy, ? extends V> compute) {
        final Object[] entries = byIdentity;
        for (int i = 0; i < entries.length; i += 2) {
            if (entries[i] == key) {
                return (V) entries[i + 1];
            }
        }
        V value = byEquality.get(key);
        if (value == null) {
            value = compute.apply(key);
            final V previous = byEquality.putIfAbsent(key, value);
            if (previous != null) {
                value = previous;
            }
        }
        if (entries.length < 2 * MAX_IDENTITY_ENTRIES) {
            // racing updates may drop an entry, which only costs a later equality lookup
            final Object[] grown = Arrays.copyOf(entries, entries.length + 2);
            grown[entries.length] = key;
            grown[entries.length + 1] = value;
            byIdentity = grown;
        }
        return value;
    }
}
