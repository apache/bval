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
package org.apache.bval.jsr.descriptor;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.validation.ConstraintDeclarationException;
import javax.validation.ValidationException;
import javax.validation.metadata.ContainerElementTypeDescriptor;
import javax.validation.valueextraction.ValueExtractor;

import org.apache.bval.jsr.GraphContext;
import org.apache.bval.jsr.metadata.ContainerElementKey;
import org.apache.bval.jsr.util.NodeImpl;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.Validate;

public class ContainerElementTypeD extends CascadableContainerD<CascadableContainerD<?, ?>, AnnotatedType>
    implements ContainerElementTypeDescriptor {

    private class Receiver implements ValueExtractor.ValueReceiver {
        private final GraphContext context;
        private Lazy<List<GraphContext>> result = new Lazy<>(ArrayList::new);

        Receiver(GraphContext context) {
            super();
            this.context = context;
        }

        @Override
        public void value(String nodeName, Object object) {
            addChild(new NodeImpl.ContainerElementNodeImpl(nodeName), object);
        }

        @Override
        public void iterableValue(String nodeName, Object object) {
            final NodeImpl node = new NodeImpl.ContainerElementNodeImpl(nodeName);
            node.setInIterable(true);
            addChild(node, object);
        }

        @Override
        public void indexedValue(String nodeName, int i, Object object) {
            final NodeImpl node = new NodeImpl.ContainerElementNodeImpl(nodeName);
            node.setIndex(Integer.valueOf(i));
            addChild(node, object);
        }

        @Override
        public void keyedValue(String nodeName, Object key, Object object) {
            final NodeImpl node = new NodeImpl.ContainerElementNodeImpl(nodeName);
            node.setKey(key);
            addChild(node, object);
        }

        private void addChild(NodeImpl node, Object value) {
            final PathImpl path = context.getPath();
            if (node.getName() != null) {
                path.addNode(node.inContainer(key.getContainerClass(), key.getTypeArgumentIndex()));
            }
            result.get().add(context.child(path, value));
        }
    }

    private final ContainerElementKey key;

    ContainerElementTypeD(ContainerElementKey key, MetadataReader.ForContainer<AnnotatedType> reader,
        CascadableContainerD<?, ?> parent) {
        super(reader, parent);
        this.key = Validate.notNull(key, "key");
    }

    @Override
    public Class<?> getContainerClass() {
        return key.getContainerClass();
    }

    @Override
    public Integer getTypeArgumentIndex() {
        return Integer.valueOf(key.getTypeArgumentIndex());
    }

    public ContainerElementKey getKey() {
        return key;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Stream<GraphContext> readImpl(GraphContext context) throws Exception {
        final ValueExtractor valueExtractor = context.getValidatorContext().getValueExtractors().find(key);
        Exceptions.raiseIf(valueExtractor == null, ConstraintDeclarationException::new, "No %s found for %s",
            ValueExtractor.class.getSimpleName(), key);

        final Receiver receiver = new Receiver(context);
        try {
            valueExtractor.extractValues(context.getValue(), receiver);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException(e);
        }
        return receiver.result.get().stream();
    }
}
