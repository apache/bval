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
import java.util.Set;

import javax.validation.executable.ExecutableType;

import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;

/**
 * Utility methods relating to {@link ExecutableType}.
 */
public class ExecutableTypes {

    private static final Set<ExecutableType> ALL_TYPES = Collections.unmodifiableSet(
        EnumSet.of(ExecutableType.CONSTRUCTORS, ExecutableType.NON_GETTER_METHODS, ExecutableType.GETTER_METHODS));

    private static final Set<ExecutableType> IMPLICIT_TYPES =
        Collections.unmodifiableSet(EnumSet.of(ExecutableType.CONSTRUCTORS, ExecutableType.NON_GETTER_METHODS));

    /**
     * Interpret occurrences of {@link ExecutableType#ALL}, {@link ExecutableType#IMPLICIT}, and
     * {@link ExecutableType#NONE}.
     * 
     * @param executableTypes
     * @return (unmodifiable) {@link Set} of {@link ExecutableType}
     */
    public static Set<ExecutableType> interpret(Collection<ExecutableType> executableTypes) {
        Validate.notNull(executableTypes);
        if (executableTypes.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<ExecutableType> result = EnumSet.copyOf(executableTypes);
        if (result.contains(ExecutableType.ALL)) {
            return ALL_TYPES;
        }
        if (result.remove(ExecutableType.IMPLICIT)) {
            if (!result.isEmpty()) {
                Exceptions.raise(IllegalArgumentException::new, "Mixing %s with other %ss is illegal.",
                    ExecutableType.IMPLICIT, ExecutableType.class.getSimpleName());
            }
            return IMPLICIT_TYPES;
        }
        result.remove(ExecutableType.NONE);
        return result.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(result);
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
