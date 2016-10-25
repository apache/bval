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

import javax.validation.GroupDefinitionException;
import java.util.LinkedList;
import java.util.List;

/**
 * Defines the order to validate groups during validation.
 * with some inspiration from reference implementation
 *
 * @author Roman Stumm
 */
public class Groups {
    /** The list of single groups. */
    final List<Group> groups = new LinkedList<Group>();

    /** The list of sequences. */
    final List<List<Group>> sequences = new LinkedList<List<Group>>();

    /**
     * Get the Groups.
     * @return {@link List} of {@link Group}.
     */
    public List<Group> getGroups() {
        return groups;
    }

    /**
     * Get the Group sequences.
     * @return {@link List} of {@link List} of {@link Group}
     */
    public List<List<Group>> getSequences() {
        return sequences;
    }

    /**
     * Insert a {@link Group}.
     * @param group to insert
     */
    void insertGroup(Group group) {
        if (!groups.contains(group)) {
            groups.add(group);
        }
    }

    /**
     * Insert a sequence.
     * @param groups {@link List} of {@link Group} to insert
     */
    void insertSequence(List<Group> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }

        if (!sequences.contains(groups)) {
            sequences.add(groups);
        }
    }

    /**
     * Assert that the default group can be expanded to <code>defaultGroups</code>.
     * @param defaultGroups
     */
    public void assertDefaultGroupSequenceIsExpandable(List<Group> defaultGroups) {
        for (List<Group> groupList : sequences) {
            int idx = groupList.indexOf(Group.DEFAULT);
            if (idx != -1) {
                ensureExpandable(groupList, defaultGroups, idx);
            }
        }
    }

    private void ensureExpandable(List<Group> groupList, List<Group> defaultGroupList, int defaultGroupIndex) {
        for (int i = 0; i < defaultGroupList.size(); i++) {
            Group group = defaultGroupList.get(i);
            if (group.isDefault()) {
                continue; // the default group is the one we want to replace
            }
            int index = groupList.indexOf(group); // sequence contains group of default group sequence
            if (index == -1) {
                continue; // if group is not in the sequence
            }

            if ((i == 0 && index == defaultGroupIndex - 1)
                || (i == defaultGroupList.size() - 1 && index == defaultGroupIndex + 1)) {
                // if we are at the beginning or end of he defaultGroupSequence and the
                // matches are either directly before or after we can continue,
                // since we basically have two groups
                continue;
            }
            throw new GroupDefinitionException(
                "Unable to expand default group list" + defaultGroupList + " into sequence " + groupList);
        }
    }

}