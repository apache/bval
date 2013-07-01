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
package org.apache.bval.jsr303;

import org.apache.bval.jsr303.util.SecureActions;
import org.apache.commons.lang3.ArrayUtils;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.MethodNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.el.ResourceBundleELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.validation.MessageInterpolator;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description: Resource bundle backed message interpolator.
 * This message resolver resolve message descriptors
 * into human-readable messages. It uses ResourceBundles to find the messages.
 * This class is threadsafe.<br/>
 */
public class DefaultMessageInterpolator implements MessageInterpolator {
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

    private static final Logger log = Logger.getLogger(DefaultMessageInterpolator.class.getName());
    private static final String DEFAULT_VALIDATION_MESSAGES = "org.apache.bval.jsr303.ValidationMessages";
    private static final String USER_VALIDATION_MESSAGES = "ValidationMessages";

    /** Regular expression used to do message interpolation. */
    private static final Pattern messageParameterPattern = Pattern.compile("(\\{[\\w\\.]+\\})");

    /** The default locale for the current user. */
    private Locale defaultLocale;

    /** User specified resource bundles hashed against their locale. */
    private final Map<Locale, ResourceBundle> userBundlesMap =
          new ConcurrentHashMap<Locale, ResourceBundle>();

    /** Builtin resource bundles hashed against their locale. */
    private final Map<Locale, ResourceBundle> defaultBundlesMap = new ConcurrentHashMap<Locale, ResourceBundle>();

    /**
     * Create a new DefaultMessageInterpolator instance.
     */
    public DefaultMessageInterpolator() {
        this(null);
    }

    /**
     * Create a new DefaultMessageInterpolator instance.
     * @param resourceBundle
     */
    public DefaultMessageInterpolator(ResourceBundle resourceBundle) {
        defaultLocale = Locale.getDefault();

        if (resourceBundle == null) {
            ResourceBundle bundle = getFileBasedResourceBundle(defaultLocale);
            if (bundle != null) {
                userBundlesMap.put(defaultLocale, bundle);
            }

        } else {
            userBundlesMap.put(defaultLocale, resourceBundle);
        }

        defaultBundlesMap.put(defaultLocale,
              ResourceBundle.getBundle(DEFAULT_VALIDATION_MESSAGES, defaultLocale));
    }

