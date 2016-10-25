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
package org.apache.bval.jsr;

import org.apache.bval.MetaBeanFactory;
import org.apache.bval.jsr.groups.Group;
import org.apache.bval.jsr.util.ClassHelper;
import org.apache.bval.jsr.xml.MetaConstraint;
import org.apache.bval.model.Features.Property;
import org.apache.bval.model.Meta;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaConstructor;
import org.apache.bval.model.MetaMethod;
import org.apache.bval.model.MetaParameter;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.FieldAccess;
import org.apache.bval.util.MethodAccess;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

import javax.validation.ConstraintDeclarationException;
import javax.validation.GroupDefinitionException;
import javax.validation.GroupSequence;
import javax.validation.groups.ConvertGroup;
import javax.validation.groups.Default;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Description: process the class annotations for JSR303 constraint validations to build the MetaBean with information
 * from annotations and JSR303 constraint mappings (defined in xml)<br/>
 */
@Privilizing(@CallTo(Reflection.class))
public class JsrMetaBeanFactory implements MetaBeanFactory {
    /** Shared log instance */
    // of dubious utility as it's static :/
    protected static final Logger log = Logger.getLogger(JsrMetaBeanFactory.class.getName());

    /** {@link javax.validation.ValidatorFactory} used */
    protected final ApacheValidatorFactory factory;

    /**
     * {@link AnnotationProcessor} used.
     */
    protected AnnotationProcessor annotationProcessor;

    /**
     * Create a new Jsr303MetaBeanFactory instance.
     * 
     * @param factory the validator factory.
     */
    public JsrMetaBeanFactory(ApacheValidatorFactory factory) {
        this.factory = factory;
        this.annotationProcessor = new AnnotationProcessor(factory);
    }

