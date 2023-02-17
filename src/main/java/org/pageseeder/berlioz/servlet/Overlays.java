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
package org.pageseeder.berlioz.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.util.Versions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple war or zip file which can be unpacked on top of the existing application.
 *
 * <p>A simple way to modularise aspect of the app.
 *
 * @author Christophe Lauret
 * @version Berlioz 0.12.3
 * @since Berlioz 0.9.26
 */
final class Overlays {

  private static final Logger LOGGER = LoggerFactory.getLogger(Overlays.class);

  private static final String[] ILLEGAL_OVERLAY_FILEPATHS = {
    "/WEB-INF/web.xml",
    "/WEB-INF/config/config.xml",
    "/WEB-INF/config/services.xml",
  };

  /**
   * Utility class.
   */
  private Overlays() {
  }

  /**
   * Look for overlays in the <code>WEB-INF/overlays/</code> directory.
   *
   * <p>They are returned in their natural order, that is by lexical name then by version.
   * This is to ensure that if there are multiple versions of the same overlay, the most
   * recent version will be processed last.
   *
   * @param root The application root (context)
   * @return The ordered list of overlays if any, never <code>null</code>
   */
  public static List<Overlay> list(final File root) {
    File webinfPath = new File(root, "WEB-INF");
    File overlays = new File(webinfPath, "overlays");
    if (overlays.exists() && overlays.isDirectory()) {
      File[] files = overlays.listFiles(f -> {
        String name = f.getName();
        return name.endsWith(".war") || name.endsWith(".zip") || name.endsWith(".jar");
      });
      List<Overlay> list = new ArrayList<>();
      if (files != null) {
        for (File f : files) {
          Overlay overlay = new Overlay(f);
          list.add(overlay);
        }
        Collections.sort(list);
      }
      return list;
    } else return Collections.emptyList();
  }

  /**
   * Overlay instance.
   *
   * @author Christophe Lauret
   * @version 16 December 2013
   */
  @Beta
  static final class Overlay implements Comparable<Overlay> {

    /**
     * The war or zip file.
     */
    private final File _source;

    /**
     * The name of the overlay
     */
    private final String _name;

    /**
     * The version of the overlay
     */
    private final String _version;

    /**
     * Create a new overlay.
     *
     * @param source The war or zip file.
     */
    private Overlay(File source) {
      this._source = source;
      String filename = source.getName();
      filename = filename.substring(0, filename.length() - 4); // always an extension
      int dash = filename.lastIndexOf('-');
      this._name = dash >= 0 ? filename.substring(0, dash) : filename;
      this._version = dash >= 0 ? filename.substring(dash+1) : "";
    }

    /**
     * @return the name
     */
    public String name() {
      return this._name;
    }

    /**
     * @return the version
     */
    public String version() {
      return this._version;
    }

    @Override
    public int compareTo(Overlay o) {
      int compare = this._name.compareTo(o._name);
      if (compare == 0) {
        compare = Versions.compare(this._version, o._version);
      }
      return compare;
    }

    /**
     * @return the source file
     */
    public File getSource() {
      return this._source;
    }

    /**
     * Unzip the file at the specified location.
     *
     * @param root The root of the web application (context path)
     *
     * @return the number of file that have been unpacked
     *
     * @throws IOException Should any error occur.
     */
    public int unpack(final File root) throws IOException {
      String rootPath = root.getCanonicalPath();
      int unpacked = 0;
      long modified = this._source.lastModified();
      try (ZipFile zip = new ZipFile(this._source)) {
        ZipEntry entry;
        for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
          entry = e.nextElement();
          String name = entry.getName();
          Path path = Paths.get(rootPath, name).normalize();
          boolean illegal = !path.startsWith(rootPath);
          for (String illegalPath : ILLEGAL_OVERLAY_FILEPATHS) {
            if (path.endsWith(illegalPath)) {
              illegal = true;
              break;
            }
          }
          if (illegal) {
            LOGGER.warn("Ignoring illegal entry: {}", name);
            continue;
          }

          // Ignore any file in the META-INF folder and any MacOS files
          if (name.startsWith("META-INF") || name.contains("__MACOSX") || name.endsWith(".DS_Store")) {
            continue;
          }
          // Ensure that the folder exists
          if (name.indexOf('/') > 0) {
            File dir = path.getParent().toFile();
            if (!dir.exists()) {
              boolean created = dir.mkdirs();
              if (!created) {
                LOGGER.warn("Unable to create parent folder of: {}", name);
              }
            }
          }
          // Only process files
          if (!entry.isDirectory()) {
            File f = path.toFile();
            if (!f.exists() || f.length() != entry.getSize() || f.lastModified() < modified) {
              try (BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry))) {
                Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
                unpacked++;
              }
            }
          }
        }
      }
      return unpacked;
    }

    @Override
    public String toString() {
      return this._name+"["+this._version+"]";
    }
  }

}
