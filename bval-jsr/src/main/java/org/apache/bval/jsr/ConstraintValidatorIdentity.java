/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.bval.jsr;

import javax.validation.ConstraintValidator;
import javax.validation.Path;

/**
 * Class that stores the needed properties to ensure that a validation is not
 * checked more than once.
 * <p>
 * These properties are:
 * <ul>
 * <li>The ref of the bean to which the validation would be applied.</li>
 * <li>The path of the property.</li>
 * <li>The ref of the {@link ConstraintValidator}.</li>
 * </ul>
 * 
 * @author Carlos Vara
 */
final class ConstraintValidatorIdentity {

    private final Object bean;
    private final Path path;
    private final ConstraintValidator<?, ?> constraintValidator;

    /**
     * Create a new ConstraintValidatorIdentity instance.
     * @param bean
     * @param path
     * @param constraintValidator
     */
    public ConstraintValidatorIdentity(Object bean, Path path, ConstraintValidator<?, ?> constraintValidator) {
        this.bean = bean;
        this.path = path;
        this.constraintValidator = constraintValidator;
    }

    /**
     * Get the referenced bean.
     * @return Object
     */
    public Object getBean() {
        return bean;
    }

    /**
     * Get the referenced property {@link Path}.
     * @return Path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Get the associated {@link ConstraintValidator}.
     * @return {@link ConstraintValidator}
     */
    public ConstraintValidator<?, ?> getConstraintValidator() {
        return constraintValidator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof ConstraintValidatorIdentity)) {
            return false;
        }

        ConstraintValidatorIdentity other = (ConstraintValidatorIdentity) obj;

        // Bean ref must be the same
        if (this.bean != other.bean) {
            return false;
        }

        // ConstraintValidator ref must be the same
        if (this.constraintValidator != other.constraintValidator) {
            return false;
        }

        // Path must be equals
        if (!this.path.equals(other.path)) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.bean == null) ? 0 : this.bean.hashCode());
        result = prime * result + ((this.path == null) ? 0 : this.path.hashCode());
        result = prime * result + ((this.constraintValidator == null) ? 0 : this.constraintValidator.hashCode());
        return result;
    }

}
