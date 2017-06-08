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
package org.pageseeder.berlioz.xml;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Define a filename filter for XML file.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
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
