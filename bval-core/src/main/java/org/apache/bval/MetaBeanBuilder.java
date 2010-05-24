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
package org.apache.bval;


import org.apache.bval.model.MetaBean;
import org.apache.bval.xml.XMLMetaBean;
import org.apache.bval.xml.XMLMetaBeanFactory;
import org.apache.bval.xml.XMLMetaBeanInfos;
import org.apache.bval.xml.XMLMetaBeanLoader;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Description: internal implementation class to construct metabeans with factories<br/>
 */
public class MetaBeanBuilder {
  private static final Log log = LogFactory.getLog(MetaBeanBuilder.class);

  /**
   * here you can install different kinds of factories to create MetaBeans from
   */
  private MetaBeanFactory[] factories;
  private XMLMetaBeanFactory xmlFactory;

  public MetaBeanBuilder() {
    this(new MetaBeanFactory[]{new IntrospectorMetaBeanFactory(),
        new XMLMetaBeanFactory()});
  }

  public MetaBeanBuilder(MetaBeanFactory[] factories) {
    setFactories(factories);
  }

  public MetaBeanFactory[] getFactories() {
    return factories;
  }

  public void setFactories(MetaBeanFactory[] factories) {
    this.factories = factories;
    updateXmlFactory();
  }

  private void updateXmlFactory() {
    for (MetaBeanFactory each : factories) {
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
/*
    *//** convenience method *//*
    public void addLastFactory(MetaBeanFactory metaBeanFactory) {
        if (factories == null) factories = new MetaBeanFactory[1];
        else {
            MetaBeanFactory[] facold = factories;
            factories = new MetaBeanFactory[facold.length + 1];
            System.arraycopy(facold, 0, factories, 0, facold.length);
        }
        factories[factories.length - 1] = metaBeanFactory;
        updateXmlFactory();
    }

    */

  /**
   * convenience method
   *//*
    public void addFirstFactory(MetaBeanFactory metaBeanFactory) {
        if (factories == null) factories = new MetaBeanFactory[1];
        else {
            MetaBeanFactory[] facold = factories;
            factories = new MetaBeanFactory[facold.length + 1];
            System.arraycopy(facold, 0, factories, 1, facold.length);
        }
        factories[0] = metaBeanFactory;
        updateXmlFactory();
    }*/
  public void addLoader(XMLMetaBeanLoader loader) {
    assertXmlFactory();
    xmlFactory.addLoader(loader);
  }

  public Map<String, MetaBean> buildAll() throws Exception {
    final Map<String, MetaBean> all = new HashMap<String, MetaBean>();
    if (xmlFactory != null) {
      xmlFactory.visitXMLBeanMeta(null, new XMLMetaBeanFactory.Visitor() {
        public void visit(XMLMetaBean empty, XMLMetaBeanInfos xmlInfos)
            throws Exception {
          if (xmlInfos.getBeans() == null) return; // empty file, ignore
          XMLMetaBeanFactory.XMLResult carrier =
              new XMLMetaBeanFactory.XMLResult(null, xmlInfos);

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

        public MetaBean getMetaBean() {
          return null;  // do nothing
        }
      });
    }
    return all;
  }

  public Map<String, MetaBean> enrichCopies(Map<String, MetaBean> all,
                                            XMLMetaBeanInfos... infosArray)
      throws Exception {
    assertXmlFactory();
    final Map<String, MetaBean> copies = new HashMap<String, MetaBean>(all.size());
    boolean nothing = true;
    XMLMetaBeanFactory.XMLResult carrier = new XMLMetaBeanFactory.XMLResult();
    for (XMLMetaBeanInfos xmlMetaBeanInfos : infosArray) {
      carrier.xmlInfos = xmlMetaBeanInfos;
      if (xmlMetaBeanInfos == null) continue;
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
    if (nothing) return all;
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

  private void assertXmlFactory() {
    if (xmlFactory == null) {
      throw new IllegalStateException("no xmlFactory available");
    }
  }

  public MetaBean buildForId(String beanInfoId) throws Exception {
    final XMLMetaBeanFactory.Visitor v;
    assertXmlFactory();
    xmlFactory.visitXMLBeanMeta(beanInfoId, v = new XMLMetaBeanFactory.Visitor() {
      private MetaBean meta;

      public MetaBean getMetaBean() {
        return meta;
      }

      public void visit(XMLMetaBean xmlMeta, XMLMetaBeanInfos xmlInfos)
          throws Exception {
        if (meta == null) {
          meta = createMetaBean(xmlMeta);
        }
        xmlFactory.enrichMetaBean(meta,
            new XMLMetaBeanFactory.XMLResult(xmlMeta, xmlInfos));
      }


    });
    if (v.getMetaBean() == null) {
      throw new IllegalArgumentException("MetaBean " + beanInfoId + " not found");
    }
    return v.getMetaBean();
  }

  private MetaBean createMetaBean(XMLMetaBean xmlMeta) throws Exception {
    return buildForClass(findLocalClass(xmlMeta.getImpl()));
  }

  protected Class<?> findLocalClass(String className) {
    if (className != null) {
      try {
        return ClassUtils.getClass(className);
      } catch (ClassNotFoundException e) {
        log.trace("class not found: " + className, e);
      }
    }
    return null;
  }

  public MetaBean buildForClass(Class<?> clazz) throws Exception {
    MetaBean meta = new MetaBean();
    if (clazz != null) { // local class here?
      meta.setBeanClass(clazz);
      meta.setId(clazz.getName()); // default id = full class name!
    }
    for (MetaBeanFactory factory : factories) {
      factory.buildMetaBean(meta);
    }
    return meta;
  }

}
