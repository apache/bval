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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.metadata.Scope;

import org.apache.bval.jsr.groups.GroupConversion;

/**
 * Common interface for populating the Bean Validation descriptors from various sources. Most implementations should
 * concern themselves with a single level of an inheritance hierarchy.
 */
public final class MetadataBuilder {

    public interface ForBean extends HasAnnotationBehavior {
        MetadataBuilder.ForClass getClass(Meta<Class<?>> meta);

        Map<String, ForContainer<Field>> getFields(Meta<Class<?>> meta);

        /**
         * Returned keys are property names per XML mapping spec.
         * 
         * @param meta
         * @return {@link Map}
         */
        Map<String, ForContainer<Method>> getGetters(Meta<Class<?>> meta);

        Map<Signature, ForExecutable<Constructor<?>>> getConstructors(Meta<Class<?>> meta);

        Map<Signature, ForExecutable<Method>> getMethods(Meta<Class<?>> meta);

        default boolean isEmpty() {
            return false;
        }
    }

    public interface ForElement<E extends AnnotatedElement> extends HasAnnotationBehavior {

        Annotation[] getDeclaredConstraints(Meta<E> meta);

        default Map<Scope, Annotation[]> getConstraintsByScope(Meta<E> meta) {
            return Collections.singletonMap(Scope.LOCAL_ELEMENT, getDeclaredConstraints(meta));
        }
    }

    public interface ForClass extends ForElement<Class<?>> {

        List<Class<?>> getGroupSequence(Meta<Class<?>> meta);
    }

    public interface ForContainer<E extends AnnotatedElement> extends MetadataBuilder.ForElement<E> {

        boolean isCascade(Meta<E> meta);

        Set<GroupConversion> getGroupConversions(Meta<E> meta);

        Map<ContainerElementKey, ForContainer<AnnotatedType>> getContainerElementTypes(Meta<E> meta);
    }

    public interface ForExecutable<E extends Executable> extends HasAnnotationBehavior {

        MetadataBuilder.ForContainer<E> getReturnValue(Meta<E> meta);

        MetadataBuilder.ForElement<E> getCrossParameter(Meta<E> meta);

        List<ForContainer<Parameter>> getParameters(Meta<E> meta);
    }

    private MetadataBuilder() {
    }
}
