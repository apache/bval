/*
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
package org.apache.bval.jsr.job;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;

import javax.validation.Path;
import javax.validation.Path.Node;

import org.apache.bval.jsr.ApacheFactoryContext;
import org.apache.bval.jsr.metadata.Meta;
import org.apache.bval.jsr.util.NodeImpl;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Validate;

public abstract class ValidateExecutable<E extends Executable, T> extends ValidationJob<T> {
    private static final Function<Method, Path.Node> METHOD_NODE =
        m -> new NodeImpl.MethodNodeImpl(m.getName(), Arrays.asList(m.getParameterTypes()));

    private static final Function<Constructor<?>, Path.Node> CONSTRUCTOR_NODE =
        c -> new NodeImpl.ConstructorNodeImpl(c.getDeclaringClass().getSimpleName(),
            Arrays.asList(c.getParameterTypes()));

    protected final E executable;

    private final Function<E, Path.Node> executableNode;

    @SuppressWarnings("unchecked")
    public ValidateExecutable(ApacheFactoryContext validatorContext, Class<?>[] groups, Meta<E> meta) {
        super(validatorContext, groups);
        this.executable = Validate.notNull(meta, IllegalArgumentException::new, "meta").getHost();

        switch (meta.getElementType()) {
        case CONSTRUCTOR:
            executableNode = (Function<E, Node>) CONSTRUCTOR_NODE;
            break;
        case METHOD:
            executableNode = (Function<E, Node>) METHOD_NODE;
            break;
        default:
            throw Exceptions.create(IllegalArgumentException::new, "Unsupported %s: %s",
                ElementType.class.getSimpleName(), meta);
        }
    }

    protected PathImpl createBasePath() {
        final PathImpl path = PathImpl.create();
        path.addNode(executableNode.apply(executable));
        return path;
    }
}