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

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bval.util.Validate;

@FunctionalInterface
public interface AnnotationBehaviorMergeStrategy
    extends Function<Iterable<? extends HasAnnotationBehavior>, AnnotationBehavior> {

    public static AnnotationBehaviorMergeStrategy first() {
        return coll -> {
            final Iterator<? extends HasAnnotationBehavior> iterator = coll.iterator();
            return iterator.hasNext() ? iterator.next().getAnnotationBehavior() : AnnotationBehavior.ABSTAIN;
        };
    }

    public static AnnotationBehaviorMergeStrategy consensus() {
        return coll -> {
            final Stream.Builder<HasAnnotationBehavior> b = Stream.builder();
            coll.forEach(b);
            final Set<AnnotationBehavior> annotationBehaviors =
                b.build().map(HasAnnotationBehavior::getAnnotationBehavior).filter(Objects::nonNull)
                    .filter(Predicate.isEqual(AnnotationBehavior.ABSTAIN).negate())
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(AnnotationBehavior.class)));
            Validate.validState(annotationBehaviors.size() <= 1,
                "Conflicting annotation inclusion behaviors found among %s", coll);
            return annotationBehaviors.isEmpty() ? AnnotationBehavior.ABSTAIN : annotationBehaviors.iterator().next();
        };
    }
}
