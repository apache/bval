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
package org.apache.bval.integration;

import org.apache.bval.BeanValidationContext;
import org.apache.bval.model.ValidationListener;


/**
 * Description: Used to bind the current validation context to the current thread.
 * Use this class when you need to append validation errors in service layers
 * without handing a ValidationContext and/or ValidationResults instance
 * through your method signatures.<br/>
 * User: roman.stumm <br/>
 * Date: 09.07.2007 <br/>
 * Time: 13:41:10 <br/>
 * Copyright: Agimatec GmbH 2008
 */
public class ThreadValidationContext extends BeanValidationContext {
    protected static final ThreadLocal<ThreadValidationContext> current =
            new ThreadLocal<ThreadValidationContext>();

    public ThreadValidationContext(ValidationListener listener) {
        super(listener);
    }

    public static ThreadValidationContext getCurrent() {
        return current.get();
    }

    public static void setCurrent(ThreadValidationContext aValidationContext) {
        if (aValidationContext == null) {
            current.remove();
        } else {
            current.set(aValidationContext);
        }
    }
}
