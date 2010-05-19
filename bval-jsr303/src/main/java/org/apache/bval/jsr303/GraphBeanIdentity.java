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
package org.apache.bval.jsr303;

/**
 * Class that stores the needed properties to avoid circular paths when
 * validating an object graph.
 * <p>
 * These properties are:
 * <ul>
 * <li>The ref of the bean to which the validation would be applied.</li>
 * <li>The current group being validated.</li>
 * </ul>
 * 
 * FIXME: Owner is currently not used in identity checking, and probably won't
 * never be. So it is likely to be deleted.
 * 
 * @author Carlos Vara
 */
public class GraphBeanIdentity {
    
    private final Object bean;
    private final Class<?> group;
    private final Class<?> owner;

    public GraphBeanIdentity(Object bean, Class<?> group, Class<?> owner) {
        this.bean = bean;
        this.group = group;
        this.owner = owner;
    }
    
    public Object getBean() {
        return bean;
    }

    public Class<?> getGroup() {
        return group;
    }

    public Class<?> getOwner() {
        return owner;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof GraphBeanIdentity)) {
            return false;
        }

        GraphBeanIdentity other = (GraphBeanIdentity) obj;

        // Bean ref must be the same
        if (this.bean != other.bean) {
            return false;
        }

        // Group ref must be the same
        if (this.group != other.group) {
            return false;
        }
        
//        // Owner ref must be the same
//        if (this.owner != other.owner) {
//            return false;
//        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((this.bean == null) ? 0 : this.bean.hashCode());
        result = prime * result
                + ((this.group == null) ? 0 : this.group.hashCode());
//        result = prime * result
//                + ((this.owner == null) ? 0 : this.owner.hashCode());
        return result;
    }

    
}
