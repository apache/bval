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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.Constraint;
import javax.validation.GroupDefinitionException;
import javax.validation.GroupSequence;
import javax.validation.ValidationException;
import javax.validation.groups.Default;

import org.apache.bval.MetaBeanFactory;
import org.apache.bval.jsr303.groups.Group;
import org.apache.bval.jsr303.util.ClassHelper;
import org.apache.bval.jsr303.util.SecureActions;
import org.apache.bval.jsr303.xml.MetaConstraint;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.FieldAccess;
import org.apache.bval.util.MethodAccess;

/**
 * Description: process the class annotations for JSR303 constraint validations to build the MetaBean with information
 * from annotations and JSR303 constraint mappings (defined in xml)<br/>
 */
public class Jsr303MetaBeanFactory implements MetaBeanFactory {
    /** Shared log instance */
    // of dubious utility as it's static :/
    protected static final Logger log = Logger.getLogger(Jsr303MetaBeanFactory.class.getName());

    /** {@link ApacheFactoryContext} used */
    protected final ApacheFactoryContext factoryContext;

    /**
     * {@link AnnotationProcessor} used.
     */
    protected AnnotationProcessor annotationProcessor;

    /**
     * Create a new Jsr303MetaBeanFactory instance.
     * 
     * @param factoryContext
     */
    public Jsr303MetaBeanFactory(ApacheFactoryContext factoryContext) {
        this.factoryContext = factoryContext;
        this.annotationProcessor = new AnnotationProcessor(factoryContext);
    }

