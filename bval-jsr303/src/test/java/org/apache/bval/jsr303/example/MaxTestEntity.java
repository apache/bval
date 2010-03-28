/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bval.jsr303.example;

import javax.validation.constraints.Max;
import java.math.BigDecimal;

/**
 * Description: <br/>
 * User: roman <br/>
 * Date: 17.11.2009 <br/>
 * Time: 15:21:45 <br/>
 * Copyright: Agimatec GmbH
 */
public class MaxTestEntity {
    @Max(100)
    private String text;
    private String property;

    @Max(300)
    private long longValue;

    private BigDecimal decimalValue;

    public String getText() {
        return text;
    }

    @Max(200)
    public String getProperty() {
        return property;
    }

    public long getLongValue() {
        return longValue;
    }

    @Max(400)
    public BigDecimal getDecimalValue() {
        return decimalValue;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    public void setDecimalValue(BigDecimal decimalValue) {
        this.decimalValue = decimalValue;
    }
}
