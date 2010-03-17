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
package com.agimatec.validation.xml;

import com.agimatec.validation.MetaBeanFactory;
import static com.agimatec.validation.model.Features.Property.JAVASCRIPT_VALIDATION_FUNCTIONS;
import com.agimatec.validation.model.FeaturesCapable;
import com.agimatec.validation.model.MetaBean;
import com.agimatec.validation.model.MetaProperty;
import com.agimatec.validation.routines.StandardValidation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.*;

/**
 * Description: Create or enrich MetaBeans from agimatec beanInfos xml<br/>
 * User: roman <br/>
 * Date: 07.10.2009 <br/>
 * Time: 13:19:05 <br/>
 * Copyright: Agimatec GmbH
 */
public class XMLMetaBeanFactory implements MetaBeanFactory {
    private static final Log log = LogFactory.getLog(XMLMetaBeanFactory.class);

    // use LinkedHashMap to keep sequence of loaders
    private final Map<XMLMetaBeanLoader, XMLMetaBeanInfos> resources =
          new LinkedHashMap();

    private StandardValidation standardValidation = StandardValidation.getInstance();

    public interface Visitor {
        /**
         * @param xmlMeta  - null or the bean found
         * @param xmlInfos - all infos in a single unit (xml file)
         * @throws Exception
         */
        void visit(XMLMetaBean xmlMeta, XMLMetaBeanInfos xmlInfos) throws Exception;

        MetaBean getMetaBean();
    }

    public static class XMLResult {
        public XMLMetaBean xmlMeta;
        public XMLMetaBeanInfos xmlInfos;

        public XMLResult(XMLMetaBean metaBean, XMLMetaBeanInfos metaInfos) {
            this.xmlMeta = metaBean;
            this.xmlInfos = metaInfos;
        }

        public XMLResult() {
        }
    }

    public void buildMetaBean(final MetaBean metaBean) throws Exception {
        if(metaBean.getId() == null) return;
         visitXMLBeanMeta(metaBean.getId(), new Visitor() {
            public void visit(XMLMetaBean xmlMeta, XMLMetaBeanInfos xmlInfos)
                  throws Exception {
                enrichMetaBean(metaBean, new XMLResult(xmlMeta, xmlInfos));
            }

            public MetaBean getMetaBean() {
                return metaBean;
            }
        });
    }

    /** XMLMetaBeanLoader are used to know "locations" where to get BeanInfos from. */
    public Collection<XMLMetaBeanLoader> getLoaders() {
        return resources.keySet();
    }

    public void addLoader(XMLMetaBeanLoader loader) {
        resources.put(loader, null);
    }

    public StandardValidation getStandardValidation() {
        return standardValidation;
    }

    /** customize the implementation of standardValidation for this builder. */
    public void setStandardValidation(StandardValidation standardValidation) {
        this.standardValidation = standardValidation;
    }

    public void enrichMetaBean(MetaBean meta, XMLResult result) throws Exception {
        if (result.xmlMeta.getId() != null) {
            meta.setId(result.xmlMeta.getId());
        }
        if (result.xmlMeta.getName() != null) {
            meta.setName(result.xmlMeta.getName());
        }
/*        if (meta.getBeanClass() == null && result.xmlMeta.getImpl() != null) {
            meta.setBeanClass(findLocalClass(result.xmlMeta.getImpl()));
        }*/
        result.xmlMeta.mergeFeaturesInto(meta);
        enrichValidations(meta, result.xmlMeta, result, false);
        if (result.xmlMeta.getProperties() != null) {
            for (XMLMetaProperty xmlProp : result.xmlMeta.getProperties()) {
                enrichElement(meta, xmlProp, result);
            }
        }
        if (result.xmlMeta.getBeanRefs() != null) {
            for (XMLMetaBeanReference xmlRef : result.xmlMeta.getBeanRefs()) {
                enrichElement(meta, xmlRef, result);
            }
        }
    }

