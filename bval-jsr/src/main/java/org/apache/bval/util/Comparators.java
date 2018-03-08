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
package org.apache.bval.util;

import java.util.Comparator;
import java.util.Iterator;

/**
 * {@link Comparator} related utilities.
 */
public class Comparators {

    /**
     * Get a {@link Comparator} capable of comparing {@link Iterable}s.
     * 
     * @param each
     * @return {@link Comparator}
     */
    public static <T, I extends Iterable<T>> Comparator<I> comparingIterables(Comparator<? super T> each) {
        return (quid, quo) -> {
            final Iterator<T> quids = quid.iterator();
            final Iterator<T> quos = quo.iterator();

            while (quids.hasNext()) {
                if (quos.hasNext()) {
                    final int rz = each.compare(quids.next(), quos.next());
                    if (rz != 0) {
                        return rz;
                    }
                    continue;
                }
                return 1;
            }
            return quos.hasNext() ? -1 : 0;
        };
    }

    private Comparators() {
    }
}
