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

import org.apache.bval.Validate;
import org.apache.bval.jsr303.groups.Group;
import org.apache.bval.jsr303.groups.GroupConversionDescriptorImpl;
import org.apache.bval.jsr303.util.ClassHelper;
import org.apache.bval.jsr303.util.SecureActions;
import org.apache.bval.model.Features;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaConstructor;
import org.apache.bval.model.MetaMethod;
import org.apache.bval.model.MetaParameter;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.Validation;
import org.apache.bval.util.AccessStrategy;
import org.apache.commons.lang3.ClassUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintDeclarationException;
import javax.validation.ConstraintTarget;
import javax.validation.Valid;
import javax.validation.groups.ConvertGroup;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstructorDescriptor;
import javax.validation.metadata.ExecutableDescriptor;
import javax.validation.metadata.GroupConversionDescriptor;
import javax.validation.metadata.MethodDescriptor;
import javax.validation.metadata.MethodType;
import javax.validation.metadata.ParameterDescriptor;
import javax.validation.metadata.PropertyDescriptor;
import javax.validation.metadata.ReturnValueDescriptor;
import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Description: Implements {@link BeanDescriptor}.<br/>
 */
public class BeanDescriptorImpl extends ElementDescriptorImpl implements BeanDescriptor {
    private static final CopyOnWriteArraySet<ConstraintValidation<?>> NO_CONSTRAINTS = new CopyOnWriteArraySet<ConstraintValidation<?>>();
    /**
     * The {@link ApacheFactoryContext} (not) used by this
     * {@link BeanDescriptorImpl}
     */
    protected final ApacheFactoryContext factoryContext;
    private final AnnotationProcessor annotationProcessor;
    private Set<ConstructorDescriptor> constrainedConstructors = new CopyOnWriteArraySet<ConstructorDescriptor>();
    private Map<Method, MethodDescriptor> methodConstraints = new HashMap<Method, MethodDescriptor>();
    private Set<MethodDescriptor> containedMethods = new CopyOnWriteArraySet<MethodDescriptor>();
    private Map<Constructor<?>, ConstructorDescriptor> contructorConstraints = new HashMap<Constructor<?>, ConstructorDescriptor>();

    protected BeanDescriptorImpl(ApacheFactoryContext factoryContext, MetaBean metaBean) {
        super(metaBean, metaBean.getBeanClass(), metaBean.getValidations());
        this.factoryContext = factoryContext;
        this.annotationProcessor = new AnnotationProcessor(factoryContext);

        buildExecutableDescriptors();
    }

    private static void addGroupConvertion(final MetaProperty prop, final PropertyDescriptorImpl edesc) {
        boolean fieldFound = false;
        boolean methodFound = false;
        Class<?> current = prop.getParentMetaBean().getBeanClass();
        while (current != null && current != Object.class && (!methodFound || !fieldFound)) {
            if (!fieldFound) {
                final Field field = SecureActions.getDeclaredField(current, prop.getName()).run();
                if (field != null) {
                    final ConvertGroup.List convertGroupList = field.getAnnotation(ConvertGroup.List.class);
                    if (convertGroupList != null) {
                        for (final ConvertGroup convertGroup : convertGroupList.value()) {
                            edesc.addGroupConversion(new GroupConversionDescriptorImpl(new Group(convertGroup.from()), new Group(convertGroup.to())));
                        }
                    }

                    final ConvertGroup convertGroup = field.getAnnotation(ConvertGroup.class);
                    if (convertGroup != null) {
                        edesc.addGroupConversion(new GroupConversionDescriptorImpl(new Group(convertGroup.from()), new Group(convertGroup.to())));
                    }
                    fieldFound = true;
                }
            }

            if (!methodFound) {
                final String name = Character.toUpperCase(prop.getName().charAt(0)) + prop.getName().substring(1);
                for (final Method method : Arrays.asList(
                        SecureActions.getDeclaredMethod(current, "is" + name).run(),
                        SecureActions.getDeclaredMethod(current, "get" + name).run())) {

                    if (method != null) {
                        final ConvertGroup.List convertGroupList = method.getAnnotation(ConvertGroup.List.class);
                        if (convertGroupList != null) {
                            for (final ConvertGroup convertGroup : convertGroupList.value()) {
                                edesc.addGroupConversion(new GroupConversionDescriptorImpl(new Group(convertGroup.from()), new Group(convertGroup.to())));
                            }
                        }

                        final ConvertGroup convertGroup = method.getAnnotation(ConvertGroup.class);
                        if (convertGroup != null) {
                            edesc.addGroupConversion(new GroupConversionDescriptorImpl(new Group(convertGroup.from()), new Group(convertGroup.to())));
                        }

                        methodFound = true;
                        break;
                    }
                }
            }

            current = current.getSuperclass();
        }

        final Collection<Annotation> annotations = prop.getFeature(Jsr303Features.Property.ANNOTATIONS_TO_PROCESS);
        if (annotations != null) {
            for (final Annotation a : annotations) {
                if (ConvertGroup.List.class.isInstance(a)) {
                    for (final ConvertGroup convertGroup : ConvertGroup.List.class.cast(a).value()) {
                        edesc.addGroupConversion(new GroupConversionDescriptorImpl(new Group(convertGroup.from()), new Group(convertGroup.to())));
                    }
                }

                if (ConvertGroup.class.isInstance(a)) {
                    final ConvertGroup convertGroup = ConvertGroup.class.cast(a);
                    edesc.addGroupConversion(new GroupConversionDescriptorImpl(new Group(convertGroup.from()), new Group(convertGroup.to())));
                }
            }
            annotations.clear();
        }

        if (!edesc.getGroupConversions().isEmpty() && !edesc.isCascaded()) {
            throw new ConstraintDeclarationException("@Valid is needed for group conversion");
        }
    }