    /**
     * {@inheritDoc} Add the validation features to the metabean that come from JSR303 annotations in the beanClass.
     */
    @Override
    public void buildMetaBean(MetaBean metabean) {
        try {
            final Class<?> beanClass = metabean.getBeanClass();
            processGroupSequence(beanClass, metabean);

            // process class, superclasses and interfaces
            final List<Class<?>> classSequence =
                ClassHelper.fillFullClassHierarchyAsList(new ArrayList<Class<?>>(), beanClass);

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
    private void processClass(Class<?> beanClass, MetaBean metabean)
        throws IllegalAccessException, InvocationTargetException {

        // if NOT ignore class level annotations
        if (!factory.getAnnotationIgnores().isIgnoreAnnotations(beanClass)) {
            annotationProcessor.processAnnotations(null, beanClass, beanClass, null,
                new AppendValidationToMeta(metabean));
        }

        final Collection<String> missingValid = new ArrayList<String>();

        final Field[] fields = Reflection.getDeclaredFields(beanClass);
        for (final Field field : fields) {
            MetaProperty metaProperty = metabean.getProperty(field.getName());
            // create a property for those fields for which there is not yet a
            // MetaProperty
            if (!factory.getAnnotationIgnores().isIgnoreAnnotations(field)) {
                AccessStrategy access = new FieldAccess(field);
                boolean create = metaProperty == null;
                if (create) {
                    metaProperty = addMetaProperty(metabean, access);
                }
                if (!annotationProcessor.processAnnotations(metaProperty, beanClass, field, access,
                    new AppendValidationToMeta(metaProperty)) && create) {
                    metabean.putProperty(metaProperty.getName(), null);
                }

                if (field.getAnnotation(ConvertGroup.class) != null) {
                    missingValid.add(field.getName());
                }
            }
        }
        final Method[] methods = Reflection.getDeclaredMethods(beanClass);
        for (final Method method : methods) {
            if (method.isSynthetic() || method.isBridge()) {
                continue;
            }
            String propName = null;
            if (method.getParameterTypes().length == 0) {
                propName = MethodAccess.getPropertyName(method);
            }
            if (propName != null) {
                if (!factory.getAnnotationIgnores().isIgnoreAnnotations(method)) {
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
            }
        }

        addXmlConstraints(beanClass, metabean);

        for (final String name : missingValid) {
            final MetaProperty metaProperty = metabean.getProperty(name);
            if (metaProperty != null && metaProperty.getFeature(Property.REF_CASCADE) == null) {
                throw new ConstraintDeclarationException("@ConvertGroup needs @Valid");
            }
        }
        missingValid.clear();
    }

    /**
     * Add cascade validation and constraints from xml mappings
     * 
     * @param beanClass
     * @param metabean
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void addXmlConstraints(Class<?> beanClass, MetaBean metabean)
        throws IllegalAccessException, InvocationTargetException {
        for (final MetaConstraint<?, ? extends Annotation> metaConstraint : factory.getMetaConstraints(beanClass)) {
            Meta meta;
            AccessStrategy access = metaConstraint.getAccessStrategy();
            boolean create = false;
            if (access == null) { // class level
                meta = null;
            } else if (access.getElementType() == ElementType.METHOD
                && !metaConstraint.getMember().getName().startsWith("get")) { // TODO: better getter test
                final Method method = Method.class.cast(metaConstraint.getMember());
                meta = metabean.getMethod(method);
                final MetaMethod metaMethod;
                if (meta == null) {
                    meta = new MetaMethod(metabean, method);
                    metaMethod = MetaMethod.class.cast(meta);
                    metabean.addMethod(method, metaMethod);
                } else {
                    metaMethod = MetaMethod.class.cast(meta);
                }
                final Integer index = metaConstraint.getIndex();
                if (index != null && index >= 0) {
                    MetaParameter param = metaMethod.getParameter(index);
                    if (param == null) {
                        param = new MetaParameter(metaMethod, index);
                        metaMethod.addParameter(index, param);
                    }
                    param.addAnnotation(metaConstraint.getAnnotation());
                } else {
                    metaMethod.addAnnotation(metaConstraint.getAnnotation());
                }
                continue;
            } else if (access.getElementType() == ElementType.CONSTRUCTOR) {
                final Constructor<?> constructor = Constructor.class.cast(metaConstraint.getMember());
                meta = metabean.getConstructor(constructor);
                final MetaConstructor metaConstructor;
                if (meta == null) {
                    meta = new MetaConstructor(metabean, constructor);
                    metaConstructor = MetaConstructor.class.cast(meta);
                    metabean.addConstructor(constructor, metaConstructor);
                } else {
                    metaConstructor = MetaConstructor.class.cast(meta);
                }
                final Integer index = metaConstraint.getIndex();
                if (index != null && index >= 0) {
                    MetaParameter param = metaConstructor.getParameter(index);
                    if (param == null) {
                        param = new MetaParameter(metaConstructor, index);
                        metaConstructor.addParameter(index, param);
                    }
                    param.addAnnotation(metaConstraint.getAnnotation());
                } else {
                    metaConstructor.addAnnotation(metaConstraint.getAnnotation());
                }
                continue;
            } else { // property level
                meta = metabean.getProperty(access.getPropertyName());
                create = meta == null;
                if (create) {
                    meta = addMetaProperty(metabean, access);
                }
            }
            if (!annotationProcessor.processAnnotation(metaConstraint.getAnnotation(), meta, beanClass,
                metaConstraint.getAccessStrategy(), new AppendValidationToMeta(meta == null ? metabean : meta), false)
                && create) {
                metabean.putProperty(access.getPropertyName(), null);
            }
        }
        for (final AccessStrategy access : factory.getValidAccesses(beanClass)) {
            if (access.getElementType() == ElementType.PARAMETER) {
                continue;
            }

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
        processGroupSequence(beanClass, metabean, JsrFeatures.Bean.GROUP_SEQUENCE);
    }

    private void processGroupSequence(Class<?> beanClass, MetaBean metabean, String key) {
        GroupSequence annotation = beanClass.getAnnotation(GroupSequence.class);
        List<Group> groupSeq = metabean.getFeature(key);
        if (groupSeq == null) {
            groupSeq =
                metabean.initFeature(key, new ArrayList<Group>(annotation == null ? 1 : annotation.value().length));
        }
        Class<?>[] groupClasses = factory.getDefaultSequence(beanClass);
        if (groupClasses == null || groupClasses.length == 0) {
            if (annotation == null) {
                groupSeq.add(Group.DEFAULT);
                return;
            } else {
                groupClasses = annotation.value();
            }
        }
        boolean containsDefault = false;
        for (final Class<?> groupClass : groupClasses) {
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
        log.log(Level.FINEST,
            String.format("Default group sequence for bean %s is: %s", beanClass.getName(), groupSeq));
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
}
