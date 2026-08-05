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
package org.apache.bval.jsr;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Size;

import org.apache.bval.constraints.SizeValidator;
import org.apache.bval.jsr.metadata.AnnotationBehavior;
import org.apache.bval.jsr.metadata.ServiceLoaderValidatorMappingProvider;
import org.apache.bval.jsr.metadata.ValidatorMapping;
import org.apache.bval.jsr.metadata.ValidatorMappingProvider;
import org.junit.Test;

/**
 * Covers the 4.0 service loader registration source: {@link ServiceLoaderValidatorMappingProvider} and the way
 * {@link ConstraintCached} composes it with the built-in, annotation-declared and XML-declared sources.
 */
public class ServiceLoadedConstraintValidatorTest {

    @Target({ TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Constraint(validatedBy = {})
    public @interface ServiceLoaded {
        String message() default "";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    @Target({ TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, ANNOTATION_TYPE })
    @Retention(RUNTIME)
    @Constraint(validatedBy = { AnnotationDeclaredValidator.class })
    public @interface AnnotationDeclared {
        String message() default "";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class ServiceLoadedValidator implements ConstraintValidator<ServiceLoaded, Object> {
        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            return true;
        }
    }

    public static class AnnotationDeclaredValidator implements ConstraintValidator<AnnotationDeclared, Object> {
        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            return true;
        }
    }

    public static class ServiceLoadedAnnotationDeclaredValidator
        implements ConstraintValidator<AnnotationDeclared, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return true;
        }
    }

    public static class SizeValidatorForBook implements ConstraintValidator<Size, Book> {
        @Override
        public boolean isValid(Book value, ConstraintValidatorContext context) {
            return true;
        }
    }

    public static class Book {
    }

    /** Not a {@link ConstraintValidator} of any determinable constraint type. */
    @SuppressWarnings("rawtypes")
    public static class RawValidator implements ConstraintValidator {
        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            return true;
        }
    }

    public static abstract class AbstractServiceLoadedValidator implements ConstraintValidator<ServiceLoaded, Object> {
    }

    private static ServiceLoaderValidatorMappingProvider provider(Class<?>... types) {
        return new ServiceLoaderValidatorMappingProvider(Arrays.asList(types));
    }

    private static ConstraintCached cache(ServiceLoaderValidatorMappingProvider serviceLoader) {
        final ConstraintCached result = new ConstraintCached();
        result.setServiceLoaderValidatorMappingProvider(serviceLoader);
        return result;
    }

    @Test
    public void testIndexedByConstraintType() {
        final ServiceLoaderValidatorMappingProvider provider =
            provider(ServiceLoadedValidator.class, SizeValidatorForBook.class);

        assertEquals(Collections.singletonList(ServiceLoadedValidator.class),
            provider.getValidatorMapping(ServiceLoaded.class).getValidatorTypes());

        assertEquals(Collections.singletonList(SizeValidatorForBook.class),
            provider.getValidatorMapping(Size.class).getValidatorTypes());
    }

    @Test
    public void testUnknownConstraintType() {
        assertNull(provider(ServiceLoadedValidator.class).getValidatorMapping(AnnotationDeclared.class));
    }

    @Test
    public void testUndeterminableConstraintTypeIgnored() {
        assertNull(provider(RawValidator.class).getValidatorMapping(ServiceLoaded.class));
    }

    @Test
    public void testAbstractValidatorIgnored() {
        assertNull(provider(AbstractServiceLoadedValidator.class).getValidatorMapping(ServiceLoaded.class));
    }

    @Test
    public void testServiceLoadedValidatorForConstraintWithoutValidatedBy() {
        assertEquals(Collections.singletonList(ServiceLoadedValidator.class),
            cache(provider(ServiceLoadedValidator.class)).getConstraintValidatorClasses(ServiceLoaded.class));
    }

    @Test
    public void testServiceLoadedValidatorMergedWithValidatedBy() {
        final List<Class<? extends ConstraintValidator<AnnotationDeclared, ?>>> validators =
            cache(provider(ServiceLoadedAnnotationDeclaredValidator.class))
                .getConstraintValidatorClasses(AnnotationDeclared.class);

        assertEquals(2, validators.size());
        assertTrue(validators.containsAll(
            Arrays.asList(AnnotationDeclaredValidator.class, ServiceLoadedAnnotationDeclaredValidator.class)));
    }

    @Test
    public void testServiceLoadedValidatorMergedWithBuiltIn() {
        final List<Class<? extends ConstraintValidator<Size, ?>>> validators =
            cache(provider(SizeValidatorForBook.class)).getConstraintValidatorClasses(Size.class);

        assertTrue(validators.contains(SizeValidatorForBook.class));
        assertTrue(validators.contains(SizeValidator.ForCharSequence.class));
    }

    @Test
    public void testXmlOverrideExcludesServiceLoadedValidator() {
        final ConstraintCached cache = cache(provider(SizeValidatorForBook.class));
        cache.add(excluding(Size.class, SizeValidator.ForCharSequence.class));

        assertEquals(Collections.singletonList(SizeValidator.ForCharSequence.class),
            cache.getConstraintValidatorClasses(Size.class));
    }

    /**
     * Stands in for an XML {@code <validated-by include-existing-validators="false">} registration.
     */
    private static ValidatorMappingProvider excluding(Class<? extends Annotation> constraintType,
        Class<? extends ConstraintValidator<?, ?>> validatorType) {
        return new ValidatorMappingProvider() {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected <A extends Annotation> ValidatorMapping<A> doGetValidatorMapping(Class<A> t) {
                return t.equals(constraintType)
                    ? new ValidatorMapping<>("test", (List) Collections.singletonList(validatorType),
                        AnnotationBehavior.EXCLUDE)
                    : null;
            }
        };
    }
}
