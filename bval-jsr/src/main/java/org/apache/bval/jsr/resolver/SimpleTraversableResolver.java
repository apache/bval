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
package org.apache.bval.jsr.resolver;

import javax.validation.Path;
import javax.validation.TraversableResolver;
import java.lang.annotation.ElementType;

/**
 * Description: traversable resolver that does always resolve.<br/>
 */
public class SimpleTraversableResolver implements TraversableResolver, CachingRelevant {
    /**
     * {@inheritDoc}
     *  @return <code>true</code>
     */
    @Override
    public boolean isReachable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
        Path pathToTraversableObject, java.lang.annotation.ElementType elementType) {
        return true;
    }

    /**
     * {@inheritDoc}
     * @return <code>true</code>
     */
    @Override
    public boolean isCascadable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
        Path pathToTraversableObject, ElementType elementType) {
        return true;
    }

    /**
     * {@inheritDoc}
     * @return <code>false</code>
     */
    @Override
    public boolean needsCaching() {
        return false; // no
    }
}
