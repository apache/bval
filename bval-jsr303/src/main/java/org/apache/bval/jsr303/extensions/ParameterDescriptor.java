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
package org.apache.bval.jsr303.extensions;

import javax.validation.metadata.ElementDescriptor;

/**
 * Description: This class will disappear when such
 * functionality is part of the JSR303 specification.<br/>
 * User: roman <br/>
 * Date: 11.11.2009 <br/>
 * Time: 12:37:59 <br/>
 * Copyright: Agimatec GmbH
 */
public interface ParameterDescriptor extends ElementDescriptor {
    /**
     * Is the parameter marked by the <code>@Valid</code> annotation.
     *
     * @return <code>true</code> if the annotation is present, <code>false</code> otherwise.
     */
    boolean isCascaded();

    int getIndex();
}

