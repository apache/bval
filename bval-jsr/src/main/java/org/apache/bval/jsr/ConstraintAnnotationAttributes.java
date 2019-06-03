/**
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
package org.apache.bval.jsr;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

import javax.validation.Constraint;
import javax.validation.ConstraintTarget;
import javax.validation.Payload;

import org.apache.bval.util.Exceptions;
import org.apache.bval.util.ObjectUtils;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.bval.util.reflection.TypeUtils;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Defines the well-known attributes of {@link Constraint} annotations.
 * 
 * @version $Rev: 1165923 $ $Date: 2011-09-06 18:07:53 -0500 (Tue, 06 Sep 2011) $
 */
@Privilizing(@CallTo(Reflection.class))
public enum ConstraintAnnotationAttributes {
    /**
     * "message"
     */
    MESSAGE("message", m -> true),

    /**
     * "groups"
     */
    GROUPS("groups", ObjectUtils::isEmptyArray),

    /**
     * "payload"
     */
    PAYLOAD("payload", ObjectUtils::isEmptyArray),

    /**
     * "validationAppliesTo"
     */
    VALIDATION_APPLIES_TO("validationAppliesTo", Predicate.isEqual(ConstraintTarget.IMPLICIT)),

    /**
     * "value" for multi-valued constraints
     */
    VALUE("value", ObjectUtils::isEmptyArray);

    @SuppressWarnings("unused")
    private static class Types {
        String message;
        Class<?>[] groups;
        Class<? extends Payload>[] payload;
        Annotation[] value;
        ConstraintTarget validationAppliesTo;
    }

    private static final Set<ConstraintAnnotationAttributes> MANDATORY =
            Collections.unmodifiableSet(EnumSet.of(ConstraintAnnotationAttributes.MESSAGE,
                ConstraintAnnotationAttributes.GROUPS, ConstraintAnnotationAttributes.PAYLOAD));

    private final Class<?> type;
    private final String attributeName;
    private final Predicate<Object> validateDefaultValue;

    private ConstraintAnnotationAttributes(final String name, Predicate<Object> validateDefaultValue) {
        this.attributeName = name;
        try {
            this.type = Types.class.getDeclaredField(getAttributeName()).getType();
        } catch (Exception e) {
            // should never happen
            throw new RuntimeException(e);
        }
        this.validateDefaultValue = Validate.notNull(validateDefaultValue, "validateDefaultValue");
    }

    /**
     * Get the expected type of the represented attribute.
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Get the attribute name represented.
     * 
     * @return String
     */
    public String getAttributeName() {
        return attributeName;
    }

    @Override
    public String toString() {
        return attributeName;
    }

    /**
     * Put <code>value</code> into a map with <code>this.attributeName</code> as
     * key.
     * 
     * @param <V>
     * @param map
     * @param value
     * @return previous value mapped to <code>this.attributeName</code>
     */
    public <V> Object put(Map<? super String, ? super V> map, V value) {
        return map.put(getAttributeName(), value);
    }

    /**
     * Get the value of <code>this.attributeName</code> from <code>map</code>.
     * 
     * @param <V>
     * @param map
     * @return V if you say so
     */
    public <V> V get(Map<? super String, ? super V> map) {
        @SuppressWarnings("unchecked")
        final V result = (V) map.get(getAttributeName());
        if (!TypeUtils.isInstance(result, getType())) {
            Exceptions.raise(IllegalStateException::new, "Invalid '%s' value: %s", getAttributeName(), result);
        }
        return result;
    }

    public <C extends Annotation> Worker<C> analyze(final Class<C> clazz) {
        if (clazz.getName().startsWith("javax.validation.constraint.")) { // cache only APIs classes to avoid memory leaks
            @SuppressWarnings({ "unchecked", "rawtypes" })
            final Worker<C> w = (Worker<C>) WORKER_CACHE.computeIfAbsent(clazz, c -> new Worker((c)));
            return w;
        }
        return new Worker<C>(clazz);
    }

    public boolean isMandatory() {
        return MANDATORY.contains(this);
    }

    public boolean isValidDefaultValue(Object o) {
        return validateDefaultValue.test(o);
    }

    // this is static but related to Worker
    private static final ConcurrentMap<Class<?>, Worker<?>> WORKER_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Method>> METHOD_BY_NAME_AND_CLASS =
        new ConcurrentHashMap<>();
    private static final Method NULL_METHOD;
    static {
        try {
            NULL_METHOD = Object.class.getMethod("hashCode"); // whatever, the only constraint here is to not use a constraint method, this value is used to cache null
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Impossible normally");
        }
    }

    @Privilizing(@CallTo(Reflection.class))
    public class Worker<C extends Annotation> {

        public final Method method;

        /**
         * Create a new Worker instance.
         * @param constraintType to handle
         */
        Worker(final Class<C> constraintType) {
            method = findMethod(constraintType, attributeName);
        }

        private Method findMethod(final Class<C> constraintType, final String attributeName) {
            ConcurrentMap<String, Method> cache =
                METHOD_BY_NAME_AND_CLASS.computeIfAbsent(constraintType, t -> new ConcurrentHashMap<>());

            final Method found = cache.get(attributeName);
            if (found != null) {
                return found;
            }
            final Method m = Reflection.getPublicMethod(constraintType, attributeName);
            if (m == null) {
                cache.putIfAbsent(attributeName, NULL_METHOD);
                return null;
            }
            return cache.computeIfAbsent(attributeName, s -> m);
        }

        public boolean isValid() {
            return method != null && method != NULL_METHOD && TypeUtils.isAssignable(method.getReturnType(), type);
        }

        /**
         * @since 2.0
         * @return {@link Type}
         */
        public Type getSpecificType() {
            return isValid() ? method.getGenericReturnType() : type;
        }

        public <T> T read(final Annotation constraint) {
            @SuppressWarnings("unchecked")
            final T result = (T) doInvoke(constraint);
            return result;
        }

        private Object doInvoke(final Annotation constraint) {
            Reflection.makeAccessible(method);
            try {
                return method.invoke(constraint);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
