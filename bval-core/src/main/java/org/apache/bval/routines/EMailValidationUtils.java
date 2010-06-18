/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval.routines;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description: holds the regexp to validate an email address<br>
 * User: roman.stumm<br>
 * Date: 17.06.2010<br>
 * Time: 10:40:59<br>
 */
public class EMailValidationUtils {
  private static String ATOM =
      "[^\\x00-\\x1F^\\(^\\)^\\<^\\>^\\@^\\,^\\;^\\:^\\\\^\\\"^\\.^\\[^\\]^\\s]";
  private static String DOMAIN = "(" + ATOM + "+(\\." + ATOM + "+)*";
  private static String IP_DOMAIN = "\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\]";
  public static final java.util.regex.Pattern DEFAULT_EMAIL_PATTERN;

  static {
    DEFAULT_EMAIL_PATTERN = java.util.regex.Pattern.compile(
        "^" + ATOM + "+(\\." + ATOM + "+)*@"
            + DOMAIN
            + "|"
            + IP_DOMAIN
            + ")$",
        java.util.regex.Pattern.CASE_INSENSITIVE
    );
  }

  public static boolean isValid(Object value) {
    return isValid(value, DEFAULT_EMAIL_PATTERN);
  }

  public static boolean isValid(Object value, Pattern aPattern) {
    if (value == null) return true;
    if (!(value instanceof String)) return false;
    String string = (String) value;
    if (string.length() == 0) return true;
    Matcher m = aPattern.matcher(string);
    return m.matches();
  }

}
