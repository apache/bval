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
package org.apache.bval.jsr.metadata;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

@Privilizing(@CallTo(Reflection.class))
public abstract class ClassLoadingValidatorMappingProvider extends ValidatorMappingProvider {

    protected final <T> Stream<Class<? extends T>> load(Stream<String> classNames, Class<T> assignableTo,
        Consumer<? super ClassNotFoundException> handleException) {
        return classNames.map(className -> {
            try {
                return Reflection.toClass(className, getClassLoader());
            } catch (ClassNotFoundException e) {
                handleException.accept(e);
                return (Class<?>) null;
            }
        }).filter(Objects::nonNull).map(c -> (Class<? extends T>) c.asSubclass(assignableTo));
    }

    protected ClassLoader getClassLoader() {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        return classloader == null ? getClass().getClassLoader() : classloader;
    }
}
