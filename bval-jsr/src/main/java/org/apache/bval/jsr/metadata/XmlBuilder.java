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
package org.apache.bval.jsr.metadata;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintDeclarationException;
import javax.validation.ConstraintTarget;
import javax.validation.Payload;
import javax.validation.ValidationException;
import javax.xml.bind.JAXBElement;

import org.apache.bval.jsr.ConstraintAnnotationAttributes;
import org.apache.bval.jsr.groups.GroupConversion;
import org.apache.bval.jsr.util.AnnotationsManager;
import org.apache.bval.jsr.util.ToUnmodifiable;
import org.apache.bval.jsr.xml.AnnotationProxyBuilder;
import org.apache.bval.jsr.xml.AnnotationType;
import org.apache.bval.jsr.xml.BeanType;
import org.apache.bval.jsr.xml.ClassType;
import org.apache.bval.jsr.xml.ConstraintMappingsType;
import org.apache.bval.jsr.xml.ConstraintType;
import org.apache.bval.jsr.xml.ConstructorType;
import org.apache.bval.jsr.xml.ContainerElementTypeType;
import org.apache.bval.jsr.xml.CrossParameterType;
import org.apache.bval.jsr.xml.ElementType;
import org.apache.bval.jsr.xml.FieldType;
import org.apache.bval.jsr.xml.GetterType;
import org.apache.bval.jsr.xml.GroupConversionType;
import org.apache.bval.jsr.xml.GroupSequenceType;
import org.apache.bval.jsr.xml.GroupsType;
import org.apache.bval.jsr.xml.MethodType;
import org.apache.bval.jsr.xml.ParameterType;
import org.apache.bval.jsr.xml.PayloadType;
import org.apache.bval.jsr.xml.ReturnValueType;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.ObjectUtils;
import org.apache.bval.util.Validate;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

@Privilizing(@CallTo(Reflection.class))
public class XmlBuilder {
    //@formatter:off
    public enum Version {
        v10("1.0"), v11("1.1"), v20("2.0");

        final BigDecimal number;
        private final String id;

        private Version(String number) {
            this.id = number;
            this.number = new BigDecimal(number);
        }

        public String getId() {
            return id;
        }
    }
    //@formatter:on

    private class ForBean<T> implements MetadataBuilder.ForBean<T> {

        private final BeanType descriptor;

        ForBean(BeanType descriptor) {
            super();
            this.descriptor = Validate.notNull(descriptor, "descriptor");
        }

        Class<?> getBeanClass() {
            return resolveClass(descriptor.getClazz());
        }

