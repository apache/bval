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

/**
 * Description: Interface of the object that holds all XMLMetaBeanLoaders <br/>
 */
public interface XMLMetaBeanRegistry {
    /**
     * add a loader for xml bean infos.
     * the registry should use the loader in the sequence they have been added.
     */
    void addLoader(XMLMetaBeanLoader loader);

    /**
     * convenience method to add a loader for a xml file in the classpath
     *
     * @param resource - path of xml file in classpath
     */
    void addResourceLoader(String resource);
}
