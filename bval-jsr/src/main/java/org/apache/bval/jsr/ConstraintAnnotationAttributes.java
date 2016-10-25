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

import org.apache.bval.util.reflection.Reflection;
import org.apache.bval.util.reflection.TypeUtils;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

import javax.validation.Constraint;
import javax.validation.ConstraintTarget;
import javax.validation.Payload;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    MESSAGE("message"),

    /**
     * "groups"
     */
    GROUPS("groups"),

    /**
     * "payload"
     */
    PAYLOAD("payload"),

    /**
     * "validationAppliesTo"
     */
    VALIDATION_APPLIES_TO("validationAppliesTo"),

    /**
     * "value" for multi-valued constraints
     */
    VALUE("value");

    @SuppressWarnings("unused")
    private static class Types {
        String message;
        Class<?>[] groups;
        Class<? extends Payload>[] payload;
        Annotation[] value;
        ConstraintTarget validationAppliesTo;
    }

    private final Type type;
    private final String attributeName;

    private ConstraintAnnotationAttributes(final String name) {
        this.attributeName = name;
        try {
            this.type = Types.class.getDeclaredField(getAttributeName()).getGenericType();
        } catch (Exception e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the expected type of the represented attribute.
     */
    public Type getType() {
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
        if (TypeUtils.isInstance(result, getType())) {
            return result;
        }
        throw new IllegalStateException(String.format("Invalid '%s' value: %s", getAttributeName(), result));
    }

    public <C extends Annotation> Worker<C> analyze(final Class<C> clazz) {
        if (clazz.getName().startsWith("javax.validation.constraint.")) { // cache only APIs classes to avoid memory leaks
            @SuppressWarnings("unchecked")
            Worker<C> w = Worker.class.cast(WORKER_CACHE.get(clazz));
            if (w == null) {
                w = new Worker<C>(clazz);
                WORKER_CACHE.putIfAbsent(clazz, w);
                return w;
            }
        }
        return new Worker<C>(clazz);
    }

    // this is static but related to Worker
    private static final ConcurrentMap<Class<?>, Worker<?>> WORKER_CACHE = new ConcurrentHashMap<Class<?>, Worker<?>>();
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Method>> METHOD_BY_NAME_AND_CLASS =
        new ConcurrentHashMap<Class<?>, ConcurrentMap<String, Method>>();
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
            ConcurrentMap<String, Method> cache = METHOD_BY_NAME_AND_CLASS.get(constraintType);
            if (cache == null) {
                cache = new ConcurrentHashMap<String, Method>();
                final ConcurrentMap<String, Method> old = METHOD_BY_NAME_AND_CLASS.putIfAbsent(constraintType, cache);
                if (old != null) {
                    cache = old;
                }
            }

            final Method found = cache.get(attributeName);
            if (found != null) {
                return found;
            }
            final Method m = Reflection.getPublicMethod(constraintType, attributeName);
            if (m == null) {
                cache.putIfAbsent(attributeName, NULL_METHOD);
                return null;
            }
            final Method oldMtd = cache.putIfAbsent(attributeName, m);
            if (oldMtd != null) {
                return oldMtd;
            }
            return m;
        }

        public boolean isValid() {
            return method != null && method != NULL_METHOD;
        }

        public <T> T read(final Annotation constraint) {
            @SuppressWarnings("unchecked")
            final T result = (T) doInvoke(constraint);
            return result;
        }

        private Object doInvoke(final Annotation constraint) {
            final boolean unset = Reflection.setAccessible(method, true);
            try {
                return method.invoke(constraint);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (unset) {
                    Reflection.setAccessible(method, false);
                }
            }
        }
    }
}