    /**
     * Returns true if the bean involves validation:
     * <ul>
     * <li>a constraint is hosted on the bean itself</li>
     * <li>a constraint is hosted on one of the bean properties, OR</li>
     * <li>a bean property is marked for cascade (<code>@Valid</code>)</li>
     * </ul>
     *
     * @return true if the bean involves validation
     */
    public boolean isBeanConstrained() {
        if (hasAnyConstraints())
            return true;
        for (MetaProperty mprop : metaBean.getProperties()) {
            if (mprop.getMetaBean() != null || mprop.getFeature(Features.Property.REF_CASCADE) != null)
                return true;
        }
        return false;
    }

    private boolean hasAnyConstraints() {
        if (hasConstraints())
            return true;
        for (MetaProperty mprop : metaBean.getProperties()) {
            if (getConstraintDescriptors(mprop.getValidations()).size() > 0)
                return true;
        }
        return false;
    }

    /**
     * Return the property level constraints for a given propertyName or {@code null} if
     * either the property does not exist or has no constraint. The returned
     * object (and associated objects including ConstraintDescriptors) are
     * immutable.
     *
     * @param propertyName property evaluated
     */
    public PropertyDescriptor getConstraintsForProperty(String propertyName) {
        if (propertyName == null || propertyName.trim().length() == 0) {
            throw new IllegalArgumentException("propertyName cannot be null or empty");
        }
        MetaProperty prop = metaBean.getProperty(propertyName);
        if (prop == null)
            return null;
        // If no constraints and not cascaded, return null
        if (prop.getValidations().length == 0 && prop.getFeature(Features.Property.REF_CASCADE) == null) {
            return null;
        }
        return getPropertyDescriptor(prop);
    }

    private PropertyDescriptor getPropertyDescriptor(MetaProperty prop) {
        PropertyDescriptorImpl edesc = prop.getFeature(Jsr303Features.Property.PropertyDescriptor);
        if (edesc == null) {
            edesc = new PropertyDescriptorImpl(prop);
            addGroupConvertion(prop, edesc);
            prop.putFeature(Jsr303Features.Property.PropertyDescriptor, edesc);
        }
        return edesc;
    }

    /**
     * {@inheritDoc}
     *
     * @return the property descriptors having at least a constraint defined
     */
    public Set<PropertyDescriptor> getConstrainedProperties() {
        Set<PropertyDescriptor> validatedProperties = new HashSet<PropertyDescriptor>();
        for (MetaProperty prop : metaBean.getProperties()) {
            if (prop.getValidations().length > 0
                    || (prop.getMetaBean() != null || prop.getFeature(Features.Property.REF_CASCADE) != null)) {
                validatedProperties.add(getPropertyDescriptor(prop));
            }
        }
        return Collections.unmodifiableSet(validatedProperties);
    }

