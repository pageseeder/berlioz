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
import java.util.Objects;
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

  private static final int MAX_ENTRIES = 10_000;
  private static final long MAX_ENTRY_SIZE = 100L * 1024 * 1024;  // 100 MB per entry
  private static final long MAX_TOTAL_SIZE = 512L * 1024 * 1024;  // 512 MB total
  private static final int MAX_COMPRESSION_RATIO = 100;

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
    private final File source;

    /**
     * The name of the overlay
     */
    private final String name;

    /**
     * The version of the overlay
     */
    private final String version;

    /**
     * Create a new overlay.
     *
     * @param source The war or zip file.
     */
    private Overlay(File source) {
      this.source = source;
      String filename = source.getName();
      filename = filename.substring(0, filename.length() - 4); // always an extension
      int dash = filename.lastIndexOf('-');
      this.name = dash >= 0 ? filename.substring(0, dash) : filename;
      this.version = dash >= 0 ? filename.substring(dash+1) : "";
    }

    /**
     * @return the name
     */
    public String name() {
      return this.name;
    }

    /**
     * @return the version
     */
    public String version() {
      return this.version;
    }

    @Override
    public int compareTo(Overlay o) {
      int compare = this.name.compareTo(o.name);
      if (compare == 0) {
        compare = Versions.compare(this.version, o.version);
      }
      return compare;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Overlay)) return false;
      Overlay other = (Overlay) o;
      return this.name.equals(other.name) && this.version.equals(other.version);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.name, this.version);
    }

    /**
     * @return the source file
     */
    public File getSource() {
      return this.source;
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
      int entries = 0;
      long totalSize = 0;
      long modified = this.source.lastModified();
      try (ZipFile zip = new ZipFile(this.source)) {
        for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
          ZipEntry entry = e.nextElement();
          if (++entries > MAX_ENTRIES)
            throw new IOException("Overlay '" + this.source.getName() + "' exceeds maximum entry count (" + MAX_ENTRIES + ")");
          String entryName = entry.getName();
          Path path = Paths.get(rootPath, entryName).normalize();
          if (isIllegal(path, rootPath)) {
            LOGGER.warn("Ignoring illegal entry: {}", entryName);
          } else if (!shouldSkip(entryName)) {
            ensureParentExists(path, entryName);
            if (!entry.isDirectory()) {
              totalSize = accumulateSize(entry, entryName, totalSize);
              unpacked += copyIfNeeded(zip, entry, path, modified);
            }
          }
        }
      }
      return unpacked;
    }

    private static boolean isIllegal(Path path, String rootPath) {
      if (!path.startsWith(rootPath)) return true;
      for (String illegalPath : ILLEGAL_OVERLAY_FILEPATHS) {
        if (path.endsWith(illegalPath.substring(1))) return true;
      }
      return false;
    }

    private static boolean shouldSkip(String entryName) {
      return entryName.startsWith("META-INF") || entryName.contains("__MACOSX") || entryName.endsWith(".DS_Store");
    }

    private static void ensureParentExists(Path path, String entryName) {
      if (entryName.contains("/")) {
        File dir = path.getParent().toFile();
        if (!dir.exists() && !dir.mkdirs()) {
          LOGGER.warn("Unable to create parent folder of: {}", entryName);
        }
      }
    }

    private long accumulateSize(ZipEntry entry, String entryName, long totalSize) throws IOException {
      long size = entry.getSize();
      long compressedSize = entry.getCompressedSize();
      if (size >= 0 && compressedSize > 0 && size / compressedSize > MAX_COMPRESSION_RATIO)
        throw new IOException("Overlay entry '" + entryName + "' has a suspicious compression ratio");
      if (size > MAX_ENTRY_SIZE)
        throw new IOException("Overlay entry '" + entryName + "' exceeds maximum entry size (" + MAX_ENTRY_SIZE + " bytes)");
      if (size <= 0) return totalSize;
      long newTotal = totalSize + size;
      if (newTotal > MAX_TOTAL_SIZE)
        throw new IOException("Overlay '" + this.source.getName() + "' exceeds maximum total size (" + MAX_TOTAL_SIZE + " bytes)");
      return newTotal;
    }

    private static int copyIfNeeded(ZipFile zip, ZipEntry entry, Path path, long modified) throws IOException {
      File f = path.toFile();
      if (f.exists() && f.length() == entry.getSize() && f.lastModified() >= modified) return 0;
      try (BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry))) {
        Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
      }
      return 1;
    }

    @Override
    public String toString() {
      return this.name +"["+this.version +"]";
    }
  }

}
