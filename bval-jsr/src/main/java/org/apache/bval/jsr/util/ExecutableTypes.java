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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.executable.ExecutableType;

import org.apache.bval.util.Validate;

/**
 * Utility methods relating to {@link ExecutableType}.
 */
public class ExecutableTypes {

    private static final Map<ExecutableType, Set<ExecutableType>> INTERPRETED_EXECUTABLE_TYPES;
    static {
        final Map<ExecutableType, Set<ExecutableType>> m = new LinkedHashMap<>();

        m.put(ExecutableType.ALL, Collections.unmodifiableSet(
            EnumSet.of(ExecutableType.CONSTRUCTORS, ExecutableType.NON_GETTER_METHODS, ExecutableType.GETTER_METHODS)));
        m.put(ExecutableType.IMPLICIT,
            Collections.unmodifiableSet(EnumSet.of(ExecutableType.CONSTRUCTORS, ExecutableType.NON_GETTER_METHODS)));
        m.put(ExecutableType.NONE, Collections.emptySet());

        INTERPRETED_EXECUTABLE_TYPES = Collections.unmodifiableMap(m);
    }

    /**
     * Interpret occurrences of {@link ExecutableType#ALL}, {@link ExecutableType#IMPLICIT}, and
     * {@link ExecutableType#NONE}.
     * 
     * @param executableTypes
     * @return (unmodifiable) {@link Set} of {@link ExecutableType}
     */
    public static Set<ExecutableType> interpret(Collection<ExecutableType> executableTypes) {
        Validate.notNull(executableTypes);

        for (Map.Entry<ExecutableType, Set<ExecutableType>> e : INTERPRETED_EXECUTABLE_TYPES.entrySet()) {
            if (e.getValue().equals(executableTypes) || executableTypes.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(executableTypes));
    }

    /**
     * Interpret occurrences of {@link ExecutableType#ALL}, {@link ExecutableType#IMPLICIT}, and
     * {@link ExecutableType#NONE}.
     * 
     * @param executableTypes
     * @return (unmodifiable) {@link Set} of {@link ExecutableType}
     */
    public static Set<ExecutableType> interpret(ExecutableType... executableTypes) {
        return interpret(Arrays.asList(executableTypes));
    }

    private ExecutableTypes() {
    }
}
