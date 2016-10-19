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
package org.apache.bval.jsr.groups.implicit;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Represents an order in the system
 */
public class Order implements Auditable {
    private String creationDate;
    private String lastUpdate;
    private String lastModifier;
    private String lastReader;

    private String orderNumber;

    @Override
    public String getCreationDate() {
        return this.creationDate;
    }

    @Override
    public String getLastUpdate() {
        return this.lastUpdate;
    }

    @Override
    public String getLastModifier() {
        return this.lastModifier;
    }

    @Override
    public String getLastReader() {
        return this.lastReader;
    }

    @NotNull
    @Size(min=10, max=10)
    public String getOrderNumber() {
        return this.orderNumber;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void setLastModifier(String lastModifier) {
        this.lastModifier = lastModifier;
    }

    public void setLastReader(String lastReader) {
        this.lastReader = lastReader;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
}