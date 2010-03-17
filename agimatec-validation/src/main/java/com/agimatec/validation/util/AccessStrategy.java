/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.agimatec.validation.util;

import java.lang.annotation.ElementType;
import java.lang.reflect.Type;

/**
 * Description: abstract class to encapsulate different strategies
 * to get the value of a Property.<br/>
 * User: roman <br/>
 * Date: 29.10.2009 <br/>
 * Time: 12:12:08 <br/>
 * Copyright: Agimatec GmbH
 */
public abstract class AccessStrategy {
    /**
     * get the value from the given instance.
     * @param instance
     * @return the value
     * @throws IllegalArgumentException in case of an error
     */
    public abstract Object get(Object instance);

    public abstract ElementType getElementType();

    public abstract Type getJavaType();

    public abstract String getPropertyName();
}
