/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.bval.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.bval.util.reflection.Reflection;
import org.apache.commons.weaver.privilizer.Privilizing;
import org.apache.commons.weaver.privilizer.Privilizing.CallTo;

/**
 * This class contains version information for BVal.
 * It uses Ant's filter tokens to convert the template into a java
 * file with current information.
 */
@Privilizing(@CallTo(Reflection.class))
public class BValVersion {

    /** Project name */
    public static final String PROJECT_NAME = "Apache BVal";
    /** Unique id of the current project/version/revision */
    public static final String PROJECT_ID;
    /** Version number */
    public static final String VERSION_NUMBER;
    /** Major release number */
    public static final int MAJOR_RELEASE;
    /** Minor release number */
    public static final int MINOR_RELEASE;
    /** Patch/point release number */
    public static final int PATCH_RELEASE;
    /** Release status */
    public static final String RELEASE_STATUS;
    /** Version control revision number */
    public static final String REVISION_NUMBER;

    static {
        Properties revisionProps = new Properties();
        try {
            InputStream in = BValVersion.class.getResourceAsStream("/META-INF/org.apache.bval.revision.properties");
            if (in != null) {
                try {
                    revisionProps.load(in);
                } finally {
                    in.close();
                }
            }
        } catch (IOException ioe) {
        }

        String vers = revisionProps.getProperty("project.version");
        if (vers == null || "".equals(vers.trim()))
            vers = "0.0.0";
        VERSION_NUMBER = vers;

        StringTokenizer tok = new StringTokenizer(VERSION_NUMBER, ".-");
        int major, minor, patch;
        try {
            major = tok.hasMoreTokens() ? Integer.parseInt(tok.nextToken()) : 0;
        } catch (Exception e) {
            major = 0;
        }

        try {
            minor = tok.hasMoreTokens() ? Integer.parseInt(tok.nextToken()) : 0;
        } catch (Exception e) {
            minor = 0;
        }

        try {
            patch = tok.hasMoreTokens() ? Integer.parseInt(tok.nextToken()) : 0;
        } catch (Exception e) {
            patch = 0;
        }

        String revision = revisionProps.getProperty("svn.revision");
        if (StringUtils.isBlank(revision)) {
            revision = "unknown";
        } else {
            tok = new StringTokenizer(revision, ":");
            String strTok = null;
            while (tok.hasMoreTokens()) {
                try {
                    strTok = tok.nextToken();
                } catch (Exception e) {
                }
            }
            if (strTok != null) {
                revision = strTok;
            }
        }

        MAJOR_RELEASE = major;
        MINOR_RELEASE = minor;
        PATCH_RELEASE = patch;
        RELEASE_STATUS = tok.hasMoreTokens() ? tok.nextToken("!") : "";
        REVISION_NUMBER = revision;
        PROJECT_ID = PROJECT_NAME + " " + VERSION_NUMBER + "-r" + REVISION_NUMBER;
    }

    /**
     * Get the project version number.
     * @return String
     */
    public static String getVersion() {
        return VERSION_NUMBER;
    }

    /**
     * Get the version control revision number.
     * @return String
     */
    public static String getRevision() {
        return REVISION_NUMBER;
    }

    /**
     * Get the project name.
     * @return String
     */
    public static String getName() {
        return PROJECT_NAME;
    }

    /**
     * Get the fully-qualified project id.
     * @return String
     */
    public static String getID() {
        return PROJECT_ID;
    }

    /**
     * Main method of this class that prints the {@link #toString()} to <code>System.out</code>.
     * @param args ignored
     */
    public static void main(String[] args) {
        System.out.println(new BValVersion().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder(80 * 40);
        appendBanner(buf);
        buf.append("\n");

        appendProperty("os.name", buf).append("\n");
        appendProperty("os.version", buf).append("\n");
        appendProperty("os.arch", buf).append("\n\n");

        appendProperty("java.version", buf).append("\n");
        appendProperty("java.vendor", buf).append("\n\n");

        buf.append("java.class.path:\n");
        final StringTokenizer tok = new StringTokenizer(Reflection.getProperty("java.class.path"));
        while (tok.hasMoreTokens()) {
            buf.append("\t").append(tok.nextToken());
            buf.append("\n");
        }
        buf.append("\n");

        appendProperty("user.dir", buf).append("\n");
        return buf.toString();
    }

    private void appendBanner(StringBuilder buf) {
        buf.append("Project").append(": ").append(getName());
        buf.append("\n");
        buf.append("Version").append(": ").append(getVersion());
        buf.append("\n");
        buf.append("Revision").append(": ").append(getRevision());
        buf.append("\n");
    }

    private StringBuilder appendProperty(String prop, StringBuilder buf) {
        return buf.append(prop).append(": ").append(Reflection.getProperty(prop));
    }
}
