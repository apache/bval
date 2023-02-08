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
package org.apache.bval.jsr;

import jakarta.validation.MessageInterpolator;
import jakarta.validation.MessageInterpolator.Context;

/**
 * Vendor-specific {@link MessageInterpolator.Context} interface extension to
 * provide access to validator configuration properties.
 */
public interface ApacheMessageContext extends Context {

    /**
     * Get the configuration property value specified by {@code propertyKey}, if available.
     * @param propertyKey
     * @return {@link String} or {@code null}
     */
    String getConfigurationProperty(String propertyKey);
}
