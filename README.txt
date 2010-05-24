 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.


Apache Bean Validation (incubating)
(C) Copyright 2010 The Apache Software Foundation.
--------------------------------------------------------------------------------

JSR 303 Bean Validation 1.0 Implementation
==========================================
This is an implementation of JSR 303 (Bean Validation), a specification of the
Java API for Javabean validation in Java EE and Java SE.
The technical objective is to provide a class level constraint declaration and
validation facility for the Java application developer, as well as a constraint
metadata repository and query API.

This implementation is based on the validation framework of agimatec GmbH,
which was contributed to the ASF under a software grant.

How to compile the project
==========================
Requirements:
0. Sources require Java SE 5 or higher. (Tested with JDK 1.5.0_22 and 1.6.0_20)
1. The project is built with maven2 (Tested with 2.0.10 and 2.2.1). 
   You need to download and install maven2 from: http://maven.apache.org/
2. Invoke maven in the root directory or a module subdirectory.

compile all projects:
---------------------
mvn install

(artifacts are generated into the target directories and your local .m2 repo)

(Optional) generate site, javadoc:
----------------------------------
mvn site

(Optional) generate source-jars:
--------------------------------
mvn source:jar
mvn source:test-jar

(Optional) generate an IntelliJ project:
----------------------------------------
mvn idea:idea

(Optional) generate Eclipse projects:
-------------------------------------
mvn eclipse:eclipse

(Committers) deploy maven-site and javadoc:
-------------------------------------------
mvn site-deploy

(Committers) Publish SNAPSHOT or Release artifacts:
---------------------------------------------------
mvn clean deploy



Getting started
===============
Refer to the project page and WIKI at:
https://cwiki.apache.org/BeanValidation/

You can checkout latest sources and releases from there.
You can also refer to the test cases in src/test/java/** for examples.

Project status
==============
* The BeanValidation project is currently hosted in the Apache Incubator.
  Please visit the following for our latest graduation status:
  http://incubator.apache.org/projects/beanvalidation.html


Feedback, questions, contribution
=================================
** Your feedback is welcome! **

Checkout our website for more details on how to acess our mailing lists
or open issues in JIRA:
  http://incubator.apache.org/bval/

