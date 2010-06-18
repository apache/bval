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
package org.apache.bval.xml;

import org.apache.bval.MetaBeanManager;
import org.apache.bval.model.MetaBean;
import org.apache.bval.util.PrivilegedActions;

import java.util.Map;

/**
 * Description: internal implementation class to construct
 * metabeans with factories and from xstream xml files.
 * You can register different XMLMetaBeanLoaders (see addLoader()) to register xstream-xml-files that
 * contain meta-data.
 * You can merge + unify meta data with method enrichCopies().
 * <br/>
 * User: roman.stumm<br>
 * Date: 17.06.2010<br>
 * Time: 09:47:14<br>
 */
public class XMLMetaBeanManager extends MetaBeanManager implements XMLMetaBeanRegistry, MetaBeanEnricher {
  public XMLMetaBeanManager() {
    this(new XMLMetaBeanBuilder());
  }

  public XMLMetaBeanManager(XMLMetaBeanBuilder builder) {
    super(builder);
  }

  public void addResourceLoader(String resource) {
    addLoader(new XMLMetaBeanURLLoader(
        PrivilegedActions.getClassLoader(getClass()).getResource(resource)));
  }

  public synchronized void addLoader(XMLMetaBeanLoader loader) {
    ((XMLMetaBeanBuilder)builder).addLoader(loader);
    cache.clear(); // clear because new loaders can affect ALL MetaBeans already created!
    complete = false;
  }

  /**
   * @param infos - the patches to apply
   * @return all MetaBeans for classes that have a xml descriptor and
   *         additional the MetaBeans loaded by the given loaders.
   *         The given loaders may also return patches for MetaBeans that have
   *         also been returned by other loaders. The beans with patches for
   *         references to patched beans will be copied.
   */
  public Map<String, MetaBean> enrichCopies(XMLMetaBeanInfos... infos) {
    Map<String, MetaBean> cached = findAll();
    try {
      Map<String, MetaBean> patched = ((XMLMetaBeanBuilder)builder).enrichCopies(cached, infos);
      for (Object entry : patched.values()) {
        MetaBean meta = (MetaBean) entry;
        computeRelationships(meta, patched);
      }
      return patched;
    } catch (RuntimeException e) {
      throw e; // do not wrap runtime exceptions
    } catch (Exception e) {
      throw new IllegalArgumentException("error enriching beanInfos", e);
    }
  }
}
