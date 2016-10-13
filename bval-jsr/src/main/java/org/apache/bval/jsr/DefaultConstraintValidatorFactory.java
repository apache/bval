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

import org.apache.bval.cdi.BValExtension;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ValidationException;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Description: create constraint instances with the default / no-arg constructor <br/>
 */
public class DefaultConstraintValidatorFactory implements ConstraintValidatorFactory, Closeable {
    private final Collection<BValExtension.Releasable<?>> releasables = new CopyOnWriteArrayList<BValExtension.Releasable<?>>();
    private volatile Boolean useCdi = null; // store it to avoid NoClassDefFoundError when cdi is not present (it is slow) + lazily (to wait cdi is started)

    /**
     * Instantiate a Constraint.
     *
     * @return Returns a new Constraint instance
     *         The ConstraintFactory is <b>not</b> responsible for calling Constraint#initialize
     */
    public <T extends ConstraintValidator<?, ?>> T getInstance(final Class<T> constraintClass) {
        if (useCdi == null) {
            synchronized (this) {
                if (useCdi == null) {
                    try {
                        useCdi = BValExtension.getBeanManager() != null;
                    } catch (final NoClassDefFoundError error) {
                        useCdi = Boolean.FALSE;
                    } catch (final Exception e) {
                        useCdi = Boolean.FALSE;
                    }
                }
            }
        }

        // 2011-03-27 jw: Do not use PrivilegedAction.
        // Otherwise any user code would be executed with the privileges of this class.
        try {
            if (useCdi) {
                try {
                    final BValExtension.Releasable<T> instance = BValExtension.inject(constraintClass);
                    if (instance != null) {
                        releasables.add(instance);
                        return instance.getInstance();
                    }
                    throw new IllegalStateException("Can't create " + constraintClass.getName());
                } catch (final Exception e) {
                    return constraintClass.newInstance();
                } catch (final NoClassDefFoundError error) {
                    return constraintClass.newInstance();
                }
            }
            return constraintClass.newInstance();
        } catch (final Exception ex) {
            throw new ValidationException("Cannot instantiate : " + constraintClass, ex);
        }
    }

    public void releaseInstance(final ConstraintValidator<?, ?> instance) {
        // no-op
    }

    public void close() throws IOException {
        for (final BValExtension.Releasable<?> releasable : releasables) {
            // ensure to call this callback
            releaseInstance(ConstraintValidator.class.cast(releasable.getInstance()));
            releasable.release();
        }
        releasables.clear();
    }
}
