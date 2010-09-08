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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.apache.bval.model.Validation;
import org.apache.commons.collections.FastHashMap;
import org.apache.commons.lang.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Description: root element of a xml-beanInfos document<br/>
 */
@XStreamAlias("beanInfos")
public class XMLMetaBeanInfos {
    @XStreamAsAttribute
    private String id;
    @XStreamAsAttribute
    private String version;
    @XStreamImplicit
    private List<XMLMetaValidator> validators;
    @XStreamImplicit
    private List<XMLMetaBean> beans;
    @XStreamOmitField
    private Map<String, XMLMetaBean> beanLookup;
    @XStreamOmitField
    private Map<String, XMLMetaValidator> validationLookup;

    /**
     * used for identification, may be empty, if there is no database origin for this object.
     * could also contain a file-name - can be used flexible...
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * used for change-detection, when some other component caches MetaBeans based on this
     * object. when the version changes, the cache could compare to its version state and recompute.
     * can be used flexible...
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<XMLMetaValidator> getValidators() {
        return validators;
    }

    public void setValidators(List<XMLMetaValidator> validators) {
        this.validators = validators;
    }

    public List<XMLMetaBean> getBeans() {
        return beans;
    }

    public void setBeans(List<XMLMetaBean> beans) {
        this.beans = beans;
    }

    public XMLMetaBean getBean(String id) {
        if (beans == null) return null;
        if (beanLookup == null) initBeanLookup();
        return beanLookup.get(id);
    }

    private void initBeanLookup() {
        beanLookup = new FastHashMap();
        for (XMLMetaBean bean : beans) {
            beanLookup.put(bean.getId(), bean);
        }
        ((FastHashMap) beanLookup).setFast(true);
    }

    private void initValidationLookup() throws Exception {
        validationLookup = new FastHashMap();
        for (XMLMetaValidator xv : validators) {
            if (xv.getJava() != null) {
                Validation validation =
                        (Validation) ClassUtils.getClass(xv.getJava()).newInstance();
                xv.setValidation(validation);
                validationLookup.put(xv.getId(), xv);
            }
        }
        ((FastHashMap) validationLookup).setFast(true);
    }

    public void addBean(XMLMetaBean bean) {
        if (beans == null) beans = new ArrayList();
        beans.add(bean);
    }

    public XMLMetaValidator getValidator(String id) throws Exception {
        if (validators == null) return null;
        if (validationLookup == null) initValidationLookup();
        return validationLookup.get(id);
    }

    public void addValidator(XMLMetaValidator validator) {
        if (validators == null) validators = new ArrayList();
        validators.add(validator);
    }
}
