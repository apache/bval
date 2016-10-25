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
package org.apache.bval.extras.constraints.file;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.io.File;

/**
 * Description: <br/>
 */
public class DirectoryValidator implements ConstraintValidator<Directory, File> {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(File value, ConstraintValidatorContext context) {
        return value.exists() && value.isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(Directory parameters) {
        // do nothing (as long as Directory has no properties)
    }

}