    public MethodDescriptor getConstraintsForMethod(String methodName, Class<?>... parameterTypes) {
        if (methodName == null) {
            throw new IllegalArgumentException("Method name can't be null");
        }

        Class<?> beanClass = metaBean.getBeanClass();
        Method method = null;
        do {
            try {
                method = beanClass.getDeclaredMethod(methodName, parameterTypes);
                break;
            } catch (final NoSuchMethodException e) {
                // no-op
            }
            beanClass = beanClass.getSuperclass();
        } while (beanClass != Object.class && beanClass != null);
        if (method == null) {
            return null;
        }

        final MethodDescriptor descriptor = methodConstraints.get(method);
        if (descriptor != null) {
            final boolean hasConstraint = descriptor.hasConstrainedParameters() || descriptor.hasConstrainedReturnValue();
            if (!hasConstraint) {
                return null;
            }
            return descriptor;
        }

        // TODO: surely remove it
        for (final MetaMethod metaMethod : metaBean.getMethods()) {
            if (metaMethod.getMethod().equals(method)) {
                final MethodDescriptorImpl methodDescriptor = createMethodDescriptor(metaMethod);
                ensureNotNullDescriptors(metaMethod.getMethod().getReturnType(), methodDescriptor);
                methodConstraints.put(method, methodDescriptor);
                containedMethods.add(methodDescriptor);
                return methodDescriptor;
            }
        }

        return null;
    }

    private MethodDescriptorImpl createMethodDescriptor(final MetaMethod metaMethod) {
        MethodDescriptorImpl edesc = metaMethod.getFeature(Jsr303Features.Method.MethodDescriptor);
        if (edesc == null) {
            edesc = new MethodDescriptorImpl(metaBean, metaMethod);
            metaMethod.putFeature(Jsr303Features.Method.MethodDescriptor, edesc);
        }
        return edesc;
    }

    public Set<MethodDescriptor> getConstrainedMethods(MethodType methodType, MethodType... methodTypes) {
        final Set<MethodDescriptor> desc = new HashSet<MethodDescriptor>();
        desc.addAll(filter(containedMethods, methodType));
        if (methodTypes != null) {
            for (final MethodType type : methodTypes) {
                desc.addAll(filter(containedMethods, type));
            }
        }
        return desc;
    }

    private static Collection<MethodDescriptor> filter(final Set<MethodDescriptor> containedMethods, final MethodType type) {
        final Collection<MethodDescriptor> list = new ArrayList<MethodDescriptor>();
        for (final MethodDescriptor d : containedMethods) {
            final boolean getter = d.getName().startsWith("get") && d.getParameterDescriptors().isEmpty();

            switch (type) {
                case GETTER:
                    if (getter) {
                        list.add(d);
                    }
                break;

                case NON_GETTER:
                    if (!getter) {
                        list.add(d);
                    }
            }
        }
        return list;
    }

    public ConstructorDescriptor getConstraintsForConstructor(Class<?>... parameterTypes) {
        final Constructor<?> declaredConstructor;
        try {
            declaredConstructor = metaBean.getBeanClass().getDeclaredConstructor(parameterTypes);
        } catch (final NoSuchMethodException e) {
            return null;
        }

        final ConstructorDescriptor descriptor = contructorConstraints.get(declaredConstructor);
        if (descriptor != null && (descriptor.hasConstrainedParameters() || descriptor.hasConstrainedReturnValue())) {
            return descriptor;
        }

        return null;
    }

    public Set<ConstructorDescriptor> getConstrainedConstructors() {
        return constrainedConstructors;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "BeanDescriptorImpl{" + "returnType=" + elementClass + '}';
    }

