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

import java.util.ArrayList;
import java.util.List;

import javax.validation.GroupDefinitionException;

import org.apache.bval.util.Exceptions;

/**
 * Defines the order to validate groups during validation. with some inspiration
 * from reference implementation
 *
 * @author Roman Stumm
 */
public class Groups {
    /** The list of sequences. */
    private final List<List<Group>> sequences = new ArrayList<>();

    /** The list of single groups. */
    final List<Group> groups = new ArrayList<>();

    /**
     * Get the Groups.
     * 
     * @return {@link List} of {@link Group}.
     */
    public List<Group> getGroups() {
        return groups;
    }

    /**
     * Get the Group sequences.
     * 
     * @return {@link List} of {@link List} of {@link Group}
     */
    public List<List<Group>> getSequences() {
        return sequences;
    }

    /**
     * Insert a {@link Group}.
     * 
     * @param group
     *            to insert
     */
    void insertGroup(Group group) {
        if (!groups.contains(group)) {
            groups.add(group);
        }
    }

    /**
     * Insert a sequence.
     * 
     * @param groups
     *            {@link List} of {@link Group} to insert
     */
    void insertSequence(List<Group> groups) {
        if (!(groups == null || groups.isEmpty() || sequences.contains(groups))) {
            sequences.add(groups);
        }
    }

    /**
     * Assert that the default group can be expanded to
     * <code>defaultGroups</code>.
     * 
     * @param defaultGroups
     */
    public void assertDefaultGroupSequenceIsExpandable(List<Group> defaultGroups) {
        for (List<Group> groupList : sequences) {
            final int idx = groupList.indexOf(Group.DEFAULT);
            if (idx >= 0) {
                ensureExpandable(groupList, defaultGroups, idx);
            }
        }
    }

    private void ensureExpandable(List<Group> groupList, List<Group> defaultGroupList, int defaultGroupIndex) {
        for (int i = 0, sz = defaultGroupList.size(); i < sz; i++) {
            final Group group = defaultGroupList.get(i);
            if (group.isDefault()) {
                continue; // the default group is the one we want to replace
            }
            // sequence contains group of default group sequence
            final int index = groupList.indexOf(group);
            if (index < 0) {
                // group is not in the sequence
                continue;
            }
            if ((i == 0 && index == defaultGroupIndex - 1)
                || (i == defaultGroupList.size() - 1 && index == defaultGroupIndex + 1)) {
                // if we are at the beginning or end of he defaultGroupSequence
                // and the matches are either directly before or after we can
                // continue, since we basically have two groups
                continue;
            }
            Exceptions.raise(GroupDefinitionException::new, "Unable to expand default group list %s into sequence %s",
                defaultGroupList, groupList);
        }
    }
}