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

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintDeclarationException;
import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintTarget;
import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.ValidationException;
import javax.validation.executable.ExecutableValidator;
import javax.validation.groups.Default;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.ParameterDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.apache.bval.DynamicMetaBean;
import org.apache.bval.MetaBeanFinder;
import org.apache.bval.jsr.groups.Group;
import org.apache.bval.jsr.groups.Groups;
import org.apache.bval.jsr.groups.GroupsComputer;
import org.apache.bval.jsr.util.ClassHelper;
import org.apache.bval.jsr.util.NodeImpl;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.jsr.util.PathNavigation;
import org.apache.bval.jsr.util.Proxies;
import org.apache.bval.jsr.util.ValidationContextTraversal;
import org.apache.bval.model.Features;
import org.apache.bval.model.FeaturesCapable;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.Validation;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.ValidationHelper;
import org.apache.bval.util.ObjectUtils;
import org.apache.bval.util.reflection.Reflection;
import org.apache.bval.util.reflection.TypeUtils;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Objects of this class are able to validate bean instances (and the associated object graphs).
 * <p/>
 * Implementation is thread-safe.
 * <p/>
 * API class
 *
 * @version $Rev: 1514672 $ $Date: 2013-08-16 14:15:12 +0200 (ven., 16 août 2013) $
 * 
 * @author Roman Stumm
 * @author Carlos Vara
 */
@Privilizing(@CallTo(Reflection.class))
public class ClassValidator implements CascadingPropertyValidator, ExecutableValidator {
    private static final Object VALIDATE_PROPERTY = new Object() {
        @Override
        public String toString() {
            return "VALIDATE_PROPERTY";
        }
    };

    /**
     * {@link ApacheFactoryContext} used
     */
    protected final ApacheFactoryContext factoryContext;

    /**
     * {@link GroupsComputer} used
     */
    protected final GroupsComputer groupsComputer = new GroupsComputer();

    private final MetaBeanFinder metaBeanFinder;

    /**
     * Create a new ClassValidator instance.
     *
     * @param factoryContext
     */
    public ClassValidator(ApacheFactoryContext factoryContext) {
        this.factoryContext = factoryContext;
        metaBeanFinder = factoryContext.getMetaBeanFinder();
    }

    // Validator implementation
    // --------------------------------------------------

    /**
     * {@inheritDoc} Validates all constraints on <code>object</code>.
     *
     * @param object object to validate
     * @param groups group or list of groups targeted for validation (default to
     *               {@link javax.validation.groups.Default})
     * @return constraint violations or an empty Set if none
     * @throws IllegalArgumentException if object is null or if null is passed to the varargs groups
     * @throws ValidationException      if a non recoverable error happens during the validation
     *                                  process
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        notNull("validated object", object);
        checkGroups(groups);

        try {
            final Class<T> objectClass = (Class<T>) object.getClass();
            final MetaBean objectMetaBean = metaBeanFinder.findForClass(objectClass);
            final GroupValidationContext<T> context = createContext(objectMetaBean, object, objectClass, groups);
            return validateBeanWithGroups(context, context.getGroups());
        } catch (final RuntimeException ex) {
            throw unrecoverableValidationError(ex, object);
        }
    }

    private <T> Set<ConstraintViolation<T>> validateBeanWithGroups(final GroupValidationContext<T> context,
        final Groups sequence) {
        final ConstraintValidationListener<T> result = context.getListener();

        // 1. process groups
        for (final Group current : sequence.getGroups()) {
            context.setCurrentGroup(current);
            validateBeanNet(context);
        }

        // 2. process sequences
        for (final List<Group> eachSeq : sequence.getSequences()) {
            for (final Group current : eachSeq) {
                context.setCurrentGroup(current);
                validateBeanNet(context);
                // if one of the group process in the sequence leads to one
                // or more validation failure,
                // the groups following in the sequence must not be
                // processed
                if (!result.isEmpty()) {
                    break;
                }
            }
            if (!result.isEmpty()) {
                break;
            }
        }
        return result.getConstraintViolations();
    }

    /**
     * {@inheritDoc} Validates all constraints placed on the property of <code>object</code> named
     * <code>propertyName</code>.
     *
     * @param object       object to validate
     * @param propertyName property to validate (ie field and getter constraints). Nested
     *                     properties may be referenced (e.g. prop[2].subpropA.subpropB)
     * @param groups       group or list of groups targeted for validation (default to
     *                     {@link javax.validation.groups.Default})
     * @return constraint violations or an empty Set if none
     * @throws IllegalArgumentException if <code>object</code> is null, if <code>propertyName</code>
     *                                  null, empty or not a valid object property or if null is
     *                                  passed to the varargs groups
     * @throws ValidationException      if a non recoverable error happens during the validation
     *                                  process
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
        return validateProperty(object, propertyName, false, groups);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, boolean cascade,
        Class<?>... groups) {
        notNull("validated object", object);

        @SuppressWarnings("unchecked")
        final Set<ConstraintViolation<T>> result =
            validateValueImpl((Class<T>) object.getClass(), object, propertyName, VALIDATE_PROPERTY, cascade, groups);
        return result;
    }

    /**
     * {@inheritDoc} Validates all constraints placed on the property named <code>propertyName</code> of the class
     * <code>beanType</code> would the property value be <code>value</code>
     * <p/>
     * <code>ConstraintViolation</code> objects return null for {@link ConstraintViolation#getRootBean()} and
     * {@link ConstraintViolation#getLeafBean()}
     *
     * @param beanType     the bean type
     * @param propertyName property to validate
     * @param value        property value to validate
     * @param groups       group or list of groups targeted for validation (default to
     *                     {@link javax.validation.groups.Default})
     * @return constraint violations or an empty Set if none
     * @throws IllegalArgumentException if <code>beanType</code> is null, if
     *                                  <code>propertyName</code> null, empty or not a valid object
     *                                  property or if null is passed to the varargs groups
     * @throws ValidationException      if a non recoverable error happens during the validation
     *                                  process
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value,
        Class<?>... groups) {
        return validateValue(beanType, propertyName, value, false, groups);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value,
        boolean cascade, Class<?>... groups) {
        return validateValueImpl(notNull("bean type", beanType), null, propertyName, value, cascade, groups);
    }

    /**
     * {@inheritDoc} Return the descriptor object describing bean constraints. The returned object (and associated
     * objects including <code>ConstraintDescriptor<code>s) are immutable.
     *
     * @param clazz class or interface type evaluated
     * @return the bean descriptor for the specified class.
     * @throws IllegalArgumentException if clazz is null
     * @throws ValidationException      if a non recoverable error happens during the metadata
     *                                  discovery or if some constraints are invalid.
     */
    @Override
    public BeanDescriptor getConstraintsForClass(final Class<?> clazz) {
        notNull("class", clazz);
        try {
            final MetaBean metaBean = metaBeanFinder.findForClass(clazz); // don't throw an exception because of a missing validator here
            BeanDescriptorImpl edesc = metaBean.getFeature(JsrFeatures.Bean.BEAN_DESCRIPTOR);
            if (edesc == null) {
                edesc = metaBean.initFeature(JsrFeatures.Bean.BEAN_DESCRIPTOR, createBeanDescriptor(metaBean));
            }
            return edesc;
        } catch (final ConstraintDefinitionException definitionEx) {
            throw definitionEx;
        } catch (final ConstraintDeclarationException declarationEx) {
            throw declarationEx;
        } catch (final RuntimeException ex) {
            throw new ValidationException("error retrieving constraints for " + clazz, ex);
        }
    }

