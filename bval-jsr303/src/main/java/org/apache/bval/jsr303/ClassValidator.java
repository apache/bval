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

import org.apache.bval.DynamicMetaBean;
import org.apache.bval.MetaBeanFinder;
import org.apache.bval.jsr303.groups.Group;
import org.apache.bval.jsr303.groups.Groups;
import org.apache.bval.jsr303.groups.GroupsComputer;
import org.apache.bval.jsr303.util.ClassHelper;
import org.apache.bval.jsr303.util.NodeImpl;
import org.apache.bval.jsr303.util.PathImpl;
import org.apache.bval.jsr303.util.PathNavigation;
import org.apache.bval.jsr303.util.Proxies;
import org.apache.bval.jsr303.util.ValidationContextTraversal;
import org.apache.bval.model.Features;
import org.apache.bval.model.FeaturesCapable;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.Validation;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.ValidationHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO: centralize treatMapsLikeBeans

/**
 * Objects of this class are able to validate bean instances (and the associated object graphs).
 * <p/>
 * Implementation is thread-safe.
 * <p/>
 * API class
 *
 * @version $Rev$ $Date$
 * 
 * @author Roman Stumm
 * @author Carlos Vara
 */
public class ClassValidator implements CascadingPropertyValidator, ExecutableValidator {
    private static final Object VALIDATE_PROPERTY = new Object() {
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

    /**
     * Create a new ClassValidator instance.
     *
     * @param factoryContext
     */
    public ClassValidator(ApacheFactoryContext factoryContext) {
        this.factoryContext = factoryContext;
    }

    /**
     * Create a new ClassValidator instance.
     *
     * @param factory
     * @deprecated provided for backward compatibility
     */
    public ClassValidator(ApacheValidatorFactory factory) {
        this(factory.usingContext());
    }

    /**
     * Get the metabean finder associated with this validator.
     *
     * @return a MetaBeanFinder
     * @see org.apache.bval.MetaBeanManagerFactory#getFinder()
     */
    protected MetaBeanFinder getMetaBeanFinder() {
        return factoryContext.getMetaBeanFinder();
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
    // @Override - not allowed in 1.5 for Interface methods
    @SuppressWarnings("unchecked")
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        if (object == null) {
            throw new IllegalArgumentException("cannot validate null");
        }
        checkGroups(groups);

        try {

            final Class<T> objectClass = (Class<T>) object.getClass();
            final MetaBean objectMetaBean = getMetaBeanFinder().findForClass(objectClass);
            final GroupValidationContext<T> context = createContext(objectMetaBean, object, objectClass, groups);
            return validateBeanWithGroups(context, context.getGroups());
        } catch (final RuntimeException ex) {
            throw unrecoverableValidationError(ex, object);
        }
    }

    private <T> Set<ConstraintViolation<T>> validateBeanWithGroups(final GroupValidationContext<T> context, final Groups sequence) {
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
    // @Override - not allowed in 1.5 for Interface methods
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
        return validateProperty(object, propertyName, false, groups);
    }

    /**
     * {@inheritDoc}
     */
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, boolean cascade,
        Class<?>... groups) {

        if (object == null)
            throw new IllegalArgumentException("cannot validate null");

        @SuppressWarnings("unchecked")
        Set<ConstraintViolation<T>> result =
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
    // @Override - not allowed in 1.5 for Interface methods
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value,
        Class<?>... groups) {
        return validateValue(beanType, propertyName, value, false, groups);
    }

