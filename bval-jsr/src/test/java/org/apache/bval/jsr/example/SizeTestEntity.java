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
package org.apache.bval.jsr.example;

import javax.validation.constraints.Size;
import java.util.Collection;
import java.util.Map;

/**
 * Description: <br/>
 */
public class SizeTestEntity {
    @Size(max = 2)
    public Map<String, String> map;
    @Size(max = 2)
    public Collection<String> coll;
    @Size(max = 2)
    public String text;

    @Size(max = 2)
    public Object[] oa;
    @Size(max = 2)
    public byte[] ba;
    @Size(max = 2)
    public int[] it;
    @Size(max = 2)
    public Integer[] oa2;
    @Size(max = 2)
    public boolean[] boa;
    @Size(max = 2)
    public char[] ca;
    @Size(max = 2)
    public double[] da;
    @Size(max = 2)
    public float[] fa;
    @Size(max = 2)
    public long[] la;
    @Size(max = 2)
    public short[] sa;
}
