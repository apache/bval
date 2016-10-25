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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.Payload;
import javax.validation.ValidationException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.apache.bval.jsr.ApacheValidatorFactory;
import org.apache.bval.jsr.ConstraintAnnotationAttributes;
import org.apache.bval.jsr.util.IOs;
import org.apache.bval.util.FieldAccess;
import org.apache.bval.util.MethodAccess;
import org.apache.bval.util.ObjectUtils;
import org.apache.bval.util.StringUtils;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privileged;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Uses JAXB to parse constraints.xml based on validation-mapping-1.0.xsd.<br>
 */
@Privilizing(@CallTo(Reflection.class))
public class ValidationMappingParser {
    private static final String VALIDATION_MAPPING_XSD = "META-INF/validation-mapping-1.1.xsd";

    private static final Set<ConstraintAnnotationAttributes> RESERVED_PARAMS = Collections
        .unmodifiableSet(EnumSet.of(ConstraintAnnotationAttributes.GROUPS, ConstraintAnnotationAttributes.MESSAGE,
            ConstraintAnnotationAttributes.PAYLOAD, ConstraintAnnotationAttributes.VALIDATION_APPLIES_TO));

    private final Set<Class<?>> processedClasses;
    private final ApacheValidatorFactory factory;

    public ValidationMappingParser(ApacheValidatorFactory factory) {
        this.factory = factory;
        this.processedClasses = new HashSet<Class<?>>();
    }

    /**
     * Parse files with constraint mappings and collect information in the factory.
     *  
     * @param xmlStreams - one or more contraints.xml file streams to parse
     */
    public void processMappingConfig(Set<InputStream> xmlStreams) throws ValidationException {
        for (final InputStream xmlStream : xmlStreams) {
            ConstraintMappingsType mapping = parseXmlMappings(xmlStream);

            final String defaultPackage = mapping.getDefaultPackage();
            processConstraintDefinitions(mapping.getConstraintDefinition(), defaultPackage);
            for (final BeanType bean : mapping.getBean()) {
                Class<?> beanClass = loadClass(bean.getClazz(), defaultPackage);
                if (!processedClasses.add(beanClass)) {
                    // spec: A given class must not be described more than once amongst all
                    //  the XML mapping descriptors.
                    throw new ValidationException(beanClass.getName() + " has already be configured in xml.");
                }

                boolean ignoreAnnotations = bean.getIgnoreAnnotations() == null ? true : bean.getIgnoreAnnotations();
                factory.getAnnotationIgnores().setDefaultIgnoreAnnotation(beanClass, ignoreAnnotations);
                processClassLevel(bean.getClassType(), beanClass, defaultPackage);
                processConstructorLevel(bean.getConstructor(), beanClass, defaultPackage, ignoreAnnotations);
                processFieldLevel(bean.getField(), beanClass, defaultPackage, ignoreAnnotations);
                final Collection<String> potentialMethodName =
                    processPropertyLevel(bean.getGetter(), beanClass, defaultPackage, ignoreAnnotations);
                processMethodLevel(bean.getMethod(), beanClass, defaultPackage, ignoreAnnotations, potentialMethodName);
                processedClasses.add(beanClass);
            }
        }
    }

    /** @param in XML stream to parse using the validation-mapping-1.0.xsd */
    private ConstraintMappingsType parseXmlMappings(final InputStream in) {
        ConstraintMappingsType mappings;
        try {
            final JAXBContext jc = JAXBContext.newInstance(ConstraintMappingsType.class);
            final Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshaller.setSchema(getSchema());
            final StreamSource stream = new StreamSource(in);
            final JAXBElement<ConstraintMappingsType> root =
                unmarshaller.unmarshal(stream, ConstraintMappingsType.class);
            mappings = root.getValue();
        } catch (final JAXBException e) {
            throw new ValidationException("Failed to parse XML deployment descriptor file.", e);
        } finally {
            IOs.closeQuietly(in);
            try {
                in.reset(); // can be read several times + we ensured it was re-readable in addMapping()
            } catch (final IOException e) {
                // no-op
            }
        }
        return mappings;
    }

