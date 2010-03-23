/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Used to generated a redirect instruction. 
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 8 October 2009
 */
final class ContentRedirect {

  /**
   * The path information to the redirected location.
   */
  private final String pathInfo;

  /**
   * A collection of parameters names required for the redirect.
   */
  private Collection<String> parameters;

  /**
   * Create a new content redirect object.
   * 
   * @param pathInfo The path information of the redirected URL.
   */
  public ContentRedirect(String pathInfo) {
    this.pathInfo = pathInfo;
  }

  /**
   * Returns the path information of the redirected URL.
   * 
   * @return the path information of the redirected URL.
   */
  public String getPathInfo() {
    return this.pathInfo;
  }

  /**
   * Adds a parameters to this redirect object.
   * 
   * @param name The name of the parameter.
   */
  public void addParameter(String name) {
    if (this.parameters == null) this.parameters = new ArrayList<String>();
    this.parameters.add(name);
  }

  /**
   * Adds a parameters to this redirect object.
   * 
   * @return An iterator over the parameter names.
   */
  public Iterator<String> getParameterNames() {
    if (this.parameters == null)
      return Collections.EMPTY_LIST.iterator();
    else
      return this.parameters.iterator();
  }

}
