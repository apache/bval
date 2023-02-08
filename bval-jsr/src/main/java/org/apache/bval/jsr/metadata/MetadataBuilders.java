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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.validation.ValidationException;

import org.apache.bval.jsr.util.Methods;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;

public class MetadataBuilders {

    private final Map<Class<?>, List<MetadataBuilder.ForBean<?>>> beanBuilders = new ConcurrentHashMap<>();

    public <T> void registerCustomBuilder(Class<T> bean, MetadataBuilder.ForBean<T> builder) {
        Validate.notNull(bean, "bean");
        Validate.notNull(builder, "builder");
        validateCustomBuilder(bean, builder);
        beanBuilders.computeIfAbsent(bean, c -> new ArrayList<>()).add(builder);
    }

    public <T> List<MetadataBuilder.ForBean<T>> getCustomBuilders(Class<T> bean) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final List<MetadataBuilder.ForBean<T>> list = (List) beanBuilders.get(bean);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public Set<Class<?>> getCustomizedTypes() {
        return beanBuilders.keySet();
    }

    private <T> void validateCustomBuilder(Class<T> bean, MetadataBuilder.ForBean<T> builder) {
        final Meta<Class<T>> meta = new Meta.ForClass<>(bean);
        final Set<String> propertyNames = builder.getGetters(meta).keySet();
        builder.getMethods(meta).keySet().stream().map(Signature::getName).filter(Methods::isGetter)
            .map(Methods::propertyName).forEach(pn -> {
                Exceptions.raiseIf(propertyNames.contains(pn), ValidationException::new,
                    "%s user metadata cannot specify both method and getter elements for %s", f -> f.args(bean, pn));
            });
    }
}
