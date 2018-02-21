package org.apache.bval.jsr.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Utility {@link Collector} definitions.
 */
public class ToUnmodifiable {

    public static <T> Collector<T, ?, Set<T>> set(Supplier<Set<T>> set) {
        return Collectors.collectingAndThen(Collectors.toCollection(set), Collections::unmodifiableSet);
    }
    
    public static <T> Collector<T, ?, Set<T>> set() {
        return Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet);
    }

    public static <T> Collector<T, ?, List<T>> list() {
        return Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> map(Function<? super T, ? extends K> keyMapper,
        Function<? super T, ? extends U> valueMapper) {
        return Collectors.collectingAndThen(Collectors.toMap(keyMapper, valueMapper), Collections::unmodifiableMap);
    }
}
