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
import java.util.*;

/**
 * Description: process the class annotations for JSR303 constraint validations
 * to build the MetaBean with information from annotations and JSR303 constraint
 * mappings (defined in xml)<br/>
 */
public class Jsr303MetaBeanFactory implements MetaBeanFactory {
    protected static final Log log = LogFactory.getLog(Jsr303MetaBeanFactory.class);
    protected static final String ANNOTATION_VALUE = "value";
    protected final ApacheFactoryContext factoryContext;

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
     * add the validation features to the metabean that come from jsr303
     * annotations in the beanClass
     */
    public void buildMetaBean(MetaBean metabean) {
        try {
            final Class<?> beanClass = metabean.getBeanClass();
            processGroupSequence(beanClass, metabean);

            // process class, superclasses and interfaces
            List<Class<?>> classSequence = new ArrayList<Class<?>>();
            fillFullClassHierarchyAsList(classSequence, beanClass);

            // start with superclasses and go down the hierarchy so that
            // the child classes are processed last to have the chance to overwrite some declarations
            // of their superclasses and that they see what they inherit at the time of processing
            for (int i = classSequence.size() - 1; i >= 0; i--) {
                Class<?> eachClass = classSequence.get(i);
                processClass(eachClass, metabean);
            }
            
            // JSR-303 2.3:
            // Groups from the main constraint annotation are inherited by the composing annotations.
            // Any groups definition on a composing annotation is ignored.
//            applyGroupInheritance(metabean);
            
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e.getTargetException());
        }
    }