    protected void enrichValidations(FeaturesCapable prop, XMLFeaturesCapable xmlProp,
                                     XMLResult result, boolean addStandard)
          throws Exception {
        if (xmlProp.getValidators() != null) {
            String[] func = prop.getFeature(JAVASCRIPT_VALIDATION_FUNCTIONS);
            List<String> jsValidators = new ArrayList<String>(
                  xmlProp.getValidators().size() + (func == null ? 0 : func.length));
            if (func != null && func.length > 0) {
                jsValidators.addAll(Arrays.asList(func));
            }
            boolean useStandard = prop instanceof MetaProperty;
            for (XMLMetaValidatorReference valRef : xmlProp.getValidators()) {
                if (standardValidation != null &&
                      valRef.getRefId().equals(standardValidation.getValidationId())) {
                    useStandard = false;
                }
                XMLMetaValidator validator =
                      result.xmlInfos.getValidator(valRef.getRefId());
                if (validator != null) {
                    if (validator.getValidation() != null) {
                        prop.addValidation(validator.getValidation());
                    }
                    if (validator.getJsFunction() != null &&
                          !jsValidators.contains(validator.getJsFunction())) {
                        jsValidators.add(validator.getJsFunction());
                    }
                }
            }
            if (!jsValidators.isEmpty()) {
                prop.putFeature(JAVASCRIPT_VALIDATION_FUNCTIONS,
                      jsValidators.toArray(new String[jsValidators.size()]));
            }
            if (useStandard && standardValidation != null) {
                if (!prop.hasValidation(standardValidation))
                    prop.addValidation(standardValidation);
            }
        } else if (addStandard && standardValidation != null &&
              !prop.hasValidation(standardValidation)) {
            prop.addValidation(standardValidation);
        }
    }

    protected MetaProperty enrichElement(MetaBean meta, XMLMetaElement xmlProp,
                                         XMLResult result) throws Exception {
        MetaProperty prop = meta.getProperty(xmlProp.getName());
        if (prop == null) {
            prop = new MetaProperty();
            prop.setName(xmlProp.getName());
            meta.putProperty(xmlProp.getName(), prop);
        }
        xmlProp.mergeInto(prop);
        enrichValidations(prop, xmlProp, result, true);
        return prop;
    }


    public void visitXMLBeanMeta(String beanId, Visitor visitor) throws Exception {
        for (Map.Entry<XMLMetaBeanLoader, XMLMetaBeanInfos> entry : resources
              .entrySet()) {
            if (entry.getValue() == null) {
                // load when not already loaded
                try {
                    entry.setValue(entry.getKey().load());
                } catch (IOException e) {
                    handleLoadException(entry.getKey(), e);
                }
            }
            if (entry.getValue() != null) { // search in loaded infos for the 'name'
                if (beanId == null) {
                    visitor.visit(null, entry.getValue());
                } else {
                    XMLMetaBean found = entry.getValue().getBean(beanId);
                    if (found != null) {
                        visitor.visit(found, entry.getValue());
                    }
                }
            }
        }
    }

    /**
     * find a bean by the bean-id (=bean.name)
     *
     * @return null or the bean found from the first loader that has it.
     */
    protected XMLResult findXMLBeanMeta(String beanId) {
        for (Map.Entry<XMLMetaBeanLoader, XMLMetaBeanInfos> entry : resources
              .entrySet()) {
            if (entry.getValue() == null) {
                // load when not already loaded
                try {
                    entry.setValue(entry.getKey().load());
                } catch (IOException e) {
                    handleLoadException(entry.getKey(), e);
                }
            }
            if (entry.getValue() != null) { // search in loaded infos for the 'name'
                XMLMetaBean found = entry.getValue().getBean(beanId);
                if (found != null) {
                    return new XMLResult(found, entry.getValue());
                }
            }
        }
        return null; // not found!
    }

    public void handleLoadException(Object loader, IOException e) {
        log.error("error loading " + loader, e);
    }

}
