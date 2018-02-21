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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.GroupDefinitionException;
import javax.validation.ValidationException;
import javax.validation.groups.Default;

import org.apache.bval.jsr.example.Address;
import org.apache.bval.jsr.example.First;
import org.apache.bval.jsr.example.Last;
import org.apache.bval.jsr.example.Second;
import org.junit.Before;
import org.junit.Test;

/**
 * GroupListComputer Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>04/09/2009</pre>
 */
public class GroupsComputerTest {
    GroupsComputer groupsComputer;

    @Before
    public void setUp() throws Exception {
        groupsComputer = new GroupsComputer();
    }

    @Test(expected = ValidationException.class)
    public void testComputeGroupsNotAnInterface() {
        groupsComputer.computeGroups(Collections.singleton(String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGroupChainForNull() {
            groupsComputer.computeGroups((Class<?>[]) null);
    }

    @Test
    public void testGroupChainForEmptySet() {
        assertEquals(Collections.singletonList(Group.DEFAULT),
            groupsComputer.computeGroups(new HashSet<Class<?>>()).getGroups());
    }

    @Test(expected = GroupDefinitionException.class)
    public void testCyclicGroupSequences() {
        groupsComputer.computeGroups(Collections.singleton(CyclicGroupSequence1.class));
    }

    @Test(expected = GroupDefinitionException.class)
    public void testCyclicGroupSequence() {
        groupsComputer.computeGroups(Collections.singleton(CyclicGroupSequence.class));
    }

    @Test
    public void testGroupDuplicates() {
        Set<Class<?>> groups = new HashSet<Class<?>>();
        groups.add(First.class);
        groups.add(Second.class);
        groups.add(Last.class);
        Groups chain = groupsComputer.computeGroups(groups);
        assertEquals(3, chain.groups.size());

        groups.clear();
        groups.add(First.class);
        groups.add(First.class);
        chain = groupsComputer.computeGroups(groups);
        assertEquals(1, chain.groups.size());

        groups.clear();
        groups.add(First.class);
        groups.add(Last.class);
        groups.add(First.class);
        chain = groupsComputer.computeGroups(groups);
        assertEquals(2, chain.groups.size());
    }

    @Test
    public void testSequenceResolution() {
        Set<Class<?>> groups = new HashSet<Class<?>>();
        groups.add(Address.Complete.class);
        Groups chain = groupsComputer.computeGroups(groups);
        Iterator<List<Group>> sequences = chain.getSequences().iterator();
        List<Group> sequence = sequences.next();

        assertEquals(Default.class, sequence.get(0).getGroup());
        assertEquals(Address.HighLevelCoherence.class, sequence.get(1).getGroup());
    }
}
