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
package org.apache.bval.jsr.xml;

import org.apache.bval.ConstructorAccess;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.FieldAccess;
import org.apache.bval.util.MethodAccess;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Description: hold parsed information from xml to complete MetaBean later<br/>
 */
//TODO move this guy up to org.apache.bval.jsr or org.apache.bval.jsr.model
//to decouple ApacheValidatorFactory from xml package and allow others to consume MetaConstraint
public class MetaConstraint<T, A extends Annotation> {

    /** The member the constraint was defined on. */
    private final Member member;

    /** The class of the bean hosting this constraint. */
    private final Class<T> beanClass;

    /** constraint annotation (proxy) */
    private final A annotation;

    private Integer index; // for parameters

    private final AccessStrategy accessStrategy;

    /**
     * Create a new MetaConstraint instance.
     * @param beanClass The class in which the constraint is defined on
     * @param member    The member on which the constraint is defined on, {@code null} if it is a class constraint}
     * @param annotation
     */
    public MetaConstraint(Class<T> beanClass, Member member, A annotation) {
        this.member = member;
        this.beanClass = beanClass;
        this.annotation = annotation;
        if (member != null) {
            accessStrategy = createAccessStrategy(member);
            /*TODO: see if can really be removed
            if (accessStrategy == null || accessStrategy.getPropertyName() == null) { // can happen if method does not follow the bean convention
                throw new ValidationException("Annotated method does not follow the JavaBeans naming convention: " + member);
            }
            */
        } else {
            this.accessStrategy = null;
        }
    }

    private static AccessStrategy createAccessStrategy(Member member) {
        if (member instanceof Method) {
            return new MethodAccess((Method) member);
        } else if (member instanceof Field) {
            return new FieldAccess((Field) member);
        } else if (member instanceof Constructor<?>) {
            return new ConstructorAccess((Constructor<?>) member);
        } else {
            return null; // class level
        }
    }

    /**
     * Get the bean class of this constraint.
     * @return Class
     */
    public Class<T> getBeanClass() {
        return beanClass;
    }

    /**
     * Get the member to which this constraint applies.
     * @return Member
     */
    public Member getMember() {
        return member;
    }

    /**
     * Get the annotation that defines this constraint.
     * @return Annotation
     */
    public A getAnnotation() {
        return annotation;
    }

    /**
     * Get the access strategy used for the associated property.
     * @return {@link AccessStrategy}
     */
    public AccessStrategy getAccessStrategy() {
        return accessStrategy;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(final int index) {
        this.index = index;
    }
}
