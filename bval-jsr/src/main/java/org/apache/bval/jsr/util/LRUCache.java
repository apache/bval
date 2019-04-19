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
package org.apache.bval.jsr.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 1L;

    private final int maximumCapacity;

    public LRUCache(int maximumCapacity) {
        super(16, 0.75f, true);
        if (maximumCapacity < 1) {
            throw new IllegalArgumentException("maximumCapacity must be > 0");
        }
        this.maximumCapacity = maximumCapacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) { // todo: synchronize?
        return super.removeEldestEntry(eldest) || size() >= maximumCapacity;
    }
}
