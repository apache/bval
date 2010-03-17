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
package com.agimatec.validation.jsr303.resolver;

import javax.validation.Path;
import javax.validation.TraversableResolver;
import java.lang.annotation.ElementType;

/**
 * Description: traversable resolver that does always resolve<br/>
 * User: roman <br/>
 * Date: 25.11.2009 <br/>
 * Time: 13:21:18 <br/>
 * Copyright: Agimatec GmbH
 */
public class SimpleTraversableResolver implements TraversableResolver, CachingRelevant {
    /** @return true */
    public boolean isReachable(Object traversableObject, Path.Node traversableProperty,
                               Class<?> rootBeanType, Path pathToTraversableObject,
                               java.lang.annotation.ElementType elementType) {
        return true;
    }

    /** @return true */

    public boolean isCascadable(Object traversableObject, Path.Node traversableProperty,
                                Class<?> rootBeanType, Path pathToTraversableObject,
                                ElementType elementType) {
        return true;
    }

    public boolean needsCaching() {
        return false;  // no
    }
}
