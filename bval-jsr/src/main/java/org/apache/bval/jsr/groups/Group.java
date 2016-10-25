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
package org.apache.bval.jsr.groups;

import javax.validation.groups.Default;

/**
 * Immutable object that wraps an interface representing a single group.
 */
public final class Group {
    /**
     * the Default Group
     */
    public static final Group DEFAULT = new Group(Default.class);

    private final Class<?> group;

    /**
     * Create a new Group instance.
     * @param group
     */
    public Group(Class<?> group) {
        this.group = group;
    }

    /**
     * Get the actual group class.
     * @return
     */
    public Class<?> getGroup() {
        return group;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Group{" + "group=" + group + '}';
    }

    /**
     * Learn whether the group represented is the default group.
     * @return boolean
     */
    public boolean isDefault() {
        return Default.class.equals(group);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Group group1 = (Group) o;

        return group != null ? group.equals(group1.group) : group1.group == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (group != null ? group.hashCode() : 0);
    }
}
