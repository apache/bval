/**
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
package org.apache.bval;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.bval.model.Features;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.Validation;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.PropertyAccess;

/**
 * Description: Top-Level API-class to validate objects or object-trees.
 * You can invoke, extend or utilize this class if you need other ways to integrate
 * validation in your application.
 * <p/>
 * This class supports cyclic object graphs by keeping track of
 * validated instances in the validation context.<br/>
 */
public class BeanValidator<T extends ValidationListener> {
    private boolean treatMapsLikeBeans = false;
    private final MetaBeanFinder metaBeanFinder;

    /**
     * convenience method. Use the global instance of MetaBeanManagerFactory.getFinder().
     */
    public BeanValidator() {
        this(MetaBeanManagerFactory.getFinder());
    }

    public BeanValidator(MetaBeanFinder metaBeanFinder) {
        this.metaBeanFinder = metaBeanFinder;
    }

    /**
     * Behavior configuration -
     * <pre>
     * parameter: treatMapsLikeBeans - true (validate maps like beans, so that
     *                             you can use Maps to validate dynamic classes or
     *                             beans for which you have the MetaBean but no instances)
     *                           - false (default), validate maps like collections
     *                             (validating the values only)
     * </pre>
     * (is still configuration to better in BeanValidationContext?)
     */
    public boolean isTreatMapsLikeBeans() {
        return treatMapsLikeBeans;
    }

    public void setTreatMapsLikeBeans(boolean treatMapsLikeBeans) {
        this.treatMapsLikeBeans = treatMapsLikeBeans;
    }

    /**
     * convenience API. validate a root object with all related objects
     * with its default metaBean definition.
     *
     * @return results - validation results found
     */
    public T validate(Object bean) {
        MetaBean metaBean =
              getMetaBeanFinder().findForClass(bean.getClass());
        return validate(bean, metaBean);
    }

    /**
     * convenience API. validate a root object with all related objects
     * according to the metaBean.
     *
     * @param bean - a single bean or a collection of beans (that share the same metaBean!)
     * @return results - validation results found
     */
    public T validate(Object bean, MetaBean metaBean) {
        ValidationContext<T> context = createContext();
        context.setBean(bean, metaBean);
        validateContext(context);
        return context.getListener();
    }

    /**
     * validate the method parameters based on @Validate annotations.
     * Requirements:
     * Parameter, that are to be validated must be annotated with @Validate
     *
     * @param method     -  a method
     * @param parameters - the parameters suitable to the method
     * @return a validation result or null when there was nothing to validate
     * @see Validate
     */
    public T validateCall(Method method, Object[] parameters) {
        if (parameters.length > 0) {
            // shortcut (for performance!)
            Annotation[][] annotations = method.getParameterAnnotations();
            ValidationContext<T> context = null;
            for (int i = 0; i < parameters.length; i++) {
                for (Annotation anno : annotations[i]) {
                    if (anno instanceof Validate) {
                        if(context == null) context = createContext();
                        if (determineMetaBean((Validate) anno, parameters[i], context)) {
                            validateContext(context);
                            break; // next parameter
                        }
                    }
                }
            }
            return context != null ? context.getListener() : null;
        }
        return null;
    }

    /** @return true when validation should happen, false to skip it */
    protected <VL extends ValidationListener> boolean determineMetaBean(Validate validate, Object parameter,
                                        ValidationContext<VL> context) {
        if (validate.value().length() == 0) {
            if (parameter == null) return false;
            Class<?> beanClass;
            if (parameter instanceof Collection<?>) {   // do not validate empty collection
                Collection<?> coll = ((Collection<?>) parameter);
                if (coll.isEmpty()) return false;
                beanClass = coll.iterator().next().getClass(); // get first object
            } else if (parameter.getClass().isArray()) {
                beanClass = parameter.getClass().getComponentType();
            } else {
                beanClass = parameter.getClass();
            }
            context.setBean(parameter,getMetaBeanFinder().findForClass(beanClass));
        } else {
            context.setBean(parameter,getMetaBeanFinder().findForId(validate.value()));
        }
        return true;
    }

    /**
     * factory method -
     * overwrite in subclasses
     */
    protected T createResults() {
        return (T) new ValidationResults();
    }

    /**
     * factory method -
     * overwrite in subclasses
     */
    protected ValidationContext<T> createContext() {
        return new BeanValidationContext<T>(createResults());
    }

    /**
     * convenience API. validate a single property.
     *
     * @param bean         - the root object
     * @param metaProperty - metadata for the property
     * @return validation results
     */
    public T validateProperty(Object bean, MetaProperty metaProperty) {
        ValidationContext<T> context = createContext();
        context.setBean(bean);
        context.setMetaProperty(metaProperty);
        validateProperty(context);
        return context.getListener();
    }

    /**
     * validate a single property only. performs all validations
     * for this property.
     */
    public <VL extends ValidationListener> void validateProperty(ValidationContext<VL> context) {
        for (Validation validation : context.getMetaProperty().getValidations()) {
            validation.validate(context);
        }
    }

