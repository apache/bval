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
package org.apache.bval.constraints;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.function.ToIntFunction;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Size;

/**
 * Description: Abstract validator impl. for @Size annotation.
 */
public abstract class SizeValidator<T> implements ConstraintValidator<Size, T> {
    public static class ForArray<T> extends SizeValidator<T> {
        public static class OfObject extends ForArray<Object[]> {
        }

        public static class OfByte extends ForArray<byte[]> {
        }

        public static class OfShort extends ForArray<short[]> {
        }

        public static class OfInt extends ForArray<int[]> {
        }

        public static class OfLong extends ForArray<long[]> {
        }

        public static class OfChar extends ForArray<char[]> {
        }

        public static class OfFloat extends ForArray<float[]> {
        }

        public static class OfDouble extends ForArray<double[]> {
        }

        public static class OfBoolean extends ForArray<boolean[]> {
        }

        protected ForArray() {
            super(Array::getLength);
        }
    }

    public static class ForCharSequence extends SizeValidator<CharSequence> {
        public ForCharSequence() {
            super(CharSequence::length);
        }
    }

    public static class ForCollection extends SizeValidator<Collection<?>> {

        public ForCollection() {
            super(Collection::size);
        }
    }

    public static class ForMap extends SizeValidator<Map<?, ?>> {
        public ForMap() {
            super(Map::size);
        }
    }

    private final ToIntFunction<? super T> sizeOf;

    protected int min;
    protected int max;

    protected SizeValidator(ToIntFunction<? super T> sizeOf) {
        super();
        this.sizeOf = sizeOf;
    }

    /**
     * Configure the constraint validator based on the elements specified at the time it was defined.
     *
     * @param constraint
     *            the constraint definition
     */
    public void initialize(Size constraint) {
        min = constraint.min();
        max = constraint.max();
        if (min < 0) {
            throw new ValidationException("Min cannot be negative");
        }
        if (max < 0) {
            throw new ValidationException("Max cannot be negative");
        }
        if (max < min) {
            throw new ValidationException("Max cannot be less than Min");
        }
    }

    @Override
    public boolean isValid(T value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        final int size = sizeOf.applyAsInt(value);
        return min <= size && size <= max;
    }
}