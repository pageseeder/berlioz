package org.weborganic.berlioz.test;

import java.io.File;

/**
 * A utility class for testing Berlioz.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 26 November 2009
 */
public class TestUtils {

  /**
   * Utility class.
   */
  private TestUtils() {
  }

  /**
   * Returns the data directory for the class being tested.
   * 
   * @param c The class being tested.
   * @return the corresponding data directory.
   */
  public static File getDataDirectory(Class<?> c) {
    File data = new File("data");
    String dir = c.getName().replace('.', File.separatorChar);
    return new File(data, dir);
  }

}