    public void buildExecutableDescriptors() {
        try {
            buildMethodConstraints();
            setConstrained(containedMethods, methodConstraints.values());

            buildConstructorConstraints();
            setConstrained(constrainedConstructors, contructorConstraints.values());
        } catch (final Exception ex) {
            if (RuntimeException.class.isInstance(ex)) {
                throw RuntimeException.class.cast(ex);
            }

            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private <A extends ExecutableDescriptor> void setConstrained(final Set<A> dest, final Collection<A> src) {
        for (final A d : src) {
            if (d.hasConstrainedParameters() || d.hasConstrainedReturnValue()) {
                dest.add(d);
            }
        }
    }

    private void buildConstructorConstraints() throws InvocationTargetException, IllegalAccessException {
        for (final Constructor<?> cons : SecureActions.getDeclaredConstructors(getMetaBean().getBeanClass()).run()) {
            final ConstructorDescriptorImpl consDesc = new ConstructorDescriptorImpl(getMetaBean(), new Validation[0]);
            contructorConstraints.put(cons, consDesc);

            final List<String> names = factoryContext.getParameterNameProvider().getParameterNames(cons);
            final boolean isInnerClass = cons.getDeclaringClass().getEnclosingClass() != null && !Modifier.isStatic(cons.getDeclaringClass().getModifiers());

            {
                final Annotation[][] paramsAnnos = cons.getParameterAnnotations();

                int idx = 0;
                if (isInnerClass) { // paramsAnnos.length = parameterTypes.length - 1 in this case
                    final ParameterDescriptorImpl paramDesc = new ParameterDescriptorImpl(getMetaBean(), new Validation[0], names.get(idx));
                    consDesc.getParameterDescriptors().add(paramDesc);
                    idx++;
                }

                for (final Annotation[] paramAnnos : paramsAnnos) {
                    if (factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotationOnParameter(cons, idx)) {
                        consDesc.getParameterDescriptors().add(new ParameterDescriptorImpl(metaBean, new Validation[0], names.get(idx)));
                    } else if (cons.getParameterTypes().length > idx) {
                        ParameterAccess access = new ParameterAccess(cons.getParameterTypes()[idx], idx);
                        consDesc.addValidations(processAnnotations(consDesc, paramAnnos, access, idx, names.get(idx)).getValidations());
                    } // else anonymous class so that's fine
                    idx++;
                }

                if (!factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotations(cons)) {
                    for (final Annotation anno : cons.getAnnotations()) {
                        if (!Valid.class.isInstance(anno)) {
                            processAnnotations(null, consDesc, cons.getDeclaringClass(), anno);
                        } else {
                            consDesc.setCascaded(true);
                        }
                    }
                }
            }

            if (factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotationOnCrossParameter(cons) && consDesc.getCrossParameterDescriptor() != null) {
                consDesc.setCrossParameterDescriptor(null);
            }
            if (factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotationOnReturn(cons) && consDesc.getReturnValueDescriptor() != null) {
                consDesc.setReturnValueDescriptor(null);
            }

            final MetaConstructor metaConstructor = metaBean.getConstructor(cons);
            if (metaConstructor != null) {
                for (final Annotation anno : metaConstructor.getAnnotations()) {
                    if (!Valid.class.isInstance(anno)) {
                        processAnnotations(null, consDesc, cons.getDeclaringClass(), anno);
                    } else {
                        consDesc.setCascaded(true);
                    }
                }

                // parameter validations
                final Collection<MetaParameter> paramsAnnos = metaConstructor.getParameters();
                for (final MetaParameter paramAnnos : paramsAnnos) {
                    final int idx = paramAnnos.getIndex();
                    final ParameterAccess access = new ParameterAccess(cons.getParameterTypes()[idx], idx);
                    processAnnotations(consDesc, paramAnnos.getAnnotations(), access, idx, names.get(idx));
                }
            }

            if (!consDesc.getGroupConversions().isEmpty() && !consDesc.isCascaded()) {
                throw new ConstraintDeclarationException("@Valid is needed to define a group conversion");
            }

            ensureNotNullDescriptors(cons.getDeclaringClass(), consDesc);
        }
    }

    private void ensureNotNullDescriptors(final Class<?> returnType, final InvocableElementDescriptor consDesc) {
        // can't be null
        if (consDesc.getCrossParameterDescriptor() == null) {
            consDesc.setCrossParameterDescriptor(new CrossParameterDescriptorImpl(getMetaBean(), NO_CONSTRAINTS));
        }
        if (consDesc.getReturnValueDescriptor() == null) {
            consDesc.setReturnValueDescriptor(new ReturnValueDescriptorImpl(getMetaBean(), returnType, NO_CONSTRAINTS, consDesc.isCascaded()));
        }
        // enforce it since ReturnValueDescriptor can be created before cascaded is set to true
        final ReturnValueDescriptorImpl returnValueDescriptor = ReturnValueDescriptorImpl.class.cast(consDesc.getReturnValueDescriptor());
        returnValueDescriptor.setCascaded(consDesc.isCascaded());
        if (returnValueDescriptor.getGroupConversions().isEmpty()) {
            // loop to not forget to map calling addGroupConversion()
            for (final GroupConversionDescriptor c : consDesc.getGroupConversions()) {
                returnValueDescriptor.addGroupConversion(c);
            }
        }

    }

    private void processAnnotations(final Method mtd, final InvocableElementDescriptor consDesc, final Class<?> clazz, final Annotation anno) throws InvocationTargetException, IllegalAccessException {
        if (mtd == null || !factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotationOnReturn(mtd)) {
            final ReturnAccess returnAccess = new ReturnAccess(clazz);
            final AppendValidationToList validations = new AppendValidationToList();
            processAnnotation(anno, consDesc, returnAccess, validations);
            final List<ConstraintValidation<?>> list = removeFromListValidationAppliesTo(validations.getValidations(), ConstraintTarget.PARAMETERS);
            consDesc.addValidations(list);

            ReturnValueDescriptorImpl returnValueDescriptor = ReturnValueDescriptorImpl.class.cast(consDesc.getReturnValueDescriptor());
            if (consDesc.getReturnValueDescriptor() != null) {
                returnValueDescriptor.getMutableConstraintDescriptors().addAll(list);
            } else {
                returnValueDescriptor = new ReturnValueDescriptorImpl(getMetaBean(), clazz, list, consDesc.isCascaded());
                consDesc.setReturnValueDescriptor(returnValueDescriptor);
            }
        }

        if (mtd == null || !factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotationOnCrossParameter(mtd)) {
            final ParametersAccess parametersAccess = new ParametersAccess();
            final AppendValidationToList validations = new AppendValidationToList();
            processAnnotation(anno, consDesc, parametersAccess, validations);
            final List<ConstraintValidation<?>> list = removeFromListValidationAppliesTo(validations.getValidations(), ConstraintTarget.RETURN_VALUE);
            consDesc.addValidations(list);
            if (consDesc.getCrossParameterDescriptor() != null) {
                CrossParameterDescriptorImpl.class.cast(consDesc.getCrossParameterDescriptor()).getMutableConstraintDescriptors().addAll(list);
            } else {
                consDesc.setCrossParameterDescriptor(new CrossParameterDescriptorImpl(getMetaBean(), list));
            }
        }
    }

    private static List<ConstraintValidation<?>> removeFromListValidationAppliesTo(final List<ConstraintValidation<?>> validations, final ConstraintTarget constraint) {
        final Iterator<ConstraintValidation<?>> i = validations.iterator();
        while (i.hasNext()) {
            if (constraint.equals(i.next().getValidationAppliesTo())) {
                i.remove();
            }
        }
        return validations;
    }

    private void buildMethodConstraints() throws InvocationTargetException, IllegalAccessException {

        Class<?> current = getMetaBean().getBeanClass();
        do {
            for (final Method method : current.getDeclaredMethods()) {
                final boolean getter = method.getName().startsWith("get") && method.getParameterTypes().length == 0;

                final MethodDescriptorImpl methodDesc = new MethodDescriptorImpl(getMetaBean(), new Validation[0], method);
                methodConstraints.put(method, methodDesc);

                final List<Class<?>> classHierarchy = new ArrayList<Class<?>>();
                ClassHelper.fillFullClassHierarchyAsList(classHierarchy, current);
                classHierarchy.remove(current);

                final Collection<Method> parents = new ArrayList<Method>();
                for (final Class<?> clazz : classHierarchy) {
                    final Method overriden = SecureActions.getDeclaredMethod(clazz, method.getName(), method.getParameterTypes()).run();
                    if (overriden != null) {
                        processMethod(overriden, methodDesc);
                        parents.add(overriden);
                    }
                }

                processMethod(method, methodDesc);

                ensureNotNullDescriptors(method.getReturnType(), methodDesc);

                // validations, TODO: if (!factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotations(method)) {
                if (parents != null) {
                    if (parents.size() > 1) {
                        for (final Method parent : parents) {
                            final InvocableElementDescriptor elementDescriptor = InvocableElementDescriptor.class.cast(factoryContext.getValidator().getConstraintsForClass(parent.getDeclaringClass()).getConstraintsForMethod(parent.getName(), parent.getParameterTypes()));
                            if (elementDescriptor != null) {
                                ensureNoParameterConstraint(elementDescriptor, "Parameter constraints can't be defined for parallel interfaces/parents");
                            } else {
                                ensureMethodDoesntDefineParameterConstraint(methodDesc);
                            }
                            ensureNoReturnValueAddedInChild(methodDesc.getReturnValueDescriptor(), parent, "Return value constraints should be the same for parent and children");
                        }
                    } else if (!parents.isEmpty()) {
                        final Method parent = parents.iterator().next();
                        ensureNoReturnValueAddedInChild(methodDesc.getReturnValueDescriptor(), parent, "Return value constraints should be at least the same for parent and children");

                        final MethodDescriptor parentDesc = factoryContext.getValidator().getConstraintsForClass(parent.getDeclaringClass()).getConstraintsForMethod(parent.getName(), parent.getParameterTypes());
                        if (parentDesc != null) {
                            final Iterator<ParameterDescriptor> parentPd = parentDesc.getParameterDescriptors().iterator();
                            for (final ParameterDescriptor pd : methodDesc.getParameterDescriptors()) {
                                final ParameterDescriptor next = parentPd.next();
                                if (pd.getConstraintDescriptors().size() != next.getConstraintDescriptors().size()) {
                                    throw new ConstraintDeclarationException("child shouldn't get more constraint than parent");
                                }
                                if (pd.isCascaded() != next.isCascaded()) { // @Valid
                                    throw new ConstraintDeclarationException("child shouldn't get more constraint than parent");
                                }
                            }
                        } else {
                            ensureMethodDoesntDefineParameterConstraint(methodDesc);
                        }
                    }

                    final Class<?>[] interfaces = method.getDeclaringClass().getInterfaces();
                    final Collection<Method> itfWithThisMethod = new ArrayList<Method>();
                    for (final Class<?> i : interfaces) {
                        final Method m = SecureActions.getDeclaredMethod(i, method.getName(), method.getParameterTypes()).run();
                        if (m != null) {
                            itfWithThisMethod.add(m);
                        }
                    }
                    if (itfWithThisMethod.size() > 1) {
                        for (final Method m : itfWithThisMethod) {
                            ensureNoConvertGroup(m, "ConvertGroup can't be used in parallel interfaces");
                        }
                    } else if (itfWithThisMethod.size() == 1) {
                        ensureNoConvertGroup(itfWithThisMethod.iterator().next(), "ConvertGroup can't be used in interface AND parent class");
                    }

                    int returnValid = 0;
                    if (method.getAnnotation(Valid.class) != null) {
                        returnValid++;
                    }
                    for (final Class<?> clazz : classHierarchy) {
                        final Method overriden = SecureActions.getDeclaredMethod(clazz, method.getName(), method.getParameterTypes()).run();
                        if (overriden != null) {
                            if (overriden.getAnnotation(Valid.class) != null) {
                                returnValid++;
                            }
                        }
                    }
                    if (returnValid > 1 && !(interfaces.length == returnValid && method.getAnnotation(Valid.class) == null)) {
                        throw new ConstraintDeclarationException("@Valid on returned value can't be set more than once");
                    }
                }

                if (getter) {
                    final MetaProperty prop = metaBean.getProperty(Introspector.decapitalize(method.getName().substring(3)));
                    if (prop != null && prop.getFeature(Features.Property.REF_CASCADE) != null) {
                        methodDesc.setCascaded(true);
                    }
                }

                if (!methodDesc.getGroupConversions().isEmpty() && !methodDesc.isCascaded()) {
                    throw new ConstraintDeclarationException("@Valid is needed to define a group conversion");
                }
            }
            current = current.getSuperclass();
        } while (current != null && current != Object.class);
    }

    private void ensureMethodDoesntDefineParameterConstraint(MethodDescriptorImpl methodDesc) {
        for (final ParameterDescriptor pd : methodDesc.getParameterDescriptors()) {
            if (!pd.getConstraintDescriptors().isEmpty()) {
                throw new ConstraintDeclarationException("child shouldn't get more constraint than parent");
            }
            if (pd.isCascaded()) { // @Valid
                throw new ConstraintDeclarationException("child shouldn't get more constraint than parent");
            }
        }
    }

    private void ensureNoReturnValueAddedInChild(final ReturnValueDescriptor returnValueDescriptor, final Method parent, final String msg) {
        final MethodDescriptor parentDesc = factoryContext.getValidator().getConstraintsForClass(parent.getDeclaringClass()).getConstraintsForMethod(parent.getName(), parent.getParameterTypes());
        if (parentDesc == null) {
            return;
        }

        final ReturnValueDescriptor parentReturnDesc = parentDesc.getReturnValueDescriptor();
        if (parentReturnDesc.isCascaded() && !returnValueDescriptor.isCascaded() || parentReturnDesc.getConstraintDescriptors().size() > returnValueDescriptor.getConstraintDescriptors().size()) {
            throw new ConstraintDeclarationException(msg);
        }
    }

    private static void ensureNoParameterConstraint(final InvocableElementDescriptor constraintsForMethod, final String msg) {
        for (final ParameterDescriptor parameterDescriptor : constraintsForMethod.getParameterDescriptors()) {
            if (!parameterDescriptor.getConstraintDescriptors().isEmpty() || parameterDescriptor.isCascaded()) {
                throw new ConstraintDeclarationException(msg);
            }
        }
    }

    private static void ensureNoConvertGroup(final Method method, final String msg) {
        for (final Annotation[] annotations : method.getParameterAnnotations()) {
            for (final Annotation a : annotations) {
                if (ConvertGroup.class.isInstance(a)) {
                    throw new ConstraintDeclarationException(msg);
                }
            }
        }
        if (method.getAnnotation(ConvertGroup.class) != null) {
            throw new ConstraintDeclarationException(msg);
        }
    }

    private void processMethod(Method method, MethodDescriptorImpl methodDesc) throws InvocationTargetException, IllegalAccessException {
        { // reflection
            if (!factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotations(method)) {
                // return value validations and/or cross-parameter validation
                for (Annotation anno : method.getAnnotations()) {
                    if (anno instanceof Valid || anno instanceof Validate) {
                        methodDesc.setCascaded(true);
                    } else {
                        processAnnotations(method, methodDesc, method.getReturnType(), anno);
                    }
                }
            }

            // parameter validations
            final Annotation[][] paramsAnnos = method.getParameterAnnotations();
            int idx = 0;
            final List<String> names = factoryContext.getParameterNameProvider().getParameterNames(method);
            for (final Annotation[] paramAnnos : paramsAnnos) {
                if (!factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotationOnParameter(method, idx)) {
                    final ParameterAccess access = new ParameterAccess(method.getParameterTypes()[idx], idx);
                    processAnnotations(methodDesc, paramAnnos, access, idx, names.get(idx));
                } else {
                    final ParameterDescriptorImpl parameterDescriptor = new ParameterDescriptorImpl(metaBean, new Validation[0], names.get(idx));
                    parameterDescriptor.setIndex(idx);
                    methodDesc.getParameterDescriptors().add(parameterDescriptor);
                }
                idx++;
            }
        }

        if (factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotationOnCrossParameter(method) && methodDesc.getCrossParameterDescriptor() != null) {
            methodDesc.setCrossParameterDescriptor(null);
        }
        if (factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotationOnReturn(method) && methodDesc.getReturnValueDescriptor() != null) {
            methodDesc.setReturnValueDescriptor(null);
        }

        final MetaMethod metaMethod = metaBean.getMethod(method);
        if (metaMethod != null) {
            for (final Annotation anno : metaMethod.getAnnotations()) {
                if (anno instanceof Valid) {
                    methodDesc.setCascaded(true);
                } else {
                    // set first param as null to force it to be read
                    processAnnotations(null, methodDesc, method.getReturnType(), anno);
                }
            }

            // parameter validations
            final Collection<MetaParameter> paramsAnnos = metaMethod.getParameters();
            final List<String> names = factoryContext.getParameterNameProvider().getParameterNames(method);
            for (final MetaParameter paramAnnos : paramsAnnos) {
                final int idx = paramAnnos.getIndex();
                final ParameterAccess access = new ParameterAccess(method.getParameterTypes()[idx], idx);
                processAnnotations(methodDesc, paramAnnos.getAnnotations(), access, idx, names.get(idx));
            }

        }

    }

    private AppendValidationToList processAnnotations(InvocableElementDescriptor methodDesc, Annotation[] paramAnnos, AccessStrategy access, int idx, String name)
            throws InvocationTargetException, IllegalAccessException {
        final AppendValidationToList validations = new AppendValidationToList();
        boolean cascaded = false;

        Group[] from = null;
        Group[] to = null;

        for (final Annotation anno : paramAnnos) {
            if (anno instanceof Valid || anno instanceof Validate) {
                cascaded = true;
            } else if (ConvertGroup.class.isInstance(anno)) {
                final ConvertGroup cg = ConvertGroup.class.cast(anno);
                from = new Group[]{new Group(cg.from())};
                to = new Group[]{new Group(cg.to())};
            } else if (ConvertGroup.List.class.isInstance(anno)) {
                final ConvertGroup.List cgl = ConvertGroup.List.class.cast(anno);
                final ConvertGroup[] groups = cgl.value();
                from = new Group[groups.length];
                to = new Group[groups.length];
                for (int i = 0; i < to.length; i++) {
                    from[i] = new Group(groups[i].from());
                    to[i] = new Group(groups[i].to());
                }
            } else {
                processAnnotation(anno, methodDesc, access, validations);
            }
        }

        ParameterDescriptorImpl paramDesc = null;
        for (final ParameterDescriptor pd : methodDesc.getParameterDescriptors()) {
            if (pd.getIndex() == idx) {
                paramDesc = ParameterDescriptorImpl.class.cast(pd);
            }
        }

        if (paramDesc == null) {
            paramDesc = new ParameterDescriptorImpl(Class.class.cast(access.getJavaType()), // set from getParameterTypes() so that's a Class<?>
                    validations.getValidations().toArray(new Validation[validations.getValidations().size()]), name);
            paramDesc.setIndex(idx);
            methodDesc.getParameterDescriptors().add(paramDesc);
            paramDesc.setCascaded(cascaded);
        } else {
            paramDesc.getMutableConstraintDescriptors().addAll(validations.getValidations());
            if (cascaded) {
                paramDesc.setCascaded(true);
            } // else keep previous config
        }
        if (paramDesc.isCascaded() && from != null) {
            for (int i = 0; i < from.length; i++) {
                paramDesc.addGroupConversion(new GroupConversionDescriptorImpl(from[i], to[i]));
            }
        } else if (from != null) {
            throw new ConstraintDeclarationException("Group conversion is only relevant for @Valid cases");
        }

        return validations;
    }

    private <A extends Annotation> void processAnnotation(A annotation, InvocableElementDescriptor desc,
                                                          AccessStrategy access, AppendValidation validations) throws InvocationTargetException, IllegalAccessException {

        if (annotation instanceof Valid || annotation instanceof Validate) {
            desc.setCascaded(true);
        } else if (ConvertGroup.class.isInstance(annotation) && ReturnAccess.class.isInstance(access)) { // access is just tested to ensure to not read it twice with cross parameter
            final ConvertGroup cg = ConvertGroup.class.cast(annotation);
            desc.addGroupConversion(new GroupConversionDescriptorImpl(new Group(cg.from()), new Group(cg.to())));
        } else if (ConvertGroup.List.class.isInstance(annotation) && ReturnAccess.class.isInstance(access)) {
            final ConvertGroup.List cgl = ConvertGroup.List.class.cast(annotation);
            for (final ConvertGroup cg : cgl.value()) {
                desc.addGroupConversion(new GroupConversionDescriptorImpl(new Group(cg.from()), new Group(cg.to())));
            }
        } else {
            Constraint vcAnno = annotation.annotationType().getAnnotation(Constraint.class);
            if (vcAnno != null) {
                annotationProcessor.processAnnotation(annotation, null, ClassUtils.primitiveToWrapper((Class<?>) access.getJavaType()), access, validations, true);
            } else {
                /**
                 * Multi-valued constraints
                 */
                if (ConstraintAnnotationAttributes.VALUE.isDeclaredOn(annotation.annotationType())) {
                    Annotation[] children = ConstraintAnnotationAttributes.VALUE.getValue(annotation);
                    if (children != null) {
                        for (Annotation child : children) {
                            processAnnotation(child, desc, access, validations); // recursion
                        }
                    }
                }
            }
        }
    }
}
