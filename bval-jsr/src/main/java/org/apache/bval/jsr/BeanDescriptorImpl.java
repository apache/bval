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

import org.apache.bval.Validate;
import org.apache.bval.jsr.groups.Group;
import org.apache.bval.jsr.groups.GroupConversionDescriptorImpl;
import org.apache.bval.jsr.util.ClassHelper;
import org.apache.bval.jsr.xml.AnnotationIgnores;
import org.apache.bval.model.Features;
import org.apache.bval.model.Features.Bean;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaConstructor;
import org.apache.bval.model.MetaMethod;
import org.apache.bval.model.MetaParameter;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.Validation;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

import javax.validation.Constraint;
import javax.validation.ConstraintDeclarationException;
import javax.validation.ConstraintTarget;
import javax.validation.Valid;
import javax.validation.groups.ConvertGroup;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
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
import java.lang.reflect.AccessibleObject;
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
@Privilizing(@CallTo(Reflection.class))
public class BeanDescriptorImpl extends ElementDescriptorImpl implements BeanDescriptor {
    private static final CopyOnWriteArraySet<ConstraintValidation<?>> NO_CONSTRAINTS = new CopyOnWriteArraySet<ConstraintValidation<?>>();
    private static final Validation[] EMPTY_VALIDATION = new Validation[0];

    /**
     * The {@link ApacheFactoryContext} (not) used by this
     * {@link BeanDescriptorImpl}
     */
    private final Set<ConstructorDescriptor> constrainedConstructors;
    private final Set<MethodDescriptor> containedMethods;
    private final ExecutableMeta meta;
    private final Boolean isBeanConstrained;
    private final Set<PropertyDescriptor> validatedProperties;

    protected BeanDescriptorImpl(final ApacheFactoryContext factoryContext, final MetaBean metaBean) {
        super(metaBean, metaBean.getBeanClass(), metaBean.getValidations());

        Set<PropertyDescriptor> procedureDescriptors = metaBean.getFeature(Bean.PROPERTIES);
        if (procedureDescriptors == null) {
            procedureDescriptors = new HashSet<PropertyDescriptor>();
            for (final MetaProperty prop : metaBean.getProperties()) {
                if (prop.getValidations().length > 0
                    || (prop.getMetaBean() != null || prop.getFeature(Features.Property.REF_CASCADE) != null)) {
                    procedureDescriptors.add(getPropertyDescriptor(prop));
                }
            }
            procedureDescriptors = metaBean.initFeature(Bean.PROPERTIES, procedureDescriptors);
        }

        ExecutableMeta executables = metaBean.getFeature(Bean.EXECUTABLES);
        if (executables == null) { // caching the result of it is important to avoid to compute it for each Validator
            executables = new ExecutableMeta(factoryContext, metaBean, getConstraintDescriptors());
            executables = metaBean.initFeature(Bean.EXECUTABLES, executables);
        }

        validatedProperties = Collections.unmodifiableSet(procedureDescriptors);
        meta = executables;
        isBeanConstrained = meta.isBeanConstrained;
        containedMethods = toConstrained(meta.methodConstraints.values());
        constrainedConstructors = toConstrained(meta.contructorConstraints.values());
    }

