package org.weborganic.berlioz.document;

import java.io.File;

/**
 * Provides utility functions to objects in this package.
 * 
 *  
 * @author Christophe Lauret (Allette Systems)
 * @version 16 August 2007
 */
public final class DocumentUtils {

  /**
   * Prevents creation of instances.
   */
  private DocumentUtils() {
  }

  /**
   * Produces the title from the specified file.
   *
   * @see #toTitle(String)
   *
   * @param file The file.
   * 
   * @return The corresponding title.
   */
  public static String toTitle(File file) {
    if (file == null) return null;
    return toTitle(file.getName());
  }

  /**
   * Produces the title from the specified file name.
   * 
   * Returns <code>null</code> if the file name is <code>null</code>.
   *
   * The title is produced by:
   * <ul>
   *   <li>Removing the file extension;</li>
   *   <li>If exist remove the date</li>
   *   <li>Converting non-ASCII characters to spaces;</li>
   *   <li>Capitalising words.</li>
   * </ul>
   *
   * For example:
   *  <var>my_file-12.xml</var> becomes "My File 12".
   *
   * @param filename The file name.
   * 
   * @return The corresponding title.
   */
  public static String toTitle(String filename) {
    if (filename == null) return null;
    // remove date
    String dateRemoved = filename.replaceAll("(19|20)\\d\\d[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])[-]", "");
    
    // remove file extension
    String extRemoved = null;
    if (dateRemoved.lastIndexOf(".") > -1) {
      extRemoved = dateRemoved.substring(0, dateRemoved.lastIndexOf("."));
    } else {
      extRemoved = dateRemoved;
    }
    // remove non alphabet/numbers characters
    String miscRemoved = extRemoved.replaceAll("[^A-Za-z0-9]", " ");
    
    // capitalising words
    String[] splitWords = miscRemoved.split(" ");
    StringBuffer capitalised = new StringBuffer();
    for (int i = 0; i < splitWords.length; i++) {
      String word = splitWords[i];
      capitalised.append(word.substring(0, 1).toUpperCase());
      capitalised.append(word.substring(1));
      if (i < (splitWords.length - 1)) capitalised.append(" ");
    }
    return capitalised.toString();
  }
}
