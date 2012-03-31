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
package org.apache.bval.jsr303.xml;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Description: This class instantiated during the parsing of the XML configuration
 * data and keeps track of the annotations which should be ignored.<br/>
 */
public final class AnnotationIgnores {

    private static final Logger log = Logger.getLogger(AnnotationIgnores.class.getName());

    /**
     * Keeps track whether the 'ignore-annotations' flag is set on bean level in the
     * xml configuration. 
     * If 'ignore-annotations' is not specified: default = true
     */
    private final Map<Class<?>, Boolean> ignoreAnnotationDefaults =
          new HashMap<Class<?>, Boolean>();

    /**
     * Keeps track of explicitly excluded members (fields and properties) for a given class.
     * If a member appears in
     * the list mapped to a given class 'ignore-annotations' was explicitly set to
     * <code>true</code> in the configuration
     * for this class.
     */
    private final Map<Class<?>, List<Member>> ignoreAnnotationOnMember =
          new HashMap<Class<?>, List<Member>>();

    private final Map<Class<?>, Boolean> ignoreAnnotationOnClass =
          new HashMap<Class<?>, Boolean>();

    /**
     * Record the ignore state for a particular annotation type.
     * @param clazz
     * @param b, default true if null
     */
    public void setDefaultIgnoreAnnotation(Class<?> clazz, Boolean b) {
        ignoreAnnotationDefaults.put(clazz, b == null || b.booleanValue());
    }

    /**
     * Learn whether the specified annotation type should be ignored.
     * @param clazz
     * @return boolean
     */
    public boolean getDefaultIgnoreAnnotation(Class<?> clazz) {
        return ignoreAnnotationDefaults.containsKey(clazz)
                && ignoreAnnotationDefaults.get(clazz);
    }

    /**
     * Ignore annotations on a particular {@link Member} of a class.
     * @param member
     */
    public void setIgnoreAnnotationsOnMember(Member member) {
        Class<?> beanClass = member.getDeclaringClass();
        List<Member> memberList = ignoreAnnotationOnMember.get(beanClass);
        if (memberList == null) {
            memberList = new ArrayList<Member>();
            ignoreAnnotationOnMember.put(beanClass, memberList);
        }
        memberList.add(member);
    }

    /**
     * Learn whether annotations should be ignored on a particular {@link Member} of a class.
     * @param member
     * @return boolean
     */
    public boolean isIgnoreAnnotations(Member member) {
        boolean ignoreAnnotation;
        Class<?> clazz = member.getDeclaringClass();
        List<Member> ignoreAnnotationForMembers = ignoreAnnotationOnMember.get(clazz);
        if (ignoreAnnotationForMembers == null ||
              !ignoreAnnotationForMembers.contains(member)) {
            ignoreAnnotation = getDefaultIgnoreAnnotation(clazz);
        } else {
            ignoreAnnotation = ignoreAnnotationForMembers.contains(member);
        }
        if (ignoreAnnotation) {
            logMessage(member, clazz);
        }
        return ignoreAnnotation;
    }

    private void logMessage(Member member, Class<?> clazz) {
        String type;
        if (member instanceof Field) {
            type = "Field";
        } else {
            type = "Property";
        }
        log.log(Level.FINEST, String.format("%s level annotations are getting ignored for %s.%s", type, clazz.getName(), member.getName()));
    }

    /**
     * Record the ignore state of a particular class. 
     * @param clazz
     * @param b
     */
    public void setIgnoreAnnotationsOnClass(Class<?> clazz, boolean b) {
        ignoreAnnotationOnClass.put(clazz, b);
    }

    /**
     * Learn whether annotations should be ignored for a given class.
     * @param clazz to check
     * @return boolean
     */
    public boolean isIgnoreAnnotations(Class<?> clazz) {
        boolean ignoreAnnotation;
        if (ignoreAnnotationOnClass.containsKey(clazz)) {
            ignoreAnnotation = ignoreAnnotationOnClass.get(clazz);
        } else {
            ignoreAnnotation = getDefaultIgnoreAnnotation(clazz);
        }
        if (ignoreAnnotation) {
        	log.log(Level.FINEST, String.format("Class level annotation are getting ignored for %s", clazz.getName()));
        }
        return ignoreAnnotation;
    }
}