    /**
     * {@inheritDoc}
     */
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value,
        boolean cascade, Class<?>... groups) {
        return validateValueImpl(checkBeanType(beanType), null, propertyName, value, cascade, groups);
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
    // @Override - not allowed in 1.5 for Interface methods
    public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        try {
            MetaBean metaBean = getMetaBeanFinder().findForClass(clazz); // don't throw an exception because of a missing validator here
            BeanDescriptorImpl edesc = metaBean.getFeature(Jsr303Features.Bean.BEAN_DESCRIPTOR);
            if (edesc == null) {
                edesc = createBeanDescriptor(metaBean);
                metaBean.putFeature(Jsr303Features.Bean.BEAN_DESCRIPTOR, edesc);
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
    // @Override - not allowed in 1.5 for Interface methods
    public <T> T unwrap(Class<T> type) {
        // FIXME 2011-03-27 jw:
        // This code is unsecure.
        // It should allow only a fixed set of classes.
        // Can't fix this because don't know which classes this method should support.

        if (type.isAssignableFrom(getClass())) {
            @SuppressWarnings("unchecked")
            final T result = (T) this;
            return result;
        } else if (!(type.isInterface() || Modifier.isAbstract(type.getModifiers()))) {
            return newInstance(type);
        } else {
            try {
                Class<?> cls = ClassUtils.getClass(type.getName() + "Impl");
                if (type.isAssignableFrom(cls)) {
                    @SuppressWarnings("unchecked")
                    final Class<? extends T> implClass = (Class<? extends T>) cls;
                    return newInstance(implClass);
                }
            } catch (ClassNotFoundException e) {
            }
            throw new ValidationException("Type " + type + " not supported");
        }
    }

    public ExecutableValidator forExecutables() {
        return this;
    }

    private <T> T newInstance(final Class<T> cls) {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            public T run() {
                try {
                    Constructor<T> cons = cls.getConstructor(ApacheFactoryContext.class);
                    if (!cons.isAccessible()) {
                        cons.setAccessible(true);
                    }
                    return cons.newInstance(factoryContext);
                } catch (final Exception ex) {
                    throw new ValidationException("Cannot instantiate : " + cls, ex);
                }
            }
        });
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
                    final List<Group> ownerDefaultGroups = context.getMetaBean().getFeature("{GroupSequence:" + owner.getCanonicalName() + "}");
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

            final Object feature = prop.getFeature(Jsr303Features.Property.PropertyDescriptor);
            if (feature != null) {
                mappedGroup = PropertyDescriptorImpl.class.cast(feature).mapGroup(group);
            } else {
                mappedGroup = group;
            }


            if (group != mappedGroup) {
                final Groups propertyGroup = groupsComputer.computeGroups(new Class<?>[]{ mappedGroup.getGroup() });
                validateCascadedBean(context, prop, propertyGroup);
            } else {
                validateCascadedBean(context, prop, null);
            }

            context.setCurrentGroup(group);
        }

    }

    // TODO: maybe add a GroupMapper to bval-core to ease this kind of thing and void to fork this method from ValidationHelper
    private void validateBean(final GroupValidationContext<?> context) {
        // execute all property level validations
        for (final PropertyDescriptor prop : getConstraintsForClass(context.getMetaBean().getBeanClass()).getConstrainedProperties()) {
            final PropertyDescriptorImpl impl = PropertyDescriptorImpl.class.cast(prop);
            checkValidationAppliesTo(impl.getConstraintDescriptors(), ConstraintTarget.PARAMETERS);
            checkValidationAppliesTo(impl.getConstraintDescriptors(), ConstraintTarget.RETURN_VALUE);

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
                checkValidationAppliesTo(ConstraintValidation.class.cast(validation).getValidationAppliesTo(), ConstraintTarget.PARAMETERS);
                checkValidationAppliesTo(ConstraintValidation.class.cast(validation).getValidationAppliesTo(), ConstraintTarget.RETURN_VALUE);
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
    private void validateCascadedBean(final GroupValidationContext<?> context, final MetaProperty prop, final Groups groups) {
        AccessStrategy[] access = prop.getFeature(Features.Property.REF_CASCADE);
        if (access != null) { // different accesses to relation
            // save old values from context
            final Object bean = context.getBean();
            final MetaBean mbean = context.getMetaBean();
            // TODO implement Validation.groups support on related bean
//            Class[] groups = prop.getFeature(Jsr303Features.Property.REF_GROUPS);
            for (final AccessStrategy each : access) {
                if (isCascadable(context, prop, each)) {
                    // modify context state for relationship-target bean
                    context.moveDown(prop, each);
                    // validate
                    if (groups == null) {
                        ValidationHelper.validateContext(context, new Jsr303ValidationCallback(context), treatMapsLikeBeans);
                    } else {
                        ValidationHelper.validateContext(context, new ValidationHelper.ValidateCallback() {
                            public void validate() {
                                validateBeanWithGroups(context, groups);
                            }
                        }, treatMapsLikeBeans);
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
                context.getRootMetaBean().getBeanClass(), beanPath, access.getElementType()))
                return false;
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
            List<Group> groupSeq = context.getMetaBean().getFeature(Jsr303Features.Bean.GROUP_SEQUENCE);
            if (groupSeq != null) {
                context.getGroups().assertDefaultGroupSequenceIsExpandable(groupSeq);
            }
            return groupSeq;
        } else {
            return null;
        }
    }

    /**
     * Generate an unrecoverable validation error
     *
     * @param ex
     * @param object
     * @return a {@link RuntimeException} of the appropriate type
     */
    @SuppressWarnings("finally")
    protected static RuntimeException unrecoverableValidationError(RuntimeException ex, Object object) {
        if (ex instanceof UnknownPropertyException || ex instanceof IncompatiblePropertyValueException) {
            // Convert to IllegalArgumentException
            return new IllegalArgumentException(ex.getMessage(), ex);
        } else if (ex instanceof ValidationException) {
            return ex; // do not wrap specific ValidationExceptions (or
            // instances from subclasses)
        } else {
            String objectId = "";
            try {
                if (object != null) {
                    objectId = object.toString();
                } else {
                    objectId = "<null>";
                }
            } catch (Exception e) {
                objectId = "<unknown>";
            } finally {
                return new ValidationException("error during validation of "
                        + objectId, ex);
            }
        }
    }

    private void validatePropertyInGroup(final GroupValidationContext<?> context) {
        final Runnable helper;
        if (context.getMetaProperty() == null) {
            helper = new Runnable() {

                public void run() {
                    ValidationHelper.validateBean(context);
                }
            };
        } else {
            helper = new Runnable() {

                public void run() {
                    ValidationHelper.validateProperty(context);
                }
            };
        }
        Group currentGroup = context.getCurrentGroup();
        List<Group> defaultGroups = expandDefaultGroup(context);
        if (defaultGroups != null) {
            for (Group each : defaultGroups) {
                context.setCurrentGroup(each);
                helper.run();
                // continue validation, even if errors already found
            }
            context.setCurrentGroup(currentGroup); // restore
        } else {
            helper.run();
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
    protected <T> GroupValidationContext<T> createContext(MetaBean metaBean, T object, Class<T> objectClass, Class<?>... groups) {
        final ConstraintValidationListener<T> listener = new ConstraintValidationListener<T>(object, objectClass);
        final GroupValidationContextImpl<T> context =
            new GroupValidationContextImpl<T>(listener, this.factoryContext.getMessageInterpolator(),
                this.factoryContext.getTraversableResolver(), factoryContext.getParameterNameProvider(), metaBean);
        context.setBean(object, metaBean);
        context.setGroups(groupsComputer.computeGroups(groups));
        return context;
    }

    /**
     * Create a {@link BeanDescriptorImpl}
     *
     * @param metaBean
     * @return {@link BeanDescriptorImpl} instance
     */
    /*
    protected BeanDescriptorImpl createBeanDescriptor(MetaBean metaBean) {
        return new BeanDescriptorImpl(factoryContext, metaBean);
    }
    */
    protected BeanDescriptorImpl createBeanDescriptor(MetaBean metaBean) {
        return new BeanDescriptorImpl(factoryContext, metaBean);
    }

    private boolean treatMapsLikeBeans = false;

    /**
     * Behavior configuration -
     * <p/>
     * <pre>
     * @return treatMapsLikeBeans - true (validate maps like beans, so that
     *                              you can use Maps to validate dynamic classes or
     *                              beans for which you have the MetaBean but no instances)
     *                            - false (default), validate maps like collections
     *                              (validating the values only)
     * </pre>
     * <p/>
     * (is still configuration to better in BeanValidationContext?)
     */
    public boolean isTreatMapsLikeBeans() {
        return treatMapsLikeBeans;
    }

    /**
     * Set whether maps are to be treated like beans.
     * 
     * <pre>
     * @param treatMapsLikeBeans - true (validate maps like beans, so that
     *                             you can use Maps to validate dynamic classes or
     *                             beans for which you have the MetaBean but no instances)
     *                           - false (default), validate maps like collections
     *                             (validating the values only)
     * </pre>
     */
    public void setTreatMapsLikeBeans(boolean treatMapsLikeBeans) {
        this.treatMapsLikeBeans = treatMapsLikeBeans;
    }

    /**
     * Checks that beanType is valid according to spec Section 4.1.1 i. Throws an {@link IllegalArgumentException} if it
     * is not.
     *
     * @param beanType Bean type to check.
     */
    private <T> Class<T> checkBeanType(Class<T> beanType) {
        if (beanType == null) {
            throw new IllegalArgumentException("Bean type cannot be null.");
        }
        return beanType;
    }

    /**
     * Checks that the property name is valid according to spec Section 4.1.1 i. Throws an
     * {@link IllegalArgumentException} if it is not.
     *
     * @param propertyName Property name to check.
     */
    private void checkPropertyName(String propertyName) {
        if (propertyName == null || propertyName.trim().length() == 0) {
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
        if (groups == null) {
            throw new IllegalArgumentException("Groups cannot be null.");
        }
        for (final Class<?> c : groups) {
            if (c == null) {
                throw new IllegalArgumentException("Group cannot be null.");
            }
        }
    }

    public <T> Set<ConstraintViolation<T>> validateConstructorParameters(Constructor<? extends T> constructor, Object[] parameterValues, Class<?>... gps) {
        notNull("Constructor", constructor);
        notNull("Groups", gps);
        notNull("Parameters", parameterValues);

        final Class<?> declaringClass = constructor.getDeclaringClass();
        final ConstructorDescriptorImpl constructorDescriptor = ConstructorDescriptorImpl.class.cast(getConstraintsForClass(declaringClass).getConstraintsForConstructor(constructor.getParameterTypes()));
        if (constructorDescriptor == null) { // no constraint
            return Collections.emptySet();
        }

        // validations
        if (parameterValues.length > 0) {
            checkValidationAppliesTo(Collections.singleton(constructorDescriptor.getCrossParameterDescriptor()), ConstraintTarget.IMPLICIT);
            checkValidationAppliesTo(constructorDescriptor.getParameterDescriptors(), ConstraintTarget.IMPLICIT);
        } else {
            checkValidationAppliesTo(Collections.singleton(constructorDescriptor.getCrossParameterDescriptor()), ConstraintTarget.PARAMETERS);
            checkValidationAppliesTo(constructorDescriptor.getParameterDescriptors(), ConstraintTarget.PARAMETERS);
        }

        final Set<ConstraintViolation<T>> violations = new HashSet<ConstraintViolation<T>>();
        final ImmutablePair<Set<ConstraintViolation<Object>>, Group> result;
        {
            final GroupValidationContext<ConstraintValidationListener<?>> context = createContext(constructorDescriptor.getMetaBean(), null, Class.class.cast(constructor.getDeclaringClass()), gps);
            context.moveDown(new NodeImpl.ConstructorNodeImpl(declaringClass.getSimpleName(), Arrays.asList(constructor.getParameterTypes())));
            result = validateParameters(context, constructorDescriptor.getParameterDescriptors(), parameterValues);
            violations.addAll(Set.class.cast(result.getLeft()));
            context.moveUp(null, null);
        }
        {
            final GroupValidationContext<Object> crossParameterContext;
            if (result.getRight() == null) {
                crossParameterContext = createContext(constructorDescriptor.getMetaBean(), parameterValues, Class.class.cast(constructor.getDeclaringClass()), gps);
            } else {
                crossParameterContext = createContext(constructorDescriptor.getMetaBean(), parameterValues, Class.class.cast(constructor.getDeclaringClass()), result.getRight().getGroup());
            }

            crossParameterContext.setBean(parameterValues);
            crossParameterContext.moveDown(new NodeImpl.ConstructorNodeImpl(declaringClass.getSimpleName(), Arrays.asList(constructor.getParameterTypes())));
            crossParameterContext.moveDown("<cross-parameter>");
            crossParameterContext.setKind(ElementKind.CROSS_PARAMETER);
            validateElementInContext(crossParameterContext, constructorDescriptor.getCrossParameterDescriptor());
            crossParameterContext.moveUp(null, null);
            crossParameterContext.moveUp(null, null);

            violations.addAll(Set.class.cast(crossParameterContext.getListener().getConstraintViolations()));
        }

        return violations;
    }

    private static void checkValidationAppliesTo(final Collection<? extends ElementDescriptor> descriptors, final ConstraintTarget forbidden) {
        for (final ElementDescriptor descriptor : descriptors) {
            for (final ConstraintDescriptor<?> consDesc : descriptor.getConstraintDescriptors()) {
                checkValidationAppliesTo(consDesc.getValidationAppliesTo(), forbidden);
            }
        }
    }

    private static void checkValidationAppliesTo(final Set<ConstraintDescriptor<?>> constraintDescriptors, final ConstraintTarget forbidden) {
        for (final ConstraintDescriptor<?> descriptor : constraintDescriptors) {
            checkValidationAppliesTo(descriptor.getValidationAppliesTo(), forbidden);
        }
    }

    private static void checkValidationAppliesTo(final ConstraintTarget configured, final ConstraintTarget forbidden) {
        if (forbidden.equals(configured)) {
            throw new ConstraintDeclarationException(forbidden.name() + " forbidden here");
        }
    }

    public <T> Set<ConstraintViolation<T>> validateConstructorReturnValue(final Constructor<? extends T> constructor, final T createdObject, final Class<?>... groups) {
        if (constructor == null) {
            throw new IllegalArgumentException("constructor shouldn't be null");
        }
        if (createdObject == null) {
            throw new IllegalArgumentException("returned value shouldn't be null");
        }

        final ConstructorDescriptorImpl methodDescriptor = ConstructorDescriptorImpl.class.cast(getConstraintsForClass(constructor.getDeclaringClass()).getConstraintsForConstructor(constructor.getParameterTypes()));
        if (methodDescriptor == null) {
            throw new ValidationException("Constructor " + constructor + " doesn't belong to class " + constructor.getDeclaringClass());
        }

        final GroupValidationContext<T> context = createContext(methodDescriptor.getMetaBean(), createdObject, (Class<T>) Proxies.classFor(createdObject.getClass()), groups);
        context.moveDown(new NodeImpl.ConstructorNodeImpl(constructor.getDeclaringClass().getSimpleName(), Arrays.asList(constructor.getParameterTypes())));
        context.moveDown(new NodeImpl.ReturnValueNodeImpl());
        context.setReturnValue(createdObject);
        validateElementInContext(context, methodDescriptor.getReturnValueDescriptor());
        final Set<ConstraintViolation<T>> constraintViolations = context.getListener().getConstraintViolations();
        context.moveUp(null, null);
        context.moveUp(null, null);

        if (methodDescriptor.isCascaded()) {
            context.moveDown(new NodeImpl.ConstructorNodeImpl(constructor.getDeclaringClass().getSimpleName(), Arrays.asList(constructor.getParameterTypes())));
            context.moveDown(new NodeImpl.ReturnValueNodeImpl());
            context.setReturnValue(createdObject);
            context.setBean(createdObject);
            for (final Group group : context.getGroups().getGroups()) {
                context.setCurrentGroup(methodDescriptor.mapGroup(group));
                validateBeanNet(context);
            }
            for (final List<Group> sequence : context.getGroups().getSequences()) {
                for (final Group group : sequence) {
                    context.setCurrentGroup(methodDescriptor.mapGroup(group));
                    validateBeanNet(context);
                }
            }
            constraintViolations.addAll(Set.class.cast(context.getListener().getConstraintViolations()));
            context.moveUp(null, null);
            context.moveUp(null, null);
        }

        return constraintViolations;
    }

    public <T> Set<ConstraintViolation<T>> validateParameters(T object, Method method, Object[] parameterValues, Class<?>... groups) {
        {
            notNull("Object", object);
            notNull("Parameters", parameterValues);
            notNull("Method", method);
            notNull("Groups", groups);
            for (final Class<?> g : groups) {
                notNull("Each group", g);
            }
        }

        final MethodDescriptorImpl methodDescriptor = findMethodDescriptor(object, method);
        if (methodDescriptor == null) { // no constraint
            return Collections.emptySet();
        }

        if (method.getParameterTypes().length > 0 && method.getReturnType() != Void.TYPE) {
            checkValidationAppliesTo(Collections.singleton(methodDescriptor.getCrossParameterDescriptor()), ConstraintTarget.IMPLICIT);
            checkValidationAppliesTo(methodDescriptor.getParameterDescriptors(), ConstraintTarget.IMPLICIT);
        } else if (method.getParameterTypes().length == 0) {
            checkValidationAppliesTo(Collections.singleton(methodDescriptor.getCrossParameterDescriptor()), ConstraintTarget.PARAMETERS);
            checkValidationAppliesTo(methodDescriptor.getParameterDescriptors(), ConstraintTarget.PARAMETERS);
        }

        final Set<ConstraintViolation<T>> violations = new HashSet<ConstraintViolation<T>>();

        final ImmutablePair<Set<ConstraintViolation<Object>>, Group> result;
        { // parameter validations
            final GroupValidationContext<ConstraintValidationListener<?>> context = createContext(methodDescriptor.getMetaBean(), object, Class.class.cast(object.getClass()), groups);
            context.setMethod(method);
            context.moveDown(new NodeImpl.MethodNodeImpl(method.getName(), Arrays.asList(method.getParameterTypes())));
            result = validateParameters(context, methodDescriptor.getParameterDescriptors(), parameterValues);
            violations.addAll(Set.class.cast(result.getLeft()));
            context.moveUp(null, null);
        }

        { // cross parameters validation
            final GroupValidationContext<Object[]> context;
            if (result.getRight() == null) {
                context = createContext(methodDescriptor.getMetaBean(), object, Class.class.cast(Object[].class), groups);
            } else {
                context = createContext(methodDescriptor.getMetaBean(), object, Class.class.cast(Object[].class), result.getRight().getGroup());
            }
            context.setMethod(method);
            context.setBean(parameterValues);
            context.moveDown(new NodeImpl.MethodNodeImpl(method.getName(), Arrays.asList(method.getParameterTypes())));
            context.moveDown(new NodeImpl.CrossParameterNodeImpl());
            validateElementInContext(context, methodDescriptor.getCrossParameterDescriptor());
            violations.addAll(Set.class.cast(context.getListener().getConstraintViolations()));
            context.moveUp(null, null);
            context.moveUp(null, null);
        }

        return violations;
    }

    private static void notNull(final String entity, final Object shouldntBeNull) {
        if (shouldntBeNull == null) {
            throw new IllegalArgumentException(entity + " shouldn't be null");
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T> Set<ConstraintViolation<T>> validateReturnValue(T object, Method method, Object returnValue, Class<?>... groups) {
        notNull("object", object);
        notNull("method", method);
        notNull("groups", groups);

        MethodDescriptorImpl methodDescriptor = findMethodDescriptor(object, method);
        if (methodDescriptor == null) {
            throw new ValidationException("Method " + method + " doesn't belong to class " + object.getClass());
        }

        if (method.getReturnType() == Void.TYPE) {
            checkValidationAppliesTo(methodDescriptor.getReturnValueDescriptor().getConstraintDescriptors(), ConstraintTarget.RETURN_VALUE);
        }

        final GroupValidationContext<?> context = createContext(methodDescriptor.getMetaBean(), returnValue, Class.class.cast(Proxies.classFor(object.getClass())), groups);
        context.moveDown(new NodeImpl.MethodNodeImpl(method.getName(), Arrays.asList(method.getParameterTypes())));
        context.moveDown(new NodeImpl.ReturnValueNodeImpl());
        context.setReturnValue(object); // switched with returnValue when creating violation, avoid to modify the validation logic
        initMetaBean(context, factoryContext.getMetaBeanFinder(), method.getReturnType());
        validateElementInContext(context, methodDescriptor.getReturnValueDescriptor());
        final ConstraintValidationListener<T> result = (ConstraintValidationListener<T>) context.getListener();
        context.moveUp(null, null);
        context.moveUp(null, null);

        return result.getConstraintViolations();
    }

    private <T> MethodDescriptorImpl findMethodDescriptor(final T object, final Method method) {
        return MethodDescriptorImpl.class.cast(getConstraintsForClass(Proxies.classFor(object.getClass())).getConstraintsForMethod(method.getName(), method.getParameterTypes()));
    }

    private <T> ImmutablePair<Set<ConstraintViolation<T>>, Group> validateParameters(GroupValidationContext<ConstraintValidationListener<?>> context,
                                                                                         List<ParameterDescriptor> paramDescriptors, Object[] parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("cannot validate null");
        }
        if (parameters.length > 0) {
            try {
                Group gp = null;
                for (int i = 0; i < parameters.length; i++) {
                    final ParameterDescriptorImpl paramDesc = (ParameterDescriptorImpl) paramDescriptors.get(i);
                    context.setBean(parameters[i]);
                    context.setParameters(parameters);
                    context.moveDown(new NodeImpl.ParameterNodeImpl(paramDesc.getName(), i));
                    final Group current = validateElementInContext(context, paramDesc);
                    if (current != null) {
                        gp = current;
                    }
                    context.moveUp(null, null);
                }

                final ConstraintValidationListener<T> result = (ConstraintValidationListener<T>) context.getListener();
                return new ImmutablePair<Set<ConstraintViolation<T>>, Group>(result.getConstraintViolations(), gp);
            } catch (final RuntimeException ex) {
                throw unrecoverableValidationError(ex, parameters);
            }
        } else {
            return new ImmutablePair<Set<ConstraintViolation<T>>, Group>(Collections.<ConstraintViolation<T>> emptySet(), null);
        }
    }

    private <T> void initMetaBean(final GroupValidationContext<T> context, final MetaBeanFinder metaBeanFinder, final Class<?> directValueClass) {
        final boolean collection = Collection.class.isAssignableFrom(directValueClass);
        final boolean map = Map.class.isAssignableFrom(directValueClass);
        if (!directValueClass.isArray()
                && (!collection || Collection.class.cast(context.getValidatedValue()).isEmpty())
                && (!map || Map.class.cast(context.getValidatedValue()).isEmpty())) {
            context.setMetaBean(metaBeanFinder.findForClass(directValueClass));
        } else if (collection) {
            context.setMetaBean(metaBeanFinder.findForClass(Collection.class.cast(context.getValidatedValue()).iterator().next().getClass()));
        } else if (map) {
            context.setMetaBean(metaBeanFinder.findForClass(Map.class.cast(context.getValidatedValue()).values().iterator().next().getClass()));
        } else {
            context.setMetaBean(metaBeanFinder.findForClass(directValueClass.getComponentType()));
        }
    }

    private <T> Group validateElementInContext(final GroupValidationContext<T> context,
                                              final ElementDescriptor eltDescriptor) {

        final ElementDescriptorImpl impl = ElementDescriptorImpl.class.cast(eltDescriptor);

        final Groups groups = context.getGroups();

        Group breakOnGroup = null;

        for (final ConstraintDescriptor<?> consDesc : eltDescriptor.getConstraintDescriptors()) {
            final ConstraintValidation<?> validation = (ConstraintValidation<?>) consDesc;

            // 1. process groups
            for (final Group current : groups.getGroups()) {
                context.setCurrentGroup(current);
                validation.validate(context);
            }
            // 2. process sequences
            for (final List<Group> eachSeq : groups.getSequences()) {
                for (final Group current : eachSeq) {
                    context.setCurrentGroup(current);
                    validation.validate(context);
                    /**
                     * if one of the group process in the sequence leads to one
                     * or more validation failure, the groups following in the
                     * sequence must not be processed
                     */
                    if (!context.getListener().isEmpty()) {
                        breakOnGroup = current;
                        break;
                    }
                }
            }
        }
        if (impl.isCascaded() && context.getValidatedValue() != null) {
            initMetaBean(context, factoryContext.getMetaBeanFinder(), context.getValidatedValue().getClass());

            // 1. process groups
            for (final Group current : groups.getGroups()) {
                context.setCurrentGroup(impl.mapGroup(current));
                ValidationHelper.validateContext(context, new Jsr303ValidationCallback(context), isTreatMapsLikeBeans());
            }
            // 2. process sequences
            for (final List<Group> eachSeq : groups.getSequences()) {
                for (final Group current : eachSeq) {
                    context.setCurrentGroup(impl.mapGroup(current));
                    ValidationHelper.validateContext(context, new Jsr303ValidationCallback(context), isTreatMapsLikeBeans());
                    /**
                     * if one of the group process in the sequence leads to one
                     * or more validation failure, the groups following in the
                     * sequence must not be processed
                     */
                    if (!context.getListener().isEmpty()) {
                        breakOnGroup = current;
                        break;
                    }
                }
            }
        }
        return breakOnGroup;
    }

    /**
     * Dispatches a call from {@link #validate()} to {@link ClassValidator#validateBeanNet(GroupValidationContext)} with
     * the current context set.
     */
    protected class Jsr303ValidationCallback implements ValidationHelper.ValidateCallback {

        private final GroupValidationContext<?> context;

        public Jsr303ValidationCallback(GroupValidationContext<?> context) {
            this.context = context;
        }

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
     * Implement {@link #validateProperty(Object, String, boolean, Class...)} and
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
            final MetaBean initialMetaBean = new DynamicMetaBean(getMetaBeanFinder());
            initialMetaBean.setBeanClass(beanType);
            GroupValidationContext<T> context = createContext(initialMetaBean, object, beanType, groups);
            ValidationContextTraversal contextTraversal = createValidationContextTraversal(context);
            PathNavigation.navigate(propertyName, contextTraversal);

            MetaProperty prop = context.getMetaProperty();
            boolean fixed = false;
            if (value != VALIDATE_PROPERTY) {
                assert !context.getPropertyPath().isRootPath();
                if (prop == null && value != null) {
                    context.setMetaBean(getMetaBeanFinder().findForClass(value.getClass()));
                }
                if (!cascade) {
                    //TCK doesn't care what type a property is if there are no constraints to validate:
                    FeaturesCapable meta = prop == null ? context.getMetaBean() : prop;
                    if (ArrayUtils.isEmpty(meta.getValidations())) {
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
