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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.GlobalSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bunch of utility functions for files.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.6
 */
public final class FileUtils {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

  /**
   * The MIME properties mapping file extensions to MIME types.
   */
  private static final Properties MEDIATYPES = new Properties();

  /** Utility classes need no constructor. */
  private FileUtils() {
  }

  /**
   * Returns the Media Type for the given file.
   *
   * <p>The media type is only based on the file extension.
   *
   * <p>This method uses the the 'mime.properties' resource file from the classpath which maps
   * file extensions to the corresponding Media Type. This file is loaded once.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2046">MIME Part Two: Media Types</a>
   * @see <a href="http://tools.ietf.org/html/rfc3023">XML Media Types</a>
   *
   * @param f The file.
   * @return the corresponding Media Type or <code>null</code> if not found.
   */
  public static @Nullable String getMediaType(File f) {
    // Load if empty
    if (MEDIATYPES.isEmpty()) {
      loadMIMEProperties();
    }
    // Lookup extension in properties file
    String name = f.getName();
    int dot = name.lastIndexOf(".");
    if (dot >= 0) return MEDIATYPES.getProperty(name.substring(dot+1));
    else
      return null;
  }

  /**
   * Indicates whether the specified file is in the specified containing file.
   *
   * @param root the container
   * @param file the file to check.
   *
   * @return <code>true</code> if the file is within the specified container;
   *         <code>false</code> otherwise.
   */
  public static boolean contains(@Nullable File root, @Nullable File file) {
    if (root == null || file == null) return false;
    try {
      String prefix = root.getCanonicalPath();
      return file.getCanonicalPath().startsWith(prefix);
    } catch (IOException | SecurityException ex) {
      return false;
    }
  }

  /**
   * Returns the path from the root file to the specified file.
   *
   * <p>Note: implementation note, only works if the root contains the specified file.
   *
   * @param root the container
   * @param file the file to check.
   *
   * @return The path to the file from the root.
   */
  public static @Nullable String path(File root, File file) {
    try {
      String from = root.getCanonicalPath();
      String to = file.getCanonicalPath();
      if (to.startsWith(from)) {
        String path = to.substring(from.length()).replace("\\", "/");
        return path.startsWith("/")? path.substring(1) : path;
      } else
        throw new IllegalArgumentException("Cannot determine the path between the specified files.");
    } catch (IOException | SecurityException ex) {
      LOGGER.warn("Unable to compute path between {} and {}", root, file, ex);
      return null;
    }
  }

  // Private helpers ------------------------------------------------------------------------------

  /**
   * Loads the MIME properties from the Berlioz jar or the local "config/mime.properties"
   */
  private static synchronized void loadMIMEProperties() {
    try (InputStream in = getMediaTypesInputStream()) {
      if (in != null) {
        MEDIATYPES.load(in);
      }
    } catch (IOException ex) {
      LOGGER.warn("Unable to load MIME properties", ex);
    }
  }

  private static @Nullable InputStream getMediaTypesInputStream() throws FileNotFoundException {
    File file = new File(GlobalSettings.getWebInf(), "config/mime.properties");
    if (file.exists()) {
      LOGGER.info("Loading MIME properties from {}", file.getAbsolutePath());
      return new FileInputStream(file);
    } else {
      LOGGER.info("Loading MIME properties from Berlioz JAR {}", file.getAbsolutePath());
      return FileUtils.class.getResourceAsStream("/mime.properties");
    }
  }

}