    /**
     * {@inheritDoc} Return an instance of the specified type allowing access to provider-specific APIs. If the Bean
     * Validation provider implementation does not support the specified class, <code>ValidationException</code> is
     * thrown.
     *
     * @param type the class of the object to be returned.
     * @return an instance of the specified class
     * @throws ValidationException if the provider does not support the call.
     */
    @Override
    public <T> T unwrap(Class<T> type) {
        // FIXME 2011-03-27 jw:
        // This code is unsecure.
        // It should allow only a fixed set of classes.
        // Can't fix this because don't know which classes this method should support.

        if (type.isAssignableFrom(getClass())) {
            @SuppressWarnings("unchecked")
            final T result = (T) this;
            return result;
        }
        if (!(type.isInterface() || Modifier.isAbstract(type.getModifiers()))) {
            return newInstance(type);
        }
        try {
            final Class<?> cls = Reflection.toClass(type.getName() + "Impl");
            if (type.isAssignableFrom(cls)) {
                @SuppressWarnings("unchecked")
                final Class<? extends T> implClass = (Class<? extends T>) cls;
                return newInstance(implClass);
            }
        } catch (ClassNotFoundException e) {
        }
        throw new ValidationException("Type " + type + " not supported");
    }

    @Override
    public ExecutableValidator forExecutables() {
        return this;
    }

    private <T> T newInstance(final Class<T> cls) {
        final Constructor<T> cons = Reflection.getDeclaredConstructor(cls, ApacheFactoryContext.class);
        if (cons == null) {
            throw new ValidationException("Cannot instantiate " + cls);
        }
        final boolean mustUnset = Reflection.setAccessible(cons, true);
        try {
            return cons.newInstance(factoryContext);
        } catch (final Exception ex) {
            throw new ValidationException("Cannot instantiate " + cls, ex);
        } finally {
            if (mustUnset) {
                Reflection.setAccessible(cons, false);
            }
        }
    }

    // Helpers
    // -------------------------------------------------------------------

    /**
     * Validates a bean and all its cascaded related beans for the currently defined group.
     * <p/>
     * Special code is present to manage the {@link Default} group.
     *
     * @param context The current context of this validation call. Must have its
     *                          {@link GroupValidationContext#getCurrentGroup()} field set.
     */
    protected void validateBeanNet(GroupValidationContext<?> context) {

        // If reached a cascaded bean which is null
        if (context.getBean() == null) {
            return;
        }

        // If reached a cascaded bean which has already been validated for the
        // current group
        if (!context.collectValidated()) {
            return;
        }

        // ### First, validate the bean

        // Default is a special case
        if (context.getCurrentGroup().isDefault()) {

            List<Group> defaultGroups = expandDefaultGroup(context);
            final ConstraintValidationListener<?> result = context.getListener();

            // If the rootBean defines a GroupSequence
            if (defaultGroups != null && defaultGroups.size() > 1) {

                int numViolations = result.violationsSize();

                // Validate the bean for each group in the sequence
                final Group currentGroup = context.getCurrentGroup();
                for (final Group each : defaultGroups) {
                    context.setCurrentGroup(each);

                    // ValidationHelper.validateBean(context);, doesn't match anymore because of @ConvertGroup
                    validateBean(context);

                    // Spec 3.4.3 - Stop validation if errors already found
                    if (result.violationsSize() > numViolations) {
                        break;
                    }
                }
                context.setCurrentGroup(currentGroup);
            } else {

                // For each class in the hierarchy of classes of rootBean,
                // validate the constraints defined in that class according
                // to the GroupSequence defined in the same class

                // Obtain the full class hierarchy
                final List<Class<?>> classHierarchy = new ArrayList<Class<?>>();
                ClassHelper.fillFullClassHierarchyAsList(classHierarchy, context.getMetaBean().getBeanClass());
                final Class<?> initialOwner = context.getCurrentOwner();

                // For each owner in the hierarchy
                for (final Class<?> owner : classHierarchy) {

                    context.setCurrentOwner(owner);

                    int numViolations = result.violationsSize();

                    // Obtain the group sequence of the owner, and use it for
                    // the constraints that belong to it
                    final List<Group> ownerDefaultGroups =
                        context.getMetaBean().getFeature("{GroupSequence:" + owner.getCanonicalName() + "}");
                    for (Group each : ownerDefaultGroups) {
                        context.setCurrentGroup(each);
                        validateBean(context);
                        // Spec 3.4.3 - Stop validation if errors already found
                        if (result.violationsSize() > numViolations) {
                            break;
                        }
                    }
                }
                context.setCurrentOwner(initialOwner);
                context.setCurrentGroup(Group.DEFAULT);
            }
        }
        // if not the default group, proceed as normal
        else {
            validateBean(context);
        }

        // ### Then, the cascaded beans (@Valid)
        for (final MetaProperty prop : context.getMetaBean().getProperties()) {
            final Group group = context.getCurrentGroup();
            final Group mappedGroup;

            final Object feature = prop.getFeature(JsrFeatures.Property.PropertyDescriptor);
            if (feature == null) {
                mappedGroup = group;
            } else {
                mappedGroup = PropertyDescriptorImpl.class.cast(feature).mapGroup(group);
            }

            if (group == mappedGroup) {
                validateCascadedBean(context, prop, null);
            } else {
                final Groups propertyGroup = groupsComputer.computeGroups(new Class<?>[] { mappedGroup.getGroup() });
                validateCascadedBean(context, prop, propertyGroup);
            }
            context.setCurrentGroup(group);
        }
    }

