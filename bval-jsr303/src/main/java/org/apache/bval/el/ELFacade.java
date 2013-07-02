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
package org.apache.bval.el;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.PropertyNotWritableException;
import javax.el.ResourceBundleELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import java.lang.reflect.Method;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

// ELProcessor or JavaEE 7 would be perfect too but this impl can be used in javaee 6
public final class ELFacade implements MessageEvaluator {
    private static final ExpressionFactory EXPRESSION_FACTORY;
    static {
        ExpressionFactory ef;
        try {
            ef = ExpressionFactory.newInstance();
        } catch (final Exception e) {
            ef = null;
        }
        EXPRESSION_FACTORY = ef;
    }

    public String interpolate(final String message, final Map<String, Object> annotationParameters, final Object validatedValue) {
        final ELResolver resolver = initResolver();
        final BValELContext context = new BValELContext(resolver);
        final VariableMapper variables = context.getVariableMapper();
        for (final Map.Entry<String, Object> var : annotationParameters.entrySet()) {
            variables.setVariable(var.getKey(), new ValueExpressionLiteral(var.getValue()));
        }
        variables.setVariable("validatedValue", new ValueExpressionLiteral(validatedValue));

        try {
            if (EXPRESSION_FACTORY != null) {
                // #{xxx} shouldn't be evaluated
                return EXPRESSION_FACTORY.createValueExpression(context, message.replace("#{", "\\#{"), String.class).getValue(context).toString();
            }
        } catch (final Exception e) {
            // no-op
        }

        return message;
    }

    private static ELResolver initResolver() {
        final CompositeELResolver resolver = new CompositeELResolver();
        resolver.add(new MapELResolver());
        resolver.add(new ListELResolver());
        resolver.add(new ArrayELResolver());
        resolver.add(new ResourceBundleELResolver());
        resolver.add(new BeanELResolver());
        return resolver;
    }

    private static class BValELContext extends ELContext {
        private final ELResolver resolver;
        private final FunctionMapper functions;
        private final VariableMapper variables;

        public BValELContext(final ELResolver resolver) {
            this.resolver = resolver;
            this.variables = new BValVariableMapper();
            this.functions = new BValFunctionMapper();
        }

        @Override
        public ELResolver getELResolver() {
            return resolver;
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return functions;
        }

        @Override
        public VariableMapper getVariableMapper() {
            return variables;
        }
    }

    private static class BValFunctionMapper extends FunctionMapper {
        @Override
        public Method resolveFunction(final String prefix, final String localName) {
            return null;
        }
    }

    private static class BValVariableMapper extends VariableMapper {
        private final Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();

        @Override
        public ValueExpression resolveVariable(final String variable) {
            if ("formatter".equals(variable)) {
                return new ValueExpressionLiteral(new BValFormatter());
            }
            return variables.get(variable);
        }

        @Override
        public ValueExpression setVariable(final String variable, final ValueExpression expression) {
            variables.put(variable, expression);
            return expression;
        }
    }

    // used to not expose all method and avoid ambiguity with format(Local, String, Object...) in EL
    public static class BValFormatter {
        private final Formatter formatter = new Formatter();

        public Formatter format(final String format, final Object ... args) {
            return formatter.format(format, args);
        }
    }

    private static final class ValueExpressionLiteral extends ValueExpression {
        private final Object value;

        public ValueExpressionLiteral(final Object value) {
            this.value = value;
        }

        @Override
        public Object getValue(final ELContext context) {
            return value;
        }

        @Override
        public void setValue(final ELContext context, final Object value) {
            throw new PropertyNotWritableException(value.toString());
        }

        @Override
        public boolean isReadOnly(final ELContext context) {
            return true;
        }

        @Override
        public Class<?> getType(final ELContext context) {
            return (this.value != null) ? this.value.getClass() : null;
        }

        @Override
        public Class<?> getExpectedType() {
            return String.class;
        }

        @Override
        public String getExpressionString() {
            return (this.value != null) ? this.value.toString() : null;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof ValueExpressionLiteral && this
                    .equals((ValueExpressionLiteral) obj));
        }

        public boolean equals(final ValueExpressionLiteral ve) {
            return (ve != null && (this.value != null && ve.value != null && (this.value == ve.value || this.value
                    .equals(ve.value))));
        }

        @Override
        public int hashCode() {
            return (this.value != null) ? this.value.hashCode() : 0;
        }

        @Override
        public boolean isLiteralText() {
            return true;
        }
    }
}