    /** @return validation-mapping-1.0.xsd based schema */
    private Schema getSchema() {
        return ValidationParser.getSchema(VALIDATION_MAPPING_XSD);
    }

    private void processClassLevel(ClassType classType, Class<?> beanClass, String defaultPackage) {
        if (classType == null) {
            return;
        }

        // ignore annotation
        if (classType.getIgnoreAnnotations() != null) {
            factory.getAnnotationIgnores().setIgnoreAnnotationsOnClass(beanClass, classType.getIgnoreAnnotations());
        }

        // group sequence
        Class<?>[] groupSequence = createGroupSequence(classType.getGroupSequence(), defaultPackage);
        if (groupSequence != null) {
            factory.addDefaultSequence(beanClass, groupSequence);
        }

        // constraints
        for (ConstraintType constraint : classType.getConstraint()) {
            MetaConstraint<?, ?> metaConstraint = createConstraint(constraint, beanClass, null, defaultPackage);
            factory.addMetaConstraint(beanClass, metaConstraint);
        }
    }

    @SuppressWarnings("unchecked")
    private <A extends Annotation, T> MetaConstraint<?, ?> createConstraint(final ConstraintType constraint,
        final Class<T> beanClass, final Member member, final String defaultPackage) {

        final Class<A> annotationClass = (Class<A>) loadClass(constraint.getAnnotation(), defaultPackage);
        final AnnotationProxyBuilder<A> annoBuilder = new AnnotationProxyBuilder<A>(annotationClass);

        if (constraint.getMessage() != null) {
            annoBuilder.setMessage(constraint.getMessage());
        }
        annoBuilder.setGroups(getGroups(constraint.getGroups(), defaultPackage));
        annoBuilder.setPayload(getPayload(constraint.getPayload(), defaultPackage));

        for (final ElementType elementType : constraint.getElement()) {
            final String name = elementType.getName();
            checkValidName(name);

            final Class<?> returnType = getAnnotationParameterType(annotationClass, name);
            final Object elementValue = getElementValue(elementType, returnType, defaultPackage);
            annoBuilder.putValue(name, elementValue);
        }
        return new MetaConstraint<T, A>(beanClass, member, annoBuilder.createAnnotation());
    }

    private void checkValidName(String name) {
        for (ConstraintAnnotationAttributes attr : RESERVED_PARAMS) {
            if (attr.getAttributeName().equals(name)) {
                throw new ValidationException(name + " is a reserved parameter name.");
            }
        }
    }

    private <A extends Annotation> Class<?> getAnnotationParameterType(final Class<A> annotationClass,
        final String name) {
        final Method m = Reflection.getPublicMethod(annotationClass, name);
        if (m == null) {
            throw new ValidationException(
                "Annotation of type " + annotationClass.getName() + " does not contain a parameter " + name + ".");
        }
        return m.getReturnType();
    }

    private Object getElementValue(ElementType elementType, Class<?> returnType, String defaultPackage) {
        removeEmptyContentElements(elementType);

        boolean isArray = returnType.isArray();
        if (!isArray) {
            if (elementType.getContent().size() != 1) {
                throw new ValidationException("Attempt to specify an array where single value is expected.");
            }
            return getSingleValue(elementType.getContent().get(0), returnType, defaultPackage);
        }
        List<Object> values = new ArrayList<Object>();
        for (Serializable s : elementType.getContent()) {
            values.add(getSingleValue(s, returnType.getComponentType(), defaultPackage));
        }
        return values.toArray((Object[]) Array.newInstance(returnType.getComponentType(), values.size()));
    }

