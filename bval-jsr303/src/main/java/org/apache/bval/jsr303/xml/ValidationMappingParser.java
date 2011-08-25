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
package org.apache.bval.jsr303.xml;


import org.apache.bval.jsr303.ApacheValidatorFactory;
import org.apache.bval.jsr303.Jsr303MetaBeanFactory;
import org.apache.bval.jsr303.util.EnumerationConverter;
import org.apache.bval.jsr303.util.IOUtils;
import org.apache.bval.jsr303.util.SecureActions;
import org.apache.bval.util.FieldAccess;
import org.apache.bval.util.MethodAccess;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang.StringUtils;

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
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;


/**
 * Uses JAXB to parse constraints.xml based on validation-mapping-1.0.xsd.<br>
 */
@SuppressWarnings("restriction")
public class ValidationMappingParser {
    //    private static final Log log = LogFactory.getLog(ValidationMappingParser.class);
    private static final String VALIDATION_MAPPING_XSD = "META-INF/validation-mapping-1.0.xsd";
    private static final String[] RESERVED_PARAMS = {
            Jsr303MetaBeanFactory.ANNOTATION_MESSAGE,
            Jsr303MetaBeanFactory.ANNOTATION_GROUPS,
            Jsr303MetaBeanFactory.ANNOTATION_PAYLOAD };

    private final Set<Class<?>> processedClasses;
    private final ApacheValidatorFactory factory;