//    /**
//     * Traverses the metabean to set the groups of the composed constraints the
//     * same as the groups from its root constraint.
//     * 
//     * @param metabean
//     *            The metabean to which group inheritance will be applied.
//     */
//    private void applyGroupInheritance(MetaBean metabean) {
//        for ( MetaProperty prop : metabean.getProperties() ) {
//            for ( Validation val : prop.getValidations() ) {
//                if ( val instanceof ConstraintValidation<?> ) { // Should always be true?
//                    ConstraintValidation<?> cv = (ConstraintValidation<?>) val;
//                    Set<Class<?>> baseGroups = cv.getGroups();
//                    for ( ConstraintValidation<?> composedVal : cv.getComposingValidations() ) {
//                        overrideGroupsAndContinueRecursion(baseGroups, composedVal);
//                    }
//                }
//            }
//        }        
//    }
//
//    /**
//     * Recursive method that sets the current {@link ConstraintValidation}
//     * groups as baseGroups, and continues the recursion with its composing
//     * validations (if any).
//     * 
//     * @param baseGroups
//     *            The groups to set in the current validation node.
//     * @param val
//     *            The current validation node.
//     */
//    private void overrideGroupsAndContinueRecursion(Set<Class<?>> baseGroups, ConstraintValidation<?> val) {
//        val.setGroups(baseGroups);
//        for ( ConstraintValidation<?> composedVal : val.getComposingValidations() ) {
//            overrideGroupsAndContinueRecursion(baseGroups, composedVal);
//        }
//    }

    /**
     * Fill the list with the full class/interface hierarchy of the given class.
     * List is ordered from the most to less specific.
     * 
     * @param allClasses
     *            The current list of classes in the hierarchy.
     * @param clazz
     *            The current class, root of the hierarchy to traverse.
     */
    private void fillFullClassHierarchyAsList(List<Class<?>> allClasses, Class<?> clazz) {
        
        if ( clazz == null || clazz == Object.class ) {
            return;
        }
        
        if ( allClasses.contains(clazz) ) {
            // No duplicates wanted, and if it is already in the list, then
            // its superclasses/interfaces will also be
            return;
        }
        
        allClasses.add(clazz);

        // Obtain the list of interfaces and superclass
        List<Class<?>> subClasses = new ArrayList<Class<?>>(Arrays.asList(clazz.getInterfaces()));
        subClasses.add(0, clazz.getSuperclass());
        
        for ( Class<?> subClass : subClasses ) {
            fillFullClassHierarchyAsList(allClasses, subClass);
        }
        
    }

    /**
     * process class annotations, field and method annotations
     *
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void processClass(Class<?> beanClass, MetaBean metabean)
          throws IllegalAccessException, InvocationTargetException {
        if (!factoryContext.getFactory().getAnnotationIgnores()
              .isIgnoreAnnotations(beanClass)) { // ignore on class level

            processAnnotations(null, beanClass, beanClass, null,
                  new AppendValidationToMeta(metabean));

            Field[] fields = beanClass.getDeclaredFields();
            for (Field field : fields) {
                MetaProperty metaProperty = metabean.getProperty(field.getName());
                // create a property for those fields for which there is not yet a MetaProperty
                if (!factoryContext.getFactory().getAnnotationIgnores()
                      .isIgnoreAnnotations(field)) {
                    if (metaProperty == null) {
                        metaProperty = createMetaProperty(metabean, field.getName(), field.getType());
                        /*if (*/
                        processAnnotations(metaProperty, beanClass, field,
                              new FieldAccess(field),
                              new AppendValidationToMeta(metaProperty));//) {
                        metabean.putProperty(metaProperty.getName(), metaProperty);
                        //}
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
                                  createMetaProperty(metabean, propName, method.getReturnType());
                            /*if (*/
                            processAnnotations(metaProperty, beanClass, method,
                                  new MethodAccess(propName, method),
                                  new AppendValidationToMeta(metaProperty));//) {
                            metabean.putProperty(propName, metaProperty);
                            //}
                        } else {
                            processAnnotations(metaProperty, beanClass, method,
                                  new MethodAccess(propName, method),
                                  new AppendValidationToMeta(metaProperty));
                        }
                    }
                } // TODO: If propName == null AND it's annotated, throw Ex
            }
        }
        addXmlConstraints(beanClass, metabean);
    }

    /** add cascade validation and constraints from xml mappings */
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
                    metaProperty = createMetaProperty(metabean,
                          meta.getAccessStrategy().getPropertyName(),
                          meta.getAccessStrategy().getJavaType());
                    metabean.putProperty(metaProperty.getName(), metaProperty);
                }
            }
            Class<? extends ConstraintValidator<?, ?>>[] constraintClasses =
                  findConstraintValidatorClasses(meta.getAnnotation(), null);
            applyConstraint(meta.getAnnotation(), constraintClasses, metaProperty, beanClass,
                  meta.getAccessStrategy(), new AppendValidationToMeta(
                  metaProperty == null ? metabean : metaProperty));
        }
        for (AccessStrategy access : factoryContext.getFactory().getValidAccesses(beanClass)) {
            MetaProperty metaProperty = metabean.getProperty(access.getPropertyName());
            if (metaProperty == null) {
                metaProperty =
                      createMetaProperty(metabean, access.getPropertyName(), access.getJavaType());
                metabean.putProperty(metaProperty.getName(), metaProperty);
            }
            processValid(metaProperty, access);
        }
    }

    private MetaProperty createMetaProperty(MetaBean parentMetaBean, String propName, Type type) {
        MetaProperty metaProperty;
        metaProperty = new MetaProperty();
        metaProperty.setName(propName);
        metaProperty.setType(type);
        metaProperty.setParentMetaBean(parentMetaBean);
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

    private boolean processAnnotation(Annotation annotation, MetaProperty prop, Class<?> owner,
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
                Class<? extends ConstraintValidator<?, ?>>[] validatorClasses;
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

    protected Class<? extends ConstraintValidator<?, ?>>[] findConstraintValidatorClasses(
          Annotation annotation, Constraint vcAnno) {
        if (vcAnno == null) {
            vcAnno = annotation.annotationType().getAnnotation(Constraint.class);
        }
        Class<? extends ConstraintValidator<?, ?>>[] validatorClasses;
        validatorClasses = factoryContext.getFactory()
              .getConstraintsCache()
              .getConstraintValidators(annotation.annotationType());
        if (validatorClasses == null) {
            validatorClasses = vcAnno.validatedBy();
            if (validatorClasses.length == 0) {
                validatorClasses = getDefaultConstraints()
                      .getValidatorClasses(annotation.annotationType());
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
        GroupSequence annotation = beanClass.getAnnotation(GroupSequence.class);
        List<Group> groupSeq = metabean.getFeature(Jsr303Features.Bean.GROUP_SEQUENCE);
        if (groupSeq == null) {
            groupSeq = new ArrayList<Group>(annotation == null ? 1 : annotation.value().length);
            metabean.putFeature(Jsr303Features.Bean.GROUP_SEQUENCE, groupSeq);
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
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected boolean applyConstraint(Annotation annotation,
                                      Class<? extends ConstraintValidator<?, ?>>[] constraintClasses,
                                      MetaProperty prop, Class<?> owner, AccessStrategy access,
                                      AppendValidation appender)
          throws IllegalAccessException, InvocationTargetException {

        final ConstraintValidator validator;
        if (constraintClasses != null) {
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
            Map<Type, Class<? extends ConstraintValidator<?, ?>>> validatorTypes =
                  TypeUtils.getValidatorsTypes(constraintClasses);
            final List<Type> assignableTypes = new ArrayList<Type>(constraintClasses.length);
            fillAssignableTypes(type, validatorTypes.keySet(), assignableTypes);
            reduceAssignableTypes(assignableTypes);
            checkOneType(assignableTypes, type, owner, annotation, access);
            validator = getConstraintValidatorFactory()
                  .getInstance(validatorTypes.get(assignableTypes.get(0)));
            // NOTE: validator initialization deferred until append phase
        } else {
            validator = null;
        }
        final AnnotationConstraintBuilder builder = new AnnotationConstraintBuilder(
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
        if (processAnnotations(prop, owner, annotation.annotationType(), access,
              new AppendValidationToBuilder(builder)) || validator != null) {  // recursion!
            appender.append(builder.getConstraintValidation());
            return true;
        } else {
            return false;
        }
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
