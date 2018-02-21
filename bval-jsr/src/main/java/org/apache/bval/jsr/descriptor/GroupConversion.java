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
package org.apache.bval.jsr.descriptor;

import java.util.Objects;
import java.util.Optional;

import javax.validation.metadata.GroupConversionDescriptor;

import org.apache.bval.util.Lazy;
import org.apache.bval.util.LazyInt;
import org.apache.bval.util.Validate;

public class GroupConversion implements GroupConversionDescriptor {
    public static class Builder {
        private final Class<?> from;

        private Builder(Class<?> from) {
            this.from = from;
        }

        public GroupConversion to(Class<?> to) {
            return new GroupConversion(from, to);
        }
    }

    public static Builder from(Class<?> from) {
        return new Builder(from);
    }

    private final Class<?> from;
    private final Class<?> to;
    private final LazyInt hashCode;
    private final Lazy<String> toString;

    private GroupConversion(Class<?> from, Class<?> to) {
        super();
        this.from = Validate.notNull(from, "from");
        this.to = Validate.notNull(to, "to");
        this.hashCode = new LazyInt(() -> Objects.hash(this.from, this.to));
        this.toString = new Lazy<>(
            () -> String.format("%s from %s to %s", GroupConversion.class.getSimpleName(), this.from, this.to));
    }

    @Override
    public Class<?> getFrom() {
        return from;
    }

    @Override
    public Class<?> getTo() {
        return to;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
            || Optional.ofNullable(obj).filter(GroupConversion.class::isInstance).map(GroupConversion.class::cast)
                .filter(gc -> Objects.equals(from, gc.from) && Objects.equals(to, gc.to)).isPresent();
    }

    @Override
    public int hashCode() {
        return hashCode.getAsInt();
    }

    @Override
    public String toString() {
        return toString.get();
    }
}
