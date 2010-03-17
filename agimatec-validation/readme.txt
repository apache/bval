agimatec-validation
===================
validation and metadata framework by agimatec GmbH

How to compile the project
==========================
Requirements:
0. Sources require java1.5 or higher. (Tested with JDK 1.5.0_12 and 1.6.0_07)
1. The project is built with maven2 (2.0.9). 
   You need to download and install maven2 from: http://maven.apache.org/
2. Invoke maven2 from within the directory of the pom.xml file

compile agimatec-validation project:
------------------------------------
mvn install

(artifacts are generated into the target directory)

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
* You can use the APIs of agimatec-validation:
  refer to classes MetaBeanManagerFactory and BeanValidator 

Feedback, questions, contribution
=================================
** Feedback is welcome! **

http://code.google.com/p/agimatec-validation
http://www.agimatec.de

Roman Stumm, agimatec GmbH, 2008, 2009
email: roman.stumm@agimatec.de
