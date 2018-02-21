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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.bval.util.Validate;

public class CompositeValidatorMappingProvider extends ValidatorMappingProvider {

    private final List<ValidatorMappingProvider> delegates;

    public CompositeValidatorMappingProvider(List<ValidatorMappingProvider> delegates) {
        super();
        this.delegates = Validate.notNull(delegates, "delegates");
        Validate.isTrue(!delegates.isEmpty(), "no delegates specified");
        Validate.isTrue(delegates.stream().noneMatch(Objects::isNull), "One or more supplied delegates was null");
    }

    @Override
    protected <A extends Annotation> ValidatorMapping<A> doGetValidatorMapping(Class<A> constraintType) {
        return ValidatorMapping.merge(delegates.stream().map(d -> d.doGetValidatorMapping(constraintType))
            .filter(Objects::nonNull).collect(Collectors.toList()), AnnotationBehaviorMergeStrategy.consensus());
    }
}
