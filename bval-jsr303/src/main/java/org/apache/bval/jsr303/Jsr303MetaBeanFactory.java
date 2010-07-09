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


import org.apache.bval.MetaBeanFactory;
import org.apache.bval.jsr303.groups.Group;
import org.apache.bval.jsr303.util.ClassHelper;
import org.apache.bval.jsr303.util.ConstraintDefinitionValidator;
import org.apache.bval.jsr303.util.SecureActions;
import org.apache.bval.jsr303.util.TypeUtils;
import org.apache.bval.jsr303.xml.MetaConstraint;
import org.apache.bval.model.Features;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.FieldAccess;
import org.apache.bval.util.MethodAccess;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.validation.*;
import javax.validation.groups.Default;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Description: process the class annotations for JSR303 constraint validations
 * to build the MetaBean with information from annotations and JSR303 constraint
 * mappings (defined in xml)<br/>
 */
public class Jsr303MetaBeanFactory implements MetaBeanFactory {
    /** Shared log instance */ //of dubious utility as it's static :/
    protected static final Log log = LogFactory.getLog(Jsr303MetaBeanFactory.class);
    /** Constant for the "value" annotation attribute specified in JSR303*/
    protected static final String ANNOTATION_VALUE = "value";
    /** {@link ApacheFactoryContext} used */
    protected final ApacheFactoryContext factoryContext;

    /**
     * Create a new Jsr303MetaBeanFactory instance.
     * @param factoryContext
     */
    public Jsr303MetaBeanFactory(ApacheFactoryContext factoryContext) {
        this.factoryContext = factoryContext;
    }

    private ConstraintValidatorFactory getConstraintValidatorFactory() {
        return factoryContext.getConstraintValidatorFactory();
    }

    private ConstraintDefaults getDefaultConstraints() {
        return factoryContext.getFactory().getDefaultConstraints();
    }

