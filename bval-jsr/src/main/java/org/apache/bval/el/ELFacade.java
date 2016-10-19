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
    private static final ELResolver RESOLVER = initResolver();

    @Override
    public String interpolate(final String message, final Map<String, Object> annotationParameters, final Object validatedValue) {
        try {
            if (EXPRESSION_FACTORY != null) {
                final BValELContext context = new BValELContext();
                final VariableMapper variables = context.getVariableMapper();
                for (final Map.Entry<String, Object> var : annotationParameters.entrySet()) {
                    variables.setVariable(var.getKey(), EXPRESSION_FACTORY.createValueExpression(var.getValue(), Object.class));
                }
                variables.setVariable("validatedValue", EXPRESSION_FACTORY.createValueExpression(validatedValue, Object.class));

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
        private final FunctionMapper functions;
        private final VariableMapper variables;

        public BValELContext() {
            this.variables = new BValVariableMapper();
            this.functions = new BValFunctionMapper();
        }

        @Override
        public ELResolver getELResolver() {
            return RESOLVER;
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
                return EXPRESSION_FACTORY.createValueExpression(new BValFormatter(), Object.class);
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
}