    // TODO: maybe add a GroupMapper to bval-core to ease this kind of thing and void to fork this method from ValidationHelper
    private void validateBean(final GroupValidationContext<?> context) {
        // execute all property level validations
        for (final PropertyDescriptor prop : getConstraintsForClass(context.getMetaBean().getBeanClass())
            .getConstrainedProperties()) {
            final PropertyDescriptorImpl impl = PropertyDescriptorImpl.class.cast(prop);
            if (!impl.isValidated(impl)) {
                checkValidationAppliesTo(impl.getConstraintDescriptors(), ConstraintTarget.PARAMETERS);
                checkValidationAppliesTo(impl.getConstraintDescriptors(), ConstraintTarget.RETURN_VALUE);
                impl.setValidated(impl); // we don't really care about concurrency here
            }

            final MetaProperty metaProperty = context.getMetaBean().getProperty(prop.getPropertyName());
            context.setMetaProperty(metaProperty);
            final Group current = context.getCurrentGroup();
            context.setCurrentGroup(impl.mapGroup(current));
            ValidationHelper.validateProperty(context);
            context.setCurrentGroup(current);
        }

        // execute all bean level validations
        context.setMetaProperty(null);
        for (final Validation validation : context.getMetaBean().getValidations()) {
            if (ConstraintValidation.class.isInstance(validation)) {
                final ConstraintValidation<?> constraintValidation = ConstraintValidation.class.cast(validation);
                if (!constraintValidation.isValidated()) {
                    checkValidationAppliesTo(constraintValidation.getValidationAppliesTo(),
                        ConstraintTarget.PARAMETERS);
                    checkValidationAppliesTo(constraintValidation.getValidationAppliesTo(),
                        ConstraintTarget.RETURN_VALUE);
                    constraintValidation.setValidated(true);
                }
            }
            validation.validate(context);
        }
    }

    /**
     * Checks if the the meta property <code>prop</code> defines a cascaded bean, and in case it does, validates it.
     *
     * @param context The current validation context.
     * @param prop    The property to cascade from (in case it is possible).
     */
    private void validateCascadedBean(final GroupValidationContext<?> context, final MetaProperty prop,
        final Groups groups) {
        final AccessStrategy[] access = prop.getFeature(Features.Property.REF_CASCADE);
        if (access != null) { // different accesses to relation
            // save old values from context
            final Object bean = context.getBean();
            final MetaBean mbean = context.getMetaBean();
            // TODO implement Validation.groups support on related bean
            //            Class[] groups = prop.getFeature(JsrFeatures.Property.REF_GROUPS);
            for (final AccessStrategy each : access) {
                if (isCascadable(context, prop, each)) {
                    // modify context state for relationship-target bean
                    context.moveDown(prop, each);
                    // validate
                    if (groups == null) {
                        ValidationHelper.validateContext(context, new JsrValidationCallback(context),
                            factoryContext.isTreatMapsLikeBeans());
                    } else {
                        ValidationHelper.validateContext(context, new ValidationHelper.ValidateCallback() {
                            @Override
                            public void validate() {
                                validateBeanWithGroups(context, groups);
                            }
                        }, factoryContext.isTreatMapsLikeBeans());
                    }
                    // restore old values in context
                    context.moveUp(bean, mbean);
                }
            }
        }
    }

    /**
     * Before accessing a related bean (marked with {@link javax.validation.Valid}), the validator has to check if it is
     * reachable and cascadable.
     *
     * @param context The current validation context.
     * @param prop    The property of the related bean.
     * @param access  The access strategy used to get the related bean value.
     * @return <code>true</code> if the validator can access the related bean, <code>false</code> otherwise.
     */
    private boolean isCascadable(GroupValidationContext<?> context, MetaProperty prop, AccessStrategy access) {

        PathImpl beanPath = context.getPropertyPath();
        final NodeImpl node = new NodeImpl.PropertyNodeImpl(prop.getName());
        if (beanPath == null) {
            beanPath = PathImpl.create();
        }
        try {
            if (!context.getTraversableResolver().isReachable(context.getBean(), node,
                context.getRootMetaBean().getBeanClass(), beanPath, access.getElementType())) {
                return false;
            }
        } catch (RuntimeException e) {
            throw new ValidationException("Error in TraversableResolver.isReachable() for " + context.getBean(), e);
        }

        try {
            if (!context.getTraversableResolver().isCascadable(context.getBean(), node,
                context.getRootMetaBean().getBeanClass(), beanPath, access.getElementType())) {
                return false;
            }
        } catch (RuntimeException e) {
            throw new ValidationException("Error TraversableResolver.isCascadable() for " + context.getBean(), e);
        }
        return true;
    }

    /**
     * in case of a default group return the list of groups for a redefined default GroupSequence
     *
     * @return null when no in default group or default group sequence not redefined
     */
    private List<Group> expandDefaultGroup(GroupValidationContext<?> context) {
        if (context.getCurrentGroup().isDefault()) {
            // mention if metaBean redefines the default group
            final List<Group> groupSeq = context.getMetaBean().getFeature(JsrFeatures.Bean.GROUP_SEQUENCE);
            if (groupSeq != null) {
                context.getGroups().assertDefaultGroupSequenceIsExpandable(groupSeq);
            }
            return groupSeq;
        }
        return null;
    }

