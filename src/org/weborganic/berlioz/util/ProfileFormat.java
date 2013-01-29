/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.weborganic.berlioz.Beta;

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
  private static NumberFormat format = new DecimalFormat("#,##0.00");

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
