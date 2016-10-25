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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.bval.jsr.example.Address;
import org.apache.bval.jsr.example.First;
import org.apache.bval.jsr.example.Last;
import org.apache.bval.jsr.example.Second;

import javax.validation.GroupDefinitionException;
import javax.validation.ValidationException;
import javax.validation.groups.Default;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * GroupListComputer Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>04/09/2009</pre>
 */
public class GroupsComputerTest extends TestCase {
    GroupsComputer groupsComputer;

    public GroupsComputerTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        groupsComputer = new GroupsComputer();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(GroupsComputerTest.class);
    }

    public void testComputeGroupsNotAnInterface() {
        Set<Class<?>> groups = new HashSet<Class<?>>();
        groups.add(String.class);
        try {
            groupsComputer.computeGroups(groups);
            fail();
        } catch (ValidationException ex) {

        }
    }

    public void testGroupChainForNull() {
        try {
            groupsComputer.computeGroups((Class<?>[]) null);
            fail();
        } catch (IllegalArgumentException ex) {

        }
    }

    public void testGroupChainForEmptySet() {
        try {
            groupsComputer.computeGroups(new HashSet<Class<?>>());
            fail();
        } catch (IllegalArgumentException ex) {

        }
    }

    public void testCyclicGroupSequences() {
        try {
            Set<Class<?>> groups = new HashSet<Class<?>>();
            groups.add(CyclicGroupSequence1.class);
            groupsComputer.computeGroups(groups);
            fail();
        } catch (GroupDefinitionException ex) {

        }
    }

    public void testCyclicGroupSequence() {
        try {
            Set<Class<?>> groups = new HashSet<Class<?>>();
            groups.add(CyclicGroupSequence.class);
            groupsComputer.computeGroups(groups);
            fail();
        } catch (GroupDefinitionException ex) {

        }
    }

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