    /**
     * Generate an unrecoverable validation error
     *
     * @param ex
     * @param object
     * @return a {@link RuntimeException} of the appropriate type
     */
    protected static RuntimeException unrecoverableValidationError(RuntimeException ex, Object object) {
        if (ex instanceof UnknownPropertyException || ex instanceof IncompatiblePropertyValueException) {
            // Convert to IllegalArgumentException
            return new IllegalArgumentException(ex.getMessage(), ex);
        }
        if (ex instanceof ValidationException) {
            return ex; // do not wrap specific ValidationExceptions (or instances from subclasses)
        }
        String objectId;
        if (object == null) {
            objectId = "<null>";
        } else {
            try {
                objectId = object.toString();
            } catch (Exception e) {
                objectId = "<unknown>";
            }
        }
        return new ValidationException("error during validation of " + objectId, ex);
    }

    private void validatePropertyInGroup(final GroupValidationContext<?> context) {
        final Runnable helper;
        if (context.getMetaProperty() == null) {
            helper = new Runnable() {

                @Override
                public void run() {
                    ValidationHelper.validateBean(context);
                }
            };
        } else {
            helper = new Runnable() {

                @Override
                public void run() {
                    ValidationHelper.validateProperty(context);
                }
            };
        }
        final List<Group> defaultGroups = expandDefaultGroup(context);
        if (defaultGroups == null) {
            helper.run();
        } else {
            final Group currentGroup = context.getCurrentGroup();
            for (Group each : defaultGroups) {
                context.setCurrentGroup(each);
                helper.run();
                // continue validation, even if errors already found
            }
            context.setCurrentGroup(currentGroup); // restore
        }
    }

    /**
     * Create a {@link GroupValidationContext}.
     *
     * @param <T>
     * @param metaBean
     * @param object
     * @param objectClass
     * @param groups
     * @return {@link GroupValidationContext} instance
     */
    protected <T> GroupValidationContext<T> createContext(MetaBean metaBean, T object, Class<T> objectClass,
        Class<?>... groups) {
        final ConstraintValidationListener<T> listener = new ConstraintValidationListener<T>(object, objectClass);
        final GroupValidationContextImpl<T> context = new GroupValidationContextImpl<T>(listener,
            factoryContext.getMessageInterpolator(), factoryContext.getTraversableResolver(),
            factoryContext.getParameterNameProvider(), factoryContext.getConstraintValidatorFactory(), metaBean);
        context.setBean(object, metaBean);
        context.setGroups(groupsComputer.computeGroups(groups));
        return context;
    }

    protected <T> GroupValidationContext<T> createInvocableContext(MetaBean metaBean, T object, Class<T> objectClass,
        Class<?>... groups) {
        final ConstraintValidationListener<T> listener = new ConstraintValidationListener<T>(object, objectClass);
        final GroupValidationContextImpl<T> context = new GroupValidationContextImpl<T>(listener,
            factoryContext.getMessageInterpolator(), factoryContext.getTraversableResolver(),
            factoryContext.getParameterNameProvider(), factoryContext.getConstraintValidatorFactory(), metaBean);
        context.setBean(object, metaBean);
        final Groups computedGroup = groupsComputer.computeGroups(groups);
        if (Collections.singletonList(Group.DEFAULT).equals(computedGroup.getGroups())
            && metaBean.getFeature(JsrFeatures.Bean.GROUP_SEQUENCE) != null) {
            final Groups sequence = new Groups();
            @SuppressWarnings("unchecked")
            final List<? extends Group> sequenceGroups =
                List.class.cast(metaBean.getFeature(JsrFeatures.Bean.GROUP_SEQUENCE));
            sequence.getGroups().addAll(sequenceGroups);
            context.setGroups(sequence);
        } else {
            context.setGroups(computedGroup);
        }
        return context;
    }

    /**
     * Create a {@link BeanDescriptorImpl}
     *
     * @param metaBean
     * @return {@link BeanDescriptorImpl} instance
     */
    protected BeanDescriptorImpl createBeanDescriptor(MetaBean metaBean) {
        return new BeanDescriptorImpl(factoryContext, metaBean);
    }

    /**
     * Checks that the property name is valid according to spec Section 4.1.1 i. Throws an
     * {@link IllegalArgumentException} if it is not.
     *
     * @param propertyName Property name to check.
     */
    private void checkPropertyName(String propertyName) {
        if (propertyName == null || propertyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Property path cannot be null or empty.");
        }
    }