    /**
     * {@inheritDoc} Add the validation features to the metabean that come from JSR303 annotations in the beanClass.
     */
    public void buildMetaBean(MetaBean metabean) {
        try {
            final Class<?> beanClass = metabean.getBeanClass();
            processGroupSequence(beanClass, metabean);

            // process class, superclasses and interfaces
            List<Class<?>> classSequence = new ArrayList<Class<?>>();
            ClassHelper.fillFullClassHierarchyAsList(classSequence, beanClass);

            // start with superclasses and go down the hierarchy so that
            // the child classes are processed last to have the chance to
            // overwrite some declarations
            // of their superclasses and that they see what they inherit at the
            // time of processing
            for (int i = classSequence.size() - 1; i >= 0; i--) {
                Class<?> eachClass = classSequence.get(i);
                processClass(eachClass, metabean);
                processGroupSequence(eachClass, metabean, "{GroupSequence:" + eachClass.getCanonicalName() + "}");
            }

        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e.getTargetException());
        }
    }

    /**
     * Process class annotations, field and method annotations.
     * 
     * @param beanClass
     * @param metabean
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void processClass(Class<?> beanClass, MetaBean metabean) throws IllegalAccessException,
        InvocationTargetException {

        // if NOT ignore class level annotations
        if (!factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotations(beanClass)) {
            annotationProcessor.processAnnotations(null, beanClass, beanClass, null, new AppendValidationToMeta(
                metabean));
        }

        final Field[] fields = doPrivileged(SecureActions.getDeclaredFields(beanClass));
        for (Field field : fields) {
            MetaProperty metaProperty = metabean.getProperty(field.getName());
            // create a property for those fields for which there is not yet a
            // MetaProperty
            if (!factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotations(field)) {
                AccessStrategy access = new FieldAccess(field);
                boolean create = metaProperty == null;
                if (create) {
                    metaProperty = addMetaProperty(metabean, access);
                }
                if (!annotationProcessor.processAnnotations(metaProperty, beanClass, field, access,
                    new AppendValidationToMeta(metaProperty)) && create) {
                    metabean.putProperty(metaProperty.getName(), null);
                }
            }
        }
        final Method[] methods = doPrivileged(SecureActions.getDeclaredMethods(beanClass));
        for (Method method : methods) {
            String propName = null;
            if (method.getParameterTypes().length == 0) {
                propName = MethodAccess.getPropertyName(method);
            }
            if (propName != null) {
                if (!factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotations(method)) {
                    AccessStrategy access = new MethodAccess(propName, method);
                    MetaProperty metaProperty = metabean.getProperty(propName);
                    boolean create = metaProperty == null;
                    // create a property for those methods for which there is
                    // not yet a MetaProperty
                    if (create) {
                        metaProperty = addMetaProperty(metabean, access);
                    }
                    if (!annotationProcessor.processAnnotations(metaProperty, beanClass, method, access,
                        new AppendValidationToMeta(metaProperty)) && create) {
                        metabean.putProperty(propName, null);
                    }
                }
            } else if (hasValidationConstraintsDefined(method)) {
                throw new ValidationException("Property " + method.getName() + " does not follow javabean conventions.");
            }
        }

        addXmlConstraints(beanClass, metabean);
    }

    /**
     * Learn whether a given Method has validation constraints defined via JSR303 annotations.
     * 
     * @param method
     * @return <code>true</code> if constraints detected
     */
    protected boolean hasValidationConstraintsDefined(Method method) {
        for (Annotation annot : method.getDeclaredAnnotations()) {
            if (hasValidationConstraintsDefined(annot)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasValidationConstraintsDefined(Annotation annot) {
        // If it is annotated with @Constraint
        if (annot.annotationType().getAnnotation(Constraint.class) != null) {
            return true;
        }

        // Check whether it is a multivalued constraint:
        if (ConstraintAnnotationAttributes.VALUE.isDeclaredOn(annot.annotationType())) {
            Annotation[] children = ConstraintAnnotationAttributes.VALUE.getValue(annot);
            if (children != null) {
                for (Annotation child : children) {
                    if (hasValidationConstraintsDefined(child)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Add cascade validation and constraints from xml mappings
     * 
     * @param beanClass
     * @param metabean
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void addXmlConstraints(Class<?> beanClass, MetaBean metabean) throws IllegalAccessException,
        InvocationTargetException {
        for (MetaConstraint<?, ? extends Annotation> meta : factoryContext.getFactory().getMetaConstraints(beanClass)) {
            MetaProperty metaProperty;
            AccessStrategy access = meta.getAccessStrategy();
            boolean create = false;
            if (access == null) { // class level
                metaProperty = null;
            } else { // property level
                metaProperty = metabean.getProperty(access.getPropertyName());
                create = metaProperty == null;
                if (create) {
                    metaProperty = addMetaProperty(metabean, access);
                }
            }
            if (!annotationProcessor.processAnnotation(meta.getAnnotation(), metaProperty, beanClass,
                meta.getAccessStrategy(), new AppendValidationToMeta(metaProperty == null ? metabean : metaProperty))
                && create) {
                metabean.putProperty(access.getPropertyName(), null);
            }
        }
        for (AccessStrategy access : factoryContext.getFactory().getValidAccesses(beanClass)) {
            MetaProperty metaProperty = metabean.getProperty(access.getPropertyName());
            boolean create = metaProperty == null;
            if (create) {
                metaProperty = addMetaProperty(metabean, access);
            }
            if (!annotationProcessor.addAccessStrategy(metaProperty, access) && create) {
                metabean.putProperty(access.getPropertyName(), null);
            }
        }
    }

    private void processGroupSequence(Class<?> beanClass, MetaBean metabean) {
        processGroupSequence(beanClass, metabean, Jsr303Features.Bean.GROUP_SEQUENCE);
    }

    private void processGroupSequence(Class<?> beanClass, MetaBean metabean, String key) {
        GroupSequence annotation = beanClass.getAnnotation(GroupSequence.class);
        List<Group> groupSeq = metabean.getFeature(key);
        if (groupSeq == null) {
            groupSeq = new ArrayList<Group>(annotation == null ? 1 : annotation.value().length);
            metabean.putFeature(key, groupSeq);
        }
        Class<?>[] groupClasses = factoryContext.getFactory().getDefaultSequence(beanClass);
        if (groupClasses == null || groupClasses.length == 0) {
            if (annotation == null) {
                groupSeq.add(Group.DEFAULT);
                return;
            } else {
                groupClasses = annotation.value();
            }
        }
        boolean containsDefault = false;
        for (Class<?> groupClass : groupClasses) {
            if (groupClass.getName().equals(beanClass.getName())) {
                groupSeq.add(Group.DEFAULT);
                containsDefault = true;
            } else if (groupClass.getName().equals(Default.class.getName())) {
                throw new GroupDefinitionException("'Default.class' must not appear in @GroupSequence! Use '"
                    + beanClass.getSimpleName() + ".class' instead.");
            } else {
                groupSeq.add(new Group(groupClass));
            }
        }
        if (!containsDefault) {
            throw new GroupDefinitionException("Redefined default group sequence must contain " + beanClass.getName());
        }
        log.log(Level.FINEST, String.format("Default group sequence for bean %s is: %s", beanClass.getName(), groupSeq));
    }

    /**
     * Add a {@link MetaProperty} to a {@link MetaBean}.
     * @param parentMetaBean
     * @param access
     * @return the created {@link MetaProperty}
     */
    public static MetaProperty addMetaProperty(MetaBean parentMetaBean, AccessStrategy access) {
        final MetaProperty result = new MetaProperty();
        final String name = access.getPropertyName();
        result.setName(name);
        result.setType(access.getJavaType());
        parentMetaBean.putProperty(name, result);
        return result;
    }




    /**
     * Perform action with AccessController.doPrivileged() if a security manager is installed.
     *
     * @param action
     *  the action to run
     * @return
     *  result of the action
     */
    private static <T> T doPrivileged(final PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
    }
}
