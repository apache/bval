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
package org.apache.bval.util;

import org.apache.bval.DynamicMetaBean;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.Validation;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.model.ValidationListener;

import java.util.List;
import java.util.Map;

/**
 * Stateless helper methods used by the validators.
 * 
 * @author Carlos Vara
 */
public class ValidationHelper {

    /**
     * Interface implemented by the call-back object passed to
     * {@link ValidationHelper#validateContext(ValidationContext, ValidateCallback, boolean)}
     * . Its {@link #validate()} method will be called accordingly for every
     * dispatch.
     */
    public static interface ValidateCallback {
        void validate();
    }

    /**
     * validate a complex 'bean' with related beans according to
     * validation rules in 'metaBean'
     * 
     * @param context
     *            - the context is initialized with: <br>
     *            &nbsp;&nbsp;bean - the root object start validation at
     *            or a collection of root objects <br>
     *            &nbsp;&nbsp;metaBean - the meta information for the root
     *            object(s)
     * @param context
     *            The current validation context.
     */
    public static void validateContext(ValidationContext<?> context, ValidateCallback s, boolean treatMapsLikeBeans) {
        if (context.getBean() != null) {
            if (!treatMapsLikeBeans && context.getBean() instanceof Map<?, ?>) {
                validateMapInContext(context, s);
            } else if (context.getBean() instanceof Iterable<?>) {
                validateIterableInContext(context, s);
            } else if (context.getBean() instanceof Object[]) {
                validateArrayInContext(context, s);
            } else { // to One Bean (or Map like Bean)
                validateBeanInContext(context, s);
            }
        }
    }

    /**
     * Validates a single object.
     * 
     * @param <VL>
     * @param context
     *            The validation context, its current bean must be a single
     *            object.
     * @param s
     */
    protected static <VL extends ValidationListener> void validateBeanInContext(ValidationContext<VL> context,
        ValidateCallback s) {
        if (getDynamicMetaBean(context) != null) {
            context.setMetaBean(getDynamicMetaBean(context).resolveMetaBean(context.getBean()));
        }
        s.validate();
    }

    /**
     * Iterates the values of an array, setting the current context
     * appropriately and validating each value.
     * 
     * @param <VL>
     * @param context
     *            The validation context, its current bean must be an array.
     */
    protected static <VL extends ValidationListener> void validateArrayInContext(ValidationContext<VL> context,
        ValidateCallback s) {
        int index = 0;
        DynamicMetaBean dyn = getDynamicMetaBean(context);
        Object[] array = (Object[]) context.getBean();
        MetaBean metaBean = context.getMetaBean();
        context.setCurrentIndex(null);

        try {
            for (Object each : array) {
                context.setCurrentIndex(index++);
                if (each == null) {
                    continue; // Null values are not validated
                }
                if (dyn == null) {
                    context.setBean(each);
                } else {
                    context.setBean(each, dyn.resolveMetaBean(each));
                }
                s.validate();
            }
        } finally {
            context.moveUp(array, metaBean);
        }
    }

    /**
     * Iterates the values of an {@link Iterable} object, setting the current
     * context appropriately and validating each value.
     * 
     * @param <VL>
     * @param context
     *            The validation context, its current bean must implement
     *            {@link Iterable}.
     */
    protected static <VL extends ValidationListener> void validateIterableInContext(ValidationContext<VL> context,
        ValidateCallback s) {

        final boolean positional = context.getBean() instanceof List<?>;
        int index = 0;
        Iterable<?> iterable = (Iterable<?>) context.getBean();
        MetaBean metaBean = context.getMetaBean();
        context.setCurrentIndex(null);

        try {
            // jsr303 spec: Each object provided by the iterator is validated.
            final DynamicMetaBean dyn = getDynamicMetaBean(context);
            for (Object each : iterable) {
                if (positional) {
                    context.setCurrentIndex(index++);
                }
                if (each == null) {
                    continue; // Null values are not validated
                }
                if (dyn == null) {
                    context.setBean(each);
                } else {
                    context.setBean(each, dyn.resolveMetaBean(each));
                }
                s.validate();
            }
        } finally {
            context.moveUp(iterable, metaBean);
        }
    }

    /**
     * Iterates the values of a {@link Map}, setting the current context
     * appropriately and validating each value.
     * 
     * @param <VL>
     * @param context
     *            The validation context, its current bean must implement
     *            {@link Map}.
     */
    protected static <VL extends ValidationListener> void validateMapInContext(ValidationContext<VL> context,
        ValidateCallback s) {
        // jsr303 spec: For Map, the value of each Map.Entry is validated (key
        // is not validated).
        Map<?, ?> currentBean = (Map<?, ?>) context.getBean();
        MetaBean metaBean = context.getMetaBean();
        final DynamicMetaBean dyn = getDynamicMetaBean(context);
        context.setCurrentKey(null);
        try {
            for (Map.Entry<?, ?> entry : currentBean.entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                context.setCurrentKey(entry.getKey());
                if (dyn == null) {
                    context.setBean(value);
                } else {
                    context.setBean(value, dyn.resolveMetaBean(value));
                }
                s.validate();
            }
        } finally {
            context.moveUp(currentBean, metaBean);
        }
    }

    /**
     * @param <VL>
     * @param context
     *            The current validation context.
     * @return the current {@link DynamicMetaBean} in context, or
     *         <code>null</code> if the current meta bean is not dynamic.
     */
    private static <VL extends ValidationListener> DynamicMetaBean getDynamicMetaBean(ValidationContext<VL> context) {
        return context.getMetaBean() instanceof DynamicMetaBean ? (DynamicMetaBean) context.getMetaBean() : null;
    }

    /**
     * Validate a single bean only, no related beans will be validated.
     */
    public static <VL extends ValidationListener> void validateBean(ValidationContext<VL> context) {
        // execute all property level validations
        for (MetaProperty prop : context.getMetaBean().getProperties()) {
            context.setMetaProperty(prop);
            validateProperty(context);
        }

        // execute all bean level validations
        context.setMetaProperty(null);
        for (Validation validation : context.getMetaBean().getValidations()) {
            validation.validate(context);
        }
    }

    /**
     * Validate a single property only. Performs all validations
     * for this property.
     */
    public static <VL extends ValidationListener> void validateProperty(ValidationContext<VL> context) {
        for (Validation validation : context.getMetaProperty().getValidations()) {
            validation.validate(context);
        }
    }
}
