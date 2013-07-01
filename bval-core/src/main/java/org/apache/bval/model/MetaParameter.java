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
package org.apache.bval.model;

import java.lang.annotation.Annotation;

public class MetaParameter extends Meta {
    private final MetaInvocable invocable;
    private final Integer index;
    private Annotation[] annotations = new Annotation[0];

    public MetaParameter(final MetaInvocable metaMethod, final Integer index) {
        this.invocable = metaMethod;
        this.index = index;
    }

    public MetaInvocable getMethod() {
        return invocable;
    }

    public Integer getIndex() {
        return index;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public void addAnnotation(final Annotation annotation) {
        final Annotation[] a = new Annotation[annotations.length + 1];
        System.arraycopy(annotations, 0, a, 0, annotations.length);
        a[a.length - 1] = annotation;
        this.annotations = a;
    }
}