    private void removeEmptyContentElements(ElementType elementType) {
        List<Serializable> contentToDelete = new ArrayList<Serializable>();
        for (Serializable content : elementType.getContent()) {
            if (content instanceof String && ((String) content).matches("[\\n ].*")) {
                contentToDelete.add(content);
            }
        }
        elementType.getContent().removeAll(contentToDelete);
    }

    @SuppressWarnings("unchecked")
    private Object getSingleValue(Serializable serializable, Class<?> returnType, String defaultPackage) {
        if (serializable instanceof String) {
            String value = (String) serializable;
            return convertToResultType(returnType, value, defaultPackage);
        }
        if (serializable instanceof JAXBElement<?>) {
            JAXBElement<?> elem = (JAXBElement<?>) serializable;
            if (String.class.equals(elem.getDeclaredType())) {
                String value = (String) elem.getValue();
                return convertToResultType(returnType, value, defaultPackage);
            }
            if (AnnotationType.class.equals(elem.getDeclaredType())) {
                AnnotationType annotationType = (AnnotationType) elem.getValue();
                try {
                    Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) returnType;
                    return createAnnotation(annotationType, annotationClass, defaultPackage);
                } catch (ClassCastException e) {
                    throw new ValidationException("Unexpected parameter value");
                }
            }
        }
        throw new ValidationException("Unexpected parameter value");
    }

    private Object convertToResultType(Class<?> returnType, String value, String defaultPackage) {
        /**
         * Class is represented by the fully qualified class name of the class.
         * spec: Note that if the raw string is unqualified,
         * default package is taken into account.
         */
        if (returnType.equals(String.class)) {
            return value;
        }
        if (returnType.equals(Class.class)) {
            ClassLoader cl = Reflection.getClassLoader(ValidationMappingParser.class);
            try {
                return Reflection.toClass(toQualifiedClassName(value, defaultPackage), cl);
            } catch (Exception e) {
                throw new ValidationException(e);
            }
        }
        if (returnType.isEnum()) {
            try {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                final Enum e = Enum.valueOf(returnType.asSubclass(Enum.class), value);
                return e;
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e);
            }
        }
        if (Byte.class.equals(returnType) || byte.class.equals(returnType)) { // spec mandates it
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
            if (value.length() > 1) {
                throw new IllegalArgumentException("a char has a length of 1");
            }
            return value.charAt(0);
        }
        throw new ValidationException(String.format("Unknown annotation value type %s", returnType.getName()));
    }

    private <A extends Annotation> Annotation createAnnotation(AnnotationType annotationType, Class<A> returnType,
        String defaultPackage) {
        AnnotationProxyBuilder<A> metaAnnotation = new AnnotationProxyBuilder<A>(returnType);
        for (ElementType elementType : annotationType.getElement()) {
            String name = elementType.getName();
            Class<?> parameterType = getAnnotationParameterType(returnType, name);
            Object elementValue = getElementValue(elementType, parameterType, defaultPackage);
            metaAnnotation.putValue(name, elementValue);
        }
        return metaAnnotation.createAnnotation();
    }

    private Class<?>[] getGroups(GroupsType groupsType, String defaultPackage) {
        if (groupsType == null) {
            return ObjectUtils.EMPTY_CLASS_ARRAY;
        }

        List<Class<?>> groupList = new ArrayList<Class<?>>();
        for (String groupClass : groupsType.getValue()) {
            groupList.add(loadClass(groupClass, defaultPackage));
        }
        return groupList.toArray(new Class[groupList.size()]);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Payload>[] getPayload(PayloadType payloadType, String defaultPackage) {
        if (payloadType == null) {
            return new Class[] {};
        }

        List<Class<? extends Payload>> payloadList = new ArrayList<Class<? extends Payload>>();
        for (String groupClass : payloadType.getValue()) {
            Class<?> payload = loadClass(groupClass, defaultPackage);
            if (!Payload.class.isAssignableFrom(payload)) {
                throw new ValidationException(
                    "Specified payload class " + payload.getName() + " does not implement javax.validation.Payload");
            }
            payloadList.add((Class<? extends Payload>) payload);
        }
        return payloadList.toArray(new Class[payloadList.size()]);
    }

    private Class<?>[] createGroupSequence(GroupSequenceType groupSequenceType, String defaultPackage) {
        if (groupSequenceType != null) {
            Class<?>[] groupSequence = new Class<?>[groupSequenceType.getValue().size()];
            int i = 0;
            for (String groupName : groupSequenceType.getValue()) {
                Class<?> group = loadClass(groupName, defaultPackage);
                groupSequence[i++] = group;
            }
            return groupSequence;
        }
        return null;
    }

    private <A> void processMethodLevel(final List<MethodType> methods, final Class<A> beanClass,
        final String defaultPackage, final boolean parentIgnoreAnn, final Collection<String> getters) {
        final List<String> methodNames = new ArrayList<String>();
        for (final MethodType methodType : methods) {
            final String methodName = methodType.getName();
            if (methodNames.contains(methodName) || getters.contains(methodName)) {
                throw new ValidationException(
                    methodName + " is defined more than once in mapping xml for bean " + beanClass.getName());
            }
            methodNames.add(methodName);

            final Method method =
                Reflection.getDeclaredMethod(beanClass, methodName, toTypes(methodType.getParameter(), defaultPackage));
            if (method == null) {
                throw new ValidationException(beanClass.getName() + " does not contain the method  " + methodName);
            }

            // ignore annotations
            final boolean ignoreMethodAnnotation =
                methodType.getIgnoreAnnotations() == null ? parentIgnoreAnn : methodType.getIgnoreAnnotations();
            factory.getAnnotationIgnores().setIgnoreAnnotationsOnMember(method, ignoreMethodAnnotation);

            final boolean ignoreAnn;
            if (methodType.getIgnoreAnnotations() == null) {
                ignoreAnn = parentIgnoreAnn;
            } else {
                ignoreAnn = methodType.getIgnoreAnnotations();
            }

            // constraints
            int i = 0;
            for (final ParameterType p : methodType.getParameter()) {
                for (final ConstraintType constraintType : p.getConstraint()) {
                    final MetaConstraint<?, ?> constraint =
                        createConstraint(constraintType, beanClass, method, defaultPackage);
                    constraint.setIndex(i);
                    factory.addMetaConstraint(beanClass, constraint);
                }
                if (p.getValid() != null) {
                    final MetaConstraint<?, ?> constraint = new MetaConstraint<A, Annotation>(beanClass, method,
                        AnnotationProxyBuilder.ValidAnnotation.INSTANCE);
                    constraint.setIndex(i);
                    factory.addMetaConstraint(beanClass, constraint);
                }

                if (p.getConvertGroup() != null) {
                    for (final GroupConversionType groupConversion : p.getConvertGroup()) {
                        final Class<?> from = loadClass(groupConversion.getFrom(), defaultPackage);
                        final Class<?> to = loadClass(groupConversion.getTo(), defaultPackage);
                        final MetaConstraint<?, ?> constraint = new MetaConstraint<A, Annotation>(beanClass, method,
                            new AnnotationProxyBuilder.ConvertGroupAnnotation(from, to));
                        constraint.setIndex(i);
                        factory.addMetaConstraint(beanClass, constraint);
                    }
                }

                boolean ignoreParametersAnnotation =
                    p.getIgnoreAnnotations() == null ? ignoreMethodAnnotation : p.getIgnoreAnnotations();
                factory.getAnnotationIgnores().setIgnoreAnnotationsOnParameter(method, i, ignoreParametersAnnotation);

                i++;
            }

            final ReturnValueType returnValue = methodType.getReturnValue();
            if (returnValue != null) {
                for (final ConstraintType constraintType : returnValue.getConstraint()) {
                    final MetaConstraint<?, ?> constraint =
                        createConstraint(constraintType, beanClass, method, defaultPackage);
                    factory.addMetaConstraint(beanClass, constraint);
                }
                if (returnValue.getValid() != null) {
                    final MetaConstraint<?, ?> constraint = new MetaConstraint<A, Annotation>(beanClass, method,
                        AnnotationProxyBuilder.ValidAnnotation.INSTANCE);
                    factory.addMetaConstraint(beanClass, constraint);
                }

                if (returnValue.getConvertGroup() != null) {
                    for (final GroupConversionType groupConversion : returnValue.getConvertGroup()) {
                        final Class<?> from = loadClass(groupConversion.getFrom(), defaultPackage);
                        final Class<?> to = loadClass(groupConversion.getTo(), defaultPackage);
                        final MetaConstraint<?, ?> constraint = new MetaConstraint<A, Annotation>(beanClass, method,
                            new AnnotationProxyBuilder.ConvertGroupAnnotation(from, to));
                        factory.addMetaConstraint(beanClass, constraint);
                    }
                }
                factory.getAnnotationIgnores().setIgnoreAnnotationOnReturn(method,
                    returnValue.getIgnoreAnnotations() == null ? ignoreAnn : returnValue.getIgnoreAnnotations());
            }

            final CrossParameterType crossParameter = methodType.getCrossParameter();
            if (crossParameter != null) {
                for (final ConstraintType constraintType : crossParameter.getConstraint()) {
                    final MetaConstraint<?, ?> constraint =
                        createConstraint(constraintType, beanClass, method, defaultPackage);
                    factory.addMetaConstraint(beanClass, constraint);
                }
                factory.getAnnotationIgnores().setIgnoreAnnotationOnCrossParameter(method,
                    crossParameter.getIgnoreAnnotations() != null ? crossParameter.getIgnoreAnnotations() : ignoreAnn);
            }
        }
    }

    private <A> void processConstructorLevel(final List<ConstructorType> constructors, final Class<A> beanClass,
        final String defaultPackage, final boolean parentIgnore) {
        for (final ConstructorType constructorType : constructors) {
            final Constructor<?> constructor =
                Reflection.getDeclaredConstructor(beanClass, toTypes(constructorType.getParameter(), defaultPackage));
            if (constructor == null) {
                throw new ValidationException(
                    beanClass.getName() + " does not contain the constructor  " + constructorType);
            }

            // ignore annotations
            final boolean ignoreMethodAnnotation =
                constructorType.getIgnoreAnnotations() == null ? parentIgnore : constructorType.getIgnoreAnnotations();
            factory.getAnnotationIgnores().setIgnoreAnnotationsOnMember(constructor, ignoreMethodAnnotation);

            final boolean ignoreAnn;
            if (constructorType.getIgnoreAnnotations() == null) {
                ignoreAnn = parentIgnore;
            } else {
                ignoreAnn = constructorType.getIgnoreAnnotations();
            }

            // constraints
            int i = 0;
            for (final ParameterType p : constructorType.getParameter()) {
                for (final ConstraintType constraintType : p.getConstraint()) {
                    final MetaConstraint<?, ?> constraint =
                        createConstraint(constraintType, beanClass, constructor, defaultPackage);
                    constraint.setIndex(i);
                    factory.addMetaConstraint(beanClass, constraint);
                }
                if (p.getValid() != null) {
                    final MetaConstraint<?, ?> constraint = new MetaConstraint<A, Annotation>(beanClass, constructor,
                        AnnotationProxyBuilder.ValidAnnotation.INSTANCE);
                    constraint.setIndex(i);
                    factory.addMetaConstraint(beanClass, constraint);
                }

                if (p.getConvertGroup() != null) {
                    for (final GroupConversionType groupConversion : p.getConvertGroup()) {
                        final Class<?> from = loadClass(groupConversion.getFrom(), defaultPackage);
                        final Class<?> to = loadClass(groupConversion.getTo(), defaultPackage);
                        final MetaConstraint<?, ?> constraint = new MetaConstraint<A, Annotation>(beanClass,
                            constructor, new AnnotationProxyBuilder.ConvertGroupAnnotation(from, to));
                        constraint.setIndex(i);
                        factory.addMetaConstraint(beanClass, constraint);
                    }
                }

                boolean ignoreParametersAnnotation =
                    p.getIgnoreAnnotations() == null ? ignoreMethodAnnotation : p.getIgnoreAnnotations();
                if (ignoreParametersAnnotation || (ignoreMethodAnnotation && p.getIgnoreAnnotations() == null)) {
                    // TODO what ?
                }
                factory.getAnnotationIgnores().setIgnoreAnnotationsOnParameter(constructor, i,
                    p.getIgnoreAnnotations() != null ? p.getIgnoreAnnotations() : ignoreAnn);

                i++;
            }

            final ReturnValueType returnValue = constructorType.getReturnValue();
            if (returnValue != null) {
                for (final ConstraintType constraintType : returnValue.getConstraint()) {
                    final MetaConstraint<?, ?> constraint =
                        createConstraint(constraintType, beanClass, constructor, defaultPackage);
                    constraint.setIndex(-1);
                    factory.addMetaConstraint(beanClass, constraint);
                }
                if (returnValue.getValid() != null) {
                    final MetaConstraint<?, ?> constraint = new MetaConstraint<A, Annotation>(beanClass, constructor,
                        AnnotationProxyBuilder.ValidAnnotation.INSTANCE);
                    constraint.setIndex(-1);
                    factory.addMetaConstraint(beanClass, constraint);
                }

                if (returnValue.getConvertGroup() != null) {
                    for (final GroupConversionType groupConversion : returnValue.getConvertGroup()) {
                        final Class<?> from = loadClass(groupConversion.getFrom(), defaultPackage);
                        final Class<?> to = loadClass(groupConversion.getTo(), defaultPackage);
                        final MetaConstraint<?, ?> constraint = new MetaConstraint<A, Annotation>(beanClass,
                            constructor, new AnnotationProxyBuilder.ConvertGroupAnnotation(from, to));
                        constraint.setIndex(-1);
                        factory.addMetaConstraint(beanClass, constraint);
                    }
                }
                factory.getAnnotationIgnores().setIgnoreAnnotationOnReturn(constructor,
                    returnValue.getIgnoreAnnotations() != null ? returnValue.getIgnoreAnnotations() : ignoreAnn);
            }

            final CrossParameterType crossParameter = constructorType.getCrossParameter();
            if (crossParameter != null) {
                for (final ConstraintType constraintType : crossParameter.getConstraint()) {
                    final MetaConstraint<?, ?> constraint =
                        createConstraint(constraintType, beanClass, constructor, defaultPackage);
                    factory.addMetaConstraint(beanClass, constraint);
                }
                factory.getAnnotationIgnores().setIgnoreAnnotationOnCrossParameter(constructor,
                    crossParameter.getIgnoreAnnotations() != null ? crossParameter.getIgnoreAnnotations() : ignoreAnn);
            }
        }
    }

    private Class<?>[] toTypes(final List<ParameterType> parameter, final String defaultPck) {
        if (parameter == null) {
            return null;
        }
        final Class<?>[] types = new Class<?>[parameter.size()];
        int i = 0;
        for (final ParameterType type : parameter) {
            types[i++] = loadClass(type.getType(), defaultPck);
        }
        return types;
    }

    private <A> void processFieldLevel(List<FieldType> fields, Class<A> beanClass, String defaultPackage,
        boolean ignoreAnnotations) {
        final List<String> fieldNames = new ArrayList<String>();
        for (FieldType fieldType : fields) {
            String fieldName = fieldType.getName();
            if (fieldNames.contains(fieldName)) {
                throw new ValidationException(
                    fieldName + " is defined more than once in mapping xml for bean " + beanClass.getName());
            }
            fieldNames.add(fieldName);

            final Field field = Reflection.getDeclaredField(beanClass, fieldName);
            if (field == null) {
                throw new ValidationException(beanClass.getName() + " does not contain the fieldType  " + fieldName);
            }

            // ignore annotations
            final boolean ignoreFieldAnnotation =
                fieldType.getIgnoreAnnotations() == null ? ignoreAnnotations : fieldType.getIgnoreAnnotations();
            factory.getAnnotationIgnores().setIgnoreAnnotationsOnMember(field, ignoreFieldAnnotation);

            // valid
            if (fieldType.getValid() != null) {
                factory.addValid(beanClass, new FieldAccess(field));
            }

            for (final GroupConversionType conversion : fieldType.getConvertGroup()) {
                final Class<?> from = loadClass(conversion.getFrom(), defaultPackage);
                final Class<?> to = loadClass(conversion.getTo(), defaultPackage);
                final MetaConstraint<?, ?> constraint = new MetaConstraint<A, Annotation>(beanClass, field,
                    new AnnotationProxyBuilder.ConvertGroupAnnotation(from, to));
                factory.addMetaConstraint(beanClass, constraint);
            }

            // constraints
            for (ConstraintType constraintType : fieldType.getConstraint()) {
                MetaConstraint<?, ?> constraint = createConstraint(constraintType, beanClass, field, defaultPackage);
                factory.addMetaConstraint(beanClass, constraint);
            }
        }
    }

    private <A> Collection<String> processPropertyLevel(List<GetterType> getters, Class<A> beanClass,
        String defaultPackage, boolean ignoreAnnotatino) {
        List<String> getterNames = new ArrayList<String>();
        for (GetterType getterType : getters) {
            final String getterName = getterType.getName();
            final String methodName = "get" + StringUtils.capitalize(getterType.getName());
            if (getterNames.contains(methodName)) {
                throw new ValidationException(
                    getterName + " is defined more than once in mapping xml for bean " + beanClass.getName());
            }
            getterNames.add(methodName);

            final Method method = getGetter(beanClass, getterName);
            if (method == null) {
                throw new ValidationException(beanClass.getName() + " does not contain the property  " + getterName);
            }

            // ignore annotations
            final boolean ignoreGetterAnnotation =
                getterType.getIgnoreAnnotations() == null ? ignoreAnnotatino : getterType.getIgnoreAnnotations();
            factory.getAnnotationIgnores().setIgnoreAnnotationsOnMember(method, ignoreGetterAnnotation);

            // valid
            if (getterType.getValid() != null) {
                factory.addValid(beanClass, new MethodAccess(getterName, method));
            }

            // ConvertGroup
            for (final GroupConversionType conversion : getterType.getConvertGroup()) {
                final Class<?> from = loadClass(conversion.getFrom(), defaultPackage);
                final Class<?> to = loadClass(conversion.getTo(), defaultPackage);
                final MetaConstraint<?, ?> constraint = new MetaConstraint<A, Annotation>(beanClass, method,
                    new AnnotationProxyBuilder.ConvertGroupAnnotation(from, to));
                factory.addMetaConstraint(beanClass, constraint);
            }

            // constraints
            for (ConstraintType constraintType : getterType.getConstraint()) {
                MetaConstraint<?, ?> metaConstraint =
                    createConstraint(constraintType, beanClass, method, defaultPackage);
                factory.addMetaConstraint(beanClass, metaConstraint);
            }
        }

        return getterNames;
    }

    @SuppressWarnings("unchecked")
    private void processConstraintDefinitions(List<ConstraintDefinitionType> constraintDefinitionList,
        String defaultPackage) {
        for (ConstraintDefinitionType constraintDefinition : constraintDefinitionList) {
            String annotationClassName = constraintDefinition.getAnnotation();

            Class<?> clazz = loadClass(annotationClassName, defaultPackage);
            if (!clazz.isAnnotation()) {
                throw new ValidationException(annotationClassName + " is not an annotation");
            }
            Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) clazz;

            ValidatedByType validatedByType = constraintDefinition.getValidatedBy();
            List<Class<? extends ConstraintValidator<?, ?>>> classes =
                new ArrayList<Class<? extends ConstraintValidator<?, ?>>>();
            /*
             If include-existing-validator is set to false,
             ConstraintValidator defined on the constraint annotation are ignored.
              */
            if (validatedByType.getIncludeExistingValidators() != null
                && validatedByType.getIncludeExistingValidators()) {
                /*
                 If set to true, the list of ConstraintValidators described in XML
                 are concatenated to the list of ConstraintValidator described on the
                 annotation to form a new array of ConstraintValidator evaluated.
                 */
                classes.addAll(findConstraintValidatorClasses(annotationClass));
            }
            for (String validatorClassName : validatedByType.getValue()) {
                Class<? extends ConstraintValidator<?, ?>> validatorClass;
                validatorClass = (Class<? extends ConstraintValidator<?, ?>>) loadClass(validatorClassName);

                if (!ConstraintValidator.class.isAssignableFrom(validatorClass)) {
                    throw new ValidationException(validatorClass + " is not a constraint validator class");
                }

                /*
                Annotation based ConstraintValidator come before XML based
                ConstraintValidator in the array. The new list is returned
                by ConstraintDescriptor.getConstraintValidatorClasses().
                 */
                if (!classes.contains(validatorClass))
                    classes.add(validatorClass);
            }
            if (factory.getConstraintsCache().containsConstraintValidator(annotationClass)) {
                throw new ValidationException(
                    "Constraint validator for " + annotationClass.getName() + " already configured.");
            } else {
                factory.getConstraintsCache().putConstraintValidator(annotationClass,
                    classes.toArray(new Class[classes.size()]));
            }
        }
    }

    private List<Class<? extends ConstraintValidator<? extends Annotation, ?>>> findConstraintValidatorClasses(
        Class<? extends Annotation> annotationType) {
        List<Class<? extends ConstraintValidator<? extends Annotation, ?>>> classes =
            new ArrayList<Class<? extends ConstraintValidator<? extends Annotation, ?>>>();

        Class<? extends ConstraintValidator<?, ?>>[] validator =
            factory.getDefaultConstraints().getValidatorClasses(annotationType);
        if (validator == null) {
            /* Collections.addAll() would be more straightforward here, but there is an Oracle compiler bug of some sort
             * that precludes this:
             */
            Class<? extends ConstraintValidator<?, ?>>[] validatedBy =
                annotationType.getAnnotation(Constraint.class).validatedBy();
            classes.addAll(Arrays.asList(validatedBy));
        } else {
            Collections.addAll(classes, validator);
        }
        return classes;
    }

    private Class<?> loadClass(String className, String defaultPackage) {
        return loadClass(toQualifiedClassName(className, defaultPackage));
    }

    private String toQualifiedClassName(String className, String defaultPackage) {
        if (!isQualifiedClass(className)) {
            if (className.startsWith("[L") && className.endsWith(";")) {
                className = "[L" + defaultPackage + "." + className.substring(2);
            } else {
                className = defaultPackage + "." + className;
            }
        }
        return className;
    }

    private boolean isQualifiedClass(String clazz) {
        return clazz.contains(".");
    }

    @Privileged
    private static Method getGetter(Class<?> clazz, String propertyName) {
        try {
            final String p = StringUtils.capitalize(propertyName);
            try {
                return clazz.getMethod("get" + p);
            } catch (NoSuchMethodException e) {
                return clazz.getMethod("is" + p);
            }
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private Class<?> loadClass(final String className) {
        ClassLoader loader = Reflection.getClassLoader(ValidationMappingParser.class);
        if (loader == null)
            loader = getClass().getClassLoader();

        try {
            return Class.forName(className, true, loader);
        } catch (ClassNotFoundException ex) {
            throw new ValidationException("Unable to load class: " + className, ex);
        }
    }

}