    private static ELResolver initResolver() {
        final CompositeELResolver resolver = new CompositeELResolver();
        resolver.add(new MapELResolver());
        resolver.add(new ListELResolver());
        resolver.add(new ArrayELResolver());
        resolver.add(new ResourceBundleELResolver());
        resolver.add(new BeanELResolver() {
            @Override // patched because G l API contains a bug with array used by formatter usage, when fixed just update the API and remove this method
            public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
                if (context == null) {
                    throw new NullPointerException("ELContext could not be nulll");
                }
                // Why static invocation is not supported
                if(base == null || method == null) {
                    return null;
                }
                if (params == null) {
                    params = new Object[0];
                }
                String methodName = (String) EXPRESSION_FACTORY.coerceToType(method, String.class);
                if (methodName.length() == 0) {
                    throw new MethodNotFoundException("The parameter method could not be zero-length");
                }
                Class<?> targetClass = base.getClass();
                if (methodName.equals("<init>") || methodName.equals("<cinit>")) {
                    throw new MethodNotFoundException(method + " is not found in target class " + targetClass.getName());
                }
                Method targetMethod = null;
                if (paramTypes == null) {
                    int paramsNumber = params.length;
                    for (final Method m : targetClass.getMethods()) {
                        if (m.getName().equals(methodName) && m.getParameterTypes().length == paramsNumber) {
                            targetMethod = m;
                            break;
                        }
                    }
                    if (targetMethod == null) {
                        for (final Method m : targetClass.getMethods()) {
                            if (m.getName().equals(methodName) && m.isVarArgs() && paramsNumber >= (m.getParameterTypes().length - 1)) {
                                targetMethod = m;
                                break;
                            }
                        }
                    }
                } else {
                    try {
                        targetMethod = targetClass.getMethod(methodName, paramTypes);
                    } catch (SecurityException e) {
                        throw new ELException(e);
                    } catch (NoSuchMethodException e) {
                        throw new MethodNotFoundException(e);
                    }
                }
                if (targetMethod == null) {
                    throw new MethodNotFoundException(method + " is not found in target class " + targetClass.getName());
                }
                if (paramTypes == null) {
                    paramTypes = targetMethod.getParameterTypes();
                }
                //Initial check whether the types and parameter values length
                if (targetMethod.isVarArgs()) {
                    if (paramTypes.length - 1 > params.length) {
                        throw new IllegalArgumentException("Inconsistent number between argument types and values");
                    }
                } else if (paramTypes.length != params.length) {
                    throw new IllegalArgumentException("Inconsistent number between argument types and values");
                }
                try {
                    Object[] finalParamValues = new Object[paramTypes.length];
                    //Only do the parameter conversion while the method is not a non-parameter one
                    if (paramTypes.length > 0) {
                        int iCurrentIndex = 0;
                        for (int iLoopSize = paramTypes.length - 1; iCurrentIndex < iLoopSize; iCurrentIndex++) {
                            finalParamValues[iCurrentIndex] = EXPRESSION_FACTORY.coerceToType(params[iCurrentIndex], paramTypes[iCurrentIndex]);
                        }
                        /**
                         * Not sure it is over-designed. Do not find detailed description about how the parameter values are passed if the method is of variable arguments.
                         * It might be an array directly passed or each parameter value passed one by one.
                         */
                        if (targetMethod.isVarArgs()) {
                            // varArgsClassType should be an array type
                            Class<?> varArgsClassType = paramTypes[iCurrentIndex];
                            // 1. If there is no parameter value left for the variable argument, create a zero-length array
                            // 2. If there is only one parameter value left for the variable argument, and it has the same array type with the varArgsClass, pass in directly
                            // 3. Else, create an array of varArgsClass type, and add all the left coerced parameter values
                            if (iCurrentIndex == params.length) {
                                finalParamValues[iCurrentIndex] = Array.newInstance(varArgsClassType.getComponentType(), 0);
                            } else if (iCurrentIndex == params.length - 1 && varArgsClassType == params[iCurrentIndex].getClass()
                                    && varArgsClassType.getClassLoader() == params[iCurrentIndex].getClass().getClassLoader()) {
                                finalParamValues[iCurrentIndex] = params[iCurrentIndex];
                            } else {
                                Object targetArray = Array.newInstance(varArgsClassType.getComponentType(), params.length - iCurrentIndex);
                                Class<?> componentClassType = varArgsClassType.getComponentType();
                                for (int i = 0, iLoopSize = params.length - iCurrentIndex; i < iLoopSize; i++) {
                                    Array.set(targetArray, i, EXPRESSION_FACTORY.coerceToType(params[iCurrentIndex + i], componentClassType));
                                }
                                finalParamValues[iCurrentIndex] = targetArray;
                            }
                        } else {
                            finalParamValues[iCurrentIndex] = EXPRESSION_FACTORY.coerceToType(params[iCurrentIndex], paramTypes[iCurrentIndex]);
                        }
                    }
                    Object retValue = targetMethod.invoke(base, finalParamValues);
                    context.setPropertyResolved(true);
                    return retValue;
                }  catch (IllegalAccessException e) {
                    throw new ELException(e);
                } catch (InvocationTargetException e) {
                    throw new ELException(e.getCause());
                }
            }
        });
        return resolver;
    }

    /** {@inheritDoc} */
    public String interpolate(String message, Context context) {
        // probably no need for caching, but it could be done by parameters since the map
        // is immutable and uniquely built per Validation definition, the comparison has to be based on == and not equals though
        return interpolate(message, context, defaultLocale);
    }

    /** {@inheritDoc} */
    public String interpolate(String message, Context context, Locale locale) {
        return interpolateMessage(message,
              context.getConstraintDescriptor().getAttributes(), locale, context.getValidatedValue());
    }

    /**
     * Runs the message interpolation according to algorithm specified in JSR 303.
     * <br/>
     * Note:
     * <br/>
     * Lookups in user bundles are recursive whereas lookups in default bundle are not!
     *
     * @param message              the message to interpolate
     * @param annotationParameters the parameters of the annotation for which to interpolate this message
     * @param locale               the <code>Locale</code> to use for the resource bundle.
     * @return the interpolated message.
     */
    private String interpolateMessage(String message,
                                      Map<String, Object> annotationParameters,
                                      Locale locale, Object validatedValue) {
        ResourceBundle userResourceBundle = findUserResourceBundle(locale);
        ResourceBundle defaultResourceBundle = findDefaultResourceBundle(locale);

        String userBundleResolvedMessage;
        String resolvedMessage = message;
        boolean evaluatedDefaultBundleOnce = false;
        do {
            // search the user bundle recursive (step1)
            userBundleResolvedMessage =
                  replaceVariables(resolvedMessage, userResourceBundle, locale, true);

            // exit condition - we have at least tried to validate against the default bundle and there were no
            // further replacements
            if (evaluatedDefaultBundleOnce &&
                  !hasReplacementTakenPlace(userBundleResolvedMessage, resolvedMessage)) {
                break;
            }

            // search the default bundle non recursive (step2)
            resolvedMessage = replaceVariables(userBundleResolvedMessage,
                  defaultResourceBundle, locale, false);

            evaluatedDefaultBundleOnce = true;
        } while (true);

        // resolve annotation attributes (step 4)
        resolvedMessage = replaceAnnotationAttributes(resolvedMessage, annotationParameters);

        final ELResolver resolver = initResolver();
        final BValELContext context = new BValELContext(resolver);
        final VariableMapper variables = context.getVariableMapper();
        for (final Map.Entry<String, Object> var : annotationParameters.entrySet()) {
            variables.setVariable(var.getKey(), new ValueExpressionLiteral(var.getValue()));
        }
        variables.setVariable("validatedValue", new ValueExpressionLiteral(validatedValue));

        try {
            if (EXPRESSION_FACTORY != null) {
                final String tmp = resolvedMessage.replace("#{", "\\#{"); // shouldn't be evaluated
                resolvedMessage = EXPRESSION_FACTORY.createValueExpression(context, tmp, String.class).getValue(context).toString();
            }
        } catch (final Exception e) {
            // no-op
        }

        // curly braces need to be scaped in the original msg, so unescape them now
        resolvedMessage = resolvedMessage.replace( "\\{", "{" ).replace( "\\}", "}" ).replace( "\\\\", "\\" ).replace( "\\$", "$" );

        return resolvedMessage;
    }

    private boolean hasReplacementTakenPlace(String origMessage, String newMessage) {
        return !origMessage.equals(newMessage);
    }

    /**
     * Search current thread classloader for the resource bundle. If not found, search validator (this) classloader.
     *
     * @param locale The locale of the bundle to load.
     * @return the resource bundle or <code>null</code> if none is found.
     */
    private ResourceBundle getFileBasedResourceBundle(Locale locale) {
        ResourceBundle rb = null;
        final ClassLoader classLoader = doPrivileged(SecureActions.getContextClassLoader());
        if (classLoader != null) {
            rb = loadBundle(classLoader, locale,
                  USER_VALIDATION_MESSAGES + " not found by thread local classloader");
        }

        // 2011-03-27 jw: No privileged action required.
        // A class can always access the classloader of itself and of subclasses.
        if (rb == null) {
            rb = loadBundle(
              getClass().getClassLoader(),
              locale,
              USER_VALIDATION_MESSAGES + " not found by validator classloader"
            );
        }
        if (rb != null) {
            log.log(Level.FINEST, String.format("%s found", USER_VALIDATION_MESSAGES));
        } else {
        	log.log(Level.FINEST, String.format("%s not found. Delegating to %s", USER_VALIDATION_MESSAGES, DEFAULT_VALIDATION_MESSAGES));
        }
        return rb;
    }

    private ResourceBundle loadBundle(ClassLoader classLoader, Locale locale,
                                      String message) {
        ResourceBundle rb = null;
        try {
            rb = ResourceBundle.getBundle(USER_VALIDATION_MESSAGES, locale, classLoader);
        } catch (MissingResourceException e) {
            log.fine(message);
        }
        return rb;
    }

    private String replaceVariables(String message, ResourceBundle bundle, Locale locale,
                                    boolean recurse) {
        final Matcher matcher = messageParameterPattern.matcher(message);
        final StringBuffer sb = new StringBuffer(64);
        String resolvedParameterValue;
        while (matcher.find()) {
            final String parameter = matcher.group(1);
            resolvedParameterValue = resolveParameter(parameter, bundle, locale, recurse);

            matcher.appendReplacement(sb, sanitizeForAppendReplacement(resolvedParameterValue));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String replaceAnnotationAttributes(final String message,
                                               final Map<String, Object> annotationParameters) {
        Matcher matcher = messageParameterPattern.matcher(message);
        StringBuffer sb = new StringBuffer(64);
        while (matcher.find()) {
            String resolvedParameterValue;
            String parameter = matcher.group(1);
            Object variable = annotationParameters.get(removeCurlyBrace(parameter));
            if (variable != null) {
                if (variable.getClass().isArray()) {
                    resolvedParameterValue = ArrayUtils.toString(variable);
                } else {
                    resolvedParameterValue = variable.toString();
                }
            } else {
                resolvedParameterValue = parameter;
            }
            matcher.appendReplacement(sb, sanitizeForAppendReplacement(resolvedParameterValue));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String resolveParameter(String parameterName, ResourceBundle bundle,
                                    Locale locale, boolean recurse) {
        String parameterValue;
        try {
            if (bundle != null) {
                parameterValue = bundle.getString(removeCurlyBrace(parameterName));
                if (recurse) {
                    parameterValue = replaceVariables(parameterValue, bundle, locale, recurse);
                }
            } else {
                parameterValue = parameterName;
            }
        } catch (final MissingResourceException e) {
            // return parameter itself
            parameterValue = parameterName;
        }

        return parameterValue;
    }

    private String removeCurlyBrace(String parameter) {
        return parameter.substring(1, parameter.length() - 1);
    }

    private ResourceBundle findDefaultResourceBundle(Locale locale) {
        ResourceBundle bundle = defaultBundlesMap.get(locale);
        if (bundle == null)
        {
            bundle = ResourceBundle.getBundle(DEFAULT_VALIDATION_MESSAGES, locale);
            defaultBundlesMap.put(locale, bundle);
        }
        return bundle;
    }

    private ResourceBundle findUserResourceBundle(Locale locale) {
        ResourceBundle bundle = userBundlesMap.get(locale);
        if (bundle == null)
        {
            bundle = getFileBasedResourceBundle(locale);
            if (bundle != null) {
                userBundlesMap.put(locale, bundle);
            }
        }
        return bundle;
    }

    /**
     * Set the default locale used by this {@link DefaultMessageInterpolator}.
     * @param locale
     */
    public void setLocale(Locale locale) {
        defaultLocale = locale;
    }

    /**
     * Escapes the string to comply with
     * {@link Matcher#appendReplacement(StringBuffer, String)} requirements.
     *
     * @param src
     *            The original string.
     * @return The sanitized string.
     */
    private String sanitizeForAppendReplacement(String src) {
        return src.replace("\\", "\\\\").replace("$", "\\$");
    }



    /**
     * Perform action with AccessController.doPrivileged() if a security manager is installed.
     *
     * @param action
     *  the action to run
     * @return
     *  result of the action
     */
    private static <T> T doPrivileged(final PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        } else {
            return action.run();
        }
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
