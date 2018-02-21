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
package org.apache.bval.jsr.valueextraction;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import javax.validation.valueextraction.ExtractedValue;
import javax.validation.valueextraction.UnwrapByDefault;
import javax.validation.valueextraction.ValueExtractor;

public abstract class OptionalExtractor {
    public static class ForObject implements ValueExtractor<Optional<@ExtractedValue ?>> {

        @Override
        public void extractValues(Optional<?> originalValue, ValueExtractor.ValueReceiver receiver) {
            receiver.value(null, originalValue.orElse(null));
        }
    }

    @UnwrapByDefault
    public static class ForInt implements ValueExtractor<@ExtractedValue(type = Integer.class) OptionalInt> {

        @Override
        public void extractValues(OptionalInt originalValue, ValueExtractor.ValueReceiver receiver) {
            receiver.value(null, originalValue.isPresent() ? Integer.valueOf(originalValue.getAsInt()) : null);
        }
    }

    @UnwrapByDefault
    public static class ForLong implements ValueExtractor<@ExtractedValue(type = Long.class) OptionalLong> {

        @Override
        public void extractValues(OptionalLong originalValue, ValueExtractor.ValueReceiver receiver) {
            receiver.value(null, originalValue.isPresent() ? Long.valueOf(originalValue.getAsLong()) : null);
        }
    }

    @UnwrapByDefault
    public static class ForDouble implements ValueExtractor<@ExtractedValue(type = Double.class) OptionalDouble> {

        @Override
        public void extractValues(OptionalDouble originalValue, ValueExtractor.ValueReceiver receiver) {
            receiver.value(null, originalValue.isPresent() ? Double.valueOf(originalValue.getAsDouble()) : null);
        }
    }
}
