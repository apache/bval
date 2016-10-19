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
package org.apache.bval.json;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.bval.example.BusinessEnum;
import org.apache.bval.example.BusinessObject;
import org.apache.bval.example.BusinessObjectAddress;
import org.apache.bval.model.DynaTypeEnum;
import org.apache.bval.model.MetaBean;
import org.apache.bval.model.MetaProperty;
import org.apache.bval.xml.XMLMetaBeanManager;
import org.apache.bval.xml.XMLMetaBeanURLLoader;
import org.junit.Before;
import org.junit.Test;

/**
 * Description: <br>
 * Author: roman.stumm<br>
 */
public class JSONGeneratorTest {
    private XMLMetaBeanManager mbm;

    @Before
    public void setUp() throws Exception {
        mbm = new XMLMetaBeanManager();
        mbm.addLoader(new XMLMetaBeanURLLoader(BusinessObject.class.getResource("test-beanInfos.xml")));
    }

    @Test
    public void testBeanInfosCustomPatchGenerated() throws Exception {
        MetaBean mbean = mbm.findForClass(BusinessObject.class);
        MetaProperty mprop = mbean.getProperty("lastName");
        assertTrue(mprop.isMandatory());

        mbm.getCache().removeFromCache(mbean);
        mbm.addLoader(new XMLMetaBeanURLLoader(BusinessObject.class.getResource("test-beanInfos-custom.xml")));
        mbean = mbm.findForClass(BusinessObject.class);
        mprop = mbean.getProperty("lastName");
        assertTrue(!mprop.isMandatory());

        JSONGenerator converter = new JSONGenerator();

        List<MetaBean> metaBeans = new ArrayList<MetaBean>(2);
        metaBeans.add(mbean);
        MetaBean mbean2 = mbm.findForId("UnknownObject");
        metaBeans.add(mbean2);
        String json = converter.toJSON(metaBeans);
        assertNotNull(json);
    }

    @Test
    public void testJSON() throws Exception {
        MetaBean info = mbm.findForClass(BusinessObject.class);
        MetaBean info2 = info.getProperty("address").getMetaBean();

        // empty default bean without xml backup
        MetaBean info3 = mbm.findForClass(BusinessObjectAddress.class);
        JSONGenerator converter = new JSONGenerator();

        List<MetaBean> metaBeans = new ArrayList<MetaBean>(2);
        metaBeans.add(info);
        metaBeans.add(info2);
        metaBeans.add(info3);
        String json = converter.toJSON(metaBeans);
        assertNotNull(json);
    }

    @Test
    public void testJSON_dynaTypeEnum() throws Exception {
        MetaBean info = mbm.findForClass(BusinessObject.class);
        MetaProperty choice = info.getProperty("choice");
        choice.setType(new DynaTypeEnum(BusinessEnum.class, "CUSTOM_1", "CUSTOM_2"));

        JSONGenerator converter = new JSONGenerator();

        List<MetaBean> metaBeans = new ArrayList<MetaBean>(1);
        metaBeans.add(info);
        String json = converter.toJSON(metaBeans);
        assertNotNull(json);
        assertTrue(json.indexOf("CUSTOM_1") > 0);
        assertTrue(json.indexOf("CUSTOM_2") > 0);
        assertTrue(json.indexOf("VALUE1") < 0);
    }
}
