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
package org.apache.bval.jsr303.xml;


import javax.validation.ValidationException;

import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.FieldAccess;
import org.apache.bval.util.MethodAccess;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Description: hold parsed information from xml to complete MetaBean later<br/>
 * User: roman <br/>
 * Date: 27.11.2009 <br/>
 * Time: 13:53:40 <br/>
 * Copyright: Agimatec GmbH
 */
public class MetaConstraint<T, A extends Annotation> {

    /** The member the constraint was defined on. */
    private final Member member;

    /** The class of the bean hosting this constraint. */
    private final Class<T> beanClass;

    /** constraint annotation (proxy) */
    private final A annotation;

    private final AccessStrategy accessStrategy;

    /**
     * @param beanClass The class in which the constraint is defined on
     * @param member    The member on which the constraint is defined on, {@code null} if it is a class constraint}
     */
    public MetaConstraint(Class<T> beanClass, Member member, A annotation) {
        this.member = member;
        this.beanClass = beanClass;
        this.annotation = annotation;
        if (member != null) {
            accessStrategy = createAccessStrategy(member);
            if (accessStrategy == null || accessStrategy.getPropertyName() ==
                  null) { // can happen if method does not follow the bean convention
                throw new ValidationException(
                      "Annotated method does not follow the JavaBeans naming convention: " +
                            member);
            }
        } else {
            this.accessStrategy = null;
        }
    }

    private static AccessStrategy createAccessStrategy(Member member) {
        if (member instanceof Method) {
            return new MethodAccess((Method) member);
        } else if (member instanceof Field) {
            return new FieldAccess((Field) member);
        } else {
            return null; // class level
        }
    }

    public Class<T> getBeanClass() {
        return beanClass;
    }

    public Member getMember() {
        return member;
    }

    public A getAnnotation() {
        return annotation;
    }

    public AccessStrategy getAccessStrategy() {
        return accessStrategy;
    }
}
