/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

/**
 * Provides a generic and uniform mechanism for the content generator to access parameters 
 * and attributes from a request. 
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 8 October 2009
 */
public final class ContentRequestHelper {

  /**
   * Prevents creation of instance. 
   */
  private ContentRequestHelper() {
    // empty constructor
  }

  /**
   * Returns the parameter called 'first-letter' from the request.
   * 
   * If param is not specified, returns "A"
   * 
   * @param req The content request
   * 
   * @return The value of the parameter, will never be <code>null</code>.
   */
  public static char getFirstLetterParameter(ContentRequest req) {
    String p = req.getParameter("first-letter");
    if (p == null) p = "A";
    return p.charAt(0);
  }

}
