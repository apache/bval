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
package org.apache.bval.jsr303;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.Map;

import javax.validation.Constraint;
import javax.validation.ConstraintDefinitionException;
import javax.validation.Payload;
import javax.validation.ValidationException;

import org.apache.bval.jsr303.util.SecureActions;
import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Defines the well-known attributes of {@link Constraint} annotations.
 * 
 * @version $Rev: 1165923 $ $Date: 2011-09-06 18:07:53 -0500 (Tue, 06 Sep 2011) $
 */
public enum ConstraintAnnotationAttributes {
    /**
     * "message"
     */
    MESSAGE,

    /**
     * "groups"
     */
    GROUPS,

    /**
     * "payload"
     */
    PAYLOAD,

    /**
     * "value" for multi-valued constraints
     */
    VALUE(true);

    @SuppressWarnings("unused")
    private static class Types {
        String message;
        Class<?>[] groups;
        Class<? extends Payload>[] payload;
        Annotation[] value;
    }

    private Type type;
    private boolean permitNullDefaultValue;

    private ConstraintAnnotationAttributes() {
        this(false);
    }

    private ConstraintAnnotationAttributes(boolean permitNullDefaultValue) {
        this.permitNullDefaultValue = permitNullDefaultValue;
        try {
            this.type = Types.class.getDeclaredField(getAttributeName()).getGenericType();
        } catch (Exception e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the expected type of the represented attribute.
     * 
     * @return Class<?>
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
        return name().toLowerCase(Locale.US);
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
        if (!TypeUtils.isInstance(value, getType())) {
            throw new IllegalArgumentException(String.format("Invalid '%s' value: %s", getAttributeName(), value));
        }
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
            throw new IllegalStateException(String.format("Invalid '%s' value: %s", getAttributeName(), result));
        }
        return result;
    }

    /**
     * Verify that this attribute is validly defined on the given type.
     * 
     * @param type
     * @throws ConstraintDefinitionException
     */
    public <A extends Annotation> void validateOn(Class<A> type) {
        new Worker<A>(type);
    }

    /**
     * Benign means of checking for an attribute's existence.
     * 
     * @param type
     * @return whether the attribute was (properly) declared
     */
    public <A extends Annotation> boolean isDeclaredOn(Class<A> type) {
        return new Worker<A>(type, true).valid;
    }

    /**
     * Get the value of this attribute from the specified constraint annotation.
     * 
     * @param constraint
     * @return Object
     */
    public <T> T getValue(Annotation constraint) {
        try {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            T result = (T) new Worker(constraint.annotationType()).read(constraint);
            return result;
        } catch (Exception e) {
            throw new ValidationException(String.format("Could not get value of %1$s() from %2$s", getType(),
                constraint));
        }
    }

    /**
     * Get the default value of this attribute on the given annotation type.
     * @param <T>
     * @param type
     * @return Object
     */
    public <T, A extends Annotation> T getDefaultValue(Class<A> type) {
        @SuppressWarnings("unchecked")
        final T result = (T) new Worker<A>(type).defaultValue;
        return result;
    }

    private static <T> T doPrivileged(final PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
    }

    private class Worker<C> {
        final Method method;
        final Object defaultValue;
        final boolean valid;

        /**
         * Create a new Worker instance.
         * @param constraintType to handle
         */
        Worker(Class<C> constraintType) {
            this(constraintType, false);
        }

        /**
         * Create a new Worker instance.
         * @param constraintType to handle
         * @param quiet whether to simply set !valid rather than throw an Exception on error
         */
        Worker(Class<C> constraintType, boolean quiet) {
            super();
            boolean _valid = true;
            Object _defaultValue = null;
            try {
                method = doPrivileged(SecureActions.getPublicMethod(constraintType, getAttributeName()));
                if (method == null) {
                    if (quiet) {
                        _valid = false;
                        return;
                    }
                    throw new ConstraintDefinitionException(String.format("Annotation %1$s has no %2$s() method",
                        constraintType, getAttributeName()));
                }

                if (!TypeUtils.isAssignable(method.getReturnType(), getType())) {
                    if (quiet) {
                        _valid = false;
                        return;
                    }
                    throw new ConstraintDefinitionException(String.format(
                        "Return type for %1$s() must be of type %2$s", getAttributeName(), getType()));
                }
                _defaultValue = method.getDefaultValue();
                if (_defaultValue == null && permitNullDefaultValue) {
                    return;
                }
                if (TypeUtils.isArrayType(getType()) && Array.getLength(_defaultValue) > 0) {
                    if (quiet) {
                        _valid = false;
                        return;
                    }
                    throw new ConstraintDefinitionException(String.format(
                        "Default value for %1$s() must be an empty array", getAttributeName()));
                }
            } finally {
                valid = _valid;
                defaultValue = _defaultValue;
            }
        }

        <T> T read(final C constraint) {
            @SuppressWarnings("unchecked")
            T result = (T) doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    try {
                        method.setAccessible(true);
                        return method.invoke(constraint);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return result;
        }
    }
}
