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
package org.apache.bval.jsr;

import static java.util.Collections.emptyEnumeration;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.validation.MessageInterpolator;

import org.apache.bval.el.MessageEvaluator;
import org.apache.bval.jsr.util.LookBehindRegexHolder;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * Description: Resource bundle backed message interpolator.
 * This message resolver resolve message descriptors
 * into human-readable messages. It uses ResourceBundles to find the messages.
 * This class is threadsafe.<br/>
 */
@Privilizing(@CallTo(Reflection.class))
public class DefaultMessageInterpolator implements MessageInterpolator {
    private static final Logger log = Logger.getLogger(DefaultMessageInterpolator.class.getName());
    private static final boolean LOG_FINEST = log.isLoggable(Level.FINEST);
    private static final String DEFAULT_VALIDATION_MESSAGES = "org.apache.bval.jsr.ValidationMessages";
    private static final String USER_VALIDATION_MESSAGES = "ValidationMessages";

    /**
     * {@link LookBehindRegexHolder} to match Bean Validation attribute patterns, considering character escaping.
     */
    private static final LookBehindRegexHolder MESSAGE_PARAMETER = new LookBehindRegexHolder(
        "(?<!(?:^|[^\\\\])(?:\\\\\\\\){0,%1$d}\\\\)\\{((?:[\\w\\.]|\\\\[\\{\\$\\}\\\\])+)\\}", n -> (n - 4) / 2);

    private static final ResourceBundle EMPTY_BUNDLE = new ResourceBundle() {
        @Override
        protected Object handleGetObject(final String key) {
            return null;
        }

        @Override
        public Enumeration<String> getKeys() {
            return emptyEnumeration();
        }
    };

    /** The default locale for the current user. */
    private Locale defaultLocale;

    /** User specified resource bundles hashed against their locale. */
    private final Map<Locale, ResourceBundle> userBundlesMap = new ConcurrentHashMap<>();

    /** Builtin resource bundles hashed against their locale. */
    private final Map<Locale, ResourceBundle> defaultBundlesMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<MessageKey, String> cachedMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, Method> toStringMethods = new ConcurrentHashMap<>();
    private final ConcurrentMap<MessageWithParamsKey, String> interpolations = new ConcurrentHashMap<>();

    private final MessageEvaluator evaluator;

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

        // feed the cache with defaults at least
        findDefaultResourceBundle(defaultLocale);
        if (resourceBundle == null) {
            findUserResourceBundle(defaultLocale);
        } else {
            userBundlesMap.put(defaultLocale, resourceBundle);
        }