    /**
     * Create a new ValidationMappingParser instance.
     * @param factory
     */
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
        for (InputStream xmlStream : xmlStreams) {
            ConstraintMappingsType mapping = parseXmlMappings(xmlStream);

            String defaultPackage = mapping.getDefaultPackage();
            processConstraintDefinitions(mapping.getConstraintDefinition(), defaultPackage);
            for (BeanType bean : mapping.getBean()) {
                Class<?> beanClass = loadClass(bean.getClazz(), defaultPackage);
                if (!processedClasses.add(beanClass)) {
                    // spec: A given class must not be described more than once amongst all
                    //  the XML mapping descriptors.
                    throw new ValidationException(
                          beanClass.getName() + " has already be configured in xml.");
                }
                factory.getAnnotationIgnores()
                      .setDefaultIgnoreAnnotation(beanClass, bean.isIgnoreAnnotations());
                processClassLevel(bean.getClassType(), beanClass, defaultPackage);
                processFieldLevel(bean.getField(), beanClass, defaultPackage);
                processPropertyLevel(bean.getGetter(), beanClass, defaultPackage);
                processedClasses.add(beanClass);
            }
        }
    }

    /** @param in XML stream to parse using the validation-mapping-1.0.xsd */
    private ConstraintMappingsType parseXmlMappings(InputStream in) {
        ConstraintMappingsType mappings;
        try {
            JAXBContext jc = JAXBContext.newInstance(ConstraintMappingsType.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshaller.setSchema(getSchema());
            StreamSource stream = new StreamSource(in);
            JAXBElement<ConstraintMappingsType> root =
                  unmarshaller.unmarshal(stream, ConstraintMappingsType.class);
            mappings = root.getValue();
        } catch (JAXBException e) {
            throw new ValidationException("Failed to parse XML deployment descriptor file.",
                  e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return mappings;
    }

    /** @return validation-mapping-1.0.xsd based schema */
    private Schema getSchema() {
        return ValidationParser.getSchema(VALIDATION_MAPPING_XSD);
    }

    private void processClassLevel(ClassType classType, Class<?> beanClass,
                                   String defaultPackage) {
        if (classType == null) {
            return;
        }

        // ignore annotation
        if (classType.isIgnoreAnnotations() != null) {
            factory.getAnnotationIgnores()
                  .setIgnoreAnnotationsOnClass(beanClass, classType.isIgnoreAnnotations());
        }

        // group sequence
        Class<?>[] groupSequence =
              createGroupSequence(classType.getGroupSequence(), defaultPackage);
        if (groupSequence != null) {
            factory.addDefaultSequence(beanClass, groupSequence);
        }

        // constraints
        for (ConstraintType constraint : classType.getConstraint()) {
            MetaConstraint<?, ?> metaConstraint =
                  createConstraint(constraint, beanClass, null, defaultPackage);
            factory.addMetaConstraint(beanClass, metaConstraint);
        }
    }

    @SuppressWarnings("unchecked")
    private <A extends Annotation, T> MetaConstraint<?, ?> createConstraint(
          ConstraintType constraint, Class<T> beanClass, Member member,
          String defaultPackage) {
        Class<A> annotationClass =
              (Class<A>) loadClass(constraint.getAnnotation(), defaultPackage);
        AnnotationProxyBuilder<A> annoBuilder = new AnnotationProxyBuilder<A>(annotationClass);

        if (constraint.getMessage() != null) {
            annoBuilder.setMessage(constraint.getMessage());
        }
        annoBuilder.setGroups(getGroups(constraint.getGroups(), defaultPackage));
        annoBuilder.setPayload(getPayload(constraint.getPayload(), defaultPackage));

        for (ElementType elementType : constraint.getElement()) {
            String name = elementType.getName();
            checkValidName(name);
            Class<?> returnType = getAnnotationParameterType(annotationClass, name);
            Object elementValue = getElementValue(elementType, returnType, defaultPackage);
            annoBuilder.putValue(name, elementValue);
        }
        return new MetaConstraint<T, A>(beanClass, member, annoBuilder.createAnnotation());
    }

    private void checkValidName(String name) {
        for (String each : RESERVED_PARAMS) {
            if (each.equals(name)) {
                throw new ValidationException(each + " is a reserved parameter name.");
            }
        }
    }

    private <A extends Annotation> Class<?> getAnnotationParameterType(
          final Class<A> annotationClass, final String name) {
        final Method m = doPrivileged(SecureActions.getPublicMethod(annotationClass, name));
        if (m == null) {
            throw new ValidationException("Annotation of type " + annotationClass.getName() +
                  " does not contain a parameter " + name + ".");
        }
        return m.getReturnType();
    }

    private Object getElementValue(ElementType elementType, Class<?> returnType,
                                   String defaultPackage) {
        removeEmptyContentElements(elementType);

        boolean isArray = returnType.isArray();
        if (!isArray) {
            if (elementType.getContent().size() != 1) {
                throw new ValidationException(
                      "Attempt to specify an array where single value is expected.");
            }
            return getSingleValue(elementType.getContent().get(0), returnType, defaultPackage);
        } else {
            List<Object> values = new ArrayList<Object>();
            for (Serializable s : elementType.getContent()) {
                values.add(getSingleValue(s, returnType.getComponentType(), defaultPackage));
            }
            return values.toArray(
                  (Object[]) Array.newInstance(returnType.getComponentType(), values.size()));
        }
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
    private Object getSingleValue(Serializable serializable, Class<?> returnType,
                                  String defaultPackage) {

        Object returnValue;
        if (serializable instanceof String) {
            String value = (String) serializable;
            returnValue = convertToResultType(returnType, value, defaultPackage);
        } else if (serializable instanceof JAXBElement<?> &&
              ((JAXBElement<?>) serializable).getDeclaredType()
                    .equals(String.class)) {
            JAXBElement<?> elem = (JAXBElement<?>) serializable;
            String value = (String) elem.getValue();
            returnValue = convertToResultType(returnType, value, defaultPackage);
        } else if (serializable instanceof JAXBElement<?> &&
              ((JAXBElement<?>) serializable).getDeclaredType()
                    .equals(AnnotationType.class)) {
            JAXBElement<?> elem = (JAXBElement<?>) serializable;
            AnnotationType annotationType = (AnnotationType) elem.getValue();
            try {
                Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) returnType;
                returnValue =
                      createAnnotation(annotationType, annotationClass, defaultPackage);
            } catch (ClassCastException e) {
                throw new ValidationException("Unexpected parameter value");
            }
        } else {
            throw new ValidationException("Unexpected parameter value");
        }
        return returnValue;

    }

    private Object convertToResultType(Class<?> returnType, String value,
                                       String defaultPackage) {
        /**
         * Class is represented by the fully qualified class name of the class.
         * spec: Note that if the raw string is unqualified,
         * default package is taken into account.
         */
        if (returnType.equals(Class.class)) {
            value = toQualifiedClassName(value, defaultPackage);
        }

        /* Converter lookup */
        Converter converter = ConvertUtils.lookup(returnType);
        if (converter == null && returnType.isEnum()) {
            converter = EnumerationConverter.getInstance();
        }

        if (converter != null) {
            return converter.convert(returnType, value);
        } else {
            return converter;
        }
    }

    private <A extends Annotation> Annotation createAnnotation(AnnotationType annotationType,
                                                               Class<A> returnType,
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
            return new Class[]{};
        }

        List<Class<?>> groupList = new ArrayList<Class<?>>();
        for (JAXBElement<String> groupClass : groupsType.getValue()) {
            groupList.add(loadClass(groupClass.getValue(), defaultPackage));
        }
        return groupList.toArray(new Class[groupList.size()]);
    }


    @SuppressWarnings("unchecked")
    private Class<? extends Payload>[] getPayload(PayloadType payloadType,
                                                  String defaultPackage) {
        if (payloadType == null) {
            return new Class[]{};
        }

        List<Class<? extends Payload>> payloadList = new ArrayList<Class<? extends Payload>>();
        for (JAXBElement<String> groupClass : payloadType.getValue()) {
            Class<?> payload = loadClass(groupClass.getValue(), defaultPackage);
            if (!Payload.class.isAssignableFrom(payload)) {
                throw new ValidationException("Specified payload class " + payload.getName() +
                      " does not implement javax.validation.Payload");
            } else {
                payloadList.add((Class<? extends Payload>) payload);
            }
        }
        return payloadList.toArray(new Class[payloadList.size()]);
    }

    private Class<?>[] createGroupSequence(GroupSequenceType groupSequenceType,
                                               String defaultPackage) {
        if (groupSequenceType != null) {
            Class<?>[] groupSequence = new Class<?>[groupSequenceType.getValue().size()];
            int i=0;
            for (JAXBElement<String> groupName : groupSequenceType.getValue()) {
                Class<?> group = loadClass(groupName.getValue(), defaultPackage);
                groupSequence[i++] = group;
            }
            return groupSequence;
        } else {
            return null;
        }
    }

    private void processFieldLevel(List<FieldType> fields, Class<?> beanClass,
                                   String defaultPackage) {
        List<String> fieldNames = new ArrayList<String>();
        for (FieldType fieldType : fields) {
            String fieldName = fieldType.getName();
            if (fieldNames.contains(fieldName)) {
                throw new ValidationException(fieldName +
                      " is defined more than once in mapping xml for bean " +
                      beanClass.getName());
            } else {
                fieldNames.add(fieldName);
            }
            final Field field = doPrivileged(SecureActions.getDeclaredField(beanClass, fieldName));
            if (field == null) {
                throw new ValidationException(
                      beanClass.getName() + " does not contain the fieldType  " + fieldName);
            }

            // ignore annotations
            boolean ignoreFieldAnnotation = fieldType.isIgnoreAnnotations() == null ? false :
                  fieldType.isIgnoreAnnotations();
            if (ignoreFieldAnnotation) {
                factory.getAnnotationIgnores().setIgnoreAnnotationsOnMember(field);
            }

            // valid
            if (fieldType.getValid() != null) {
                factory.addValid(beanClass, new FieldAccess(field));
            }

            // constraints
            for (ConstraintType constraintType : fieldType.getConstraint()) {
                MetaConstraint<?, ?> constraint =
                      createConstraint(constraintType, beanClass, field, defaultPackage);
                factory.addMetaConstraint(beanClass, constraint);
            }
        }
    }

    private void processPropertyLevel(List<GetterType> getters, Class<?> beanClass,
                                      String defaultPackage) {
        List<String> getterNames = new ArrayList<String>();
        for (GetterType getterType : getters) {
            String getterName = getterType.getName();
            if (getterNames.contains(getterName)) {
                throw new ValidationException(getterName +
                      " is defined more than once in mapping xml for bean " +
                      beanClass.getName());
            } else {
                getterNames.add(getterName);
            }
            final Method method = getGetter(beanClass, getterName);
            if (method == null) {
                throw new ValidationException(
                      beanClass.getName() + " does not contain the property  " + getterName);
            }

            // ignore annotations
            boolean ignoreGetterAnnotation = getterType.isIgnoreAnnotations() == null ? false :
                  getterType.isIgnoreAnnotations();
            if (ignoreGetterAnnotation) {
                factory.getAnnotationIgnores().setIgnoreAnnotationsOnMember(method);
            }

            // valid
            if (getterType.getValid() != null) {
                factory.addValid(beanClass, new MethodAccess(getterName, method));
            }

            // constraints
            for (ConstraintType constraintType : getterType.getConstraint()) {
                MetaConstraint<?, ?> metaConstraint =
                      createConstraint(constraintType, beanClass, method, defaultPackage);
                factory.addMetaConstraint(beanClass, metaConstraint);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processConstraintDefinitions(
          List<ConstraintDefinitionType> constraintDefinitionList, String defaultPackage) {
        for (ConstraintDefinitionType constraintDefinition : constraintDefinitionList) {
            String annotationClassName = constraintDefinition.getAnnotation();

            Class<?> clazz = loadClass(annotationClassName, defaultPackage);
            if (!clazz.isAnnotation()) {
                throw new ValidationException(annotationClassName + " is not an annotation");
            }
            Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) clazz;

            ValidatedByType validatedByType = constraintDefinition.getValidatedBy();
            List<Class<? extends ConstraintValidator<?,?>>> classes = new ArrayList();
            /*
             If include-existing-validator is set to false,
             ConstraintValidator defined on the constraint annotation are ignored.
              */
            if (validatedByType.isIncludeExistingValidators() != null &&
                  validatedByType.isIncludeExistingValidators()) {
                /*
                 If set to true, the list of ConstraintValidators described in XML
                 are concatenated to the list of ConstraintValidator described on the
                 annotation to form a new array of ConstraintValidator evaluated.
                 */
                classes.addAll(findConstraintValidatorClasses(annotationClass));
            }
            for (JAXBElement<String> validatorClassName : validatedByType.getValue()) {
                Class<? extends ConstraintValidator<?, ?>> validatorClass;
                validatorClass = (Class<? extends ConstraintValidator<?, ?>>)
                      loadClass(validatorClassName.getValue());


                if (!ConstraintValidator.class.isAssignableFrom(validatorClass)) {
                    throw new ValidationException(
                          validatorClass + " is not a constraint validator class");
                }

                /*
                Annotation based ConstraintValidator come before XML based
                ConstraintValidator in the array. The new list is returned
                by ConstraintDescriptor.getConstraintValidatorClasses().
                 */
                if (!classes.contains(validatorClass)) classes.add(validatorClass);
            }
            if (factory.getConstraintsCache().containsConstraintValidator(annotationClass)) {
                throw new ValidationException("Constraint validator for " +
                      annotationClass.getName() + " already configured.");
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
        if (validator != null) {
            classes
                  .addAll(Arrays.asList(validator));
        } else {
            Class<? extends ConstraintValidator<?, ?>>[] validatedBy = annotationType
                  .getAnnotation(Constraint.class)
                  .validatedBy();
            classes.addAll(Arrays.asList(validatedBy));
        }
        return classes;
    }

    private Class<?> loadClass(String className, String defaultPackage) {
        return loadClass(toQualifiedClassName(className, defaultPackage));
    }

    private String toQualifiedClassName(String className, String defaultPackage) {
        if (!isQualifiedClass(className)) {
            className = defaultPackage + "." + className;
        }
        return className;
    }

    private boolean isQualifiedClass(String clazz) {
        return clazz.contains(".");
    }



    private static <T> T doPrivileged(final PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
    }



    private static Method getGetter(final Class<?> clazz, final String propertyName) {
        return doPrivileged(new PrivilegedAction<Method>() {
            public Method run() {
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
        });

    }



    private Class<?> loadClass(final String className) {
        ClassLoader loader = doPrivileged(SecureActions.getContextClassLoader());
        if (loader == null)
            loader = getClass().getClassLoader();

        try {
            return Class.forName(className, true, loader);
        } catch (ClassNotFoundException ex) {
            throw new ValidationException("Unable to load class: " + className, ex);
        }
    }

}
