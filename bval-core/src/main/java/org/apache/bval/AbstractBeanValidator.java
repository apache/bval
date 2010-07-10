// TODO: Remove
///**
// *  Licensed to the Apache Software Foundation (ASF) under one or more
// *  contributor license agreements.  See the NOTICE file distributed with
// *  this work for additional information regarding copyright ownership.
// *  The ASF licenses this file to You under the Apache License, Version 2.0
// *  (the "License"); you may not use this file except in compliance with
// *  the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// */
//package org.apache.bval;
//
//import org.apache.bval.model.MetaProperty;
//import org.apache.bval.model.Validation;
//import org.apache.bval.model.ValidationContext;
//import org.apache.bval.model.ValidationListener;
//
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//
///**
// * Description: <br>
// * User: roman.stumm<br>
// * Date: 18.06.2010<br>
// * Time: 11:25:26<br>
// * viaboxx GmbH, 2010
// */
//public abstract class AbstractBeanValidator {
//  private boolean treatMapsLikeBeans = false;
//
//  /**
//   * Behavior configuration -
//   * <pre>
//   * parameter: treatMapsLikeBeans - true (validate maps like beans, so that
//   *                             you can use Maps to validate dynamic classes or
//   *                             beans for which you have the MetaBean but no instances)
//   *                           - false (default), validate maps like collections
//   *                             (validating the values only)
//   * </pre>
//   * (is still configuration to better in BeanValidationContext?)
//   */
//  public boolean isTreatMapsLikeBeans() {
//    return treatMapsLikeBeans;
//  }
//
//  public void setTreatMapsLikeBeans(boolean treatMapsLikeBeans) {
//    this.treatMapsLikeBeans = treatMapsLikeBeans;
//  }
//
//  /**
//   * validate a single bean only. no related beans will be validated
//   */
//  public <VL extends ValidationListener> void validateBean(ValidationContext<VL> context) {
//    // execute all property level validations
//    for (MetaProperty prop : context.getMetaBean().getProperties()) {
//      context.setMetaProperty(prop);
//      validateProperty(context);
//    }
//    
//    // execute all bean level validations
//    context.setMetaProperty(null);
//    for (Validation validation : context.getMetaBean().getValidations()) {
//      validation.validate(context);
//    }
//  }
//
//
//  /**
//   * validate a single property only. performs all validations
//   * for this property.
//   */
//  public <VL extends ValidationListener> void validateProperty(ValidationContext<VL> context) {
//    for (Validation validation : context.getMetaProperty().getValidations()) {
//      validation.validate(context);
//    }
//  }
//
//  protected <VL extends ValidationListener> void validateBeanInContext(ValidationContext<VL> context) {
//    if (getDynamicMetaBean(context) != null) {
//      context.setMetaBean(
//          getDynamicMetaBean(context).resolveMetaBean(context.getBean()));
//    }
//    validateBeanNet(context);
//  }
//
//  /**
//   * Iterates the values of an array, setting the current context
//   * appropriately and validating each value.
//   * 
//   * @param <VL>
//   * @param context
//   *            The validation context, its current bean must be an array.
//   */
//  protected <VL extends ValidationListener> void validateArrayInContext(ValidationContext<VL> context) {
//    int index = 0;
//    DynamicMetaBean dyn = getDynamicMetaBean(context);
//    for (Object each : ((Object[]) context.getBean())) {
//      context.setCurrentIndex(index++);
//      if (each == null) continue; // or throw IllegalArgumentException? (=> spec)
//      if (dyn != null) {
//        context.setBean(each, dyn.resolveMetaBean(each));
//      } else {
//        context.setBean(each);
//      }
//      validateBeanNet(context);
//    }
//  }
//
//  /**
//   * Iterates the values of an {@link Iterable} object, setting the current
//   * context appropriately and validating each value.
//   * 
//   * @param <VL>
//   * @param context The validation context, its current bean must implement
//   *            {@link Iterable}.
//   */
//  protected <VL extends ValidationListener> void validateIterableInContext(ValidationContext<VL> context) {
//      
//    final boolean positional = context.getBean() instanceof List<?>;
//    int index = 0;
//    context.setCurrentIndex(null);
//    
//    // jsr303 spec: Each object provided by the iterator is validated.
//    final DynamicMetaBean dyn = getDynamicMetaBean(context);
//    for ( Object each : (Iterable<?>) context.getBean() ) {
//      if ( positional ) {
//          context.setCurrentIndex(index++);
//      }
//      if (each == null) {
//          continue; // Null values are not validated
//      }
//      if (dyn != null) {
//          context.setBean(each, dyn.resolveMetaBean(each));
//      } else {
//          context.setBean(each);
//      }
//      validateBeanNet(context);
//    }
//  }
//
//  /**
//   * Iterates the values of a {@link Map}, setting the current context
//   * appropriately and validating each value.
//   * 
//   * @param <VL>
//   * @param context
//   *            The validation context, its current bean must implement
//   *            {@link Map}.
//   */
//  @SuppressWarnings("unchecked")
//  protected <VL extends ValidationListener> void validateMapInContext(ValidationContext<VL> context) {
//    // jsr303 spec: For Map, the value of each Map.Entry is validated (key is not validated).
//    Iterator<Map.Entry<Object, Object>> it = ((Map<Object, Object>) context.getBean()).entrySet().iterator();
//    final DynamicMetaBean dyn = getDynamicMetaBean(context);
//    while (it.hasNext()) { // to Many
//      Map.Entry<Object, Object> entry = it.next();
//      context.setCurrentKey(entry.getKey());
//      if (entry.getValue() == null) {
//        continue; // Null values are not validated
//      }
//      if (dyn != null) {
//        context.setBean(entry.getValue(), dyn.resolveMetaBean(entry.getValue()));
//      } else {
//        context.setBean(entry.getValue());
//      }
//      validateBeanNet(context);
//    }
//  }
//
//  private <VL extends ValidationListener> DynamicMetaBean getDynamicMetaBean(ValidationContext<VL> context) {
//    return context.getMetaBean() instanceof DynamicMetaBean ?
//        (DynamicMetaBean) context.getMetaBean() : null;
//  }
//
//  /**
//   * internal validate a bean (=not a collection of beans) and its related beans
//   */
//  protected abstract <VL extends ValidationListener> void validateBeanNet(ValidationContext<VL> context);
//
//
//  /**
//   * validate a complex 'bean' with related beans according to
//   * validation rules in 'metaBean'
//   *
//   * @param context - the context is initialized with:
//   *                <br>&nbsp;&nbsp;bean - the root object start validation at
//   *                or a collection of root objects
//   *                <br>&nbsp;&nbsp;metaBean - the meta information for the root object(s)
//   * @param context The current validation context.
//   * @return a new instance of validation results
//   *         <p/>
//   *         Methods defined in {@link BeanValidator} take care of setting the path
//   *         and current bean correctly and call
//   *         {@link #validateBeanNet(ValidationContext)} for each individual bean.
//   */
//  protected void validateContext(ValidationContext<?> context) {
//    if (context.getBean() != null) {
//      if (!treatMapsLikeBeans && context.getBean() instanceof Map<?, ?>) {
//        validateMapInContext(context);
//      } else if (context.getBean() instanceof Iterable<?>) {
//        validateIterableInContext(context);
//      } else if (context.getBean() instanceof Object[]) {
//        validateArrayInContext(context);
//      } else { // to One Bean (or Map like Bean)
//        validateBeanInContext(context);
//      }
//    }
//  }  
//}
