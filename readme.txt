agimatec-validation
===================

How to compile the project
==========================
Requirements:
0. Sources require java1.5 or higher. (Tested with JDK 1.5.0_12 and 1.6.0_07)
1. The project is built with maven2 (Tested with 2.0.9, 2.0.10 and 2.2.0). 
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

(Optional) deploy maven-site and javadoc:
-----------------------------------------
mvn site-deploy

[ Note:
  You must set the properties ${agimatec-site-id} and ${agimatec-site-url} to
  adequate values. You can do that by adding them to your maven settings.xml.
  This is the place where the server credenticals for uploads are kept. ]
 
(Optional) Publish SNAPSHOT or Release artifacts
------------------------------------------------
mvn clean deploy -Prelease

[ Note:
  You will need to add the following information to your .m2/settings.xml
  so the release plugin can log into the https://oss.sonatype.org/ site
  to deploy the artifacts -
        <server>
            <id>agimatec-snapshots</id>
            <username>${ossrh-uid}</username>
            <password>${ossrh-pwd}</password>
        </server>
        <server>
            <id>agimatec-releases</id>
            <username>${ossrh-uid}</username>
            <password>${ossrh-pwd}</password>
        </server>
  After deploying, log into https://oss.sonatype.org/, select Staging, right-
  click on the new com.agimatec-### staging repo in the bottom pane and 
  select Close, then right-click and choose Promote to Agimatec Releases.


Getting started
---------------
Refer to the project page and WIKI at:
http://code.google.com/p/agimatec-validation

You can checkout latest sources and releases from there.
You can also refer to the test cases in src/test/java/** for examples.


Feedback, questions, contribution
=================================
** Feedback is welcome! **

http://code.google.com/p/agimatec-validation
http://groups.google.com/group/agimatec-validation
http://www.agimatec.de

Roman Stumm, agimatec GmbH, 2008, 2009
email: roman.stumm@agimatec.de
