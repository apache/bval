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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.bval.MetaBeanFinder;
import org.apache.bval.example.BusinessObject;
import org.apache.bval.model.MetaBean;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Description: <br>
 * User: roman.stumm<br>
 * Date: 17.06.2010<br>
 * Time: 10:28:48<br>
 */
public class XMLMetaBeanManagerTest {
    private static XMLMetaBeanManager mbm = new XMLMetaBeanManager();

    @BeforeClass
    public static void setUp() throws Exception {
        mbm.addLoader(new XMLMetaBeanURLLoader(BusinessObject.class.getResource("test-beanInfos.xml")));
    }

    @Test
    public void testEnrichCopies() throws Exception {
        Map<String, MetaBean> copies = mbm.enrichCopies(
            new XMLMetaBeanURLLoader(BusinessObject.class.getResource("test-beanInfos-custom.xml")).load());
        assertNotNull(copies);
        MetaBean mb = copies.get(BusinessObject.class.getName());
        assertFalse(mb.getProperty("lastName").isMandatory());
        MetaBean mb2 = mbm.findForClass(BusinessObject.class);
        assertTrue(mb2.getProperty("lastName").isMandatory());
    }

    @Test
    public void testCopy() {
        MetaBean mb = mbm.findForClass(BusinessObject.class);
        MetaBean mb2 = mb.copy();
        assertNotSame(mb, mb2);
        assertNotSame(mb.getProperty("dateBirth"), mb2.getProperty("dateBirth"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testFindForClass() throws Exception {
        MetaBeanFinder finder = mbm;
        MetaBean info = finder.findForClass(BusinessObject.class);
        assertNotNull(info);
        assertSame(info, info.getProperty("address").getMetaBean().getProperty("owner").getMetaBean());
        assertSame(info, info.getProperty("addresses").getMetaBean().getProperty("owner").getMetaBean());
        assertTrue(info.getProperty("email").getJavaScriptValidations().length > 0);
    }

    @Test
    public void testFindAll() {
        Map<String, MetaBean> all = mbm.findAll();
        assertNotNull(all);
        Map<String, MetaBean> all2 = mbm.findAll();
        assertEquals(all.size(), all2.size());
        assertSame(all.get(BusinessObject.class.getName()), all2.get(BusinessObject.class.getName()));
        assertNotNull(all.get(BusinessObject.class.getName()));
        MetaBean bean = all.get(BusinessObject.class.getName());
        assertSame(bean, bean.getProperty("address").getMetaBean().getProperty("owner").getMetaBean());
        assertSame(bean, bean.getProperty("addresses").getMetaBean().getProperty("owner").getMetaBean());
    }

    static void assertNotSame(Object o1, Object o2) {
        assertFalse(o1 == o2);
    }
}
