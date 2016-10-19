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
package org.apache.bval.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * MetaProperty Tester.
 *
 * @author <Authors name>
 * @since <pre>02/12/2009</pre>
 * @version 1.0
 */
public class MetaPropertyTest {

    @Test
    public void testGetTypeClass() throws Exception {
        MetaProperty prop = new MetaProperty();
        prop.setType(String.class);
        assertEquals(String.class, prop.getTypeClass());
        assertEquals(String.class, prop.getType());
        prop.setType(new DynaTypeEnum(ExampleEnum.class, ExampleEnum.VALUE1.name(),
              ExampleEnum.VALUE3.name()));
        assertEquals(ExampleEnum.class, prop.getTypeClass());
        assertEquals(2, ((DynaTypeEnum)prop.getType()).getEnumConstants().length);
    }

}
