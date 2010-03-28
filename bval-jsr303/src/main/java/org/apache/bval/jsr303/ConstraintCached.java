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
package org.apache.bval.jsr303;

import javax.validation.ConstraintValidator;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * Description: hold the relationship annotation->validatedBy[] ConstraintValidator classes
 * that are already parsed in a cache.<br/>
 * User: roman <br/>
 * Date: 27.11.2009 <br/>
 * Time: 11:48:26 <br/>
 * Copyright: Agimatec GmbH
 */
public class ConstraintCached {
    private final Map<Class<? extends Annotation>, Class<? extends ConstraintValidator<?, ?>>[]> classes =
          new HashMap();

    public <A extends Annotation> void putConstraintValidator(Class<A> annotationClass,
                                                              Class<? extends ConstraintValidator<?, ?>>[] definitionClasses) {
        classes.put(annotationClass, definitionClasses);
    }

    public boolean containsConstraintValidator(Class<? extends Annotation> annotationClass) {
        return classes.containsKey(annotationClass);
    }

    public <A extends Annotation> Class<? extends ConstraintValidator<?, ?>>[] getConstraintValidators(
          Class<A> annotationClass) {
        return classes.get(annotationClass);
    }

}
