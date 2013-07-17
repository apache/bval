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
package org.apache.bval.arquillian;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.container.WebContainer;
import org.jboss.shrinkwrap.impl.base.MemoryMapArchiveImpl;
import org.jboss.shrinkwrap.impl.base.NodeImpl;

import java.lang.reflect.Field;
import java.util.Map;

public class CdiMethodValidationProcessor implements ApplicationArchiveProcessor {
    private static final String BEANS_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "       xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee\n" +
            "      http://java.sun.com/xml/ns/javaee/beans_1_0.xsd\">\n" +
            "  <interceptors>\n" +
            "    <class>org.apache.bval.cdi.BValInterceptor</class>\n" +
            "  </interceptors>\n" +
            "</beans>";

    public void process(final Archive<?> applicationArchive, final TestClass testClass) {
        final String path;
        if (WebContainer.class.isInstance(applicationArchive)) {
            path = "WEB-INF/beans.xml";
        } else {
            path = "META-INF-INF/beans.xml";
        }

        final Node beansXml = applicationArchive.get(path);
        if (beansXml != null && beansXml.getAsset() == EmptyAsset.INSTANCE) {
            final Map<ArchivePath, NodeImpl> content = getInternal(applicationArchive);
            content.remove(ArchivePaths.create(path));
            if (path.startsWith("META-INF")) {
                ManifestContainer.class.cast(applicationArchive).addAsManifestResource(new StringAsset(BEANS_XML), "beans.xml");
            } else {
                WebContainer.class.cast(applicationArchive).addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml");
            }
        }
    }

    private Map getInternal(Archive<?> applicationArchive) {
        return get(Map.class, get(MemoryMapArchiveImpl.class, applicationArchive, "archive"), "content");
    }

    private static <T> T get(final Class<T> clazz, final Object instance, final String field) {
        Class<?> c = instance.getClass();
        while (c != Object.class && c != null) {
            final Field f;
            try {
                f = c.getDeclaredField(field);
                f.setAccessible(true);
                return clazz.cast(f.get(instance));
            } catch (final Exception e) {
                // no-op
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
