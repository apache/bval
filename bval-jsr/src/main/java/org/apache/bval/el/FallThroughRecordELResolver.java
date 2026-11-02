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
package org.apache.bval.el;

import java.lang.reflect.RecordComponent;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.el.ELContext;
import jakarta.el.RecordELResolver;

/**
 * {@link RecordELResolver} that resolves properties named after a component of the {@link Record} and falls through
 * to the next resolver for everything else.
 * <p>
 * The plain {@link RecordELResolver} claims every property of a record and invokes any public no-arg method of that
 * name. This resolver leaves other properties unresolved, so JavaBean getters declared on a record are still handled
 * by a subsequent {@link jakarta.el.BeanELResolver}, and arbitrary methods are not invoked through property syntax.
 */
class FallThroughRecordELResolver extends RecordELResolver {
    /**
     * Component names per record class. {@link Class#getRecordComponents()} is not cached by the JDK and builds new
     * {@link RecordComponent} instances on every call. {@link ClassValue} keeps the entry tied to the class lifecycle,
     * so the cache does not pin application classloaders.
     */
    private static final ClassValue<Set<String>> COMPONENT_NAMES = new ClassValue<>() {
        @Override
        protected Set<String> computeValue(final Class<?> type) {
            return Stream.of(type.getRecordComponents()).map(RecordComponent::getName)
                .collect(Collectors.toUnmodifiableSet());
        }
    };

    @Override
    public Object getValue(final ELContext context, final Object base, final Object property) {
        final String component = componentName(context, base, property);
        return component == null ? null : super.getValue(context, base, component);
    }

    @Override
    public Class<?> getType(final ELContext context, final Object base, final Object property) {
        final String component = componentName(context, base, property);
        return component == null ? null : super.getType(context, base, component);
    }

    @Override
    public boolean isReadOnly(final ELContext context, final Object base, final Object property) {
        final String component = componentName(context, base, property);
        return component != null && super.isReadOnly(context, base, component);
    }

    @Override
    public void setValue(final ELContext context, final Object base, final Object property, final Object value) {
        final String component = componentName(context, base, property);
        if (component != null) {
            super.setValue(context, base, component, value);
        }
    }

    /**
     * @return {@code property} as a String if {@code base} is a {@link Record} with a component of that name,
     *         otherwise {@code null}
     */
    private static String componentName(final ELContext context, final Object base, final Object property) {
        Objects.requireNonNull(context);
        if (base instanceof Record && property != null) {
            final String name = property.toString();
            if (COMPONENT_NAMES.get(base.getClass()).contains(name)) {
                return name;
            }
        }
        return null;
    }
}
