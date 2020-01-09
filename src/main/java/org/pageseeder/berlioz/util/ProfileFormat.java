/*
 * Copyright 2015 Allette Systems (Australia)
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

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.pageseeder.berlioz.Beta;

/**
 * A format for the profiling results.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.14 - 22 January 2013
 * @since Berlioz 0.9.14
 */
@Beta
public final class ProfileFormat {

  /**
   * The format for the nano time.
   */
  private static final NumberFormat format = new DecimalFormat("#,##0.00");

  /** Utility class */
  private ProfileFormat() {
  }

  /**
   * Formats the time specified in nano seconds as milliseconds with 2 decimals.
   *
   * @param nanotime the time in nano seconds.
   * @return the time formatted in milliseconds with 2 decimals.
   */
  public static synchronized String format(long nanotime) {
    return format.format(nanotime * 0.000001);
  }

}