    private static void addGroupConvertion(final MetaProperty prop, final PropertyDescriptorImpl edesc) {
        boolean fieldFound = false;
        boolean methodFound = false;
        Class<?> current = prop.getParentMetaBean().getBeanClass();
        while (current != null && current != Object.class && (!methodFound || !fieldFound)) {
            if (!fieldFound) {
                final Field field = Reflection.getDeclaredField(current, prop.getName());
                if (field != null) {
                    processConvertGroup(edesc, field);
                    fieldFound = true;
                }
            }
            if (!methodFound) {
                final String name = Character.toUpperCase(prop.getName().charAt(0)) + prop.getName().substring(1);
                Method m = Reflection.getDeclaredMethod(current, "get" + name);
                if (m == null) {
                    final Method isAccessor = Reflection.getDeclaredMethod(current, "is" + name);
                    if (isAccessor != null && boolean.class.equals(isAccessor.getReturnType())) {
                        m = isAccessor;
                    }
                }
                if (m != null) {
                    processConvertGroup(edesc, m);
                    methodFound = true;
                }
            }
            current = current.getSuperclass();
        }

        final Collection<Annotation> annotations = prop.getFeature(JsrFeatures.Property.ANNOTATIONS_TO_PROCESS);
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

    private static void processConvertGroup(final ElementDescriptorImpl edesc, final AccessibleObject accessible) {
        final ConvertGroup.List convertGroupList = accessible.getAnnotation(ConvertGroup.List.class);
        if (convertGroupList != null) {
            for (final ConvertGroup convertGroup : convertGroupList.value()) {
                edesc.addGroupConversion(new GroupConversionDescriptorImpl(new Group(convertGroup.from()), new Group(convertGroup.to())));
            }
        }
        final ConvertGroup convertGroup = accessible.getAnnotation(ConvertGroup.class);
        if (convertGroup != null) {
            edesc.addGroupConversion(new GroupConversionDescriptorImpl(new Group(convertGroup.from()), new Group(convertGroup.to())));
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
    @Override
    public boolean isBeanConstrained() {
        return isBeanConstrained;
    }

    /**
     * Return the property level constraints for a given propertyName or {@code null} if
     * either the property does not exist or has no constraint. The returned
     * object (and associated objects including ConstraintDescriptors) are
     * immutable.
     *
     * @param propertyName property evaluated
     */
    @Override
    public PropertyDescriptor getConstraintsForProperty(String propertyName) {
        if (propertyName == null || propertyName.trim().length() == 0) {
            throw new IllegalArgumentException("propertyName cannot be null or empty");
        }
        final MetaProperty prop = metaBean.getProperty(propertyName);
        if (prop == null) {
            return null;
        }
        // If no constraints and not cascaded, return null
        if (prop.getValidations().length == 0 && prop.getFeature(Features.Property.REF_CASCADE) == null) {
            return null;
        }
        return getPropertyDescriptor(prop);
    }

    private PropertyDescriptor getPropertyDescriptor(final MetaProperty prop) {
        PropertyDescriptorImpl edesc = prop.getFeature(JsrFeatures.Property.PropertyDescriptor);
        if (edesc == null) {
            edesc = new PropertyDescriptorImpl(prop);
            addGroupConvertion(prop, edesc);
            prop.putFeature(JsrFeatures.Property.PropertyDescriptor, edesc);
        }
        return edesc;
    }

    /**
     * {@inheritDoc}
     *
     * @return the property descriptors having at least a constraint defined
     */
    @Override
    public Set<PropertyDescriptor> getConstrainedProperties() {
        return Collections.unmodifiableSet(validatedProperties);
    }

    public MethodDescriptor getInternalConstraintsForMethod(final String methodName, final Class<?>... parameterTypes) {
        if (methodName == null) {
            throw new IllegalArgumentException("Method name can't be null");
        }
        return meta.methodConstraints.get(methodName + Arrays.toString(parameterTypes));
    }

    @Override
    public MethodDescriptor getConstraintsForMethod(final String methodName, final Class<?>... parameterTypes) {
        if (methodName == null) {
            throw new IllegalArgumentException("Method name can't be null");
        }
        final MethodDescriptor methodDescriptor = meta.methodConstraints.get(methodName + Arrays.toString(parameterTypes));
        if (methodDescriptor != null && (methodDescriptor.hasConstrainedParameters() || methodDescriptor.hasConstrainedReturnValue())) {
            return methodDescriptor;
        }
        return null;
    }

    @Override
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
            final boolean getter =
                d.getParameterDescriptors().isEmpty()
                    && (d.getName().startsWith("get") || (d.getName().startsWith("is") && boolean.class.equals(d
                        .getReturnValueDescriptor().getElementClass())));

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

    @Override
    public ConstructorDescriptor getConstraintsForConstructor(final Class<?>... parameterTypes) {
        final ConstructorDescriptor descriptor = meta.contructorConstraints.get(Arrays.toString(parameterTypes));
        if (descriptor != null && (descriptor.hasConstrainedParameters() || descriptor.hasConstrainedReturnValue())) {
            return descriptor;
        }
        return null;
    }

    @Override
    public Set<ConstructorDescriptor> getConstrainedConstructors() {
        return constrainedConstructors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "BeanDescriptorImpl{" + "returnType=" + elementClass + '}';
    }

    private static <A extends ExecutableDescriptor> Set<A> toConstrained(final Collection<A> src) {
        final Set<A> dest = new HashSet<A>();
        for (final A d : src) {
            if (d.hasConstrainedParameters() || d.hasConstrainedReturnValue()) {
                dest.add(d);
            }
        }
        return Collections.unmodifiableSet(dest);
    }

    private static class ExecutableMeta {
        private final ApacheFactoryContext factoryContext;
        private final AnnotationProcessor annotationProcessor;
        private final MetaBean metaBean;
        private final Map<String, MethodDescriptor> methodConstraints = new HashMap<String, MethodDescriptor>();
        private final Map<String, ConstructorDescriptor> contructorConstraints = new HashMap<String, ConstructorDescriptor>();
        private Boolean isBeanConstrained = null;

        private ExecutableMeta(final ApacheFactoryContext factoryContext, final MetaBean metaBean1, final Collection<ConstraintDescriptor<?>> constraintDescriptors) {
            this.metaBean = metaBean1;
            this.factoryContext = factoryContext;
            this.annotationProcessor = new AnnotationProcessor(factoryContext.getFactory());

            buildExecutableDescriptors();

            boolean hasAnyContraints;
            if (constraintDescriptors.isEmpty()) {
                hasAnyContraints = false;
                for (final MetaProperty mprop : metaBean.getProperties()) {
                    if (!getConstraintDescriptors(mprop.getValidations()).isEmpty()) {
                        hasAnyContraints = true;
                        break;
                    }
                }
            } else {
                hasAnyContraints = true;
            }

            // cache isBeanConstrained
            if (hasAnyContraints) {
                isBeanConstrained = true;
            } else {
                isBeanConstrained = false;
                for (final MetaProperty mprop : metaBean.getProperties()) {
                    if (mprop.getMetaBean() != null || mprop.getFeature(Features.Property.REF_CASCADE) != null) {
                        isBeanConstrained = true;
                        break;
                    }
                }
            }
        }

        private void buildConstructorConstraints() throws InvocationTargetException, IllegalAccessException {
            for (final Constructor<?> cons : Reflection.getDeclaredConstructors(metaBean.getBeanClass())) {
                final ConstructorDescriptorImpl consDesc = new ConstructorDescriptorImpl(metaBean, EMPTY_VALIDATION);
                contructorConstraints.put(Arrays.toString(cons.getParameterTypes()), consDesc);

                final List<String> names = factoryContext.getParameterNameProvider().getParameterNames(cons);
                final boolean isInnerClass = cons.getDeclaringClass().getEnclosingClass() != null && !Modifier.isStatic(cons.getDeclaringClass().getModifiers());

                final AnnotationIgnores annotationIgnores = factoryContext.getFactory().getAnnotationIgnores();

                {
                    final Annotation[][] paramsAnnos = cons.getParameterAnnotations();

                    int idx = 0;
                    if (isInnerClass) { // paramsAnnos.length = parameterTypes.length - 1 in this case
                        final ParameterDescriptorImpl paramDesc = new ParameterDescriptorImpl(metaBean, EMPTY_VALIDATION, names.get(idx));
                        consDesc.getParameterDescriptors().add(paramDesc);
                        idx++;
                    }

                    for (final Annotation[] paramAnnos : paramsAnnos) {
                        if (annotationIgnores.isIgnoreAnnotationOnParameter(cons, idx)) {
                            consDesc.getParameterDescriptors().add(new ParameterDescriptorImpl(metaBean, EMPTY_VALIDATION, names.get(idx)));
                        } else if (cons.getParameterTypes().length > idx) {
                            ParameterAccess access = new ParameterAccess(cons.getParameterTypes()[idx], idx);
                            consDesc.addValidations(processAnnotations(consDesc, paramAnnos, access, idx, names.get(idx)).getValidations());
                        } // else anonymous class so that's fine
                        idx++;
                    }

                    if (!annotationIgnores.isIgnoreAnnotations(cons)) {
                        for (final Annotation anno : cons.getAnnotations()) {
                            if (Valid.class.isInstance(anno)) {
                                consDesc.setCascaded(true);
                            } else {
                                processAnnotations(null, consDesc, cons.getDeclaringClass(), anno);
                            }
                        }
                    }
                }

                if (annotationIgnores.isIgnoreAnnotationOnCrossParameter(cons) && consDesc.getCrossParameterDescriptor() != null) {
                    consDesc.setCrossParameterDescriptor(null);
                }
                if (annotationIgnores.isIgnoreAnnotationOnReturn(cons) && consDesc.getReturnValueDescriptor() != null) {
                    consDesc.setReturnValueDescriptor(null);
                }

                final MetaConstructor metaConstructor = metaBean.getConstructor(cons);
                if (metaConstructor != null) {
                    for (final Annotation anno : metaConstructor.getAnnotations()) {
                        if (Valid.class.isInstance(anno)) {
                            consDesc.setCascaded(true);
                        } else {
                            processAnnotations(null, consDesc, cons.getDeclaringClass(), anno);
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

                if (consDesc.getGroupConversions().isEmpty() || consDesc.isCascaded()) {
                    ensureNotNullDescriptors(cons.getDeclaringClass(), consDesc);
                } else {
                    throw new ConstraintDeclarationException("@Valid is needed to define a group conversion");
                }
            }
        }

        private void ensureNotNullDescriptors(final Class<?> returnType, final InvocableElementDescriptor consDesc) {
            // can't be null
            if (consDesc.getCrossParameterDescriptor() == null) {
                consDesc.setCrossParameterDescriptor(new CrossParameterDescriptorImpl(metaBean, NO_CONSTRAINTS));
            }
            if (consDesc.getReturnValueDescriptor() == null) {
                consDesc.setReturnValueDescriptor(new ReturnValueDescriptorImpl(metaBean, returnType, NO_CONSTRAINTS, consDesc.isCascaded()));
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
                if (consDesc.getReturnValueDescriptor() == null) {
                    returnValueDescriptor = new ReturnValueDescriptorImpl(metaBean, clazz, list, consDesc.isCascaded());
                    consDesc.setReturnValueDescriptor(returnValueDescriptor);
                } else {
                    returnValueDescriptor.getMutableConstraintDescriptors().addAll(list);
                }
            }

            if (mtd == null || !factoryContext.getFactory().getAnnotationIgnores().isIgnoreAnnotationOnCrossParameter(mtd)) {
                final ParametersAccess parametersAccess = new ParametersAccess();
                final AppendValidationToList validations = new AppendValidationToList();
                processAnnotation(anno, consDesc, parametersAccess, validations);
                final List<ConstraintValidation<?>> list = removeFromListValidationAppliesTo(validations.getValidations(), ConstraintTarget.RETURN_VALUE);
                consDesc.addValidations(list);
                if (consDesc.getCrossParameterDescriptor() == null) {
                    consDesc.setCrossParameterDescriptor(new CrossParameterDescriptorImpl(metaBean, list));
                } else {
                    CrossParameterDescriptorImpl.class.cast(consDesc.getCrossParameterDescriptor()).getMutableConstraintDescriptors().addAll(list);
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

            final Class<?> current = metaBean.getBeanClass();
            final List<Class<?>> classHierarchy = ClassHelper.fillFullClassHierarchyAsList(new ArrayList<Class<?>>(), current);
            classHierarchy.remove(current);

            for (final Method method : Reflection.getDeclaredMethods(current)) {
                if (Modifier.isStatic(method.getModifiers()) || method.isSynthetic()) {
                    continue;
                }

                final boolean propertyAccessor =
                    method.getParameterTypes().length == 0
                        && (method.getName().startsWith("get") && !Void.TYPE.equals(method.getReturnType()) || method
                            .getName().startsWith("is") && Boolean.TYPE.equals(method.getReturnType()));

                final String key = method.getName() + Arrays.toString(method.getParameterTypes());
                MethodDescriptorImpl methodDesc = MethodDescriptorImpl.class.cast(methodConstraints.get(key));
                if (methodDesc == null) {
                    methodDesc = new MethodDescriptorImpl(metaBean, EMPTY_VALIDATION, method);
                    methodConstraints.put(key, methodDesc);
                } else {
                    continue;
                }

                final Collection<Method> parents = new ArrayList<Method>();
                for (final Class<?> clazz : classHierarchy) {
                    final Method overridden = Reflection.getDeclaredMethod(clazz, method.getName(), method.getParameterTypes());
                    if (overridden != null) {
                        parents.add(overridden);
                        processMethod(overridden, methodDesc);
                    }
                }

                processMethod(method, methodDesc);

                ensureNotNullDescriptors(method.getReturnType(), methodDesc);

                if (parents != null) {
                    if (parents.size() > 1) {
                        for (final Method parent : parents) {
                            final MethodDescriptor parentDec = factoryContext.getValidator().getConstraintsForClass(parent.getDeclaringClass()).getConstraintsForMethod(parent.getName(), parent.getParameterTypes());
                            if (parentDec != null) {
                                ensureNoParameterConstraint(InvocableElementDescriptor.class.cast(parentDec), "Parameter constraints can't be defined for parallel interfaces/parents");
                            } else {
                                ensureMethodDoesntDefineParameterConstraint(methodDesc);
                            }
                            ensureNoReturnValueAddedInChild(methodDesc.getReturnValueDescriptor(), parentDec, "Return value constraints should be the same for parent and children");
                        }
                    } else if (parents.size() == 1) {
                        final Method parent = parents.iterator().next();
                        final MethodDescriptor parentDesc = factoryContext.getValidator().getConstraintsForClass(parent.getDeclaringClass()).getConstraintsForMethod(parent.getName(), parent.getParameterTypes());
                        ensureNoReturnValueAddedInChild(methodDesc.getReturnValueDescriptor(), parentDesc, "Return value constraints should be at least the same for parent and children");

                        if (parentDesc == null) {
                            ensureMethodDoesntDefineParameterConstraint(methodDesc);
                        } else {
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
                        }
                    }

                    final Class<?>[] interfaces = method.getDeclaringClass().getInterfaces();
                    final Collection<Method> itfWithThisMethod = new ArrayList<Method>();
                    for (final Class<?> i : interfaces) {
                        final Method m = Reflection.getDeclaredMethod(i, method.getName(), method.getParameterTypes());
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
                        final Method overridden = Reflection.getDeclaredMethod(clazz, method.getName(), method.getParameterTypes());
                        if (overridden != null) {
                            if (overridden.getAnnotation(Valid.class) != null) {
                                returnValid++;
                            }
                        }
                    }
                    if (returnValid > 1 && !(interfaces.length == returnValid && method.getAnnotation(Valid.class) == null)) {
                        throw new ConstraintDeclarationException("@Valid on returned value can't be set more than once");
                    }
                }

                if (propertyAccessor) {
                    final MetaProperty prop = metaBean.getProperty(Introspector.decapitalize(method.getName().substring(3)));
                    if (prop != null && prop.getFeature(Features.Property.REF_CASCADE) != null) {
                        methodDesc.setCascaded(true);
                    }
                }

                if (!methodDesc.getGroupConversions().isEmpty() && !methodDesc.isCascaded()) {
                    throw new ConstraintDeclarationException("@Valid is needed to define a group conversion");
                }
            }

            for (final Class<?> parent : classHierarchy) {
                final BeanDescriptorImpl desc = BeanDescriptorImpl.class.cast(factoryContext.getValidator().getConstraintsForClass(parent));
                for (final String s : desc.meta.methodConstraints.keySet()) {
                    if (!methodConstraints.containsKey(s)) { // method from the parent only
                        methodConstraints.put(s, desc.meta.methodConstraints.get(s));
                    }
                }
            }
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

        private void ensureNoReturnValueAddedInChild(final ReturnValueDescriptor returnValueDescriptor, final MethodDescriptor parentMtdDesc, final String msg) {
            if (parentMtdDesc == null) {
                return;
            }
            final ReturnValueDescriptor parentReturnDesc = parentMtdDesc.getReturnValueDescriptor();
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

        private void processMethod(final Method method, final MethodDescriptorImpl methodDesc) throws InvocationTargetException, IllegalAccessException {
            final AnnotationIgnores annotationIgnores = factoryContext.getFactory().getAnnotationIgnores();

            { // reflection
                if (!annotationIgnores.isIgnoreAnnotations(method)) {
                    // return value validations and/or cross-parameter validation
                    for (final Annotation anno : method.getAnnotations()) {
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
                    if (annotationIgnores.isIgnoreAnnotationOnParameter(method, idx)) {
                        final ParameterDescriptorImpl parameterDescriptor = new ParameterDescriptorImpl(metaBean, EMPTY_VALIDATION, names.get(idx));
                        parameterDescriptor.setIndex(idx);
                        methodDesc.getParameterDescriptors().add(parameterDescriptor);
                    } else {
                        final ParameterAccess access = new ParameterAccess(method.getParameterTypes()[idx], idx);
                        processAnnotations(methodDesc, paramAnnos, access, idx, names.get(idx));
                    }
                    idx++;
                }
            }

            if (annotationIgnores.isIgnoreAnnotationOnCrossParameter(method) && methodDesc.getCrossParameterDescriptor() != null) {
                methodDesc.setCrossParameterDescriptor(null);
            }
            if (annotationIgnores.isIgnoreAnnotationOnReturn(method) && methodDesc.getReturnValueDescriptor() != null) {
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
                    processConstraint(anno, methodDesc, access, validations);
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
                final List<ParameterDescriptor> parameterDescriptors = methodDesc.getParameterDescriptors();
                if (!parameterDescriptors.contains(paramDesc)) {
                    parameterDescriptors.add(paramDesc);
                }
                paramDesc.setCascaded(cascaded);
            } else {
                final List<ConstraintValidation<?>> newValidations = validations.getValidations();
                for (final ConstraintValidation<?> validation : newValidations) { // don't add it if exactly the same is already here
                    boolean alreadyHere = false;
                    for (final ConstraintDescriptor<?> existing : paramDesc.getMutableConstraintDescriptors()) {
                        if (existing.getAnnotation().annotationType().equals(validation.getAnnotation().annotationType())) { // TODO: make it a bit finer
                            alreadyHere = true;
                            break;
                        }
                    }
                    if (!alreadyHere) {
                        paramDesc.getMutableConstraintDescriptors().add(validation);
                    }
                }
                if (cascaded) {
                    paramDesc.setCascaded(true);
                } // else keep previous config
            }
            if (from != null) {
                if (paramDesc.isCascaded()) {
                    for (int i = 0; i < from.length; i++) {
                        paramDesc.addGroupConversion(new GroupConversionDescriptorImpl(from[i], to[i]));
                    }
                } else {
                    throw new ConstraintDeclarationException("Group conversion is only relevant for @Valid cases");
                }
            }
            return validations;
        }

        private <A extends Annotation> void processAnnotation(final A annotation, final InvocableElementDescriptor desc,
                                                              final AccessStrategy access, final AppendValidation validations) throws InvocationTargetException, IllegalAccessException {
            if (annotation.annotationType().getName().startsWith("java.lang.annotation.")) {
                return;
            }

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
                processConstraint(annotation, desc, access, validations);
            }
        }

        private <A extends Annotation> void processConstraint(final A annotation,
            final InvocableElementDescriptor desc, final AccessStrategy access, final AppendValidation validations)
            throws IllegalAccessException, InvocationTargetException {
            final Constraint vcAnno = annotation.annotationType().getAnnotation(Constraint.class);
            if (vcAnno == null) {
                /*
                 * Multi-valued constraints
                 */
                final ConstraintAnnotationAttributes.Worker<? extends Annotation> worker = ConstraintAnnotationAttributes.VALUE.analyze(annotation.annotationType());
                if (worker.isValid()) {
                    final Object value = worker.read(annotation);
                    if (Annotation[].class.isInstance(value)) {
                        final Annotation[] children = Annotation[].class.cast(value);
                        if (children != null) {
                            for (Annotation child : children) {
                                processAnnotation(child, desc, access, validations); // recursion
                            }
                        }
                    }
                }
            } else {
                annotationProcessor.processAnnotation(
                        annotation, null, ClassUtils.primitiveToWrapper((Class<?>) access.getJavaType()),
                        access, validations, true);
            }
        }

        private void buildExecutableDescriptors() {
            try {
                buildMethodConstraints();
                buildConstructorConstraints();
            } catch (final Exception ex) {
                if (RuntimeException.class.isInstance(ex)) {
                    throw RuntimeException.class.cast(ex);
                }
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }
    }
}
