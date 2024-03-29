/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bval.extras.constraints.checkdigit;

import jakarta.validation.ConstraintValidator;
import java.lang.annotation.Annotation;

/**
 * ABA Number Validator Test.
 */
public class ABANumberValidatorTest extends AbstractCheckDigitTest {

    @Override
    protected ConstraintValidator<? extends Annotation, ? super String> getConstraint() {
        return new ABANumberValidator();
    }

    @Override
    protected String[] getValid() {
        return new String[] { "123456780", "123123123", "011000015", "111000038", "231381116", "121181976" };
    }

}
