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

import org.apache.bval.model.*;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.PropertyAccess;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Description: Top-Level API-class to validate objects or object-trees.
 * You can invoke, extend or utilize this class if you need other ways to integrate
 * validation in your application.
 * <p/>
 * This class supports cyclic object graphs by keeping track of
 * validated instances in the validation context.<br/>
 */
public class BeanValidator<T extends ValidationListener> extends AbstractBeanValidator {
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
            if (context == null) context = createContext();
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

  /**
   * @return true when validation should happen, false to skip it
   */
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
      context.setBean(parameter, getMetaBeanFinder().findForClass(beanClass));
    } else {
      context.setBean(parameter, getMetaBeanFinder().findForId(validate.value()));
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
   * internal validate a bean (=not a collection of beans) and its related beans
   */
  protected <VL extends ValidationListener> void validateBeanNet(ValidationContext<VL> context) {
    if (context.collectValidated()) {
      validateBean(context);
      for (MetaProperty prop : context.getMetaBean().getProperties()) {
        validateRelatedBean(context, prop);
      }
    }
  }

  protected <VL extends ValidationListener> void validateRelatedBean(ValidationContext<VL> context, MetaProperty prop) {
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

  /**
   * the metabean finder associated with this validator.
   *
   * @return a MetaBeanFinder
   * @see org.apache.bval.MetaBeanManagerFactory#getFinder()
   */
  public MetaBeanFinder getMetaBeanFinder() {
    return metaBeanFinder;
  }
}
