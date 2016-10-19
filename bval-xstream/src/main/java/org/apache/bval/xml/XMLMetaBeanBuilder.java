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
package org.apache.bval.xml;

import org.apache.bval.IntrospectorMetaBeanFactory;
import org.apache.bval.MetaBeanBuilder;
import org.apache.bval.MetaBeanFactory;
import org.apache.bval.model.MetaBean;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Description: <br>
 * User: roman.stumm<br>
 * Date: 17.06.2010<br>
 * Time: 10:10:46<br>
 */
public class XMLMetaBeanBuilder extends MetaBeanBuilder {
    private XMLMetaBeanFactory xmlFactory;

    public XMLMetaBeanBuilder(MetaBeanFactory[] factories) {
        setFactories(factories);
    }

    public XMLMetaBeanBuilder() {
        setFactories(new MetaBeanFactory[] { new IntrospectorMetaBeanFactory(), new XMLMetaBeanFactory() });
    }

    @Override
    public void setFactories(MetaBeanFactory[] factories) {
        super.setFactories(factories);
        updateXmlFactory();
    }

    public void addLoader(XMLMetaBeanLoader loader) {
        assertXmlFactory();
        xmlFactory.addLoader(loader);
    }

    @Override
    public MetaBean buildForId(String beanInfoId) throws Exception {
        final XMLMetaBeanFactory.Visitor v;
        assertXmlFactory();
        xmlFactory.visitXMLBeanMeta(beanInfoId, v = new XMLMetaBeanFactory.Visitor() {
            private MetaBean meta;

            @Override
            public MetaBean getMetaBean() {
                return meta;
            }

            @Override
            public void visit(XMLMetaBean xmlMeta, XMLMetaBeanInfos xmlInfos) throws Exception {
                if (meta == null) {
                    meta = createMetaBean(xmlMeta);
                }
                xmlFactory.enrichMetaBean(meta, new XMLMetaBeanFactory.XMLResult(xmlMeta, xmlInfos));
            }

        });
        if (v.getMetaBean() == null) {
            throw new IllegalArgumentException("MetaBean " + beanInfoId + " not found");
        }
        return v.getMetaBean();
    }

    @Override
    public Map<String, MetaBean> buildAll() throws Exception {
        final Map<String, MetaBean> all = super.buildAll();
        if (xmlFactory != null) {
            xmlFactory.visitXMLBeanMeta(null, new XMLMetaBeanFactory.Visitor() {
                @Override
                public void visit(XMLMetaBean empty, XMLMetaBeanInfos xmlInfos) throws Exception {
                    if (xmlInfos.getBeans() == null)
                        return; // empty file, ignore
                    XMLMetaBeanFactory.XMLResult carrier = new XMLMetaBeanFactory.XMLResult(null, xmlInfos);

                    for (XMLMetaBean xmlMeta : xmlInfos.getBeans()) {
                        MetaBean meta = all.get(xmlMeta.getId());
                        if (meta == null) {
                            meta = createMetaBean(xmlMeta);
                            all.put(xmlMeta.getId(), meta);
                        }
                        carrier.xmlMeta = xmlMeta;
                        xmlFactory.enrichMetaBean(meta, carrier);
                    }
                }

                @Override
                public MetaBean getMetaBean() {
                    return null; // do nothing
                }
            });
        }
        return all;
    }

    public Map<String, MetaBean> enrichCopies(Map<String, MetaBean> all, XMLMetaBeanInfos... infosArray)
        throws Exception {
        assertXmlFactory();
        final Map<String, MetaBean> copies = new HashMap<String, MetaBean>(all.size());
        boolean nothing = true;
        XMLMetaBeanFactory.XMLResult carrier = new XMLMetaBeanFactory.XMLResult();
        for (XMLMetaBeanInfos xmlMetaBeanInfos : infosArray) {
            carrier.xmlInfos = xmlMetaBeanInfos;
            if (xmlMetaBeanInfos == null)
                continue;
            try {
                for (XMLMetaBean xmlMeta : xmlMetaBeanInfos.getBeans()) {
                    nothing = false;
                    MetaBean copy = copies.get(xmlMeta.getId());
                    if (copy == null) { // ist noch nicht kopiert
                        MetaBean meta = all.get(xmlMeta.getId());
                        if (meta == null) { // gibt es nicht
                            copy = createMetaBean(xmlMeta);
                        } else { // gibt es, jetzt kopieren
                            copy = meta.copy();
                        }
                        copies.put(xmlMeta.getId(), copy);
                    }
                    carrier.xmlMeta = xmlMeta;
                    xmlFactory.enrichMetaBean(copy, carrier);
                }
            } catch (IOException e) {
                xmlFactory.handleLoadException(xmlMetaBeanInfos, e);
            }
        }
        if (nothing)
            return all;
        for (Map.Entry<String, MetaBean> entry : all.entrySet()) {
            /*
             * alle unveraenderten werden AUCH KOPIERT (nur zwar nur, wegen
             * potentieller CrossReferenzen durch Relationships)
             */
            if (!copies.containsKey(entry.getKey())) {
                if (entry.getValue().hasRelationships()) {
                    copies.put(entry.getKey(), (MetaBean) entry.getValue().copy());
                } else { // no relationship: do not clone()
                    copies.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return copies;
    }

    private MetaBean createMetaBean(XMLMetaBean xmlMeta) throws Exception {
        return buildForClass(findLocalClass(xmlMeta.getImpl()));
    }

    private void updateXmlFactory() {
        for (MetaBeanFactory each : getFactories()) {
            if (each instanceof XMLMetaBeanFactory) { // use the first one!
                xmlFactory = (XMLMetaBeanFactory) each;
                return;
            }
        }
        xmlFactory = null; // none
    }

    public XMLMetaBeanFactory getXmlFactory() {
        return xmlFactory;
    }

    private void assertXmlFactory() {
        if (xmlFactory == null) {
            throw new IllegalStateException("no xmlFactory available");
        }
    }
}
