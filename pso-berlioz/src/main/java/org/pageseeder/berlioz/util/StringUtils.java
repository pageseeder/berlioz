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

import org.eclipse.jdt.annotation.Nullable;

/**
 * Few utility methods for string.
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.11.0
 */
public final class StringUtils {

  private StringUtils() {
    // Utility class
  }

  /**
   * @return true if the string is empty or null
   */
  public static boolean isBlank(@Nullable String value) {
    return value == null || value.trim().isEmpty();
  }

  /**
   * @return substring after the first occurrence of the delimiter
   * If the string does not contain the delimiter it returns the original string value
   */
  public static String substringAfter(@Nullable String value, @Nullable String delimiter) {
    String after = "";
    if (!isBlank(value) && delimiter != null) {
      if (delimiter.isEmpty()) {
        after = value;
      } else {
        String[] substring = value.split(delimiter, 2);
        after = substring.length > 1 ? substring[1] : value;
      }
    }
    return after;
  }

  /**
   * @return substring before the first occurrence of the delimiter
   */
  public static String substringBefore(@Nullable String value, @Nullable String delimiter) {
    String before = "";
    if (!isBlank(value) && delimiter != null) {
      if (delimiter.isEmpty()) {
        before = value;
      } else {
        String[] substring = value.split(delimiter, 2);
        before = substring[0];
      }
    }

    return before;
  }
}