    /**
     * validate a complex 'bean' with related beans according to
     * validation rules in 'metaBean'
     *
     * @param context - the context is initialized with:
     *                <br>&nbsp;&nbsp;bean - the root object start validation at
     *                or a collection of root objects
     *                <br>&nbsp;&nbsp;metaBean - the meta information for the root object(s)
     * @return a new instance of validation results
     */
    public <VL extends ValidationListener> void validateContext(ValidationContext<VL> context) {
        if (context.getBean() != null) {
            if (!treatMapsLikeBeans && context.getBean() instanceof Map<?, ?>) {
                validateMapInContext(context);
            } else if (context.getBean() instanceof Iterable<?>) {
                validateIteratableInContext(context);
            } else if (context.getBean() instanceof Object[]) {
                validateArrayInContext(context);
            } else { // to One Bean (or Map like Bean) 
                validateBeanInContext(context);
            }
        }
    }

    private <VL extends ValidationListener> void validateBeanInContext(ValidationContext<VL> context) {
        if (getDynamicMetaBean(context) != null) {
            context.setMetaBean(
                  getDynamicMetaBean(context).resolveMetaBean(context.getBean()));
        }
        validateBeanNet(context);
    }

    private <VL extends ValidationListener> void validateArrayInContext(ValidationContext<VL> context) {
        int index = 0;
        DynamicMetaBean dyn = getDynamicMetaBean(context);
        for (Object each : ((Object[]) context.getBean())) {
            context.setCurrentIndex(index++);
            if (each == null) continue; // or throw IllegalArgumentException? (=> spec)
            if (dyn != null) {
                context.setBean(each, dyn.resolveMetaBean(each));
            } else {
                context.setBean(each);
            }
            validateBeanNet(context);
        }
    }

    private <VL extends ValidationListener> DynamicMetaBean getDynamicMetaBean(ValidationContext<VL> context) {
        return context.getMetaBean() instanceof DynamicMetaBean ?
              (DynamicMetaBean) context.getMetaBean() : null;
    }

    /** Any object implementing java.lang.Iterable is supported */
    private <VL extends ValidationListener> void validateIteratableInContext(ValidationContext<VL> context) {
        Iterator<?> it = ((Iterable<?>) context.getBean()).iterator();
        int index = 0;
        // jsr303 spec: Each object provided by the iterator is validated.
        final DynamicMetaBean dyn = getDynamicMetaBean(context);
        while (it.hasNext()) { // to Many
            Object each = it.next();
            context.setCurrentIndex(index++);
            if (each == null)
                continue; // enhancement: throw IllegalArgumentException? (=> spec)
            if (dyn != null) {
                context.setBean(each, dyn.resolveMetaBean(each));
            } else {
                context.setBean(each);
            }
            validateBeanNet(context);
        }
    }

    private <VL extends ValidationListener> void validateMapInContext(ValidationContext<VL> context) {
        // jsr303 spec: For Map, the value of each Map.Entry is validated (key is not validated).
        Iterator<Map.Entry<Object, Object>> it = ((Map<Object, Object>) context.getBean()).entrySet().iterator();
        final DynamicMetaBean dyn = getDynamicMetaBean(context);
        while (it.hasNext()) { // to Many
            Map.Entry<Object, Object> entry = it.next();
            context.setCurrentKey(entry.getKey());
            if (entry.getValue() == null)
                continue; // enhancement: throw IllegalArgumentException? (=> spec)
            if (dyn != null) {
                context.setBean(entry.getValue(), dyn.resolveMetaBean(entry.getValue()));
            } else {
                context.setBean(entry.getValue());
            }
            validateBeanNet(context);
        }
    }

    /** internal validate a bean (=not a collection of beans) and its related beans */
    protected <VL extends ValidationListener> void validateBeanNet(ValidationContext<VL> context) {
        if (context.collectValidated()) {
            validateBean(context);
            for (MetaProperty prop : context.getMetaBean().getProperties()) {
                validateRelatedBean(context, prop);
            }
        }
    }

    private <VL extends ValidationListener> void validateRelatedBean(ValidationContext<VL> context, MetaProperty prop) {
        AccessStrategy[] access = prop.getFeature(Features.Property.REF_CASCADE);
        if (access == null && prop.getMetaBean() != null) { // single property access strategy
            // save old values from context
            final Object bean = context.getBean();
            final MetaBean mbean = context.getMetaBean();
            // modify context state for relationship-target bean
            context.moveDown(prop, new PropertyAccess(bean.getClass(), prop.getName()));
            validateContext(context);
            // restore old values in context
            context.moveUp(bean, mbean);
        } else if (access != null) { // different accesses to relation
            // save old values from context
            final Object bean = context.getBean();
            final MetaBean mbean = context.getMetaBean();
            for (AccessStrategy each : access) {
                // modify context state for relationship-target bean
                context.moveDown(prop, each);
                validateContext(context);
                // restore old values in context
                context.moveUp(bean, mbean);
            }
        }
    }

    /** validate a single bean only. no related beans will be validated */
    public <VL extends ValidationListener> void validateBean(ValidationContext<VL> context) {
        /**
         * execute all property level validations
         */
        for (MetaProperty prop : context.getMetaBean().getProperties()) {
            context.setMetaProperty(prop);
            validateProperty(context);
        }
        /**
         * execute all bean level validations
         */
        context.setMetaProperty(null);
        for (Validation validation : context.getMetaBean().getValidations()) {
            validation.validate(context);
        }
    }

    /**
     * the metabean finder associated with this validator.
     * @see org.apache.bval.MetaBeanManagerFactory#getFinder() 
     * @return a MetaBeanFinder
     */
    public MetaBeanFinder getMetaBeanFinder() {
        return metaBeanFinder;
    }
}