    /**
     * {@inheritDoc}
     * Add the validation features to the metabean that come from JSR303
     * annotations in the beanClass.
     */
    public void buildMetaBean(MetaBean metabean) {
        try {
            final Class<?> beanClass = metabean.getBeanClass();
            processGroupSequence(beanClass, metabean);

            // process class, superclasses and interfaces
            List<Class<?>> classSequence = new ArrayList<Class<?>>();
            ClassHelper.fillFullClassHierarchyAsList(classSequence, beanClass);

            // start with superclasses and go down the hierarchy so that
            // the child classes are processed last to have the chance to overwrite some declarations
            // of their superclasses and that they see what they inherit at the time of processing
            for (int i = classSequence.size() - 1; i >= 0; i--) {
                Class<?> eachClass = classSequence.get(i);
                processClass(eachClass, metabean);
                processGroupSequence(eachClass, metabean, "{GroupSequence:"+eachClass.getCanonicalName()+"}");
            }
            
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e.getTargetException());
        }
    }

    /**
     * Process class annotations, field and method annotations.
     * @param beanClass
     * @param metabean
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void processClass(Class<?> beanClass, MetaBean metabean)
          throws IllegalAccessException, InvocationTargetException {
        
        // if NOT ignore class level annotations
        if (!factoryContext.getFactory().getAnnotationIgnores()
              .isIgnoreAnnotations(beanClass)) { 
            processAnnotations(null, beanClass, beanClass, null,
                  new AppendValidationToMeta(metabean));
        }

        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            MetaProperty metaProperty = metabean.getProperty(field.getName());
            // create a property for those fields for which there is not yet a MetaProperty
            if (!factoryContext.getFactory().getAnnotationIgnores()
                  .isIgnoreAnnotations(field)) {
                if (metaProperty == null) {
                    metaProperty = addMetaProperty(metabean, field.getName(), field.getType());
                    processAnnotations(metaProperty, beanClass, field,
                          new FieldAccess(field),
                          new AppendValidationToMeta(metaProperty));//) {
                } else {
                    processAnnotations(metaProperty, beanClass, field,
                          new FieldAccess(field),
                          new AppendValidationToMeta(metaProperty));
                }
            }
        }
        Method[] methods = beanClass.getDeclaredMethods();
        for (Method method : methods) {

            String propName = null;
            if (method.getParameterTypes().length == 0) {
                propName = MethodAccess.getPropertyName(method);
            }
            if (propName != null) {
                if (!factoryContext.getFactory().getAnnotationIgnores()
                      .isIgnoreAnnotations(method)) {
                    MetaProperty metaProperty = metabean.getProperty(propName);
                    // create a property for those methods for which there is not yet a MetaProperty
                    if (metaProperty == null) {
                        metaProperty =
                              addMetaProperty(metabean, propName, method.getReturnType());
                        processAnnotations(metaProperty, beanClass, method,
                              new MethodAccess(propName, method),
                              new AppendValidationToMeta(metaProperty));//) {
                    } else {
                        processAnnotations(metaProperty, beanClass, method,
                              new MethodAccess(propName, method),
                              new AppendValidationToMeta(metaProperty));
                    }
                }
            }
            else if ( hasValidationConstraintsDefined(method) ) {
                throw new ValidationException("Property " + method.getName() + " does not follow javabean conventions.");
            }
        }

        addXmlConstraints(beanClass, metabean);
    }

    /**
     * Learn whether a given Method has validation constraints defined via JSR303 annotations.
     * @param method
     * @return <code>true</code> if constraints detected
     */
    protected boolean hasValidationConstraintsDefined(Method method) {
        boolean ret = false;
        for ( Annotation annot : method.getDeclaredAnnotations() ) {
            if ( true == (ret = hasValidationConstraintsDefined(annot)) ) {
                break;
            }
        }
        return ret;
    }

    private boolean hasValidationConstraintsDefined(Annotation annot) {
        // If it is annotated with @Constraint
        if ( annot.annotationType().getAnnotation(Constraint.class) != null ) {
            return true;
        }
        boolean ret = false;
        
        // Check in case it is a multivalued constraint
        Object value = null;
        try {
            value = SecureActions.getAnnotationValue(annot, ANNOTATION_VALUE);
        } catch (IllegalAccessException e) {
            // Swallow it
        } catch (InvocationTargetException e) {
            // Swallow it
        }
        
        if ( value instanceof Annotation[] ) {
            for (Annotation annot2 : (Annotation[])value ) {
                if ( true == (ret = hasValidationConstraintsDefined(annot2)) ) {
                    break;
                }
            }
        }
        
        return ret;
    }

    /**
     * Add cascade validation and constraints from xml mappings
     * @param beanClass
     * @param metabean
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @SuppressWarnings("unchecked")
    private void addXmlConstraints(Class<?> beanClass, MetaBean metabean)
          throws IllegalAccessException, InvocationTargetException {
        for (MetaConstraint<?, ? extends Annotation> meta : factoryContext.getFactory()
              .getMetaConstraints(beanClass)) {
            MetaProperty metaProperty;
            if (meta.getAccessStrategy() == null) { // class level
                metaProperty = null;
            } else { // property level
                metaProperty =
                      metabean.getProperty(meta.getAccessStrategy().getPropertyName());
                if (metaProperty == null) {
                    metaProperty = addMetaProperty(metabean,
                          meta.getAccessStrategy().getPropertyName(),
                          meta.getAccessStrategy().getJavaType());
                }
            }
            Class<? extends ConstraintValidator<? extends Annotation, ?>>[] constraintClasses =
                  findConstraintValidatorClasses(meta.getAnnotation(), null);
            applyConstraint(
                    (Annotation) meta.getAnnotation(),
                    (Class<? extends ConstraintValidator<Annotation, ?>>[]) constraintClasses,
                    metaProperty, beanClass, meta.getAccessStrategy(),
                    new AppendValidationToMeta(metaProperty == null ? metabean
                            : metaProperty));
        }
        for (AccessStrategy access : factoryContext.getFactory().getValidAccesses(beanClass)) {
            MetaProperty metaProperty = metabean.getProperty(access.getPropertyName());
            if (metaProperty == null) {
                metaProperty =
                      addMetaProperty(metabean, access.getPropertyName(), access.getJavaType());
            }
            processValid(metaProperty, access);
        }
    }

    private MetaProperty addMetaProperty(MetaBean parentMetaBean, String propName, Type type) {
        MetaProperty metaProperty;
        metaProperty = new MetaProperty();
        metaProperty.setName(propName);
        metaProperty.setType(type);
        parentMetaBean.putProperty(propName, metaProperty);
        return metaProperty;
    }

    private boolean processAnnotations(MetaProperty prop, Class<?> owner,
                                       AnnotatedElement element, AccessStrategy access,
                                       AppendValidation appender)
          throws IllegalAccessException, InvocationTargetException {

        boolean changed = false;
        for (Annotation annotation : element.getDeclaredAnnotations()) {
            changed |= processAnnotation(annotation, prop, owner, access, appender);
        }
        return changed;
    }

    private <A extends Annotation> boolean processAnnotation(A annotation, MetaProperty prop, Class<?> owner,
                                      AccessStrategy access, AppendValidation appender)
          throws IllegalAccessException, InvocationTargetException {
        if (annotation instanceof Valid) {
            return processValid(prop, access);
        } else {
            /**
             * An annotation is considered a constraint definition if its retention
             * policy contains RUNTIME and if the annotation itself is annotated with
             * javax.validation.Constraint.
             */
            Constraint vcAnno = annotation.annotationType().getAnnotation(Constraint.class);
            if (vcAnno != null) {
                ConstraintDefinitionValidator.validateConstraintDefinition(annotation);
                Class<? extends ConstraintValidator<A, ?>>[] validatorClasses;
                validatorClasses = findConstraintValidatorClasses(annotation, vcAnno);
                return applyConstraint(annotation, validatorClasses, prop, owner, access,
                      appender);
            } else {
                /**
                 * Multi-valued constraints:
                 * To support this requirement, the bean validation provider treats
                 * regular annotations (annotations not annotated by @Constraint)
                 * whose value element has a return type of an array of
                 * constraint annotations in a special way.
                 */
                Object result = SecureActions.getAnnotationValue(annotation, ANNOTATION_VALUE);
                if (result != null && result instanceof Annotation[]) {
                    boolean changed = false;
                    for (Annotation each : (Annotation[]) result) {
                        changed |= processAnnotation(each, prop, owner, access, appender);
                    }
                    return changed;
                }
            }
        }
        return false;
    }

    /**
     * Find available {@link ConstraintValidation} classes for a given constraint annotation.
     * @param annotation
     * @param vcAnno
     * @return {@link ConstraintValidation} implementation class array
     */
    @SuppressWarnings("unchecked")
    protected <A extends Annotation> Class<? extends ConstraintValidator<A, ?>>[] findConstraintValidatorClasses(
          A annotation, Constraint vcAnno) {
        if (vcAnno == null) {
            vcAnno = annotation.annotationType().getAnnotation(Constraint.class);
        }
        Class<? extends ConstraintValidator<A, ?>>[] validatorClasses;
        Class<A> annotationType = (Class<A>) annotation.annotationType();
        validatorClasses = factoryContext.getFactory()
              .getConstraintsCache()
              .getConstraintValidators(annotationType);
        if (validatorClasses == null) {
            validatorClasses = (Class<? extends ConstraintValidator<A, ?>>[]) vcAnno.validatedBy();
            if (validatorClasses.length == 0) {
                validatorClasses = getDefaultConstraints()
                      .getValidatorClasses(annotationType);
            }
        }
        return validatorClasses;
    }

    private boolean processValid(MetaProperty prop, AccessStrategy access) {
        if (prop != null/* && prop.getMetaBean() == null*/) {
            AccessStrategy[] strategies = prop.getFeature(Features.Property.REF_CASCADE);
            if (strategies == null) {
                strategies = new AccessStrategy[]{access};
                prop.putFeature(Features.Property.REF_CASCADE, strategies);
            } else {
                if (!ArrayUtils.contains(strategies, access)) {
                    AccessStrategy[] strategies_new =
                          new AccessStrategy[strategies.length + 1];
                    System.arraycopy(strategies, 0, strategies_new, 0, strategies.length);
                    strategies_new[strategies.length] = access;
                    prop.putFeature(Features.Property.REF_CASCADE, strategies_new);
                }
            }
            return true;
        }
        return false;
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
                throw new GroupDefinitionException(
                      "'Default.class' must not appear in @GroupSequence! Use '" +
                            beanClass.getSimpleName() + ".class' instead.");
            } else {
                groupSeq.add(new Group(groupClass));
            }
        }
        if (!containsDefault) {
            throw new GroupDefinitionException(
                  "Redefined default group sequence must contain " + beanClass.getName());
        }
        if (log.isDebugEnabled()) {
            log.debug("Default group sequence for bean " + beanClass.getName() + " is: " +
                  groupSeq);
        }
    }

    /**
     * Apply a constraint to the specified <code>appender</code>.
     * @param annotation constraint annotation
     * @param constraintClasses known {@link ConstraintValidator} implementation classes for <code>annotation</code>
     * @param prop meta-property
     * @param owner type
     * @param access strategy
     * @param appender
     * @return success flag
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    /*
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected <A extends Annotation> boolean applyConstraint(A annotation,
                                      Class<? extends ConstraintValidator<A, ?>>[] constraintClasses,
                                      MetaProperty prop, Class<?> owner, AccessStrategy access,
                                      AppendValidation appender)
          throws IllegalAccessException, InvocationTargetException {

        final ConstraintValidator<A, ?> validator;
        if (constraintClasses != null && constraintClasses.length > 0) {
            Type type = determineTargetedType(owner, access);
            /**
             * spec says in chapter 3.5.3.:
             * The ConstraintValidator chosen to validate a
             * declared type T is the one where the type supported by the
             * ConstraintValidator is a supertype of T and where
             * there is no other ConstraintValidator whose supported type is a
             * supertype of T and not a supertype of the chosen
             * ConstraintValidator supported type.
             */
            Map<Type, Class<? extends ConstraintValidator<A, ?>>> validatorTypes =
                  (Map<Type, Class<? extends ConstraintValidator<A, ?>>>) TypeUtils.getValidatorsTypes(constraintClasses);
            final List<Type> assignableTypes = new ArrayList<Type>(constraintClasses.length);
            fillAssignableTypes(type, validatorTypes.keySet(), assignableTypes);
            reduceAssignableTypes(assignableTypes);
            checkOneType(assignableTypes, type, owner, annotation, access);
            validator = getConstraintValidatorFactory()
                  .getInstance(validatorTypes.get(assignableTypes.get(0)));
            if ( validator == null ) {
                throw new ValidationException("Factory returned null validator for: " + validatorTypes.get(assignableTypes.get(0)));
            }
            // NOTE: validator initialization deferred until append phase
        } else {
            validator = null;
        }
        final AnnotationConstraintBuilder<A> builder = new AnnotationConstraintBuilder<A>(
              constraintClasses, validator, annotation, owner, access);

        // JSR-303 3.4.4: Add implicit groups
        if ( prop != null && prop.getParentMetaBean() != null ) {
            MetaBean parentMetaBean = prop.getParentMetaBean();
            // If:
            //  - the owner is an interface
            //  - the class of the metabean being build is different than the owner
            //  - and only the Default group is defined
            // Then: add the owner interface as implicit groups
            if ( builder.getConstraintValidation().getOwner().isInterface() &&
                    parentMetaBean.getBeanClass() != builder.getConstraintValidation().getOwner() &&
                    builder.getConstraintValidation().getGroups().size() == 1 &&
                    builder.getConstraintValidation().getGroups().contains(Default.class) ) {
                Set<Class<?>> groups = builder.getConstraintValidation().getGroups();
                groups.add(builder.getConstraintValidation().getOwner());
                builder.getConstraintValidation().setGroups(groups);
            }
        }

        // If already building a constraint composition tree, ensure that:
        //  - the parent groups are inherited
        //  - the parent payload is inherited
        if ( appender instanceof AppendValidationToBuilder ) {
            AppendValidationToBuilder avb = (AppendValidationToBuilder) appender;
            builder.getConstraintValidation().setGroups(avb.getInheritedGroups());
            builder.getConstraintValidation().setPayload(avb.getInheritedPayload());
        }
        
        // process composed constraints:
        // here are not other superclasses possible, because annotations do not inherit!
        processAnnotations(prop, owner, annotation.annotationType(), access, new AppendValidationToBuilder(builder));
        
        // Even if the validator is null, it must be added to mimic the RI impl
        appender.append(builder.getConstraintValidation());
        return true;
    }

    private void checkOneType(List<Type> types, Type targetType, Class<?> owner, Annotation anno,
                              AccessStrategy access) {

        if (types.isEmpty()) {
            StringBuilder buf = new StringBuilder()
                  .append("No validator could be found for type ")
                  .append(stringForType(targetType))
                  .append(". See: @")
                  .append(anno.annotationType().getSimpleName())
                  .append(" at ").append(stringForLocation(owner, access));
            throw new UnexpectedTypeException(buf.toString());
        } else if (types.size() > 1) {
            StringBuilder buf = new StringBuilder();
            buf.append("Ambiguous validators for type ");
            buf.append(stringForType(targetType));
            buf.append(". See: @")
                  .append(anno.annotationType().getSimpleName())
                  .append(" at ").append(stringForLocation(owner, access));
            buf.append(". Validators are: ");
            boolean comma = false;
            for (Type each : types) {
                if (comma) buf.append(", ");
                comma = true;
                buf.append(each);
            }
            throw new UnexpectedTypeException(buf.toString());
        }
    }

    /** implements spec chapter 3.5.3. ConstraintValidator resolution algorithm. */
    private Type determineTargetedType(Class<?> owner, AccessStrategy access) {
        // if the constraint declaration is hosted on a class or an interface,
        // the targeted type is the class or the interface.
        if (access == null) return owner;
        Type type = access.getJavaType();
        if (type == null) return Object.class;
        if (type instanceof Class<?>) type = ClassUtils.primitiveToWrapper((Class<?>) type);
        return type;
    }

    private String stringForType(Type clazz) {
        if (clazz instanceof Class<?>) {
            if (((Class<?>) clazz).isArray()) {
                return ((Class<?>) clazz).getComponentType().getName() + "[]";
            } else {
                return ((Class<?>) clazz).getName();
            }
        } else {
            return clazz.toString();
        }
    }

    private String stringForLocation(Class<?> owner, AccessStrategy access) {
        if (access != null) {
            return access.toString();
        } else {
            return owner.getName();
        }
    }

    private void fillAssignableTypes(Type type, Set<Type> validatorsTypes,
                                     List<Type> suitableTypes) {
        for (Type validatorType : validatorsTypes) {
            if (TypeUtils.isAssignable(validatorType, type) &&
                  !suitableTypes.contains(validatorType)) {
                suitableTypes.add(validatorType);
            }
        }
    }

    /**
     * Tries to reduce all assignable classes down to a single class.
     *
     * @param assignableTypes The set of all classes which are assignable to the class of the value to be validated and
     *                        which are handled by at least one of the validators for the specified constraint.
     */
    private void reduceAssignableTypes(List<Type> assignableTypes) {
        if (assignableTypes.size() <= 1) {
            return; // no need to reduce
        }
        boolean removed;
        do {
            removed = false;
            final Type type = assignableTypes.get(0);
            for (int i = 1; i < assignableTypes.size(); i++) {
                Type nextType = assignableTypes.get(i);
                if (TypeUtils.isAssignable(type, nextType)) {
                    assignableTypes.remove(0);
                    i--;
                    removed = true;
                } else if (TypeUtils.isAssignable(nextType, type)) {
                    assignableTypes.remove(i--);
                    removed = true;
                }
            }
        } while (removed && assignableTypes.size() > 1);
    }
}
