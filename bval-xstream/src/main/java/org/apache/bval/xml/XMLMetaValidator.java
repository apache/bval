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
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.apache.bval.model.Validation;

import java.io.Serializable;

/**
 * Description: <br/>
 */
@XStreamAlias("validator")
public class XMLMetaValidator implements Serializable {

    @XStreamOmitField
    private static final long serialVersionUID = 1L;

    @XStreamAsAttribute
    private String id;
    @XStreamAsAttribute
    private String java; // implementation of Validation

    @XStreamAsAttribute
    private String jsFunction; // name of java script function

    @XStreamOmitField
    private transient Validation validation;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJava() {
        return java;
    }

    public void setJava(String java) {
        this.java = java;
    }

    public void setValidation(Validation validation) {
        this.validation = validation;
    }

    public Validation getValidation() {
        return validation;
    }

    public String getJsFunction() {
        return jsFunction;
    }

    public void setJsFunction(String jsFunction) {
        this.jsFunction = jsFunction;
    }
}