        @Override
        public MetadataBuilder.ForClass<T> getClass(Meta<Class<T>> meta) {
            final ClassType classType = descriptor.getClassType();
            return classType == null ? EmptyBuilder.instance().<T> forBean().getClass(meta)
                : new XmlBuilder.ForClass<T>(classType);
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Field>> getFields(Meta<Class<T>> meta) {
            return descriptor.getField().stream()
                .collect(ToUnmodifiable.map(FieldType::getName, XmlBuilder.ForField::new));
        }

        @Override
        public Map<String, MetadataBuilder.ForContainer<Method>> getGetters(Meta<Class<T>> meta) {
            return descriptor.getGetter().stream()
                .collect(ToUnmodifiable.map(GetterType::getName, XmlBuilder.ForGetter::new));
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Constructor<? extends T>>> getConstructors(Meta<Class<T>> meta) {
            if (!atLeast(Version.v11)) {
                return Collections.emptyMap();
            }
            final Function<ConstructorType, Class<?>[]> params = ct -> ct.getParameter().stream()
                .map(ParameterType::getType).map(XmlBuilder.this::resolveClass).toArray(Class[]::new);

            final Function<ConstructorType, Signature> signature =
                ct -> new Signature(meta.getHost().getName(), params.apply(ct));

            return descriptor.getConstructor().stream()
                .collect(Collectors.toMap(signature, XmlBuilder.ForConstructor::new));
        }

        @Override
        public Map<Signature, MetadataBuilder.ForExecutable<Method>> getMethods(Meta<Class<T>> meta) {
            if (!atLeast(Version.v11)) {
                return Collections.emptyMap();
            }
            final Function<MethodType, Class<?>[]> params = mt -> mt.getParameter().stream().map(ParameterType::getType)
                .map(XmlBuilder.this::resolveClass).toArray(Class[]::new);

            final Function<MethodType, Signature> signature = mt -> new Signature(mt.getName(), params.apply(mt));

            return descriptor.getMethod().stream().collect(Collectors.toMap(signature, XmlBuilder.ForMethod::new));
        }

        @Override
        public final AnnotationBehavior getAnnotationBehavior() {
            return descriptor.getIgnoreAnnotations() ? AnnotationBehavior.EXCLUDE : AnnotationBehavior.INCLUDE;
        }
    }

    private class NonRootLevel<SELF extends NonRootLevel<SELF, D>, D> implements HasAnnotationBehavior {
        protected final D descriptor;
        private Lazy<Boolean> getIgnoreAnnotations;

        public NonRootLevel(D descriptor) {
            super();
            this.descriptor = Validate.notNull(descriptor, "descriptor");
        }

        @Override
        public final AnnotationBehavior getAnnotationBehavior() {
            return Optional.ofNullable(getIgnoreAnnotations).map(Lazy::get)
                .map(b -> b.booleanValue() ? AnnotationBehavior.EXCLUDE : AnnotationBehavior.INCLUDE)
                .orElse(AnnotationBehavior.ABSTAIN);
        }

        @SuppressWarnings("unchecked")
        final SELF withGetIgnoreAnnotations(Function<D, Boolean> getIgnoreAnnotations) {
            Validate.notNull(getIgnoreAnnotations);
            this.getIgnoreAnnotations = new Lazy<>(() -> getIgnoreAnnotations.apply(descriptor));
            return (SELF) this;
        }
    }

    private class ForElement<SELF extends XmlBuilder.ForElement<SELF, E, D>, E extends AnnotatedElement, D>
        extends NonRootLevel<SELF, D> implements MetadataBuilder.ForElement<E> {

        private Lazy<Annotation[]> getDeclaredConstraints;

        ForElement(D descriptor) {
            super(descriptor);
        }

        @Override
        public final Annotation[] getDeclaredConstraints(Meta<E> meta) {
            return lazy(getDeclaredConstraints, "getDeclaredConstraints");
        }

        final SELF withGetConstraintTypes(Function<D, List<ConstraintType>> getConstraintTypes) {
            return withGetDeclaredConstraints(getConstraintTypes
                .andThen(l -> l.stream().map(XmlBuilder.this::createConstraint).toArray(Annotation[]::new)));
        }

        @SuppressWarnings("unchecked")
        final SELF withGetDeclaredConstraints(Function<D, Annotation[]> getDeclaredConstraints) {
            this.getDeclaredConstraints = new Lazy<>(() -> getDeclaredConstraints.apply(descriptor));
            return (SELF) this;
        }
    }

    private class ForClass<T> extends ForElement<ForClass<T>, Class<T>, ClassType> implements MetadataBuilder.ForClass<T> {

        ForClass(ClassType descriptor) {
            super(descriptor);
            this.withGetConstraintTypes(ClassType::getConstraint)
                .withGetIgnoreAnnotations(ClassType::getIgnoreAnnotations);
        }

        @Override
        public List<Class<?>> getGroupSequence(Meta<Class<T>> meta) {
            final GroupSequenceType groupSequence = descriptor.getGroupSequence();
            return groupSequence == null ? null
                : groupSequence.getValue().stream().map(XmlBuilder.this::resolveClass).collect(ToUnmodifiable.list());
        }
    }

    private class ForContainer<SELF extends XmlBuilder.ForContainer<SELF, E, D>, E extends AnnotatedElement, D>
        extends XmlBuilder.ForElement<SELF, E, D> implements MetadataBuilder.ForContainer<E> {

        private Lazy<Boolean> isCascade;
        private Lazy<Set<GroupConversion>> getGroupConversions;
        private Lazy<List<ContainerElementTypeType>> getContainerElementTypes;

        ForContainer(D descriptor) {
            super(descriptor);
        }

        @Override
        public boolean isCascade(Meta<E> meta) {
            return Boolean.TRUE.equals(lazy(isCascade, "isCascade"));
        }

        @Override
        public Set<GroupConversion> getGroupConversions(Meta<E> meta) {
            return lazy(getGroupConversions, "getGroupConversions");
        }

        @Override
        public Map<ContainerElementKey, MetadataBuilder.ForContainer<AnnotatedType>> getContainerElementTypes(
            Meta<E> meta) {
            if (!atLeast(Version.v20)) {
                return Collections.emptyMap();
            }
            final List<ContainerElementTypeType> elements = lazy(getContainerElementTypes, "getContainerElementTypes");
            final AnnotatedType annotatedType = meta.getAnnotatedType();
            final E host = meta.getHost();

            if (annotatedType instanceof AnnotatedParameterizedType) {
                final AnnotatedType[] actualTypeArguments =
                    ((AnnotatedParameterizedType) annotatedType).getAnnotatedActualTypeArguments();

                return elements.stream().collect(ToUnmodifiable.map(cet -> {
                    Integer typeArgumentIndex = cet.getTypeArgumentIndex();
                    if (typeArgumentIndex == null) {
                        Exceptions.raiseIf(actualTypeArguments.length > 1, ValidationException::new,
                            "Missing required type argument index for %s", host);
                        typeArgumentIndex = Integer.valueOf(0);
                    }
                    return new ContainerElementKey(annotatedType, typeArgumentIndex);
                }, XmlBuilder.ForContainerElementType::new));
            }
            if (!elements.isEmpty()) {
                Exceptions.raise(ValidationException::new, "Illegally specified %d container element type(s) for %s",
                    elements.size(), host);
            }
            return Collections.emptyMap();
        }

        @SuppressWarnings("unchecked")
        SELF withGetValid(Function<D, String> getValid) {
            Validate.notNull(getValid);
            this.isCascade = new Lazy<>(() -> getValid.apply(descriptor) != null);
            return (SELF) this;
        }

        @SuppressWarnings("unchecked")
        SELF withGetGroupConversions(Function<D, List<GroupConversionType>> getGroupConversions) {
            Validate.notNull(getGroupConversions);

            this.getGroupConversions = new Lazy<>(() -> {
                return getGroupConversions.apply(descriptor).stream().map(gc -> {
                    final Class<?> source = resolveClass(gc.getFrom());
                    final Class<?> target = resolveClass(gc.getTo());
                    return GroupConversion.from(source).to(target);
                }).collect(ToUnmodifiable.set());
            });
            return (SELF) this;
        }

        @SuppressWarnings("unchecked")
        SELF withGetContainerElementTypes(Function<D, List<ContainerElementTypeType>> getContainerElementTypes) {
            Validate.notNull(getContainerElementTypes);
            this.getContainerElementTypes = new Lazy<>(() -> getContainerElementTypes.apply(descriptor));
            return (SELF) this;
        }
    }

    private class ForContainerElementType
        extends ForContainer<ForContainerElementType, AnnotatedType, ContainerElementTypeType> {

        ForContainerElementType(ContainerElementTypeType descriptor) {
            super(descriptor);
            this.withGetConstraintTypes(ContainerElementTypeType::getConstraint)
                .withGetValid(ContainerElementTypeType::getValid)
                .withGetGroupConversions(ContainerElementTypeType::getConvertGroup)
                .withGetContainerElementTypes(ContainerElementTypeType::getContainerElementType);
        }
    }

    private class ForField extends XmlBuilder.ForContainer<ForField, Field, FieldType> {

        ForField(FieldType descriptor) {
            super(descriptor);
            this.withGetIgnoreAnnotations(FieldType::getIgnoreAnnotations)
                .withGetConstraintTypes(FieldType::getConstraint).withGetValid(FieldType::getValid)
                .withGetGroupConversions(FieldType::getConvertGroup)
                .withGetContainerElementTypes(FieldType::getContainerElementType);
        }
    }

    private class ForGetter extends XmlBuilder.ForContainer<ForGetter, Method, GetterType> {

        ForGetter(GetterType descriptor) {
            super(descriptor);
            this.withGetIgnoreAnnotations(GetterType::getIgnoreAnnotations)
                .withGetConstraintTypes(GetterType::getConstraint).withGetValid(GetterType::getValid)
                .withGetGroupConversions(GetterType::getConvertGroup)
                .withGetContainerElementTypes(GetterType::getContainerElementType);
        }
    }

    private abstract class ForExecutable<SELF extends ForExecutable<SELF, E, D>, E extends Executable, D>
        extends NonRootLevel<SELF, D> implements MetadataBuilder.ForExecutable<E> {

        Lazy<ReturnValueType> getReturnValue;
        Lazy<CrossParameterType> getCrossParameter;
        Lazy<List<ParameterType>> getParameters;

        ForExecutable(D descriptor) {
            super(descriptor);
        }

        @Override
        public MetadataBuilder.ForElement<E> getCrossParameter(Meta<E> meta) {
            final CrossParameterType cp = lazy(getCrossParameter, "getCrossParameter");
            if (cp == null) {
                return EmptyBuilder.instance().<E> forExecutable().getCrossParameter(meta);
            }
            return new XmlBuilder.ForCrossParameter<>(cp);
        }

        @Override
        public MetadataBuilder.ForContainer<E> getReturnValue(Meta<E> meta) {
            final ReturnValueType rv = lazy(getReturnValue, "getReturnValue");
            if (rv == null) {
                return EmptyBuilder.instance().<E> forExecutable().getReturnValue(meta);
            }
            return new XmlBuilder.ForReturnValue<>(rv);
        }

        @Override
        public List<MetadataBuilder.ForContainer<Parameter>> getParameters(Meta<E> meta) {
            return lazy(getParameters, "getParameters").stream().map(XmlBuilder.ForParameter::new)
                .collect(Collectors.toList());
        }

        @SuppressWarnings("unchecked")
        SELF withGetReturnValue(Function<D, ReturnValueType> getReturnValue) {
            Validate.notNull(getReturnValue);
            this.getReturnValue = new Lazy<>(() -> getReturnValue.apply(descriptor));
            return (SELF) this;
        }

        @SuppressWarnings("unchecked")
        SELF withGetCrossParameter(Function<D, CrossParameterType> getCrossParameter) {
            Validate.notNull(getCrossParameter);
            this.getCrossParameter = new Lazy<>(() -> getCrossParameter.apply(descriptor));
            return (SELF) this;
        }

        @SuppressWarnings("unchecked")
        SELF withGetParameters(Function<D, List<ParameterType>> getParameters) {
            Validate.notNull(getParameters);
            this.getParameters = new Lazy<>(() -> getParameters.apply(descriptor));
            return (SELF) this;
        }
    }

    private class ForConstructor<T> extends ForExecutable<ForConstructor<T>, Constructor<? extends T>, ConstructorType> {

        ForConstructor(ConstructorType descriptor) {
            super(descriptor);
            this.withGetIgnoreAnnotations(ConstructorType::getIgnoreAnnotations)
                .withGetReturnValue(ConstructorType::getReturnValue)
                .withGetCrossParameter(ConstructorType::getCrossParameter)
                .withGetParameters(ConstructorType::getParameter);
        }
    }

    private class ForMethod extends ForExecutable<ForMethod, Method, MethodType> {

        ForMethod(MethodType descriptor) {
            super(descriptor);
            this.withGetIgnoreAnnotations(MethodType::getIgnoreAnnotations)
                .withGetReturnValue(MethodType::getReturnValue).withGetCrossParameter(MethodType::getCrossParameter)
                .withGetParameters(MethodType::getParameter);
        }
    }

    private class ForParameter extends ForContainer<ForParameter, Parameter, ParameterType> {

        ForParameter(ParameterType descriptor) {
            super(descriptor);
            this.withGetIgnoreAnnotations(ParameterType::getIgnoreAnnotations)
                .withGetConstraintTypes(ParameterType::getConstraint).withGetValid(ParameterType::getValid)
                .withGetGroupConversions(ParameterType::getConvertGroup)
                .withGetContainerElementTypes(ParameterType::getContainerElementType);
        }
    }

    private class ForCrossParameter<E extends Executable>
        extends ForElement<ForCrossParameter<E>, E, CrossParameterType> {

        ForCrossParameter(CrossParameterType descriptor) {
            super(descriptor);
            this.withGetIgnoreAnnotations(CrossParameterType::getIgnoreAnnotations)
                .withGetDeclaredConstraints(d -> d.getConstraint().stream()
                    .map(ct -> createConstraint(ct, ConstraintTarget.PARAMETERS)).toArray(Annotation[]::new));
        }
    }

    private class ForReturnValue<E extends Executable> extends ForContainer<ForReturnValue<E>, E, ReturnValueType> {

        ForReturnValue(ReturnValueType descriptor) {
            super(descriptor);
            this.withGetDeclaredConstraints(d -> d.getConstraint().stream()
                .map(ct -> createConstraint(ct, ConstraintTarget.RETURN_VALUE)).toArray(Annotation[]::new))
                .withGetIgnoreAnnotations(ReturnValueType::getIgnoreAnnotations).withGetValid(ReturnValueType::getValid)
                .withGetGroupConversions(ReturnValueType::getConvertGroup)
                .withGetContainerElementTypes(ReturnValueType::getContainerElementType);
        }
    }

    private static final Set<ConstraintAnnotationAttributes> RESERVED_PARAMS = Collections
        .unmodifiableSet(EnumSet.of(ConstraintAnnotationAttributes.GROUPS, ConstraintAnnotationAttributes.MESSAGE,
            ConstraintAnnotationAttributes.PAYLOAD, ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO));

    static final <T> T lazy(Lazy<T> lazy, String name) {
        Validate.validState(lazy != null, "%s not set", name);
        return lazy.get();
    }

    private final ConstraintMappingsType constraintMappings;
    private final BigDecimal version;

    public XmlBuilder(ConstraintMappingsType constraintMappings) {
        super();
        this.constraintMappings = constraintMappings;
        Validate.notNull(constraintMappings, "constraintMappings");

        BigDecimal v;
        try {
            v = new BigDecimal(constraintMappings.getVersion());
        } catch (NumberFormatException e) {
            v = Version.v10.number;
        }
        this.version = v;
    }

    public Map<Class<?>, MetadataBuilder.ForBean<?>> forBeans() {
        return constraintMappings.getBean().stream().map(XmlBuilder.ForBean::new)
            .collect(ToUnmodifiable.map(XmlBuilder.ForBean::getBeanClass, Function.identity()));
    }

    public String getDefaultPackage() {
        return constraintMappings.getDefaultPackage();
    }

    boolean atLeast(Version v) {
        return version.compareTo(v.number) >= 0;
    }

    <T> Class<T> resolveClass(String className) {
        return loadClass(toQualifiedClassName(className));
    }

    private String toQualifiedClassName(String className) {
        if (isQualifiedClass(className)) {
            return className;
        }
        if (className.startsWith("[L") && className.endsWith(";")) {
            return "[L" + getDefaultPackage() + "." + className.substring(2);
        }
        return getDefaultPackage() + "." + className;
    }

    private boolean isQualifiedClass(String clazz) {
        return clazz.indexOf('.') >= 0;
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> loadClass(final String fqn) {
        ClassLoader loader = Reflection.getClassLoader(XmlBuilder.class);
        if (loader == null) {
            loader = getClass().getClassLoader();
        }
        try {
            return (Class<T>) Class.forName(fqn, true, loader);
        } catch (ClassNotFoundException ex) {
            throw Exceptions.create(ValidationException::new, ex, "Unable to load class: %d", fqn);
        }
    }

    private Class<?>[] loadClasses(Supplier<Stream<String>> classNames) {
        return streamClasses(classNames).toArray(Class[]::new);
    }

    private Stream<Class<?>> streamClasses(Supplier<Stream<String>> classNames) {
        return classNames.get().map(this::loadClass);
    }

    private <A extends Annotation, T> A createConstraint(final ConstraintType constraint) {
        return createConstraint(constraint, ConstraintTarget.IMPLICIT);
    }

    private <A extends Annotation, T> A createConstraint(final ConstraintType constraint, ConstraintTarget target) {
        final Class<A> annotationClass = this.<A> loadClass(toQualifiedClassName(constraint.getAnnotation()));
        final AnnotationProxyBuilder<A> annoBuilder = new AnnotationProxyBuilder<A>(annotationClass);

        if (constraint.getMessage() != null) {
            annoBuilder.setMessage(constraint.getMessage());
        }
        annoBuilder.setGroups(getGroups(constraint.getGroups()));
        annoBuilder.setPayload(getPayload(constraint.getPayload()));

        if (AnnotationsManager.declaresAttribute(annotationClass,
            ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO.getAttributeName())) {
            annoBuilder.setValidationAppliesTo(target);
        }
        for (final ElementType elementType : constraint.getElement()) {
            final String name = elementType.getName();
            checkValidName(name);

            final Class<?> returnType = getAnnotationParameterType(annotationClass, name);
            final Object elementValue = getElementValue(elementType, returnType);
            annoBuilder.setValue(name, elementValue);
        }
        return annoBuilder.createAnnotation();
    }

    private void checkValidName(String name) {
        Exceptions.raiseIf(RESERVED_PARAMS.stream().map(ConstraintAnnotationAttributes::getAttributeName)
            .anyMatch(Predicate.isEqual(name)), ValidationException::new, "%s is a reserved parameter name.", name);
    }

    private <A extends Annotation> Class<?> getAnnotationParameterType(final Class<A> annotationClass,
        final String name) {
        final Method m = Reflection.getPublicMethod(annotationClass, name);
        Exceptions.raiseIf(m == null, ValidationException::new,
            "Annotation of type %s does not contain a parameter %s.", annotationClass.getName(), name);
        return m.getReturnType();
    }

    private Object getElementValue(ElementType elementType, Class<?> returnType) {
        removeEmptyContentElements(elementType);

        final List<Serializable> content = elementType.getContent();
        final int sz = content.size();
        if (returnType.isArray()) {
            final Object result = Array.newInstance(returnType.getComponentType(), sz);
            for (int i = 0; i < sz; i++) {
                Array.set(result, i, getSingleValue(content.get(i), returnType.getComponentType()));
            }
            return result;
        }
        Exceptions.raiseIf(sz != 1, ValidationException::new,
            "Attempt to specify an array where single value is expected.");

        return getSingleValue(content.get(0), returnType);
    }

    private void removeEmptyContentElements(ElementType elementType) {
        for (Iterator<Serializable> iter = elementType.getContent().iterator(); iter.hasNext();) {
            final Serializable content = iter.next();
            if (content instanceof String && ((String) content).matches("[\\n ].*")) {
                iter.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object getSingleValue(Serializable serializable, Class<?> returnType) {
        if (serializable instanceof String) {
            return convertToResultType(returnType, (String) serializable);
        }
        if (serializable instanceof JAXBElement<?>) {
            final JAXBElement<?> elem = (JAXBElement<?>) serializable;
            if (String.class.equals(elem.getDeclaredType())) {
                return convertToResultType(returnType, (String) elem.getValue());
            }
            if (AnnotationType.class.equals(elem.getDeclaredType())) {
                AnnotationType annotationType = (AnnotationType) elem.getValue();
                try {
                    return createAnnotation(annotationType, (Class<? extends Annotation>) returnType);
                } catch (ClassCastException e) {
                    throw new ValidationException("Unexpected parameter value");
                }
            }
        }
        throw new ValidationException("Unexpected parameter value");
    }

    private Object convertToResultType(Class<?> returnType, String value) {
        /**
         * Class is represented by the fully qualified class name of the class. spec: Note that if the raw string is
         * unqualified, default package is taken into account.
         */
        if (String.class.equals(returnType)) {
            return value;
        }
        if (Class.class.equals(returnType)) {
            return resolveClass(value);
        }
        if (returnType.isEnum()) {
            try {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                final Enum e = Enum.valueOf(returnType.asSubclass(Enum.class), value);
                return e;
            } catch (IllegalArgumentException e) {
                throw new ConstraintDeclarationException(e);
            }
        }
        if (Byte.class.equals(returnType) || byte.class.equals(returnType)) {
            // spec mandates it:
            return Byte.parseByte(value);
        }
        if (Short.class.equals(returnType) || short.class.equals(returnType)) {
            return Short.parseShort(value);
        }
        if (Integer.class.equals(returnType) || int.class.equals(returnType)) {
            return Integer.parseInt(value);
        }
        if (Long.class.equals(returnType) || long.class.equals(returnType)) {
            return Long.parseLong(value);
        }
        if (Float.class.equals(returnType) || float.class.equals(returnType)) {
            return Float.parseFloat(value);
        }
        if (Double.class.equals(returnType) || double.class.equals(returnType)) {
            return Double.parseDouble(value);
        }
        if (Boolean.class.equals(returnType) || boolean.class.equals(returnType)) {
            return Boolean.parseBoolean(value);
        }
        if (Character.class.equals(returnType) || char.class.equals(returnType)) {
            Exceptions.raiseIf(value.length() > 1, ConstraintDeclarationException::new,
                "a char must have a length of 1");
            return value.charAt(0);
        }
        return Exceptions.raise(ValidationException::new, "Unknown annotation value type %s", returnType.getName());
    }

    private <A extends Annotation> Annotation createAnnotation(AnnotationType annotationType, Class<A> returnType) {
        final AnnotationProxyBuilder<A> metaAnnotation = new AnnotationProxyBuilder<>(returnType);
        for (ElementType elementType : annotationType.getElement()) {
            final String name = elementType.getName();
            metaAnnotation.setValue(name, getElementValue(elementType, getAnnotationParameterType(returnType, name)));
        }
        return metaAnnotation.createAnnotation();
    }

    private Class<?>[] getGroups(GroupsType groupsType) {
        if (groupsType == null) {
            return ObjectUtils.EMPTY_CLASS_ARRAY;
        }
        return loadClasses(groupsType.getValue()::stream);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Payload>[] getPayload(PayloadType payloadType) {
        if (payloadType == null) {
            return (Class<? extends Payload>[]) ObjectUtils.EMPTY_CLASS_ARRAY;
        }
        return streamClasses(payloadType.getValue()::stream).peek(pc -> {
            Exceptions.raiseUnless(Payload.class.isAssignableFrom(pc), ConstraintDeclarationException::new,
                "Specified payload class %s does not implement %s", pc.getName(), Payload.class.getName());
        }).<Class<? extends Payload>> map(pc -> pc.asSubclass(Payload.class)).toArray(Class[]::new);
    }
}
