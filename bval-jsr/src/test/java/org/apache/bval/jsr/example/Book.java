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


import org.apache.bval.constraints.NotEmpty;

import javax.validation.GroupSequence;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@GroupSequence({First.class, Second.class, Book.class, Last.class})
public class Book {
    @NotNull(groups = First.class)
    @NotEmpty(groups = First.class)
    private String title;

    @Size(max = 30, groups = Second.class)
    private String subtitle;

    @Valid
    @NotNull(groups = First.class)
    private Author author;

    @NotNull
    private int uselessField;

    private int unconstraintField;

    public int getUnconstraintField() {
        return unconstraintField;
    }

    public void setUnconstraintField(int unconstraintField) {
        this.unconstraintField = unconstraintField;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    @GroupSequence(value = {First.class, Second.class, Last.class})
    public interface All {
    }
}