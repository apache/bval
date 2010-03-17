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
package com.agimatec.validation.jsr303;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.validation.MessageInterpolator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description: Resource bundle backed message interpolator.
 * This message resolver resolve message descriptors
 * into human-readable messages. It uses ResourceBundles to find the messages.
 * This class is threadsafe.<br/>
 * User: roman.stumm <br/>
 * Date: 02.04.2008 <br/>
 * Time: 17:21:51 <br/>
 * Copyright: Agimatec GmbH 2008
 */
public class DefaultMessageInterpolator implements MessageInterpolator {
    private static final Log log = LogFactory.getLog(DefaultMessageInterpolator.class);
    private static final String DEFAULT_VALIDATION_MESSAGES =
          "com.agimatec.validation.jsr303.ValidationMessages";
    private static final String USER_VALIDATION_MESSAGES = "ValidationMessages";

    /** Regular expression used to do message interpolation. */
    private static final Pattern messageParameterPattern =
          Pattern.compile("(\\{[\\w\\.]+\\})");

    /** The default locale for the current user. */
    private Locale defaultLocale;

    /** User specified resource bundles hashed against their locale. */
    private final Map<Locale, ResourceBundle> userBundlesMap =
          new ConcurrentHashMap<Locale, ResourceBundle>();

    /** Builtin resource bundles hashed against there locale. */
    private final Map<Locale, ResourceBundle> defaultBundlesMap =
          new ConcurrentHashMap<Locale, ResourceBundle>();

    public DefaultMessageInterpolator() {
        this(null);
    }

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

    /** {@inheritDoc} */
    public String interpolate(String message, Context context) {
        // probably no need for caching, but it could be done by parameters since the map
        // is immutable and uniquely built per Validation definition, the comparaison has to be based on == and not equals though
        return interpolateMessage(message,
              context.getConstraintDescriptor().getAttributes(), defaultLocale);
    }

    /** {@inheritDoc} */
    public String interpolate(String message, Context context, Locale locale) {
        return interpolateMessage(message,
              context.getConstraintDescriptor().getAttributes(), locale);
    }

    /**
     * Runs the message interpolation according to alogrithm specified in JSR 303.
     * <br/>
     * Note:
     * <br/>
     * Lookups in user bundles is recursive whereas lookups in default bundle are not!
     *
     * @param message              the message to interpolate
     * @param annotationParameters the parameters of the annotation for which to interpolate this message
     * @param locale               the <code>Locale</code> to use for the resource bundle.
     * @return the interpolated message.
     */
    private String interpolateMessage(String message,
                                      Map<String, Object> annotationParameters,
                                      Locale locale) {
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
        resolvedMessage =
              replaceAnnotationAttributes(resolvedMessage, annotationParameters);
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
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            rb = loadBundle(classLoader, locale,
                  USER_VALIDATION_MESSAGES + " not found by thread local classloader");
        }
        if (rb == null) {
            rb = loadBundle(this.getClass().getClassLoader(), locale,
                  USER_VALIDATION_MESSAGES + " not found by validator classloader");
        }
        if (log.isDebugEnabled()) {
            if (rb != null) {
                log.debug(USER_VALIDATION_MESSAGES + " found");
            } else {
                log.debug(USER_VALIDATION_MESSAGES + " not found. Delegating to " +
                      DEFAULT_VALIDATION_MESSAGES);
            }
        }
        return rb;
    }

    private ResourceBundle loadBundle(ClassLoader classLoader, Locale locale,
                                      String message) {
        ResourceBundle rb = null;
        try {
            rb = ResourceBundle.getBundle(USER_VALIDATION_MESSAGES, locale, classLoader);
        } catch (MissingResourceException e) {
            log.trace(message);
        }
        return rb;
    }

    private String replaceVariables(String message, ResourceBundle bundle, Locale locale,
                                    boolean recurse) {
        Matcher matcher = messageParameterPattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        String resolvedParameterValue;
        while (matcher.find()) {
            String parameter = matcher.group(1);
            resolvedParameterValue = resolveParameter(parameter, bundle, locale, recurse);

            matcher.appendReplacement(sb, resolvedParameterValue);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String replaceAnnotationAttributes(String message,
                                               Map<String, Object> annotationParameters) {
        Matcher matcher = messageParameterPattern.matcher(message);
        StringBuffer sb = new StringBuffer();
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
                resolvedParameterValue = message;
            }
            matcher.appendReplacement(sb, resolvedParameterValue);
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
                    parameterValue =
                          replaceVariables(parameterValue, bundle, locale, recurse);
                }
            } else {
                parameterValue = parameterName;
            }
        } catch (MissingResourceException e) {
            // return parameter itself
            parameterValue = parameterName;
        }
        return parameterValue;
    }

    private String removeCurlyBrace(String parameter) {
        return parameter.substring(1, parameter.length() - 1);
    }

    private ResourceBundle findDefaultResourceBundle(Locale locale) {
        if (defaultBundlesMap.containsKey(locale)) {
            return defaultBundlesMap.get(locale);
        }

        ResourceBundle bundle =
              ResourceBundle.getBundle(DEFAULT_VALIDATION_MESSAGES, locale);
        defaultBundlesMap.put(locale, bundle);
        return bundle;
    }

    private ResourceBundle findUserResourceBundle(Locale locale) {
        if (userBundlesMap.containsKey(locale)) {
            return userBundlesMap.get(locale);
        }

        ResourceBundle bundle = getFileBasedResourceBundle(locale);
        if (bundle != null) {
            userBundlesMap.put(locale, bundle);
        }
        return bundle;
    }

    public void setLocale(Locale locale) {
        defaultLocale = locale;
    }

}
