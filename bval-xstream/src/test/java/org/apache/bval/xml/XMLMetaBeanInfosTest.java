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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.bval.example.BusinessObject;
import org.apache.bval.example.BusinessObjectAddress;
import org.junit.Test;

/**
 * XMLMetaBean Tester.
 */
public class XMLMetaBeanInfosTest {

    @Test
    public void testBeanInfosToXML() {
        XMLMetaBeanInfos infos = new XMLMetaBeanInfos();
        infos.setBeans(new ArrayList<XMLMetaBean>());
        infos.setValidators(new ArrayList<XMLMetaValidator>());

        XMLMetaValidator validator = new XMLMetaValidator();
        validator.setId("mandatory");
        validator.setJava("org.apache.bval.MandatoryValidator");

        infos.getValidators().add(validator);

        validator = new XMLMetaValidator();
        validator.setId("email");
        validator.setJava("org.apache.bval.EMailValidation");

        infos.getValidators().add(validator);

        XMLMetaBean bean = new XMLMetaBean();
        bean.putFeature("DOMAIN", "TestProfile");
        bean.putFeature("label-key", "business-object-label");
        bean.setId("User");
        bean.setImpl(BusinessObject.class.getName());
        bean.setProperties(new ArrayList<XMLMetaProperty>());
        XMLMetaProperty property = new XMLMetaProperty();
        property.setName("userId");
        property.setMandatory(XMLMetaValue.MANDATORY);
        bean.getProperties().add(property);

        property = new XMLMetaProperty();
        property.setName("firstName");
        property.setMandatory(XMLMetaValue.MANDATORY);
        property.setMaxLength(100);
        bean.getProperties().add(property);

        property = new XMLMetaProperty();
        property.setName("lastName");
        property.setMandatory(XMLMetaValue.MANDATORY);
        property.setMaxLength(100);
        bean.getProperties().add(property);

        property = new XMLMetaProperty();
        property.setName("title");
        property.setMandatory(XMLMetaValue.OPTIONAL);
        property.setMaxLength(10);
        bean.getProperties().add(property);

        property = new XMLMetaProperty();
        property.setName("dateBirth");
        property.setMandatory(XMLMetaValue.OPTIONAL);
        property.setTimeLag(XMLMetaValue.TIMELAG_Past);
        bean.getProperties().add(property);

        property = new XMLMetaProperty();
        property.setName("validTo");
        property.setMandatory(XMLMetaValue.OPTIONAL);
        property.setTimeLag(XMLMetaValue.TIMELAG_Future);
        bean.getProperties().add(property);

        property = new XMLMetaProperty();
        property.setName("email");
        property.putFeature(XMLMetaValue.ANNOKEY_Widget, "entry");
        property.putFeature(XMLMetaValue.ANNOKEY_TableColumn, true);
        Map<String, String> formatterMap = new HashMap<String, String>();
        formatterMap.put("locale", "DE");
        formatterMap.put("style", "info");
        property.putFeature("ajax-formatter", formatterMap);
        property.addValidator("email");
        bean.getProperties().add(property);

        infos.getBeans().add(bean);

        XMLMetaBean bean2 = new XMLMetaBean();
        bean2.setId("Address");
        bean2.setImpl(BusinessObjectAddress.class.getName());
        property = new XMLMetaProperty();
        property.setName("city");
        bean2.putProperty(property);
        property = new XMLMetaProperty();
        property.setName("country");
        property.setMaxLength(10);
        property.setMandatory(XMLMetaValue.MANDATORY);
        bean2.putProperty(property);

        XMLMetaBeanReference relation = new XMLMetaBeanReference();
        relation.setName("address");
        relation.setBeanId("Address");
        relation.setMandatory(XMLMetaValue.OPTIONAL);
        bean.putBeanRef(relation);

        infos.getBeans().add(bean2);

        String xml = XMLMapper.getInstance().getXStream().toXML(infos);
        XMLMetaBeanInfos infos2 = (XMLMetaBeanInfos) XMLMapper.getInstance().getXStream().fromXML(xml);
        assertEquals(2, infos2.getBeans().size());
    }

    @Test
    public void testMaxValueParsing() {
        String xml = "\n" + "<beanInfos>  <bean id=\"org.apache.bval.test.model.Profile\">\n"
            + "    <property name=\"activationDay\" minValue=\"1\" maxValue=\"31\"/>\n"
            + "    <property name=\"activationMonth\" minValue=\"1\" maxValue=\"12\"/>\n" + "  </bean></beanInfos>";
        XMLMetaBeanInfos beanInfos = (XMLMetaBeanInfos) XMLMapper.getInstance().getXStream().fromXML(xml);
        assertNotNull(beanInfos);
        assertEquals(Integer.valueOf(31), beanInfos.getBeans().get(0).getProperty("activationDay").getMaxValue());
        assertEquals(Integer.valueOf(1), beanInfos.getBeans().get(0).getProperty("activationDay").getMinValue());
    }

}
