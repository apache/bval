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
package org.apache.bval.jsr.valueextraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.validation.ValidationException;
import jakarta.validation.valueextraction.ValueExtractor;

import org.apache.bval.jsr.GraphContext;
import org.apache.bval.jsr.metadata.ContainerElementKey;
import org.apache.bval.jsr.util.NodeImpl;
import org.apache.bval.jsr.util.PathImpl;
import org.apache.bval.util.Exceptions;
import org.apache.bval.util.Lazy;
import org.apache.bval.util.Validate;

/**
 * Utility class to extract values from a {@link GraphContext} using a {@link ValueExtractor}.
 */
public final class ExtractValues {

    private static class Receiver implements ValueExtractor.ValueReceiver {
        private final GraphContext context;
        private final ContainerElementKey containerElementKey;
        private final Lazy<List<GraphContext>> result = new Lazy<>(ArrayList::new);

        Receiver(GraphContext context, ContainerElementKey containerElementKey) {
            super();
            this.context = context;
            this.containerElementKey = containerElementKey;
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
            path.addNode(
                node.inContainer(containerElementKey.getContainerClass(), containerElementKey.getTypeArgumentIndex()));
            result.get().add(context.child(path, value));
        }
    }

    private ExtractValues() {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static List<GraphContext> extract(GraphContext context, ContainerElementKey containerElementKey,
        ValueExtractor<?> valueExtractor) {
        Validate.notNull(context, "context");
        Validate.notNull(containerElementKey, "containerElementKey");
        if (valueExtractor != null) {
            Exceptions.raiseIf(context.getValue() == null, IllegalStateException::new,
                "Cannot extract values from null");
            final Receiver receiver = new Receiver(context, containerElementKey);
            try {
                ((ValueExtractor) valueExtractor).extractValues(context.getValue(), receiver);
            } catch (ValidationException e) {
                throw e;
            } catch (Exception e) {
                throw new ValidationException(e);
            }
            return receiver.result.optional().orElse(Collections.emptyList());
        }
        return Collections.singletonList(context);
    }
}
