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
package org.apache.bval.jsr303;

import junit.framework.Assert;
import junit.framework.TestCase;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Min;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Locale;
import java.util.Set;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Checks the correct parsing of constraint definitions.
 * 
 * @author Carlos Vara
 */
public class ConstraintDefinitionsTest extends TestCase {
    
    static ValidatorFactory factory;

    static {
        factory = Validation.buildDefaultValidatorFactory();
        ((DefaultMessageInterpolator) factory.getMessageInterpolator()).setLocale(Locale.ENGLISH);
    }

    private Validator getValidator() {
        return factory.getValidator();
    }
    
    
    /**
     * Checks the correct parsing of a constraint with an array of constraints
     * as attributes.
     */
    public void testCustomAttributes() {
        Validator validator = getValidator();
        BeanDescriptor constraints = validator.getConstraintsForClass(Person.class);
        Set<ConstraintDescriptor<?>> ageConstraints = constraints.getConstraintsForProperty("age").getConstraintDescriptors();
        
        Assert.assertEquals("There should be 2 constraints in 'age'", ageConstraints.size(), 2);
        for ( ConstraintDescriptor<?> cd : ageConstraints ) {
            Assert.assertEquals("Annotation should be @Min", cd.getAnnotation().annotationType().getName(), Min.class.getName());
        }
    }

    
    public static class Person {
        @MinList({
            @Min(value=20),
            @Min(value=30)
        })
        public Integer age;
    }
    
}

@Target({ METHOD, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Documented
@interface MinList {
    Min[] value();
}