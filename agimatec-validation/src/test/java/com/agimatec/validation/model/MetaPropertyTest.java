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
package com.agimatec.validation.model;

import com.agimatec.validation.example.BusinessEnum;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * MetaProperty Tester.
 *
 * @author <Authors name>
 * @since <pre>02/12/2009</pre>
 * @version 1.0
 */
public class MetaPropertyTest extends TestCase {
    public MetaPropertyTest(String name) {
        super(name);
    }


    public void testGetTypeClass() throws Exception {
        MetaProperty prop = new MetaProperty();
        prop.setType(String.class);
        assertEquals(String.class, prop.getTypeClass());
        assertEquals(String.class, prop.getType());
        prop.setType(new DynaTypeEnum(BusinessEnum.class, BusinessEnum.VALUE1.name(),
              BusinessEnum.VALUE3.name()));
        assertEquals(BusinessEnum.class, prop.getTypeClass());
        assertEquals(2, ((DynaTypeEnum)prop.getType()).getEnumConstants().length);
    }


    public static Test suite() {
        return new TestSuite(MetaPropertyTest.class);
    }
}
