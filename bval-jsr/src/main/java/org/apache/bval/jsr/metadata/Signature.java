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
package org.apache.bval.jsr.metadata;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bval.jsr.util.Methods;
import org.apache.bval.util.Comparators;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.LazyInt;
import org.apache.bval.util.StringUtils;
import org.apache.bval.util.Validate;

public final class Signature implements Comparable<Signature> {
    private static final Comparator<Signature> COMPARATOR = Comparator.nullsFirst(Comparator
        .comparing(Signature::getName).thenComparing(Comparator.comparing(s -> Arrays.asList(s.getParameterTypes()),
            Comparators.comparingIterables(Comparator.comparing(Class::getName)))));

    public static Signature of(Executable x) {
        return new Signature(x.getName(), x.getParameterTypes());
    }

    private final String name;
    private final Class<?>[] parameterTypes;
    private final LazyInt hashCode;
    private final Lazy<String> toString;

    public Signature(String name, Class<?>... parameterTypes) {
        super();
        this.name = Validate.notNull(name, "name");
        Validate.isTrue(StringUtils.isNotBlank(name), "name is blank");
        this.parameterTypes = Validate.notNull(parameterTypes, "parameterTypes").clone();
        hashCode = new LazyInt(() -> Arrays.deepHashCode(new Object[] { this.name, this.parameterTypes }));
        toString = new Lazy<>(() -> String.format("%s: %s(%s)", getClass().getSimpleName(), this.name,
            Stream.of(this.parameterTypes).map(Class::getName).collect(Collectors.joining(", "))));
    }

    public String getName() {
        return name;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes.clone();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || Optional.ofNullable(obj).filter(Signature.class::isInstance).map(Signature.class::cast)
            .filter(sig -> Objects.equals(name, sig.name) && Objects.deepEquals(parameterTypes, sig.parameterTypes))
            .isPresent();
    }

    @Override
    public int hashCode() {
        return hashCode.getAsInt();
    }

    @Override
    public String toString() {
        return toString.get();
    }

    @Override
    public int compareTo(Signature sig) {
        return COMPARATOR.compare(this, sig);
    }

    public boolean isGetter() {
        return parameterTypes.length == 0 && Methods.isGetter(name);
    }
}
