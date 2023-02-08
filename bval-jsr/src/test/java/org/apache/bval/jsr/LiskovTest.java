/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.bval.jsr;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;

import org.junit.Test;

public class LiskovTest {
    @Test
    public void testBVal167() {
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            factory.getValidator().getConstraintsForClass(Impl.class);
        }
    }

    public interface Api {
        String read(@NotNull String key);
    }

    public interface Api2 extends Api {
        @Override
        String read(String key);
    }

    public static abstract class Base implements Api {
        @Override
        public String read(final String key) {
            return null;
        }
    }

    public static class Impl extends Base implements Api2 {
    }
}
