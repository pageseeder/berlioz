/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.GlobalSettings;

/**
 * A bunch of utility functions for files.
 * 
 * @author Christophe Lauret
 * @version 25 May 2010
 */
public final class FileUtils {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

  /**
   * The MIME properties mapping file extensions to MIME types. 
   */
  private static final Properties MIME = new Properties(); 

  /** Utility classes need no constructor. */
  private FileUtils() {
  }

  /**
   * Returns the MIME type for the given file.
   * 
   * <p>The MIME type is only based on the file extension.
   * 
   * <p>This method uses the the 'mime.properties' resource file from the classpath which maps
   * file extensions to MIME type. This file is loaded once. 
   * 
   * @param f The file.
   * @return the corresponding MIME type or <code>null</code> if not found.
   */
  public static String getMIMEType(File f) {
    // Load if empty
    if (MIME.isEmpty()) loadMIMEProperties();
    // Lookup extension in properties file
    String name = f.getName();
    int dot = name.lastIndexOf(".");
    if (dot >= 0) {
      return MIME.getProperty(name.substring(dot+1));
    } else {
      return null;
    }
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
  public static boolean contains(File root, File file) {
    if (root == null || file == null) return false;
    try {
      String prefix = root.getCanonicalPath();
      return file.getCanonicalPath().startsWith(prefix);
    } catch (IOException ex) {
      return false;
    } catch (SecurityException ex) {
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
  public static String path(File root, File file) {
    if (root == null || file == null)
      throw new NullPointerException("Cannot determine the path between the specified files.");
    try {
      String from = root.getCanonicalPath();
      String to = file.getCanonicalPath();
      if (to.startsWith(from)) {
        String path = to.substring(from.length()).replace("\\", "/");
        return path.startsWith("/")? path.substring(1) : path;
      } else {
        throw new IllegalArgumentException("Cannot determine the path between the specified files.");
      }
    } catch (IOException ex) {
      // TODO handle exception
      return null;
    } catch (SecurityException ex) {
      // TODO handle exception
      return null;
    }
  }

  // Private helpers ------------------------------------------------------------------------------

  /**
   * Loads the properties.
   */
  private static synchronized void loadMIMEProperties() {
    File file = new File(GlobalSettings.getRepository(), "config/mime.properties");
    try {
      LOGGER.info("Loading MIME properties from {}", file.getAbsolutePath());
      Class<FileUtils> c = FileUtils.class;
      InputStream in = c.getResourceAsStream("/mime.properties");
      MIME.load(in);
      in.close();
    } catch (IOException ex) {
      LOGGER.warn("Unable to read conf properties for template", ex);
    }
  }

}