    /**
     * Checks that the groups array is valid according to spec Section 4.1.1 i. Throws an
     * {@link IllegalArgumentException} if it is not.
     *
     * @param groups The groups to check.
     */
    private void checkGroups(Class<?>[] groups) {
        for (final Class<?> c : notNull("groups", groups)) {
            notNull("group", c);
        }
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateConstructorParameters(Constructor<? extends T> constructor,
        Object[] parameterValues, Class<?>... gps) {
        notNull("Constructor", constructor);
        notNull("Groups", gps);
        notNull("Parameters", parameterValues);

        final Class<?> declaringClass = constructor.getDeclaringClass();
        final ConstructorDescriptorImpl constructorDescriptor = ConstructorDescriptorImpl.class
            .cast(getConstraintsForClass(declaringClass).getConstraintsForConstructor(constructor.getParameterTypes()));
        if (constructorDescriptor == null) { // no constraint
            return Collections.emptySet();
        }

        // sanity checks
        if (!constructorDescriptor.isValidated(constructor)) {
            if (parameterValues.length == 0) {
                checkValidationAppliesTo(Collections.singleton(constructorDescriptor.getCrossParameterDescriptor()),
                    ConstraintTarget.PARAMETERS);
                checkValidationAppliesTo(constructorDescriptor.getParameterDescriptors(), ConstraintTarget.PARAMETERS);
            } else {
                checkValidationAppliesTo(Collections.singleton(constructorDescriptor.getCrossParameterDescriptor()),
                    ConstraintTarget.IMPLICIT);
                checkValidationAppliesTo(constructorDescriptor.getParameterDescriptors(), ConstraintTarget.IMPLICIT);
            }
            constructorDescriptor.setValidated(constructor);
        }

        // validations
        return validateInvocationParameters(constructor, parameterValues, constructorDescriptor, gps,
            new NodeImpl.ConstructorNodeImpl(declaringClass.getSimpleName(),
                Arrays.asList(constructor.getParameterTypes())),
            null);
    }

    private <T> Set<ConstraintViolation<T>> validateInvocationParameters(final Member invocable,
        final Object[] parameterValues, final InvocableElementDescriptor constructorDescriptor, final Class<?>[] gps,
        final NodeImpl rootNode, final Object rootBean) {
        final Set<ConstraintViolation<T>> violations = new HashSet<ConstraintViolation<T>>();

        @SuppressWarnings("unchecked")
        final GroupValidationContext<ConstraintValidationListener<?>> parametersContext = createInvocableContext(
            constructorDescriptor.getMetaBean(), rootBean, Class.class.cast(invocable.getDeclaringClass()), gps);

        @SuppressWarnings("unchecked")
        final GroupValidationContext<Object> crossParameterContext = createContext(constructorDescriptor.getMetaBean(),
            rootBean, Class.class.cast(invocable.getDeclaringClass()), gps);

        if (rootBean == null) {
            final Constructor<?> m = Constructor.class.cast(invocable);
            parametersContext.setConstructor(m);
            crossParameterContext.setConstructor(m);
        } else { // could be more sexy but that's ok for now
            final Method m = Method.class.cast(invocable);
            parametersContext.setMethod(m);
            crossParameterContext.setMethod(m);
        }

        final Groups groups = parametersContext.getGroups();

        final List<ParameterDescriptor> parameterDescriptors = constructorDescriptor.getParameterDescriptors();
        final ElementDescriptorImpl crossParamDescriptor =
            ElementDescriptorImpl.class.cast(constructorDescriptor.getCrossParameterDescriptor());
        final Set<ConstraintDescriptor<?>> crossParamConstraints = crossParamDescriptor.getConstraintDescriptors();

        crossParameterContext.setBean(parameterValues);
        crossParameterContext.moveDown(rootNode);
        crossParameterContext.moveDown("<cross-parameter>");
        crossParameterContext.setKind(ElementKind.CROSS_PARAMETER);

        parametersContext.moveDown(rootNode);
        parametersContext.setParameters(parameterValues);

        for (final Group current : groups.getGroups()) {
            for (int i = 0; i < parameterValues.length; i++) {
                final ParameterDescriptorImpl paramDesc =
                    ParameterDescriptorImpl.class.cast(parameterDescriptors.get(i));
                parametersContext.setBean(parameterValues[i]);
                parametersContext.moveDown(new NodeImpl.ParameterNodeImpl(paramDesc.getName(), i));
                for (final ConstraintDescriptor<?> constraintDescriptor : paramDesc.getConstraintDescriptors()) {
                    final ConstraintValidation<?> validation = ConstraintValidation.class.cast(constraintDescriptor);
                    parametersContext.setCurrentGroup(paramDesc.mapGroup(current));
                    validation.validateGroupContext(parametersContext);
                }
                parametersContext.moveUp(null, null);
            }

            for (final ConstraintDescriptor<?> d : crossParamConstraints) {
                final ConstraintValidation<?> validation = ConstraintValidation.class.cast(d);
                crossParameterContext.setCurrentGroup(crossParamDescriptor.mapGroup(current));
                validation.validateGroupContext(crossParameterContext);
            }

            if (gps.length == 0 && parametersContext.getListener().getConstraintViolations().size()
                + crossParameterContext.getListener().getConstraintViolations().size() > 0) {
                break;
            }
        }

        for (final Group current : groups.getGroups()) {
            for (int i = 0; i < parameterValues.length; i++) {
                final ParameterDescriptorImpl paramDesc =
                    ParameterDescriptorImpl.class.cast(parameterDescriptors.get(i));
                if (paramDesc.isCascaded() && parameterValues[i] != null) {
                    parametersContext.setBean(parameterValues[i]);
                    parametersContext.moveDown(new NodeImpl.ParameterNodeImpl(paramDesc.getName(), i));
                    initMetaBean(parametersContext, factoryContext.getMetaBeanFinder(), parameterValues[i].getClass());
                    parametersContext.setCurrentGroup(paramDesc.mapGroup(current));
                    ValidationHelper.validateContext(parametersContext, new JsrValidationCallback(parametersContext),
                        factoryContext.isTreatMapsLikeBeans());
                    parametersContext.moveUp(null, null);
                }
            }
        }

        for (final List<Group> eachSeq : groups.getSequences()) {
            for (final Group current : eachSeq) {
                for (int i = 0; i < parameterValues.length; i++) {
                    final ParameterDescriptorImpl paramDesc =
                        ParameterDescriptorImpl.class.cast(parameterDescriptors.get(i));
                    parametersContext.setBean(parameterValues[i]);
                    parametersContext.moveDown(new NodeImpl.ParameterNodeImpl(paramDesc.getName(), i));
                    for (final ConstraintDescriptor<?> constraintDescriptor : paramDesc.getConstraintDescriptors()) {
                        final ConstraintValidation<?> validation =
                            ConstraintValidation.class.cast(constraintDescriptor);
                        parametersContext.setCurrentGroup(paramDesc.mapGroup(current));
                        validation.validateGroupContext(parametersContext);
                    }
                    parametersContext.moveUp(null, null);
                }

                for (final ConstraintDescriptor<?> d : crossParamConstraints) {
                    final ConstraintValidation<?> validation = ConstraintValidation.class.cast(d);
                    crossParameterContext.setCurrentGroup(crossParamDescriptor.mapGroup(current));
                    validation.validateGroupContext(crossParameterContext);
                }

                if (parametersContext.getListener().getConstraintViolations().size()
                    + crossParameterContext.getListener().getConstraintViolations().size() > 0) {
                    break;
                }
            }

            for (final Group current : eachSeq) {
                for (int i = 0; i < parameterValues.length; i++) {
                    final ParameterDescriptorImpl paramDesc =
                        ParameterDescriptorImpl.class.cast(parameterDescriptors.get(i));
                    if (paramDesc.isCascaded() && parameterValues[i] != null) {
                        parametersContext.setBean(parameterValues[i]);
                        parametersContext.moveDown(new NodeImpl.ParameterNodeImpl(paramDesc.getName(), i));
                        initMetaBean(parametersContext, factoryContext.getMetaBeanFinder(),
                            parameterValues[i].getClass());
                        parametersContext.setCurrentGroup(paramDesc.mapGroup(current));
                        ValidationHelper.validateContext(parametersContext,
                            new JsrValidationCallback(parametersContext), factoryContext.isTreatMapsLikeBeans());
                        parametersContext.moveUp(null, null);
                    }
                }
            }
        }
        if (constructorDescriptor.isCascaded()) {
            if (parametersContext.getValidatedValue() != null) {
                initMetaBean(parametersContext, factoryContext.getMetaBeanFinder(),
                    parametersContext.getValidatedValue().getClass());

                for (final Group current : groups.getGroups()) {
                    parametersContext.setCurrentGroup(constructorDescriptor.mapGroup(current));
                    ValidationHelper.validateContext(parametersContext, new JsrValidationCallback(parametersContext),
                        factoryContext.isTreatMapsLikeBeans());
                }
                for (final List<Group> eachSeq : groups.getSequences()) {
                    for (final Group current : eachSeq) {
                        parametersContext.setCurrentGroup(constructorDescriptor.mapGroup(current));
                        ValidationHelper.validateContext(parametersContext,
                            new JsrValidationCallback(parametersContext), factoryContext.isTreatMapsLikeBeans());
                        if (!parametersContext.getListener().isEmpty()) {
                            break;
                        }
                    }
                }
            }
            if (crossParameterContext.getValidatedValue() != null) {
                initMetaBean(crossParameterContext, factoryContext.getMetaBeanFinder(),
                    crossParameterContext.getValidatedValue().getClass());

                for (final Group current : groups.getGroups()) {
                    crossParameterContext.setCurrentGroup(constructorDescriptor.mapGroup(current));
                    ValidationHelper.validateContext(crossParameterContext,
                        new JsrValidationCallback(crossParameterContext), factoryContext.isTreatMapsLikeBeans());
                }
                for (final List<Group> eachSeq : groups.getSequences()) {
                    for (final Group current : eachSeq) {
                        crossParameterContext.setCurrentGroup(constructorDescriptor.mapGroup(current));
                        ValidationHelper.validateContext(crossParameterContext,
                            new JsrValidationCallback(crossParameterContext), factoryContext.isTreatMapsLikeBeans());
                        if (!crossParameterContext.getListener().isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        final Set<ConstraintViolation<T>> parameterViolations =
            Set.class.cast(parametersContext.getListener().getConstraintViolations());
        violations.addAll(parameterViolations);
        @SuppressWarnings("unchecked")
        final Set<ConstraintViolation<T>> crossParameterViolations =
            Set.class.cast(crossParameterContext.getListener().getConstraintViolations());
        violations.addAll(crossParameterViolations);

        return violations;
    }

    private static void checkValidationAppliesTo(final Collection<? extends ElementDescriptor> descriptors,
        final ConstraintTarget forbidden) {
        for (final ElementDescriptor descriptor : descriptors) {
            for (final ConstraintDescriptor<?> consDesc : descriptor.getConstraintDescriptors()) {
                checkValidationAppliesTo(consDesc.getValidationAppliesTo(), forbidden);
            }
        }
    }

    private static void checkValidationAppliesTo(final Set<ConstraintDescriptor<?>> constraintDescriptors,
        final ConstraintTarget forbidden) {
        for (final ConstraintDescriptor<?> descriptor : constraintDescriptors) {
            checkValidationAppliesTo(descriptor.getValidationAppliesTo(), forbidden);
        }
    }

    private static void checkValidationAppliesTo(final ConstraintTarget configured, final ConstraintTarget forbidden) {
        if (forbidden.equals(configured)) {
            throw new ConstraintDeclarationException(forbidden.name() + " forbidden here");
        }
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateConstructorReturnValue(final Constructor<? extends T> constructor,
        final T createdObject, final Class<?>... gps) {
        notNull("Constructor", constructor);
        notNull("Returned value", createdObject);

        final Class<? extends T> declaringClass = constructor.getDeclaringClass();
        final ConstructorDescriptorImpl methodDescriptor = ConstructorDescriptorImpl.class
            .cast(getConstraintsForClass(declaringClass).getConstraintsForConstructor(constructor.getParameterTypes()));
        if (methodDescriptor == null) {
            throw new ValidationException("Constructor " + constructor + " doesn't belong to class " + declaringClass);
        }

        return validateReturnedValue(
            new NodeImpl.ConstructorNodeImpl(declaringClass.getSimpleName(),
                Arrays.asList(constructor.getParameterTypes())),
            createdObject, declaringClass, methodDescriptor, gps, null);
    }

    private <T> Set<ConstraintViolation<T>> validateReturnedValue(final NodeImpl rootNode, final T createdObject,
        final Class<?> clazz, final InvocableElementDescriptor methodDescriptor, final Class<?>[] gps,
        final Object rootBean) {
        final ElementDescriptorImpl returnedValueDescriptor =
            ElementDescriptorImpl.class.cast(methodDescriptor.getReturnValueDescriptor());
        final Set<ConstraintDescriptor<?>> returnedValueConstraints =
            returnedValueDescriptor.getConstraintDescriptors();

        @SuppressWarnings("unchecked")
        final GroupValidationContext<T> context = createInvocableContext(methodDescriptor.getMetaBean(), createdObject,
            Class.class.cast(Proxies.classFor(clazz)), gps);
        context.moveDown(rootNode);
        context.moveDown(new NodeImpl.ReturnValueNodeImpl());
        context.setReturnValue(rootBean);

        final Groups groups = context.getGroups();

        for (final Group current : groups.getGroups()) {
            for (final ConstraintDescriptor<?> d : returnedValueConstraints) {
                final ConstraintValidation<?> validation = ConstraintValidation.class.cast(d);
                context.setCurrentGroup(returnedValueDescriptor.mapGroup(current));
                validation.validateGroupContext(context);
            }
            if (gps.length == 0 && !context.getListener().getConstraintViolations().isEmpty()) {
                break;
            }
        }

        int currentViolationNumber = context.getListener().getConstraintViolations().size();
        for (final Group current : groups.getGroups()) {
            if (returnedValueDescriptor.isCascaded() && context.getValidatedValue() != null) {
                context.setBean(createdObject);
                initMetaBean(context, factoryContext.getMetaBeanFinder(), context.getValidatedValue().getClass());

                context.setCurrentGroup(methodDescriptor.mapGroup(current));
                ValidationHelper.validateContext(context, new JsrValidationCallback(context),
                    factoryContext.isTreatMapsLikeBeans());

                if (currentViolationNumber < context.getListener().getConstraintViolations().size()) {
                    break;
                }
            }
        }

        for (final List<Group> eachSeq : groups.getSequences()) {
            for (final Group current : eachSeq) {
                for (final ConstraintDescriptor<?> d : returnedValueConstraints) {
                    final ConstraintValidation<?> validation = ConstraintValidation.class.cast(d);
                    context.setCurrentGroup(current);
                    validation.validateGroupContext(context);
                }
                if (!context.getListener().getConstraintViolations().isEmpty()) {
                    break;
                }
            }

            currentViolationNumber = context.getListener().getConstraintViolations().size();
            for (final Group current : eachSeq) {
                if (returnedValueDescriptor.isCascaded() && context.getValidatedValue() != null) {
                    context.setBean(createdObject);
                    initMetaBean(context, factoryContext.getMetaBeanFinder(), context.getValidatedValue().getClass());

                    context.setCurrentGroup(methodDescriptor.mapGroup(current));
                    ValidationHelper.validateContext(context, new JsrValidationCallback(context),
                        factoryContext.isTreatMapsLikeBeans());

                    if (currentViolationNumber < context.getListener().getConstraintViolations().size()) {
                        break;
                    }
                }
            }
        }

        return context.getListener().getConstraintViolations();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateParameters(T object, Method method, Object[] parameterValues,
        Class<?>... groups) {
        notNull("Object", object);
        notNull("Parameters", parameterValues);
        notNull("Method", method);
        notNull("Groups", groups);
        for (final Class<?> g : groups) {
            notNull("Each group", g);
        }

        final MethodDescriptorImpl methodDescriptor = findMethodDescriptor(object, method);
        if (methodDescriptor == null
            || !(methodDescriptor.hasConstrainedParameters() || methodDescriptor.hasConstrainedReturnValue())) { // no constraint
            return Collections.emptySet();
        }

        if (!methodDescriptor.isValidated(method)) {
            if (method.getParameterTypes().length == 0) {
                checkValidationAppliesTo(Collections.singleton(methodDescriptor.getCrossParameterDescriptor()),
                    ConstraintTarget.PARAMETERS);
                checkValidationAppliesTo(methodDescriptor.getParameterDescriptors(), ConstraintTarget.PARAMETERS);
            } else if (!Void.TYPE.equals(method.getReturnType())) {
                checkValidationAppliesTo(Collections.singleton(methodDescriptor.getCrossParameterDescriptor()),
                    ConstraintTarget.IMPLICIT);
                checkValidationAppliesTo(methodDescriptor.getParameterDescriptors(), ConstraintTarget.IMPLICIT);
            }
            methodDescriptor.setValidated(method);
        }
        return validateInvocationParameters(method, parameterValues, methodDescriptor, groups,
            new NodeImpl.MethodNodeImpl(method.getName(), Arrays.asList(method.getParameterTypes())), object);
    }

    private static <T> T notNull(final String entity, final T shouldntBeNull) {
        if (shouldntBeNull == null) {
            throw new IllegalArgumentException(entity + " cannot be null");
        }
        return shouldntBeNull;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Set<ConstraintViolation<T>> validateReturnValue(T object, Method method, Object returnValue,
        Class<?>... groups) {
        notNull("object", object);
        notNull("method", method);
        notNull("groups", groups);

        final MethodDescriptorImpl methodDescriptor = findMethodDescriptor(object, method);
        if (methodDescriptor == null) {
            throw new ValidationException("Method " + method + " doesn't belong to class " + object.getClass());
        }

        if (method.getReturnType() == Void.TYPE) {
            checkValidationAppliesTo(methodDescriptor.getReturnValueDescriptor().getConstraintDescriptors(),
                ConstraintTarget.RETURN_VALUE);
        }

        @SuppressWarnings("unchecked")
        final Set<ConstraintViolation<T>> result = Set.class.cast(validateReturnedValue(
            new NodeImpl.MethodNodeImpl(method.getName(), Arrays.asList(method.getParameterTypes())), returnValue,
            object.getClass(), methodDescriptor, groups, object));
        return result;
    }

    private <T> MethodDescriptorImpl findMethodDescriptor(final T object, final Method method) {
        return MethodDescriptorImpl.class
            .cast(BeanDescriptorImpl.class.cast(getConstraintsForClass(Proxies.classFor(method.getDeclaringClass())))
                .getInternalConstraintsForMethod(method.getName(), method.getParameterTypes()));
    }

    private <T> void initMetaBean(final GroupValidationContext<T> context, final MetaBeanFinder metaBeanFinder,
        final Class<?> directValueClass) {
        if (directValueClass.isArray()) {
            context.setMetaBean(metaBeanFinder.findForClass(directValueClass.getComponentType()));
            return;
        }
        if (Collection.class.isAssignableFrom(directValueClass)) {
            final Collection<?> coll = Collection.class.cast(context.getValidatedValue());
            if (!coll.isEmpty()) {
                context.setMetaBean(metaBeanFinder.findForClass(coll.iterator().next().getClass()));
                return;
            }
        }
        if (Map.class.isAssignableFrom(directValueClass)) {
            final Map<?, ?> m = Map.class.cast(context.getValidatedValue());
            if (!m.isEmpty()) {
                context.setMetaBean(metaBeanFinder.findForClass(m.values().iterator().next().getClass()));
                return;
            }
        }
        context.setMetaBean(metaBeanFinder.findForClass(directValueClass));
    }

    /**
     * Dispatches a call from {@link #validate()} to {@link ClassValidator#validateBeanNet(GroupValidationContext)} with
     * the current context set.
     */
    protected class JsrValidationCallback implements ValidationHelper.ValidateCallback {

        private final GroupValidationContext<?> context;

        public JsrValidationCallback(GroupValidationContext<?> context) {
            this.context = context;
        }

        @Override
        public void validate() {
            validateBeanNet(context);
        }

    }

    /**
     * Create a {@link ValidationContextTraversal} instance for this {@link ClassValidator}.
     * 
     * @param validationContext
     * @return {@link ValidationContextTraversal}
     */
    protected ValidationContextTraversal createValidationContextTraversal(GroupValidationContext<?> validationContext) {
        return new ValidationContextTraversal(validationContext);
    }

    /**
     * Implement {@link #validateProperty(Object, String, boolean, Class[])} } and
     * {@link #validateValue(Class, String, Object, boolean, Class...)}.
     * 
     * @param <T>
     * @param beanType
     * @param object
     * @param propertyName
     * @param value
     * @param cascade
     * @param groups
     * @return {@link ConstraintViolation} {@link Set}
     */
    private <T> Set<ConstraintViolation<T>> validateValueImpl(Class<T> beanType, T object, String propertyName,
        Object value, final boolean cascade, Class<?>... groups) {

        assert (object == null) ^ (value == VALIDATE_PROPERTY);
        checkPropertyName(propertyName);
        checkGroups(groups);

        try {
            final MetaBean initialMetaBean = new DynamicMetaBean(metaBeanFinder);
            initialMetaBean.setBeanClass(beanType);
            GroupValidationContext<T> context = createContext(initialMetaBean, object, beanType, groups);
            ValidationContextTraversal contextTraversal = createValidationContextTraversal(context);
            PathNavigation.navigate(propertyName, contextTraversal);

            MetaProperty prop = context.getMetaProperty();
            boolean fixed = false;
            if (value != VALIDATE_PROPERTY) {
                assert !context.getPropertyPath().isRootPath();
                if (prop == null && value != null) {
                    context.setMetaBean(metaBeanFinder.findForClass(value.getClass()));
                }
                if (!cascade) {
                    //TCK doesn't care what type a property is if there are no constraints to validate:
                    FeaturesCapable meta = prop == null ? context.getMetaBean() : prop;

                    Validation[] validations = meta.getValidations();
                    if (validations == null || validations.length == 0) {
                        return Collections.<ConstraintViolation<T>> emptySet();
                    }
                }
                if (!TypeUtils.isAssignable(value == null ? null : value.getClass(), contextTraversal.getType())) {
                    throw new IncompatiblePropertyValueException(String.format(
                        "%3$s is not a valid value for property %2$s of type %1$s", beanType, propertyName, value));
                }
                if (prop == null) {
                    context.setBean(value);
                } else {
                    context.setFixedValue(value);
                    fixed = true;
                }
            }
            boolean doCascade = cascade && (prop == null || prop.getMetaBean() != null);

            Object bean = context.getBean();

            ConstraintValidationListener<T> result = context.getListener();
            Groups sequence = context.getGroups();

            // 1. process groups

            for (Group current : sequence.getGroups()) {
                context.setCurrentGroup(current);

                if (!doCascade || prop != null) {
                    validatePropertyInGroup(context);
                }
                if (doCascade) {
                    contextTraversal.moveDownIfNecessary();
                    if (context.getMetaBean() instanceof DynamicMetaBean) {
                        context.setMetaBean(context.getMetaBean().resolveMetaBean(
                            ObjectUtils.defaultIfNull(context.getBean(), contextTraversal.getRawType())));
                    }
                    validateBeanNet(context);
                    if (prop != null) {
                        context.moveUp(bean, prop.getParentMetaBean());
                        context.setMetaProperty(prop);
                        if (fixed) {
                            context.setFixedValue(value);
                        }
                    }
                }
            }

            // 2. process sequences

            int groupViolations = result.getConstraintViolations().size();

            outer: for (List<Group> eachSeq : sequence.getSequences()) {
                for (Group current : eachSeq) {
                    context.setCurrentGroup(current);

                    if (!doCascade || prop != null) {
                        validatePropertyInGroup(context);
                    }
                    if (doCascade) {
                        contextTraversal.moveDownIfNecessary();
                        if (context.getMetaBean() instanceof DynamicMetaBean) {
                            context.setMetaBean(context.getMetaBean().resolveMetaBean(
                                ObjectUtils.defaultIfNull(context.getBean(), contextTraversal.getRawType())));
                        }
                        validateBeanNet(context);
                        if (prop != null) {
                            context.moveUp(bean, prop.getParentMetaBean());
                            context.setMetaProperty(prop);
                            if (fixed) {
                                context.setFixedValue(value);
                            }
                        }
                    }
                    /**
                     * if one of the group process in the sequence leads to one or more validation failure, the groups
                     * following in the sequence must not be processed
                     */
                    if (result.getConstraintViolations().size() > groupViolations)
                        break outer;
                }
            }
            return result.getConstraintViolations();
        } catch (RuntimeException ex) {
            throw unrecoverableValidationError(ex, ObjectUtils.defaultIfNull(object, value));
        }
    }
}
