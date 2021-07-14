/*
 * Copyright 2021 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.util;

/**
 * Few utility methods for string.
 * @author Carlos Cabral
 * @since 21 November 2019
 */
public class StringUtils {
  /**
   *
   * @param value
   * @return true if the string is empty or null
   */
  public static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  /**
   *
   * @param value
   * @param delimiter
   * @return substring after the first occurrence of the delimiter
   * If the string does not contain the delimiter it returns the original string value
   */
  public static String substringAfter (String value, String delimiter) {
    String after = "";
    if (!isBlank(value) && delimiter != null) {
      if (delimiter.length() == 0) {
        after = value;
      } else {
        String[] substring = value.split(delimiter, 2);
        after = substring.length > 1 ? substring[1] : value;
      }
    }
    return after;
  }

  /**
   *
   * @param value
   * @param delimiter
   * @return substring before the first occurrence of the delimiter
   */
  public static String substringBefore (String value, String delimiter) {
    String before = "";
    if (!isBlank(value) && delimiter != null) {
      if (delimiter.length() == 0) {
        before = value;
      } else {
        String[] substring = value.split(delimiter, 2);
        before = substring[0];
      }
    }

    return before;
  }
}
