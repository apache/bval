agimatec-jsr303
===============

JSR 303: bean-validation by agimatec GmbH
==========================================
This is an implementation of JSR 303 (Bean Validation), a specification of the Java
API for Javabean validation in Java EE and Java SE.
The technical objective is to provide a class level constraint declaration and validation
facility for the Java application developer, as well as a constraint metadata repository
and query API.
This implementation is based on the validation framework of agimatec GmbH,
that is in production since 2007 and offers additional features,
like XML-based extensible metadata, code generation (JSON for AJAX applications),
JSR303 annotation support.

How to compile the project
==========================
Requirements:
0. Sources require java1.5 or higher. (Tested with JDK 1.5.0_12 and 1.6.0_07)
1. The project is built with maven2 (2.0.9). 
   You need to download and install maven2 from: http://maven.apache.org/
2. Invoke maven2 from within the directory of the pom.xml file

When building the project from source, you need the compatible version validation-api.jar:

Check out the reference implementation and build it first:
svn checkout http://anonsvn.jboss.org/repos/hibernate/beanvalidation/trunk/validation-api validation-api
cd validation-api
mvn clean install

[As long as there is no public maven repository to get the artifact of validation-api from.]

compile agimatec-jsr303 project:
------------------------------------
mvn install
(artifacts are generated into the target directory)

compile with alternative dependencies (geronimo):
-------------------------------------------------
compile agimatec-jsr303 using geronimo artifacts for validation-api
instead of reference implementation of validation-api:

mvn install -Dagimatec-on-geronimo

(optional) generate site, javadoc:
-----------------------
mvn site

(optional) generate source-jars:
---------------------
mvn source:jar
mvn source:test-jar

(optional) generate an IntelliJ project:
-----------------------------
mvn idea:idea

(optional) deploy maven-site and javadoc:
------------------------------
[ Note:
  You must set the properties ${agimatec-site-id} and ${agimatec-site-url} to
  adequate values. You can do that by adding them to your maven settings.xml. This is the place
  where the server credenticals for uploads are kept. ]
 
mvn site-deploy

Getting started
---------------
Refer to the project page and WIKI at:
http://code.google.com/p/agimatec-validation

You can checkout latest sources and releases from there.
You can also refer to the test cases in src/test/java/** for examples.

Project status
==============
* The agimatec-validation framework is older than the JSR 303 specification, but the similarities
  were striking so that the adaption to the standard was possible withing a short time.
  There are still things to be done...

* The specification is in beta state and subject to change.

* Please verify that the version of validation-api.jar you are using
  is compatible with agimatec-validation.jar

* You can use the framework with the JSR303 interfaces if you want to strictly use the standard.
  Refer to classes in packages javax.validation and com.agimatec.validation.jsr303
  
  or you can access the propriatary APIs of agimatec-validation for additional features.
  Refer to classes MetaBeanManagerFactory and BeanValidator.

Feedback, questions, contribution
=================================
** Your feedback is welcome! **

http://code.google.com/p/agimatec-validation
http://www.agimatec.de

Roman Stumm, agimatec GmbH, 2008, 2009, 2010
email: roman.stumm@agimatec.de
