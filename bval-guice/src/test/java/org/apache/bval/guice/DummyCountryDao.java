/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bval.guice;

import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 
 *
 * @version $Id$
 */
@Singleton
public class DummyCountryDao {

    @Validate(
        groups = { Insert.class },
        validateReturnedValue = true
    )
    public Country insertCountry(@NotNull(groups = { Insert.class }) String name,
            @NotNull(groups = { Insert.class }) @Size(max = 2, groups = { Insert.class, Update.class }) String iso2Code,
            @NotNull(groups = { Insert.class }) @Size(max = 3, groups = { Insert.class, Update.class }) String iso3Code) {
        Country country = new Country();
        country.setName(name);
        country.setIso2Code(iso2Code);
        country.setIso3Code(iso3Code);
        return country;
    }

    @Validate(
        groups = { Update.class },
        rethrowExceptionsAs = DummyException.class,
        exceptionMessage = "This is just a dummy message %s"
    )
    public int updateCountry(@Valid Country country) {
        return 0;
    }

}
