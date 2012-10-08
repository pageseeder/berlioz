/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

/**
 * An abstract implementation of the content generator interface.
 *
 * <p>This class can be used as a base class for content generator implementations.
 *
 * @deprecated This base class is no longer required and will be removed
 *
 * @author Christophe Lauret (Weborganic)
 * @version 26 November 2009
 */
@Deprecated
public abstract class ContentGeneratorBase implements ContentGenerator {

  /**
   * The path info of this content generator.
   */
  private String _pathInfo;

// methods  ------------------------------------------------------------------------------------

  /**
   * Returns the sub path info of this generator.
   * 
   * @param req The content request
   * @return pathInfo The path info
   * 
   * @deprecated Not useful now that we use URI Patterns.
   */
  @Deprecated public final String getSubPath(ContentRequest req) {
    String pathInfo = req.getPathInfo();
    int wildcard = this._pathInfo.indexOf('*');
    if (wildcard >= 0 && pathInfo.startsWith(this._pathInfo.substring(0, wildcard)))
      return pathInfo.substring(wildcard);
    else return "";
  }

  /**
   * Sets the path info of this generator.
   * 
   * @param pathInfo The path info
   */
  public final void setPathInfo(String pathInfo) {
    this._pathInfo = pathInfo;
  }

  /**
   * Returns the path info of this generator.
   * 
   * @return pathInfo The path info
   */
  public final String getPathInfo() {
    return this._pathInfo;
  }

}