        MessageEvaluator ev;
        try {
            ev = MessageEvaluator.class
                .cast(getClass().getClassLoader().loadClass("org.apache.bval.el.ELFacade")
                                .getConstructor().newInstance());
        } catch (final Throwable e) { // can be exception or error
            ev = null;
        }
        evaluator = ev;
    }

    /** {@inheritDoc} */
    @Override
    public String interpolate(final String message, final Context context) {
        // probably no need for caching, but it could be done by parameters since the map
        // is immutable and uniquely built per Validation definition, the comparison has to be based on == and not equals though
        return interpolate(message, context, defaultLocale);
    }

    /** {@inheritDoc} */
    @Override
    public String interpolate(final String message, final Context context, final Locale locale) {
        if (!message.contains("{")) {
            return resolveEscapeSequences(message);
        }

        final ResourceBundle userResourceBundle = findUserResourceBundle(locale);
        final ResourceBundle defaultResourceBundle = findDefaultResourceBundle(locale);

        final Map<String, Object> annotationParameters = context.getConstraintDescriptor().getAttributes();
        String userBundleResolvedMessage;
        String resolvedMessage = message;
        boolean evaluatedDefaultBundleOnce = false;
        do {
            // search the user bundle recursive (step1)
            userBundleResolvedMessage = replaceVariables(resolvedMessage, userResourceBundle, locale, true);

            // exit condition - we have at least tried to validate against the default bundle and there were no
            // further replacements
            if (evaluatedDefaultBundleOnce && !hasReplacementTakenPlace(userBundleResolvedMessage, resolvedMessage)) {
                break;
            }
            // search the default bundle non recursive (step2)
            resolvedMessage = replaceVariables(userBundleResolvedMessage, defaultResourceBundle, locale, false);
            evaluatedDefaultBundleOnce = true;
        } while (true);

        // resolve annotation attributes (step 4)
        resolvedMessage = replaceAnnotationAttributes(resolvedMessage, annotationParameters);

        // EL handling
        if (evaluateExpressionLanguage(message, context)) {
            resolvedMessage = evaluator.interpolate(resolvedMessage, annotationParameters, context.getValidatedValue());
        }
        return resolveEscapeSequences(resolvedMessage);
    }

    private boolean evaluateExpressionLanguage(String template, Context context) {
        if (evaluator != null) {
            if (Objects.equals(template, context.getConstraintDescriptor().getMessageTemplate())) {
                return true;
            }
            final Optional<ApacheMessageContext> apacheMessageContext = Optional.of(context).map(ctx -> {
                try {
                    return ctx.unwrap(ApacheMessageContext.class);
                } catch (Exception e) {
                    return null;
                }
            });
            return !apacheMessageContext.isPresent() || apacheMessageContext
                .map(amc -> amc.getConfigurationProperty(
                    ApacheValidatorConfiguration.Properties.CUSTOM_TEMPLATE_EXPRESSION_EVALUATION))
                .filter(Boolean::parseBoolean).isPresent();
        }
        return false;
    }

    private String resolveEscapeSequences(String s) {
        int pos = s.indexOf('\\');
        if (pos < 0) {
            return s;
        }
        StringBuilder result = new StringBuilder(s.length());
 
        int prev = 0;
        do {
            if (pos + 1 >= s.length()) {
                break;
            }
            if ("\\{}$".indexOf(s.charAt(pos + 1)) >= 0) {
                result.append(s, prev, pos);
                prev = pos + 1;
            }
            pos = s.indexOf('\\', pos + 2);
        } while (pos > 0);

        if (prev < s.length()) {
            result.append(s, prev, s.length());
        }
        return result.toString();
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
        final ClassLoader classLoader = Reflection.loaderFromThreadOrClass(DefaultMessageInterpolator.class);
        ResourceBundle rb = loadBundle(classLoader, locale, USER_VALIDATION_MESSAGES);
        if (LOG_FINEST) {
            if (rb == null) {
                log.log(Level.FINEST, String.format("%s not found. Delegating to %s", USER_VALIDATION_MESSAGES,
                    DEFAULT_VALIDATION_MESSAGES));
            } else {
                log.log(Level.FINEST, String.format("%s found", USER_VALIDATION_MESSAGES));
            }
        }
        return rb == null ? EMPTY_BUNDLE : rb;
    }

    private ResourceBundle loadBundle(ClassLoader classLoader, Locale locale, String message) {
        try {
            return ResourceBundle.getBundle(USER_VALIDATION_MESSAGES, locale, classLoader);
        } catch (final MissingResourceException e) {
            log.fine(message);
            return null;
        }
    }

    private String replaceVariables(String message, ResourceBundle bundle, Locale locale, boolean recurse) {
        final MessageKey key = new MessageKey(locale, message, bundle);
        final String cached = cachedMessages.get(key);
        if (cached != null) {
            return cached;
        }
        final String value = doReplaceVariables(message, bundle, locale, recurse);
        cachedMessages.putIfAbsent(key, value);
        return value;
    }

    private String doReplaceVariables(String message, ResourceBundle bundle, Locale locale, boolean recurse) {
        final Matcher matcher = MESSAGE_PARAMETER.matcher(message);
        final StringBuilder sb = new StringBuilder(64);
        int prev = 0;
        while (matcher.find()) {
            int start = matcher.start();
            if (start > prev) {
                sb.append(message, prev, start);
            }
            sb.append(resolveParameter(matcher.group(1), bundle, locale, recurse).orElseGet(matcher::group));
            prev = matcher.end();
        }
        if (prev < message.length()) {
            sb.append(message, prev, message.length());
        }
        return sb.toString();
    }

    private String replaceAnnotationAttributes(final String message, final Map<String, Object> annotationParameters) {
        final MessageWithParamsKey key = new MessageWithParamsKey(message, annotationParameters);
        String result = interpolations.get(key);
        if (result == null) {
            final Matcher matcher = MESSAGE_PARAMETER.matcher(message);
            if (!matcher.find()) {
                result = message;
            } else {
                final StringBuilder sb = new StringBuilder(64);
                int prev = 0;
                do {
                    int start = matcher.start();
                    String resolvedParameterValue;
                    String parameter = matcher.group(1);
                    Object variable = annotationParameters.get(parameter);
                    if (variable == null) {
                        resolvedParameterValue = matcher.group();
                    } else if (Object[].class.isInstance(variable)) {
                        resolvedParameterValue = Arrays.toString((Object[]) variable);
                    } else if (variable.getClass().isArray()) {
                        try {
                            resolvedParameterValue = (String) getToStringMethod(variable).invoke(null, variable);
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                            throw new IllegalStateException("Could not expand array " + variable);
                        }
                    } else {
                        resolvedParameterValue = variable.toString();
                    }
                    if (start > prev) {
                        sb.append(message, prev, start);
                    }
                    sb.append(resolvedParameterValue);
                    prev = matcher.end();
                } while (matcher.find());
                if (prev < message.length()) {
                    sb.append(message, prev, message.length());
                }
                result = sb.toString();
            }
            interpolations.putIfAbsent(key, result);
        }
        return result;
    }

    public void clearCache() {
        cachedMessages.clear();
        interpolations.clear();
    }

    private Method getToStringMethod(final Object variable) {
        final Class<?> variableClass = variable.getClass();
        Method method = toStringMethods.get(variableClass);
        if (method == null) {
            method = Reflection.getDeclaredMethod(Arrays.class, "toString", variableClass);
            toStringMethods.putIfAbsent(variableClass, method);
        }
        return method;
    }

    private Optional<String> resolveParameter(String parameterName, ResourceBundle bundle, Locale locale,
        boolean recurse) {
        try {
            final String string = bundle.getString(parameterName);
            return of(recurse ? replaceVariables(string, bundle, locale, recurse) : string);
        } catch (final MissingResourceException e) {
            return empty();
        }
    }

    private ResourceBundle findDefaultResourceBundle(Locale locale) {
        return defaultBundlesMap.computeIfAbsent(locale,
            k -> ResourceBundle.getBundle(DEFAULT_VALIDATION_MESSAGES, locale));
    }

    private ResourceBundle findUserResourceBundle(Locale locale) {
        return userBundlesMap.computeIfAbsent(locale, this::getFileBasedResourceBundle);
    }

    /**
     * Set the default locale used by this {@link DefaultMessageInterpolator}.
     * @param locale
     */
    public void setLocale(Locale locale) {
        defaultLocale = locale;
        clearCache(); // cause there is a probability of 50% the old cache will be replaced by the new locale
    }

    private static class MessageKey {
        private final Locale locale;
        private final String key;
        private final ResourceBundle bundle;
        private final int hash;

        private MessageKey(final Locale locale, final String key, final ResourceBundle bundle) {
            this.locale = locale;
            this.key = key;
            this.bundle = bundle;
            this.hash = Objects.hash(locale, key, bundle);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final MessageKey that = MessageKey.class.cast(o);
            return locale.equals(that.locale) && key.equals(that.key) && bundle.equals(that.bundle);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static class MessageWithParamsKey {
        private final String message;
        private final Map<String, Object> annotationParameters;
        private final int hash;

        private MessageWithParamsKey(final String message, final Map<String, Object> annotationParameters) {
            this.message = message;
            this.annotationParameters = annotationParameters;
            this.hash = Objects.hash(message, annotationParameters);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final MessageWithParamsKey that = MessageWithParamsKey.class.cast(o);
            return message.equals(that.message) && annotationParameters.equals(that.annotationParameters);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
