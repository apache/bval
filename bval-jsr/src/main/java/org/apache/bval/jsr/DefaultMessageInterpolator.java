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

import org.apache.bval.el.MessageEvaluator;
import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

import javax.validation.MessageInterpolator;

import java.util.Arrays;
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
@Privilizing(@CallTo(Reflection.class))
public class DefaultMessageInterpolator implements MessageInterpolator {
    private static final Logger log = Logger.getLogger(DefaultMessageInterpolator.class.getName());
    private static final boolean LOG_FINEST = log.isLoggable(Level.FINEST);
    private static final String DEFAULT_VALIDATION_MESSAGES = "org.apache.bval.jsr.ValidationMessages";
    private static final String USER_VALIDATION_MESSAGES = "ValidationMessages";

    /** Regular expression used to do message interpolation. */
    private static final Pattern messageParameterPattern = Pattern.compile("(\\{[\\w\\.]+\\})");

    /** The default locale for the current user. */
    private Locale defaultLocale;

    /** User specified resource bundles hashed against their locale. */
    private final Map<Locale, ResourceBundle> userBundlesMap = new ConcurrentHashMap<Locale, ResourceBundle>();

    /** Builtin resource bundles hashed against their locale. */
    private final Map<Locale, ResourceBundle> defaultBundlesMap = new ConcurrentHashMap<Locale, ResourceBundle>();

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

        MessageEvaluator ev = null;
        try {
            ev = MessageEvaluator.class
                .cast(getClass().getClassLoader().loadClass("org.apache.bval.el.ELFacade").newInstance());
        } catch (final Throwable e) { // can be exception or error
            // no-op
        }
        evaluator = ev;
    }

    /** {@inheritDoc} */
    @Override
    public String interpolate(String message, Context context) {
        // probably no need for caching, but it could be done by parameters since the map
        // is immutable and uniquely built per Validation definition, the comparison has to be based on == and not equals though
        return interpolate(message, context, defaultLocale);
    }

    /** {@inheritDoc} */
    @Override
    public String interpolate(String message, Context context, Locale locale) {
        return interpolateMessage(message, context.getConstraintDescriptor().getAttributes(), locale,
            context.getValidatedValue());
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
    private String interpolateMessage(String message, Map<String, Object> annotationParameters, Locale locale,
        Object validatedValue) {
        ResourceBundle userResourceBundle = findUserResourceBundle(locale);
        ResourceBundle defaultResourceBundle = findDefaultResourceBundle(locale);

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
        if (evaluator != null) {
            resolvedMessage = evaluator.interpolate(resolvedMessage, annotationParameters, validatedValue);
        }

        // curly braces need to be scaped in the original msg, so unescape them now
        resolvedMessage =
            resolvedMessage.replace("\\{", "{").replace("\\}", "}").replace("\\\\", "\\").replace("\\$", "$");

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
        final ClassLoader classLoader = Reflection.getClassLoader(DefaultMessageInterpolator.class);
        if (classLoader != null) {
            rb = loadBundle(classLoader, locale, USER_VALIDATION_MESSAGES + " not found by thread local classloader");
        }

        // 2011-03-27 jw: No privileged action required.
        // A class can always access the classloader of itself and of subclasses.
        if (rb == null) {
            rb = loadBundle(getClass().getClassLoader(), locale,
                USER_VALIDATION_MESSAGES + " not found by validator classloader");
        }
        if (LOG_FINEST) {
            if (rb != null) {
                log.log(Level.FINEST, String.format("%s found", USER_VALIDATION_MESSAGES));
            } else {
                log.log(Level.FINEST, String.format("%s not found. Delegating to %s", USER_VALIDATION_MESSAGES,
                    DEFAULT_VALIDATION_MESSAGES));
            }
        }
        return rb;
    }

    private ResourceBundle loadBundle(ClassLoader classLoader, Locale locale, String message) {
        ResourceBundle rb = null;
        try {
            rb = ResourceBundle.getBundle(USER_VALIDATION_MESSAGES, locale, classLoader);
        } catch (final MissingResourceException e) {
            log.fine(message);
        }
        return rb;
    }

    private String replaceVariables(String message, ResourceBundle bundle, Locale locale, boolean recurse) {
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

    private String replaceAnnotationAttributes(final String message, final Map<String, Object> annotationParameters) {
        Matcher matcher = messageParameterPattern.matcher(message);
        StringBuffer sb = new StringBuffer(64);
        while (matcher.find()) {
            String resolvedParameterValue;
            String parameter = matcher.group(1);
            Object variable = annotationParameters.get(removeCurlyBrace(parameter));
            if (variable != null) {
                if (variable.getClass().isArray()) {
                    resolvedParameterValue = Arrays.toString((Object[]) variable);
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

    private String resolveParameter(String parameterName, ResourceBundle bundle, Locale locale, boolean recurse) {
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
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(DEFAULT_VALIDATION_MESSAGES, locale);
            defaultBundlesMap.put(locale, bundle);
        }
        return bundle;
    }

    private ResourceBundle findUserResourceBundle(Locale locale) {
        ResourceBundle bundle = userBundlesMap.get(locale);
        if (bundle == null) {
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
}
