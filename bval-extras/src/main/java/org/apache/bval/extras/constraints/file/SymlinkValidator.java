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
import java.io.IOException;


/**
 * Description: <br/>
 */
public class SymlinkValidator implements ConstraintValidator<Symlink, File> {

    /**
     * The Windows separator character.
     */
    private static final char WINDOWS_SEPARATOR = '\\';

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(File value, ConstraintValidatorContext context) {
        if (!value.exists()) {
            return false;
        }

        // routine kindly borrowed from Apache Commons-IO

        if (File.separatorChar == WINDOWS_SEPARATOR) {
            return false;
        }

        try {
            File fileInCanonicalDir = null;
            if (value.getParent() == null) {
                fileInCanonicalDir = value;
            } else {
                File canonicalDir = value.getParentFile().getCanonicalFile();
                fileInCanonicalDir = new File(canonicalDir, value.getName());
            }

            return (!fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile()));
        } catch (IOException e) {
            // TODO: is it true?
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(Symlink parameters) {
        // do nothing (as long as Symlink has no properties)
    }

}
