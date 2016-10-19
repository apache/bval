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

import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.constraints.Pattern;

import org.apache.bval.util.PropertyAccess;
import org.junit.Test;

/**
 * Description: <br>
 * User: roman.stumm<br>
 * Date: 21.04.2010<br>
 * Time: 14:21:45<br>
 */
public class TckReproducerTest extends ValidationTestBase {

    private static <T> void assertCorrectNumberOfViolations(Set<ConstraintViolation<T>> violations,
        int expectedViolations) {
        assertEquals("Wrong number of constraint violations. Expected: " + expectedViolations + " Actual: "
            + violations.size(), expectedViolations, violations.size());
    }

    @Test
    public void testPropertyAccessOnNonPublicClass() throws Exception {
        Car car = new Car("USd-298");
        assertEquals(car.getLicensePlateNumber(), PropertyAccess.getProperty(car, "licensePlateNumber"));

        assertCorrectNumberOfViolations(validator.validateProperty(car, "licensePlateNumber", First.class,
            org.apache.bval.jsr.example.Second.class), 1);

        car.setLicensePlateNumber("USD-298");
        assertCorrectNumberOfViolations(validator.validateProperty(car, "licensePlateNumber", First.class,
            org.apache.bval.jsr.example.Second.class), 0);
    }

    static class Car {
        @Pattern(regexp = "[A-Z][A-Z][A-Z]-[0-9][0-9][0-9]", groups = { First.class, Second.class })
        private String licensePlateNumber;

        Car(String licensePlateNumber) {
            this.licensePlateNumber = licensePlateNumber;
        }

        public String getLicensePlateNumber() {
            return licensePlateNumber;
        }

        public void setLicensePlateNumber(String licensePlateNumber) {
            this.licensePlateNumber = licensePlateNumber;
        }
    }

    interface First {
    }

    interface Second {
    }
}
