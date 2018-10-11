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

import java.lang.reflect.Method;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

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

import org.apache.bval.jsr.util.LookBehindRegexHolder;

// ELProcessor or JavaEE 7 would be perfect too but this impl can be used in javaee 6
public final class ELFacade implements MessageEvaluator {
    private enum EvaluationType {
        IMMEDIATE("\\$"), DEFERRED("#");

        /**
         * {@link LookBehindRegexHolder} to recognize a non-escaped EL
         * expression of this evaluation type, hallmarked by a trigger
         * character.
         */
        private final LookBehindRegexHolder regex;

        private EvaluationType(String trigger) {
            this.regex = new LookBehindRegexHolder(
                String.format("(?<!(?:^|[^\\\\])(?:\\\\\\\\){0,%%d}\\\\)%s\\{", trigger), n -> (n - 3) / 2);
        }
    }

    private static final ELResolver RESOLVER = initResolver();

    private final ExpressionFactory expressionFactory = ExpressionFactory.newInstance();

    @Override
    public String interpolate(final String message, final Map<String, Object> annotationParameters,
        final Object validatedValue) {
        try {
            if (EvaluationType.IMMEDIATE.regex.matcher(message).find()) {
                final BValELContext context = new BValELContext();
                final VariableMapper variables = context.getVariableMapper();
                annotationParameters.forEach(
                    (k, v) -> variables.setVariable(k, expressionFactory.createValueExpression(v, Object.class)));

                variables.setVariable("validatedValue",
                    expressionFactory.createValueExpression(validatedValue, Object.class));

                // Java Bean Validation does not support EL expressions that look like JSP "deferred" expressions
                return expressionFactory.createValueExpression(context,
                    EvaluationType.DEFERRED.regex.matcher(message).replaceAll("\\$0"), String.class).getValue(context)
                    .toString();
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

    private class BValELContext extends ELContext {
        private final FunctionMapper functions = new BValFunctionMapper();
        private final VariableMapper variables = new BValVariableMapper();

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

    private class BValVariableMapper extends VariableMapper {
        private final Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();

        @Override
        public ValueExpression resolveVariable(final String variable) {
            if ("formatter".equals(variable)) {
                return expressionFactory.createValueExpression(new BValFormatter(), Object.class);
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

        public Formatter format(final String format, final Object... args) {
            return formatter.format(format, args);
        }
    }
}
