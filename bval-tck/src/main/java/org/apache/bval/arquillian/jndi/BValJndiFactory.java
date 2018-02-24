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
package org.apache.bval.arquillian.jndi;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.validation.Validation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Hashtable;

// mock a context to satisfy lookups
public class BValJndiFactory implements InitialContextFactory {
    public Context getInitialContext(final Hashtable<?, ?> environment) throws NamingException {
        return Context.class.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[] { Context.class }, new InvocationHandler() {
                public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                    if (method.getName().equals("lookup") && args != null && args.length == 1
                        && String.class.isInstance(args[0])) {
                        if ("java:comp/ValidatorFactory".equals(args[0])) {
                            return Validation.byDefaultProvider().configure().buildValidatorFactory();
                        }
                        if ("java:comp/Validator".equals(args[0])) {
                            return Validation.byDefaultProvider().configure().buildValidatorFactory().getValidator();
                        }
                    }
                    return null;
                }
            }));
    }
}
