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
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: internal implementation class to construct 
 * metabeans with factories<br/>
 */
public class MetaBeanBuilder {
  private static final Log log = LogFactory.getLog(MetaBeanBuilder.class);

  /**
   * here you can install different kinds of factories to create MetaBeans from
   */
  private MetaBeanFactory[] factories;


  public MetaBeanBuilder() {
    this(new MetaBeanFactory[]{new IntrospectorMetaBeanFactory()});
  }

  public MetaBeanBuilder(MetaBeanFactory[] factories) {
    setFactories(factories);
  }

  public MetaBeanFactory[] getFactories() {
    return factories;
  }

  public void setFactories(MetaBeanFactory[] factories) {
    this.factories = factories;
  }

  public MetaBean buildForId(String beanInfoId) throws Exception {
    throw new IllegalArgumentException("MetaBean " + beanInfoId + " not found");
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


  public Map<String, MetaBean> buildAll() throws Exception {
    return new HashMap<String, MetaBean>();
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
