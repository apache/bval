/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.bval.jsr.util;

import org.apache.bval.DynamicMetaBean;
import org.apache.bval.jsr.JsrMetaBeanFactory;
import org.apache.bval.jsr.UnknownPropertyException;
import org.apache.bval.jsr.util.PathNavigation.CallbackProcedure;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.model.ValidationContext;
import org.apache.bval.util.AccessStrategy;
import org.apache.bval.util.IndexedAccess;
import org.apache.bval.util.KeyedAccess;
import org.apache.bval.util.PropertyAccess;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.annotation.ElementType;
import java.lang.reflect.Type;

/**
 * {@link ValidationContext} traversal {@link CallbackProcedure}.
 * 
 * @version $Rev: 1137074 $ $Date: 2011-06-17 18:20:30 -0500 (Fri, 17 Jun 2011) $
 */
public class ValidationContextTraversal extends CallbackProcedure {
    private static class NullSafePropertyAccess extends AccessStrategy {
        private final PropertyAccess wrapped;

        /**
         * Create a new NullSafePropertyAccess instance.
         * 
         * @param clazz
         * @param propertyName
         */
        public NullSafePropertyAccess(Class<?> clazz, String propertyName) {
            wrapped = PropertyAccess.getInstance(clazz, propertyName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object get(Object bean) {
            return bean == null ? null : wrapped.get(bean);
        }

        @Override
        public ElementType getElementType() {
            return wrapped.getElementType();
        }

        @Override
        public Type getJavaType() {
            return wrapped.getJavaType();
        }

        @Override
        public String getPropertyName() {
            return wrapped.getPropertyName();
        }
    }

    private final ValidationContext<?> validationContext;
    private Type type;
    private Class<?> rawType;

    /**
     * Create a new {@link ValidationContextTraversal} instance.
     * 
     * @param validationContext
     */
    public ValidationContextTraversal(ValidationContext<?> validationContext) {
        this.validationContext = validationContext;
        init();
    }

    /**
     * Initialize from {@link ValidationContext}.
     */
    public void init() {
        this.rawType = validationContext.getMetaBean().getBeanClass();
        this.type = this.rawType;
    }

    /**
     * {@inheritDoc}
     */
    public void handleIndexOrKey(String token) {
        moveDownIfNecessary();

        AccessStrategy access;
        if (IndexedAccess.getJavaElementType(type) != null) {
            try {
                Integer index = token == null ? null : Integer.valueOf(token);
                access = new IndexedAccess(type, index);
                validationContext.setCurrentIndex(index);
            } catch (NumberFormatException e) {
                throw new UnknownPropertyException(String.format("Cannot parse %s as an array/iterable index", token),
                    e);
            }
        } else if (KeyedAccess.getJavaElementType(type) != null) {
            access = new KeyedAccess(type, token);
            validationContext.setCurrentKey(token);
        } else {
            throw new UnknownPropertyException(String.format("Cannot determine index/key type for %s", type));
        }
        Object value = validationContext.getBean();
        Object child = value == null ? null : access.get(value);
        setType(child == null ? access.getJavaType() : child.getClass());
        validationContext.setBean(child,
            validationContext.getMetaBean().resolveMetaBean(child == null ? rawType : child));
    }

    /**
     * {@inheritDoc}
     */
    public void handleProperty(String token) {
        moveDownIfNecessary();

        MetaBean metaBean = validationContext.getMetaBean();

        if (metaBean instanceof DynamicMetaBean) {
            metaBean = metaBean.resolveMetaBean(ObjectUtils.defaultIfNull(validationContext.getBean(), rawType));
            validationContext.setMetaBean(metaBean);
        }
        MetaProperty mp = metaBean.getProperty(token);
        if (mp == null) {
            // TODO this could indicate a property hosted on a superclass; should we shunt the context traversal down a path based on that type?

            PropertyAccess access = PropertyAccess.getInstance(rawType, token);
            if (access.isKnown()) {
                // add heretofore unknown, but valid, property on the fly:
                mp = JsrMetaBeanFactory.addMetaProperty(metaBean, access);
            } else {
                throw new UnknownPropertyException("unknown property '" + token + "' in " + metaBean.getId());
            }
        }
        validationContext.setMetaProperty(mp);
        setType(mp.getType());
    }

    /**
     * If we currently have a property, navigate the context such that the property becomes the bean, in preparation for
     * another property.
     * 
     * @param validationContext
     */
    public void moveDownIfNecessary() {
        MetaProperty mp = validationContext.getMetaProperty();
        if (mp != null) {
            if (mp.getMetaBean() == null) {
                throw new UnknownPropertyException(String.format("Property %s.%s is not cascaded", mp
                    .getParentMetaBean().getId(), mp.getName()));
            }
            validationContext.moveDown(mp, new NullSafePropertyAccess(validationContext.getMetaBean().getBeanClass(),
                mp.getName()));
        }
    }

    /**
     * Set the type of the expression processed thus far.
     * 
     * @param type
     */
    protected void setType(Type type) {
        this.rawType = TypeUtils.getRawType(type, this.type);
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public void handleGenericInIterable() {
        throw new UnsupportedOperationException("Cannot navigate a ValidationContext to []");
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the rawType
     */
    public Class<?> getRawType() {
        return rawType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void complete() {
        super.complete();
        if (validationContext.getMetaProperty() != null) {
            return;
        }
        if (validationContext.getMetaBean() instanceof DynamicMetaBean) {
            validationContext.setMetaBean(validationContext.getMetaBean().resolveMetaBean(
                ObjectUtils.defaultIfNull(validationContext.getBean(), rawType)));
        }
    }
}