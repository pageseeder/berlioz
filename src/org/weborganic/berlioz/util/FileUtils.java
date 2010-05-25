package org.weborganic.berlioz.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.logging.ZLogger;
import org.weborganic.berlioz.logging.ZLoggerFactory;

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
  private static final ZLogger LOGGER = ZLoggerFactory.getLogger(FileUtils.class);

  /**
   * The MIME properties mapping file extensions to MIME types. 
   */
  private static final Properties MIME = new Properties(); 

  /** Utility classes need no constructor. */
  private FileUtils() {}

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

  // Private helpers ------------------------------------------------------------------------------

  /**
   * Loads the properties.
   * 
   * @return Properties. Always.
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
