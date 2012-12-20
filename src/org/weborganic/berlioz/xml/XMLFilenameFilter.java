/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.xml;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Define a filename filter for XML file.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.6.0 - 20 August 2004
 * @since Berlioz 0.6
 */
public final class XMLFilenameFilter implements FilenameFilter {

  /**
   * Accepts only file which extension is XML (case insensitive).
   *
   * @param dir  The directory containing the file.
   * @param name The name of the file.
   *
   * @return <code>true</code> if xml file; <code>false</code> otherwise.
   *
   * @see FilenameFilter#accept(java.io.File, java.lang.String)
   */
  @Override
  public boolean accept(File dir, String name) {
    int dot = name.lastIndexOf(".");
    if (dot == -1)
      return false;
    else
      return ".xml".equalsIgnoreCase(name.substring(dot));
  }

}
