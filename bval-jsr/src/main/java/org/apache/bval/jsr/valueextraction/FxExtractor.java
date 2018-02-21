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
import java.util.function.BooleanSupplier;

import javax.validation.valueextraction.ExtractedValue;
import javax.validation.valueextraction.UnwrapByDefault;
import javax.validation.valueextraction.ValueExtractor;

import org.apache.bval.util.reflection.Reflection;

import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlySetProperty;
import javafx.beans.value.ObservableValue;

@SuppressWarnings("restriction")
public abstract class FxExtractor {
    public static class Activation implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                return Reflection.toClass("javafx.beans.Observable") != null;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }

    @UnwrapByDefault
    public static class ForObservableValue implements ValueExtractor<ObservableValue<@ExtractedValue ?>> {

        @Override
        public void extractValues(ObservableValue<?> originalValue, ValueExtractor.ValueReceiver receiver) {
            receiver.value(null, originalValue.getValue());
        }
    }

    public static class ForListProperty implements ValueExtractor<ReadOnlyListProperty<@ExtractedValue ?>> {

        @Override
        public void extractValues(ReadOnlyListProperty<?> originalValue, ValueExtractor.ValueReceiver receiver) {
            Optional.ofNullable(originalValue.getValue()).ifPresent(l -> {
                for (int i = 0, sz = l.size(); i < sz; i++) {
                    receiver.indexedValue("<list element>", i, l.get(i));
                }
            });
        }
    }

    public static class ForSetProperty implements ValueExtractor<ReadOnlySetProperty<@ExtractedValue ?>> {

        @Override
        public void extractValues(ReadOnlySetProperty<?> originalValue, ValueExtractor.ValueReceiver receiver) {
            Optional.ofNullable(originalValue.getValue())
                .ifPresent(s -> s.forEach(e -> receiver.iterableValue("<iterable element>", e)));
        }
    }

    public static class ForMapPropertyKey implements ValueExtractor<ReadOnlyMapProperty<@ExtractedValue ?, ?>> {

        @Override
        public void extractValues(ReadOnlyMapProperty<?, ?> originalValue, ValueExtractor.ValueReceiver receiver) {
            Optional.ofNullable(originalValue.getValue())
                .ifPresent(m -> m.keySet().forEach(k -> receiver.keyedValue("<map key>", k, k)));
        }
    }

    public static class ForMapPropertyValue implements ValueExtractor<ReadOnlyMapProperty<?, @ExtractedValue ?>> {

        @Override
        public void extractValues(ReadOnlyMapProperty<?, ?> originalValue, ValueExtractor.ValueReceiver receiver) {
            Optional.ofNullable(originalValue.getValue()).ifPresent(
                m -> m.entrySet().forEach(e -> receiver.keyedValue("<map value>", e.getKey(), e.getValue())));
        }
    }
}
