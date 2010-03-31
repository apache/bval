Apache BeanValidation
=====================

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
0. Sources require Java SE 5 or higher. (Tested with JDK 1.5.0_12 and 1.6.0_07)
1. The project is built with Apache Maven2 (2.0.9 or later). 
   You need to download and install Maven2 from: http://maven.apache.org/
2. Invoke maven2 from within the directory of the pom.xml file
   mvn

(optional) generate site, javadoc:
-----------------------
mvn site

(optional) generate source-jars:
---------------------
mvn source:jar
mvn source:test-jar

(optional) generate Eclipse projects:
-----------------------------
mvn eclipse:eclipse

(optional) generate an IntelliJ project:
-----------------------------
mvn idea:idea

(optional) deploy maven-site and javadoc:
------------------------------
[ Note: Only for committers]  
mvn site-deploy


Getting started
---------------
Refer to the project website at:
http://incubator.apache.org/beanvalidation/

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
  http://incubator.apache.org/beanvalidation/
  
